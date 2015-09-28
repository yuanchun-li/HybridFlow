package com.lynnlyc.web.taintanalysis;

/**
 * Created by liyc on 9/28/15.
 */
public class JSInitTaintNode extends JSTaintNode {
    public String value;
    public JSInitTaintNode(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JSInitTaintNode)) return false;
        if (value == null) return ((JSInitTaintNode) o).value == null;
        return value.equals(((JSInitTaintNode) o).value);
    }

    public String toString() {
        return value;
    }
}
