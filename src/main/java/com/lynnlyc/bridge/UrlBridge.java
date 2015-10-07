package com.lynnlyc.bridge;

import com.lynnlyc.Config;
import com.lynnlyc.Util;
import com.lynnlyc.web.WebManager;
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

    @Override
    public void export2app() {
        VirtualWebview.v().setJavaSinkMethod(context.getInvokedMethod());
    }

    @Override
    public void export2web() {
        try {
            String url_file_name = String.format("%s/url_page_%d.html",
                    Config.htmlDirPath, this.url_id);
            URL url = new URL(this.url);
            File url_file = new File(url_file_name);
            FileUtils.copyURLToFile(url, url_file);
            VirtualWebview.v().addPossibleURL(this.url);
        } catch (MalformedURLException e) {
            Util.LOGGER.warning("malformed url: " + this.url);
        } catch (UnknownHostException e) {
            Util.LOGGER.warning("unknown host: " + this.url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
