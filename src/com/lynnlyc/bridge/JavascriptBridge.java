package com.lynnlyc.bridge;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by yuanchun on 5/17/15.
 * Package: webview-flow
 */
public class JavascriptBridge extends Bridge {
    public BridgeContext context;
    public String script;
    public int js_id;
    private static int js_count = 0;
    public JavascriptBridge(BridgeContext context, String script) {
        this.context = context;
        this.script = script;
        this.js_id = js_count++;
    }
    public String toString() {
        return String.format("JavascriptBridge:\n[id]%d,\n[context]%s,\n[script]%s\n",
                this.js_id, this.context, this.script);
    }
}
