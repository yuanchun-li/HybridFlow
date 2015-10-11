package com.lynnlyc.bridge;

import com.lynnlyc.Config;
import com.lynnlyc.Util;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Created by yuanchun on 5/4/15.
 * Package: webview-flow
 */
public class UrlBridge extends Bridge {
    public final BridgeContext context;
    public final String url;
    public final int url_id;
    public static int url_count = 0;
    public UrlBridge(BridgeContext context, String url) {
        this.context = context;
        URL availableURL;
        try {
            availableURL = new URL(url);
        } catch (MalformedURLException e) {
            Util.LOGGER.warning("malformed url: " + url);
            availableURL = null;
        }
        if (availableURL != null) this.url = availableURL.toString();
        else this.url = "unknown:" + url;
        this.url_id = url_count++;
    }
    public String toString() {
        return String.format("UrlBridge:\n[id]%d,\n[context]%s,\n[url]%s\n[bridgePath](J)(ARGS)%s --> (H)(CODE)%s\n",
                this.url_id, this.context, this.url, this.context.getInvokedMethod(), this.url);
    }

    @Override
    public void export2app() {
        VirtualWebview.v().setJavaMethodArgsAsSink(context.getInvokedMethod());
    }

    @Override
    public void export2web() {
        VirtualWebview.v().setJSCodeAsSource(String.format("url_%s", this.url));
        VirtualWebview.v().addPossibleURL(this.url);
    }
}

