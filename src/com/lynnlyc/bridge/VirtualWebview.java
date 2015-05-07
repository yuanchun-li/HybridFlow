package com.lynnlyc.bridge;

import com.ibm.wala.ipa.callgraph.CGNode;
import soot.SootClass;

import java.util.HashSet;

/**
 * Created by yuanchun on 5/4/15.
 * Package: webview-flow
 */
public class VirtualWebview {
    private HashSet<VirtualJavascriptInterface> jsInterfaces;
    private HashSet<String> possibleUrls;
    private HashSet<SootClass> possibleWebViewClients;
    private HashSet<SootClass> possibleChromeClients;
    private HashSet<CGNode> possibleCGNodes;
}
