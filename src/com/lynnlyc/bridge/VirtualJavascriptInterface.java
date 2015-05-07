package com.lynnlyc.bridge;

import soot.SootClass;

/**
 * Created by yuanchun on 5/6/15.
 * Package: webview-flow
 */
public class VirtualJavascriptInterface {
    private SootClass interfaceClass;
    private String interfaceName;
    public VirtualJavascriptInterface(SootClass interfaceClass, String interfaceName) {
        this.interfaceClass = interfaceClass;
        this.interfaceName = interfaceName;
    }
}
