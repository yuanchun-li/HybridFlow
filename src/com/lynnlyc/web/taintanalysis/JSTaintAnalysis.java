/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.lynnlyc.web.taintanalysis;

import java.util.*;

import com.ibm.wala.cast.ir.ssa.AstLexicalAccess;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorKillAll;
import com.ibm.wala.dataflow.graph.BitVectorKillGen;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.ExplodedInterproceduralCFG;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.*;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Computes interprocedural reaching definitions for static fields in a context-insensitive manner.
 */
public class JSTaintAnalysis {

    /**
     * the exploded interprocedural control-flow graph on which to compute the analysis
     */
    private final ExplodedInterproceduralCFG icfg;

    /**
     * maps call graph node and instruction index of putstatic instructions to more compact numbering for bitvectors
     */
    private final OrdinalSetMapping<Pair<CGNode, Integer>> instrNumbering;

    /**
     * for resolving field references in putstatic instructions
     */
    private final IClassHierarchy cha;

    /**
     * maps each static field to the numbers of the statements (in {@link #instrNumbering}) that define it; used for kills in flow
     * functions
     */
    private final Map<IField, BitVector> staticField2DefStatements = HashMapFactory.make();

    private static final boolean VERBOSE = true;

    private final Map<Pair<CGNode, Integer>, HashSet<String>> invokeInstTags;

    private final OrdinalSetMapping<JSTaintNode> taintNodeNumbering;

    private final Map<Integer, BitVector> taintNodeChain = HashMapFactory.make();

    private final HashSet<SourceSink> sourceSinks;

    private final JSTaintNode mockSource = new JSInitTaintNode("mockSource");

    private final JSTaintNode mockSink = new JSInitTaintNode("mockSink");

    public JSTaintAnalysis(CallGraph cg, HashSet<SourceSink> sourceSinks) {
        this.icfg = ExplodedInterproceduralCFG.make(cg);
        this.cha = cg.getClassHierarchy();
        this.sourceSinks = sourceSinks;
        this.instrNumbering = numberInstructions();
        this.invokeInstTags = tagInvokeInsts();
        this.taintNodeNumbering = numberTaintNodes();
        this.taintInit();
    }

    private OrdinalSetMapping<Pair<CGNode, Integer>> numberInstructions() {
        return null;
    }

    private void taintInit() {
        // fields
        Iterator<IClass> classIterator = cha.getLoaders()[0].iterateAllClasses();
        while (classIterator.hasNext()) {
            IClass cls = classIterator.next();
            HashSet<String> clsTags = new HashSet<>();
            clsTags.add(cls.getReference().toString());

            for (IField field : cls.getAllFields()) {
                HashSet<String> fieldTags = new HashSet<>();
                fieldTags.addAll(clsTags);
                fieldTags.add(field.getReference().getSignature());
                fieldTags.add(field.getFieldTypeReference().toString());

                JSTaintNode node = new JSFieldTaintNode(field);

                for (SourceSink sourceSink : this.getMatchedSourceAndSink(fieldTags)) {
                    if (sourceSink.isSource) markNode(node, mockSource);
                    else markNode(node, mockSink);
                }
            }
        }

        for (CGNode cgNode : icfg.getCallGraph()) {
            IMethod method = cgNode.getMethod();
            HashSet<String> methodTags = new HashSet<>();
            methodTags.add(method.getSignature());
            methodTags.add(method.getDeclaringClass().getReference().toString());
            HashSet<SourceSink> methodSourceSinks = this.getMatchedSourceAndSink(methodTags);

            boolean argsAreSource = false, retIsSink = false;

            for (SourceSink sourceSink : methodSourceSinks) {
                if (sourceSink.isArgs && sourceSink.isSource) argsAreSource = true;
                if (!sourceSink.isArgs && !sourceSink.isSource) retIsSink = true;
            }

            // instructions
            IR ir = cgNode.getIR();
            if (ir == null) {
                continue;
            }
            SSAInstruction[] instructions = ir.getInstructions();
            for (int i = 0; i < instructions.length; i++) {
                SSAInstruction instruction = instructions[i];
                if (instruction instanceof JavaScriptInvoke) {
                    Pair<CGNode, Integer> instPair = Pair.make(cgNode, i);
                    JSTaintNode taintNode = new JSInstrutionTaintNode(instPair);
                    HashSet<String> invokeMethodTags = this.invokeInstTags.get(instPair);
                    boolean argsAreSink = false, retIsSource = false;
                    for (SourceSink sourceSink : this.getMatchedSourceAndSink(invokeMethodTags)) {
                        if (sourceSink.isSource && !sourceSink.isArgs) retIsSource = true;
                        if (!sourceSink.isSource && sourceSink.isArgs) argsAreSink = true;
                    }
                    if (retIsSource) this.markNode(taintNode, mockSource);
                    if (argsAreSink) this.markNode(taintNode, mockSink);
                }
                else if (retIsSink && instruction instanceof SSAReturnInstruction) {
                    JSTaintNode taintNode = new JSInstrutionTaintNode(Pair.make(cgNode, i));
                    this.markNode(taintNode, mockSink);
                }
            }

            // locals
            if (argsAreSource) {
                icfg.getCallGraph().getEntrypointNodes().add(cgNode);
                int[] paraIds = cgNode.getIR().getSymbolTable().getParameterValueNumbers();
                for (int i : paraIds) {
                    JSTaintNode taintNode = new JSLocalTaintNode(Pair.make(method, i));
                    this.markNode(taintNode, mockSource);
                }
            }
        }

    }

    private void markNode(JSTaintNode site, JSTaintNode mark) {
        int siteId = this.taintNodeNumbering.getMappedIndex(site);
        int markId = this.taintNodeNumbering.getMappedIndex(mark);
        BitVector bv = this.taintNodeChain.get(siteId);
        if (bv == null) {
            bv = new BitVector();
            this.taintNodeChain.put(siteId, bv);
        }
        bv.set(markId);
    }

    private HashSet<SourceSink> getMatchedSourceAndSink(HashSet<String> tags) {
        HashSet<SourceSink> matchedSourceSinks = new HashSet<>();
        for (SourceSink sourceSink : sourceSinks) {
            if (sourceSink.matches(tags)) matchedSourceSinks.add(sourceSink);
        }
        return matchedSourceSinks;
    }

    // generating a numbering of fields in program
    private OrdinalSetMapping<JSTaintNode> numberTaintNodes() {
        ArrayList<JSTaintNode> taintNodes = new ArrayList<>();

        IClassHierarchy cha = this.cha;

        // init
        taintNodes.add(mockSource);
        taintNodes.add(mockSink);

        // fields
        Iterator<IClass> classIterator = cha.getLoaders()[0].iterateAllClasses();
        while (classIterator.hasNext()) {
            IClass cls = classIterator.next();
            for (IField field : cls.getAllFields()) {
                taintNodes.add(new JSFieldTaintNode(field));
            }
        }

        // instructions
        for (CGNode node : icfg.getCallGraph()) {
            IR ir = node.getIR();
            if (ir == null) {
                continue;
            }
            SSAInstruction[] instructions = ir.getInstructions();

            for (int i = 0; i < instructions.length; i++) {
                taintNodes.add(new JSInstrutionTaintNode(Pair.make(node, i)));
            }
        }

        // locals
        for (CGNode node : icfg.getCallGraph()) {
            IMethod m = node.getMethod();
            int maxValueNumber = node.getIR().getSymbolTable().getMaxValueNumber();

            for (int i = 0; i <= maxValueNumber; i++) {
                taintNodes.add(new JSLocalTaintNode(Pair.make(m, i)));
            }
        }
        return new ObjectArrayMapping<JSTaintNode>(taintNodes.toArray(new JSTaintNode[taintNodes.size()]));
    }

    private Map<Pair<CGNode, Integer>, HashSet<String>> tagInvokeInsts() {
        Map<Pair<CGNode, Integer>, HashSet<String>> invokeInstTags = new HashMap<>();

        for (CGNode node : icfg.getCallGraph()) {
            String m = node.getMethod().getDeclaringClass().getName().toString();
            if (m.startsWith("Lprologue.js") || m.startsWith("Lpreamble.js"))
                continue;

            IR ir = node.getIR();
            if (ir == null) continue;

            SSAInstruction[] instructions = ir.getInstructions();
            for (int i = 0; i < instructions.length; i++) {
                SSAInstruction instruction = instructions[i];
                if (instruction instanceof JavaScriptInvoke) {
                    JavaScriptInvoke invokeInst = (JavaScriptInvoke) instruction;
                    Pair<CGNode, Integer> invokeInstItem = Pair.make(node, i);
                    HashSet<String> tags = this.getReachingTags(node, invokeInst);
                    invokeInstTags.put(invokeInstItem, tags);
                }
            }
        }

        return invokeInstTags;
    }

    private HashSet<String> getReachingTags(CGNode node, SSAInstruction inst) {
        HashSet<String> tags = new HashSet<>();
        if (node == null || inst == null) return tags;

        DefUse du = node.getDU();

        if (inst instanceof JavaScriptInvoke) {
            JavaScriptInvoke invoke_inst = (JavaScriptInvoke) inst;
            tags.addAll(getReachingTags(node, invoke_inst.getFunction()));
        }
        else if (inst instanceof SSANewInstruction) {
            SSANewInstruction new_inst = (SSANewInstruction) inst;
            tags.add(new_inst.getNewSite().toString());
        }
        else if (inst instanceof SSAGetInstruction) {
            SSAGetInstruction get_inst = (SSAGetInstruction) inst;
            FieldReference field = get_inst.getDeclaredField();
            tags.add(field.getSignature());
        }
        else if (inst instanceof AstLexicalRead) {
            for (AstLexicalAccess.Access access : ((AstLexicalRead) inst).getAccesses()) {
                tags.add(access.variableName);
            }
        }
        for (int j = 0; j < inst.getNumberOfUses(); j++) {
            tags.addAll(getReachingTags(node, inst.getUse(j)));
        }

        return tags;
    }

    private HashSet<String> getReachingTags(CGNode node, int reg) {
        SSAInstruction def = node.getDU().getDef(reg);
        if (def != null)
            return getReachingTags(node, def);
        else {
            HashSet<String> tags = new HashSet<>();
            SymbolTable st = node.getIR().getSymbolTable();
            String valueString = st.getValueString(reg);
            tags.add(valueString);
            return tags;
        }
    }

    private class TransferFunctions implements ITransferFunctionProvider<BasicBlockInContext<IExplodedBasicBlock>, BitVectorVariable> {

        /**
         * our meet operator is set union
         */
        @Override
        public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
            return BitVectorUnion.instance();
        }

        @Override
        public UnaryOperator<BitVectorVariable> getNodeTransferFunction(BasicBlockInContext<IExplodedBasicBlock> node) {
            IExplodedBasicBlock ebb = node.getDelegate();
            SSAInstruction instruction = ebb.getInstruction();
            int instructionIndex = ebb.getFirstInstructionIndex();
            CGNode cgNode = node.getNode();
            if (instruction instanceof SSAPutInstruction && ((SSAPutInstruction) instruction).isStatic()) {
                // kill all defs of the same static field, and gen this instruction
                final SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
                final IField field = cha.resolveField(putInstr.getDeclaredField());
                assert field != null;
                BitVector kill = staticField2DefStatements.get(field);
                BitVector gen = new BitVector();
                gen.set(instrNumbering.getMappedIndex(Pair.make(cgNode, instructionIndex)));
                return new BitVectorKillGen(kill, gen);
            } else {
                // identity function for non-putstatic instructions
                return BitVectorIdentity.instance();
            }
        }

        /**
         * here we need an edge transfer function for call-to-return edges (see
         * {@link #getEdgeTransferFunction(BasicBlockInContext, BasicBlockInContext)})
         */
        @Override
        public boolean hasEdgeTransferFunctions() {
            return true;
        }

        @Override
        public boolean hasNodeTransferFunctions() {
            return true;
        }

        /**
         * for direct call-to-return edges at a call site, the edge transfer function will kill all facts, since we only want to
         * consider facts that arise from going through the callee
         */
        @Override
        public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(BasicBlockInContext<IExplodedBasicBlock> src,
                                                                        BasicBlockInContext<IExplodedBasicBlock> dst) {
            if (isCallToReturnEdge(src, dst)) {
                return BitVectorKillAll.instance();
            } else {
                return BitVectorIdentity.instance();
            }
        }

        private boolean isCallToReturnEdge(BasicBlockInContext<IExplodedBasicBlock> src, BasicBlockInContext<IExplodedBasicBlock> dst) {
            SSAInstruction srcInst = src.getDelegate().getInstruction();
            return srcInst instanceof SSAAbstractInvokeInstruction && src.getNode().equals(dst.getNode());
        }

    }

    /**
     * run the analysis
     *
     * @return the solver used for the analysis, which contains the analysis result
     */
    public BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>> analyze() {
        // the framework describes the dataflow problem, in particular the underlying graph and the transfer functions
        BitVectorFramework<BasicBlockInContext<IExplodedBasicBlock>, Pair<CGNode, Integer>> framework = new BitVectorFramework<BasicBlockInContext<IExplodedBasicBlock>, Pair<CGNode, Integer>>(
                icfg, new TransferFunctions(), instrNumbering);
        BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>> solver = new BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>>(
                framework);
        try {
            solver.solve(null);
        } catch (CancelException e) {
            // this shouldn't happen
            assert false;
        }
        if (VERBOSE) {
            for (BasicBlockInContext<IExplodedBasicBlock> ebb : icfg) {
                System.out.println(ebb);
                System.out.println(ebb.getDelegate().getInstruction());
                System.out.println(solver.getIn(ebb));
                System.out.println(solver.getOut(ebb));
            }
        }
        return solver;
    }

    /**
     * gets putstatic instruction corresponding to some fact number from a bitvector in the analysis result
     */
    public Pair<CGNode, Integer> getNodeAndInstrForNumber(int num) {
        return instrNumbering.getMappedObject(num);
    }
}
