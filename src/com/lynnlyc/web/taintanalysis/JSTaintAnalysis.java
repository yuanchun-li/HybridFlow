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

import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
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
    private final OrdinalSetMapping<Pair<CGNode, Integer>> putInstrNumbering;

    /**
     * for resolving field references in putstatic instructions
     */
    private final IClassHierarchy cha;

    /**
     * maps each static field to the numbers of the statements (in {@link #putInstrNumbering}) that define it; used for kills in flow
     * functions
     */
    private final Map<IField, BitVector> staticField2DefStatements = HashMapFactory.make();

    private static final boolean VERBOSE = true;

    private final Map<Pair<CGNode, Integer>, HashSet<String>> invokeInstTags;

    private final OrdinalSetMapping<JSVariable> variableNumbering;
    public JSTaintAnalysis(CallGraph cg, HashSet<SourceSink> sourceSinks) {
        this.icfg = ExplodedInterproceduralCFG.make(cg);
        this.cha = cg.getClassHierarchy();
        this.putInstrNumbering = numberPutStatics();
        this.variableNumbering = numberVariables();
        this.invokeInstTags = tagInvokeInsts();
    }

    // generating a numbering of fields in program
    private OrdinalSetMapping<JSVariable> numberVariables() {
        ArrayList<JSVariable> variables = new ArrayList<>();

        IClassHierarchy cha = this.cha;
        CallGraph cg = this.icfg.getCallGraph();

        Iterator<IClass> classIterator = cha.getLoaders()[0].iterateAllClasses();
        while (classIterator.hasNext()) {
            IClass cls = classIterator.next();
            for (IField field : cls.getAllFields()) {
                variables.add(new JSFieldVariable(field));
            }
        }


        // Local variables
//        for (CGNode node : icfg.getCallGraph()) {
//            ir = node.getIR();
//            SSAInstruction[] instructions = ir.getInstructions();
//            for (int i = 0; i < instructions.length; i++) {
//                SSAInstruction instruction = instructions[i];
//                if (instruction instanceof AstGlobalRead || instruction instanceof AstLexicalRead) {
//                    SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
//                    // instrNum is the number that will be assigned to this putstatic
//                    int instrNum = putInstrs.size();
//                    putInstrs.add(Pair.make(node, i));
//                    // also update the mapping of static fields to def'ing statements
//                    IField field = cha.resolveField(putInstr.getDeclaredField());
//                    assert field != null;
//                    BitVector bv = staticField2DefStatements.get(field);
//                    if (bv == null) {
//                        bv = new BitVector();
//                        staticField2DefStatements.put(field, bv);
//                    }
//                    bv.set(instrNum);
//                }
//            }
//        }
        return new ObjectArrayMapping<JSVariable>(variables.toArray(new JSVariable[variables.size()]));
    }


    /**
     * generate a numbering of the putstatic instructions
     */
    @SuppressWarnings("unchecked")
    private OrdinalSetMapping<Pair<CGNode, Integer>> numberPutStatics() {
        ArrayList<Pair<CGNode, Integer>> putInstrs = new ArrayList<Pair<CGNode, Integer>>();
        for (CGNode node : icfg.getCallGraph()) {
            IR ir = node.getIR();
            if (ir == null) {
                continue;
            }
            SSAInstruction[] instructions = ir.getInstructions();
            for (int i = 0; i < instructions.length; i++) {
                SSAInstruction instruction = instructions[i];
                if (instruction instanceof SSAPutInstruction && ((SSAPutInstruction) instruction).isStatic()) {
                    SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
                    // instrNum is the number that will be assigned to this putstatic
                    int instrNum = putInstrs.size();
                    putInstrs.add(Pair.make(node, i));
                    // also update the mapping of static fields to def'ing statements
                    IField field = cha.resolveField(putInstr.getDeclaredField());
                    assert field != null;
                    BitVector bv = staticField2DefStatements.get(field);
                    if (bv == null) {
                        bv = new BitVector();
                        staticField2DefStatements.put(field, bv);
                    }
                    bv.set(instrNum);
                }
            }
        }
        return new ObjectArrayMapping<Pair<CGNode, Integer>>(putInstrs.toArray(new Pair[putInstrs.size()]));
    }

    private Map<Pair<CGNode, Integer>, HashSet<String>> tagInvokeInsts() {
        Map<Pair<CGNode, Integer>, HashSet<String>> invokeInstTags = new HashMap<>();

        for (CGNode node : icfg.getCallGraph()) {
            IR ir = node.getIR();
            if (ir == null) continue;

            SSAInstruction[] instructions = ir.getInstructions();
            for (int i = 0; i < instructions.length; i++) {
                SSAInstruction instruction = instructions[i];
                if (instruction instanceof JavaScriptInvoke) {
                    JavaScriptInvoke invokeInst = (JavaScriptInvoke) instruction;
                    Pair<CGNode, Integer> invokeInstItem = Pair.make(node, i);

                    HashSet<String> tags = new HashSet<>();
                    HashSet<Integer> tagRegs = new HashSet<>();

                    tagRegs.add(invokeInst.getFunction());
                    for (int j = 0; j < invokeInst.getNumberOfUses(); j++) {
                        tagRegs.add(invokeInst.getUse(j));
                    }

                    for (int tagReg : tagRegs) {
                        tags.addAll(this.getReachingTags(node, tagReg));
                    }

                    invokeInstTags.put(invokeInstItem, tags);
                }
            }
        }

        return invokeInstTags;
    }

    private HashSet<String> getReachingTags(CGNode node, int reg) {
        HashSet<String> tags = new HashSet<>();

        return tags;
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
                gen.set(putInstrNumbering.getMappedIndex(Pair.make(cgNode, instructionIndex)));
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
                icfg, new TransferFunctions(), putInstrNumbering);
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
        return putInstrNumbering.getMappedObject(num);
    }
}
