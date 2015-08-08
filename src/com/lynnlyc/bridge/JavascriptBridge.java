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
    public JavascriptBridge(BridgeContext context, String script) {
        this.context = context;
        this.script = script;
        this.jsTempFile = null;
    }
    public String toString() {
        return String.format("JavascriptBridge:\n[context]%s,\n[script]%s\n", this.context, this.script);
    }

    public File jsTempFile;
    private static int js_id = 0;
    public File getTempFile() {
        if (jsTempFile == null) {
            String jsTempFileName = String.format("javascriptBridge_%d.js", js_id);
            jsTempFile = new File(jsTempFileName);
            try {
                FileUtils.write(jsTempFile, script);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jsTempFile;
    }
}
