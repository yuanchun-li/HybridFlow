package com.lynnlyc.web.taintanalysis;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Pair;

/**
 * Created by liyc on 9/28/15.
 * js instruction as TaintNode
 */
public class JSInstrutionTaintNode implements JSTaintNode {
    public Pair<CGNode, Integer> value;

    public JSInstrutionTaintNode(Pair<CGNode, Integer> value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JSInstrutionTaintNode)) return false;
        if (value == null) return ((JSInstrutionTaintNode) o).value == null;
        return value.equals(((JSInstrutionTaintNode) o).value);
    }

    public String toString() {
        return String.format("{{instruction:%d|%s|%s}}", value.snd,
                value.fst.getIR().getInstructions()[value.snd],
                value.fst.getMethod().getSignature());
    }

    public SSAInstruction getInstruction() {
        return value.fst.getIR().getInstructions()[value.snd];
    }
}
