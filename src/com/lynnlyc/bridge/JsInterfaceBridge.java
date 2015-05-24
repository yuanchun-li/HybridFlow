package com.lynnlyc.bridge;

/**
 * Created by yuanchun on 5/6/15.
 * Package: webview-flow
 */
public class JsInterfaceBridge extends Bridge {
    private soot.Type interfaceType;
    private String interfaceName;
    private BridgeContext context;
    public JsInterfaceBridge(soot.Type interfaceType, String interfaceName, BridgeContext context) {
        this.interfaceType = interfaceType;
        this.interfaceName = interfaceName;
        this.context = context;
    }

    public String toString() {
        return String.format("JsInterfaceBridge:\n[context]%s,\n[Type]%s,\n[name]%s\n", this.context, this.interfaceType, this.interfaceName);
    }
}
