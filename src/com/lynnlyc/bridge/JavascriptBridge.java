package com.lynnlyc.bridge;

/**
 * Created by yuanchun on 5/17/15.
 * Package: webview-flow
 */
public class JavascriptBridge extends Bridge {
    private BridgeContext context;
    private String script;
    public JavascriptBridge(BridgeContext context, String script) {
        this.context = context;
        this.script = script;
    }
    public String toString() {
        return String.format("JavascriptBridge:\n[context]%s,\n[script]%s\n", this.context, this.script);
    }
}
