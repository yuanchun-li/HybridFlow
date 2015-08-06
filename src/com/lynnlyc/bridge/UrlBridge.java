package com.lynnlyc.bridge;

/**
 * Created by yuanchun on 5/4/15.
 * Package: webview-flow
 */
public class UrlBridge extends Bridge {
    public BridgeContext context;
    private String url;
    public UrlBridge(BridgeContext context, String url) {
        this.context = context;
        this.url = url;
    }
    public String toString() {
        return String.format("UrlBridge:\n[context]%s,\n[url]%s\n", this.context, this.url);
    }
}
