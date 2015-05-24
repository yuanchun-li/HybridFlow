package com.lynnlyc.bridge;

import com.lynnlyc.Util;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * Created by yuanchun on 5/4/15.
 * Package: webview-flow
 */
public class VirtualWebview {
    private static VirtualWebview virtualWebview;
    public static VirtualWebview v() {
        if (virtualWebview == null) {
            virtualWebview = new VirtualWebview();
        }
        return virtualWebview;
    }
    private HashSet<JsInterfaceBridge> jsInterfaceBridges;
    private HashSet<JavascriptBridge> javascriptBridges;
    private HashSet<UrlBridge> urlBridges;
    private HashSet<EventBridge> eventBridges;

    private VirtualWebview() {
        jsInterfaceBridges = new HashSet<JsInterfaceBridge>();
        javascriptBridges = new HashSet<JavascriptBridge>();
        urlBridges = new HashSet<UrlBridge>();
        eventBridges = new HashSet<EventBridge>();
    }

    public void dump(PrintStream os) {
        os.println("======Virtual Webview Bridges======");
        os.println("------   JsInterfaceBridges  ------");
        for (JsInterfaceBridge jsInterfaceBridge : jsInterfaceBridges) {
            os.println(jsInterfaceBridge);
        }

        os.println("------   JavascriptBridges   ------");
        for (JavascriptBridge javascriptBridge : javascriptBridges) {
            os.println(javascriptBridge);
        }

        os.println("------      UrlBridges       ------");
        for (UrlBridge urlBridge : urlBridges) {
            os.println(urlBridge);
        }

        os.println("------     EventBridges      ------");
        for (EventBridge eventBridge : eventBridges) {
            os.println(eventBridge);
        }
        os.println("======     End of Bridges    ======");
    }

    public void addBridge(Bridge bridge) {
        if (bridge instanceof JsInterfaceBridge) {
            jsInterfaceBridges.add((JsInterfaceBridge) bridge);
        }
        else if (bridge instanceof JavascriptBridge) {
            javascriptBridges.add((JavascriptBridge) bridge);
        }
        else if (bridge instanceof UrlBridge) {
            urlBridges.add((UrlBridge) bridge);
        }
        else if (bridge instanceof EventBridge) {
            eventBridges.add((EventBridge) bridge);
        }
        else {
            Util.LOGGER.log(Level.WARNING, "Unknown bridge type." + bridge);
        }
    }
}
