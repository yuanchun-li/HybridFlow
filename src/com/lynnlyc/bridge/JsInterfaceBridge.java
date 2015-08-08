package com.lynnlyc.bridge;

import soot.SootClass;

/**
 * Created by yuanchun on 5/6/15.
 * Package: webview-flow
 */
public class JsInterfaceBridge extends Bridge {
    public SootClass interfaceClass;
    public String interfaceName;
    private BridgeContext context;
    public JsInterfaceBridge(SootClass interfaceClass, String interfaceName, BridgeContext context) {
        this.interfaceClass = interfaceClass;
        this.interfaceName = interfaceName;
        this.context = context;
    }

    public String toString() {
        return String.format("JsInterfaceBridge:\n[context]%s,\n[Type]%s,\n[name]%s\n",
                this.context, this.interfaceClass, this.interfaceName);
    }
}
