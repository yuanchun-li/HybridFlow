package com.lynnlyc.web.taintanalysis;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.collections.Pair;

/**
 * Created by liyc on 9/27/15.
 */
public class JSLocalVariable extends JSVariable {
    public Pair<CGNode, Integer> value;

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JSLocalVariable)) return false;
        if (value == null) return ((JSLocalVariable) o).value == null;
        return value.equals(((JSLocalVariable) o).value);
    }
}
