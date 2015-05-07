package com.lynnlyc.bridge;

import com.ibm.wala.ipa.callgraph.CGNode;
import soot.jimple.internal.JInvokeStmt;

/**
 * Created by yuanchun on 5/4/15.
 * Package: webview-flow
 */
public class App2WebBridge extends WebviewBridge {
    private JInvokeStmt source;
    private CGNode target;
}
