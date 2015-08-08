package com.lynnlyc.web;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by yuanchun on 5/5/15.
 * Package: webview-flow
 */
public class WebManager {
    public static WebManager v() {
        if (webManager == null) {
            webManager = new WebManager();
        }
        return webManager;
    }

    private static WebManager webManager = null;
    private WebManager() {
    }

    public void analyseScript(File script) {
        try {
            this.analyseHTML(script.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void analyseHTML(URL url) {
        // TODO implement this method
    }
}
