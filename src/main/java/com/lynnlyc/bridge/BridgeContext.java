package com.lynnlyc.bridge;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

/**
 * Created by yuanchun on 5/17/15.
 * the context of a bridge. i.e. where the bridge is constructed
 */
public class BridgeContext {
    public final SootMethod method;
    public final Unit unit;
    public final int unitId;
    public BridgeContext(SootMethod method, Unit unit, int unitId) {
        this.unit = unit;
        this.unitId = unitId;
        this.method = method;
    }

    public String toString() {
        return String.format("{method}%s, {unit%d}%s", method, unitId, unit);
    }

    public SootMethod getInvokedMethod() {
        if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr()) {
            return ((Stmt) unit).getInvokeExpr().getMethod();
        }
        return null;
    }
}
