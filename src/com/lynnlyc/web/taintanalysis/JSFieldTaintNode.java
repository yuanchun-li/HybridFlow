package com.lynnlyc.web.taintanalysis;

import com.ibm.wala.classLoader.IField;

/**
 * Created by liyc on 9/27/15.
 */
public class JSFieldTaintNode extends JSTaintNode {
    public IField value;

    public JSFieldTaintNode(IField field) {
        value = field;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JSFieldTaintNode)) return false;
        if (value == null) return ((JSFieldTaintNode) o).value == null;
        return value.equals(((JSFieldTaintNode) o).value);
    }

    public String toString() {
        return String.format("%s", value.getReference().getSignature());
    }
}
