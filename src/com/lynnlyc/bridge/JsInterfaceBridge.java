package com.lynnlyc.bridge;

/**
 * Created by yuanchun on 5/6/15.
 * Package: webview-flow
 */
public class JsInterfaceBridge extends Bridge {
    private soot.Type interfaceType;
    private String interfaceName;
    public JsInterfaceBridge(soot.Type interfaceType, String interfaceName) {
        this.interfaceType = interfaceType;
        this.interfaceName = interfaceName;
    }

    public String toString() {
        return String.format("JsInterfaceBridge:\n[Type]%s,\n[name]%s\n", this.interfaceType, this.interfaceName);
    }
}
