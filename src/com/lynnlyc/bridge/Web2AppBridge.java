package com.lynnlyc.bridge;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import soot.SootMethod;
import soot.jimple.internal.JInvokeStmt;

/**
 * Created by yuanchun on 5/4/15.
 * Package: webview-flow
 */
public class Web2AppBridge extends WebviewBridge {
    private SSAInvokeInstruction source;
    private SootMethod target;
}
