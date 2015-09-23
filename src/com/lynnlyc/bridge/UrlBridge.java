package com.lynnlyc.bridge;

/**
 * Created by yuanchun on 5/4/15.
 * Package: webview-flow
 */
public class UrlBridge extends Bridge {
    public BridgeContext context;
    public String url;
    public int url_id;
    public static int url_count = 0;
    public UrlBridge(BridgeContext context, String url) {
        this.context = context;
        this.url = url;
        this.url_id = url_count++;
    }
    public String toString() {
        return String.format("UrlBridge:\n[id]%d,\n[context]%s,\n[url]%s\n",
                this.url_id, this.context, this.url);
    }
}
