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
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
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
import com.lynnlyc.Util;

/**
 * Computes interprocedural reaching definitions for static fields in a context-insensitive manner.
 */
public class JSTaintAnalysis {

    /**
     * the exploded interprocedural control-flow graph on which to compute the analysis
     */
    private final ExplodedInterproceduralCFG icfg;

    /**
     * for resolving field references in putstatic instructions
     */
    private final IClassHierarchy cha;

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
        this.invokeInstTags = tagInvokeInsts();
        this.taintNodeNumbering = numberTaintNodes();
        this.taintInit();
    }

    private void taintInit() {
        // fields
        for (CGNode cgNode : icfg.getCallGraph()) {
            for (SSAInstruction instruction : cgNode.getIR().getInstructions()) {
                if (instruction instanceof SSAFieldAccessInstruction) {
                    FieldReference fr = ((SSAFieldAccessInstruction) instruction).getDeclaredField();
                    HashSet<String> fieldTags = new HashSet<>();
                    fieldTags.add(fr.getSignature());
                    fieldTags.add(fr.getFieldType().toString());

                    JSTaintNode node = new JSFieldTaintNode(fr);

                    for (SourceSink sourceSink : this.getMatchedSourceAndSink(fieldTags)) {
                        if (sourceSink.isSource) markNode(node, mockSource);
                        else markNode(mockSink, node);
                    }
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
                    if (argsAreSink) this.markNode(mockSink, taintNode);
                }
                else if (retIsSink && instruction instanceof SSAReturnInstruction) {
                    JSTaintNode taintNode = new JSInstrutionTaintNode(Pair.make(cgNode, i));
                    this.markNode(mockSink, taintNode);
                }
            }

            // locals
            if (argsAreSource) {
                icfg.getCallGraph().getEntrypointNodes().add(cgNode);
                int[] paraIds = cgNode.getIR().getSymbolTable().getParameterValueNumbers();
                for (int i : paraIds) {
                    JSTaintNode taintNode = new JSLocalTaintNode(Pair.make(method.getReference(), i));
                    this.markNode(taintNode, mockSource);
                }
            }
        }

    }

    public void markNode(JSTaintNode site, JSTaintNode mark) {
        int siteId = this.taintNodeNumbering.getMappedIndex(site);
        int markId = this.taintNodeNumbering.getMappedIndex(mark);
        markNodeId(siteId, markId);
    }

    public void markNodeId(int siteId, int markId) {
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
        for (CGNode cgNode : icfg.getCallGraph()) {
            for (SSAInstruction instruction : cgNode.getIR().getInstructions()) {
                if (instruction instanceof SSAFieldAccessInstruction) {
                    FieldReference fr = ((SSAFieldAccessInstruction) instruction).getDeclaredField();
                    JSTaintNode node = new JSFieldTaintNode(fr);
                    taintNodes.add(node);
                }
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
                taintNodes.add(new JSLocalTaintNode(Pair.make(m.getReference(), i)));
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

            if (instruction == null) return BitVectorIdentity.instance();

            int instructionIndex = ebb.getFirstInstructionIndex();
            CGNode cgNode = node.getNode();
            IMethod method = cgNode.getMethod();

            JSTaintNode mediatorTaintNode = new JSInstrutionTaintNode(Pair.make(cgNode, instructionIndex));
            int mediatorTaintNodeId = taintNodeNumbering.getMappedIndex(mediatorTaintNode);

            HashSet<JSTaintNode> defTaintNodes = new HashSet<>();
            for (int i = 0; i < instruction.getNumberOfDefs(); i++)
                defTaintNodes.add(new JSLocalTaintNode(Pair.make(method.getReference(), instruction.getDef(i))));
            if (instruction instanceof SSAPutInstruction)
                defTaintNodes.add(new JSFieldTaintNode(((SSAPutInstruction) instruction).getDeclaredField()));

            HashSet<JSTaintNode> useTaintNodes = new HashSet<>();
            for (int i = 0; i < instruction.getNumberOfUses(); i++)
                useTaintNodes.add(new JSLocalTaintNode(Pair.make(method.getReference(), instruction.getUse(i))));
            if (instruction instanceof SSAGetInstruction)
                useTaintNodes.add(new JSFieldTaintNode(((SSAGetInstruction) instruction).getDeclaredField()));

            HashSet<Integer> defTaintNodeIds = new HashSet<>();
            HashSet<Integer> useTaintNodeIds = new HashSet<>();
            for (JSTaintNode taintNode : defTaintNodes) {
                defTaintNodeIds.add(taintNodeNumbering.getMappedIndex(taintNode));
            }
            for (JSTaintNode useNode : useTaintNodes) {
                useTaintNodeIds.add(taintNodeNumbering.getMappedIndex(useNode));
            }

            // Propagation
            for (int useNodeId : useTaintNodeIds) {
                BitVector useNodeValue = taintNodeChain.get(useNodeId);
                if (useNodeValue != null)
                    markNodeId(mediatorTaintNodeId, useNodeId);
            }
            BitVector mediatorValue = taintNodeChain.get(mediatorTaintNodeId);
            if (mediatorValue != null) {
                for (int defNodeId : defTaintNodeIds) {
                    markNodeId(defNodeId, mediatorTaintNodeId);
                }
            }

            BitVector gen = new BitVector();
            BitVector kill = new BitVector();
            BitVector defined = new BitVector();

            for (JSTaintNode defNode : defTaintNodes) {
                int defNodeId = taintNodeNumbering.getMappedIndex(defNode);
                defined.set(defNodeId);
            }

            if (mediatorValue != null) {
                gen = defined;
            }

            return new BitVectorKillGen(kill, gen);
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
            SSAInstruction srcInst = src.getDelegate().getInstruction();
            SSAInstruction dstInst = dst.getDelegate().getInstruction();

            if (!(srcInst instanceof JavaScriptInvoke) && (src.getNode().equals(dst.getNode()))) {
                // normal edge
                return BitVectorIdentity.instance();
            }

            if (src.getNode().equals(dst.getNode())) {
                // call to return
                return BitVectorIdentity.instance();
            }
            else  {
                if (srcInst instanceof JavaScriptInvoke && dst.isEntryBlock()) {
                    // call to entry
                    int numPara = ((JavaScriptInvoke) srcInst).getNumberOfParameters();
                    int numMethodPara = dst.getNode().getIR().getNumberOfParameters();
                    if (numPara != numMethodPara && numPara != numMethodPara + 1) {
                        Util.LOGGER.warning("parameter number mismatch!");
                        return BitVectorIdentity.instance();
                    }

                    BitVector gen = new BitVector();
                    for (int i = 0; i < numMethodPara; i++) {
                        int paraSrcId = srcInst.getUse(i);
                        JSTaintNode paraSrcNode = new JSLocalTaintNode(Pair.make(src.getMethod().getReference(), paraSrcId));
                        int paraSrcNodeId = taintNodeNumbering.getMappedIndex(paraSrcNode);

                        int paraDstId = dst.getNode().getIR().getSymbolTable().getParameter(i);
                        JSTaintNode paraDstNode = new JSLocalTaintNode(Pair.make(dst.getMethod().getReference(), paraDstId));
                        int paraDstNodeId = taintNodeNumbering.getMappedIndex(paraDstNode);

                        BitVector paraSrcNodeValue = taintNodeChain.get(paraSrcNodeId);
                        if (paraSrcNodeValue != null) {
                            markNodeId(paraDstNodeId, paraSrcNodeId);
                            gen.set(paraDstNodeId);
                        }
                    }
                    return new BitVectorKillGen(new BitVector(), gen);
                }
                else if (src.isExitBlock() && !dst.isExitBlock()) {
                    // exit to return
                    SSAInstruction invokeInst = dst.getNode().getIR().getInstructions()[dst.getFirstInstructionIndex() - 1];
                    if (invokeInst instanceof JavaScriptInvoke) {
                        int retDstId = invokeInst.getDef();
                        JSTaintNode retDstNode = new JSLocalTaintNode(Pair.make(dst.getMethod().getReference(), retDstId));
                        int retDstNodeId = taintNodeNumbering.getMappedIndex(retDstNode);

                        BitVector gen = new BitVector();

                        for (SSAInstruction inst : src.getNode().getIR().getInstructions()) {
                            if (inst instanceof SSAReturnInstruction) {
                                for (int i = 0; i < inst.getNumberOfUses(); i++) {
                                    int retSrcId = inst.getUse(i);
                                    JSTaintNode retSrcNode = new JSLocalTaintNode(Pair.make(src.getMethod().getReference(), retSrcId));
                                    int retSrcNodeId = taintNodeNumbering.getMappedIndex(retSrcNode);

                                    BitVector paraSrcNodeValue = taintNodeChain.get(retSrcNodeId);
                                    if (paraSrcNodeValue != null) {
                                        markNodeId(retDstNodeId, retSrcNodeId);
                                        gen.set(retDstNodeId);
                                    }
                                }
                            }
                        }
                        return new BitVectorKillGen(new BitVector(), gen);
                    }
                    else {
                        Util.LOGGER.warning("exit-to-return is not pointing to a invoke instruction");
                    }
                }
            }
            return BitVectorIdentity.instance();
        }
    }

    /**
     * run the analysis
     *
     * @return the solver used for the analysis, which contains the analysis result
     */
    public BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>> analyze() {
        // the framework describes the dataflow problem, in particular the underlying graph and the transfer functions
        BitVectorFramework<BasicBlockInContext<IExplodedBasicBlock>, JSTaintNode> framework = new BitVectorFramework<>(
                icfg, new TransferFunctions(), this.taintNodeNumbering);
        BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>> solver = new BitVectorSolver<BasicBlockInContext<IExplodedBasicBlock>>(
                framework);
        try {
            solver.solve(null);
        } catch (CancelException e) {
            // this shouldn't happen
            assert false;
        }
        if (VERBOSE) {
            for (List<Integer> path : this.getTaintPathsFrom(taintNodeNumbering.getMappedIndex(mockSource))) {
                System.out.println("");
                System.out.println(this.path2String(path, true));
                System.out.println(this.path2String(path, false));
            }
        }
        return solver;
    }

    public HashSet<Integer> getTaintMarks(int site) {
        BitVector bv = taintNodeChain.get(site);
        HashSet<Integer> marks = new HashSet<>();

        if (bv == null) {
            return marks;
        }

        for (int i = 0; i < bv.length(); i++) {
            if (bv.get(i)) {
                marks.add(i);
            }
        }
        return marks;
    }

    public HashSet<Integer> getMarkedSites(int mark) {
        HashSet<Integer> sites = new HashSet<>();

        for (int site : taintNodeChain.keySet()) {
            BitVector marks = taintNodeChain.get(site);
            if (marks != null && marks.get(mark)) sites.add(site);
        }

        return sites;
    }

    public Set<List<Integer>> getTaintPathsFrom(Integer sourceId) {
        HashSet<List<Integer>> paths = new HashSet<>();

        HashSet<Integer> markedSites = this.getMarkedSites(sourceId);

        if (markedSites.isEmpty()) {
            List<Integer> path = new ArrayList<>();
            path.add(sourceId);
            paths.add(path);
        }
        else {
            for (int site : markedSites) {
                Set<List<Integer>> succPaths = getTaintPathsFrom(site);
                for (List<Integer> path : succPaths) {
                    path.add(sourceId);
                }
                paths.addAll(succPaths);
            }
        }

        return paths;
    }

    public Set<List<Integer>> getTaintPathsTo(Integer sinkId) {
        HashSet<List<Integer>> paths = new HashSet<>();

        HashSet<Integer> taintMarks = this.getTaintMarks(sinkId);

        if (taintMarks.isEmpty()) {
            List<Integer> path = new ArrayList<>();
            path.add(sinkId);
            paths.add(path);
        }
        else {
            for (int mark : taintMarks) {
                Set<List<Integer>> prevPaths = getTaintPathsTo(mark);
                for (List<Integer> path : prevPaths) {
                    path.add(sinkId);
                }
                paths.addAll(prevPaths);
            }
        }

        return paths;
    }

    public String path2String(List<Integer> path, boolean numbered) {
        String numberPathStr = "";
        String pathStr = "";
        for (int node : path) {
            String nodeStr = taintNodeNumbering.getMappedObject(node).toString();
            numberPathStr = String.format("%d --> %s", node, numberPathStr);
            pathStr = String.format("%s --> %s", nodeStr, pathStr);
        }
        if (numbered) return numberPathStr;
        else return pathStr;
    }
}
