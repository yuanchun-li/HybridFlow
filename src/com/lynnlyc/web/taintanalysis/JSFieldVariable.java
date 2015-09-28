package com.lynnlyc.web.taintanalysis;

import com.ibm.wala.classLoader.IField;

/**
 * Created by liyc on 9/27/15.
 */
public class JSFieldVariable extends JSVariable {
    public IField value;

    public JSFieldVariable(IField field) {
        value = field;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JSFieldVariable)) return false;
        if (value == null) return ((JSFieldVariable) o).value == null;
        return value.equals(((JSFieldVariable) o).value);
    }
}
