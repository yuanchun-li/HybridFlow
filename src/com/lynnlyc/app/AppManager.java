package com.lynnlyc.app;

import beaver.Parser;
import com.lynnlyc.Config;
import com.lynnlyc.Util;
import com.lynnlyc.bridge.*;
import dk.brics.automaton.Automaton;
import dk.brics.string.StringAnalysis;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by yuanchun on 5/5/15.
 * Package: webview-flow
 */
public class AppManager {
    public String appFilePath;
    private boolean isPrepared = false;
    private boolean isPTAFinished = false;
    private boolean isJSAFinished = false;
    private PointsToAnalysis pta = null;
    private StringAnalysis jsa = null;

    private SootClass WebViewClass;
    private SootClass WebChromeClientClass;
    private SootClass WebViewClientClass;
    private SootMethod loadUrlMethod;
    private SootMethod addJavascriptInterfaceMethod;

    private HashSet<SootClass> webviewClasses;

    public AppManager(String appFilePath) {
        this.appFilePath = appFilePath;
        this.prepare();
    }

    // entry points of soot-based app analysis
    public List<SootMethod> appEntryPoints = new ArrayList<SootMethod>();

    private void prepare() {
        if (!Config.isInitialized) {
            Util.LOGGER.log(Level.WARNING, "Configuration not initialized");
            return;
        }
        Scene.v().loadNecessaryClasses();

        WebViewClass = Scene.v().getSootClass("android.webkit.WebView");
        WebChromeClientClass = Scene.v().getSootClass("android.webkit.WebChromeClient");
        WebViewClientClass = Scene.v().getSootClass("android.webkit.WebViewClient");
        loadUrlMethod = Scene.v().getMethod(Util.loadUrlSig);
        addJavascriptInterfaceMethod = Scene.v().getMethod(Util.addJavascriptInterfaceSig);

        // filter all support classes and R classes
        HashSet<SootClass> androidLibClasses = new HashSet<SootClass>();
        for(SootClass cls : Scene.v().getApplicationClasses()) {
            String cl = cls.getName();
            if (cl.startsWith("android.support")) {
                androidLibClasses.add(cls);
            }
            else if (cl.startsWith("android.support"))
                androidLibClasses.add(cls);
            else if (cl.endsWith(".R") || cl.contains(".R$"))
                androidLibClasses.add(cls);
        }

        for(SootClass cls : androidLibClasses) {
            cls.setLibraryClass();
        }


        // filter all webview related classes
        webviewClasses = new HashSet<SootClass>();
        for(SootClass cls : Scene.v().getApplicationClasses()) {
            if (cls.getSuperclass() == WebChromeClientClass) {
                webviewClasses.add(cls);
                continue;
            }
            if (cls.getSuperclass() == WebViewClientClass) {
                webviewClasses.add(cls);
                continue;
            }
            Iterator mi = cls.getMethods().iterator();
            boolean flag = false;
            while(mi.hasNext()) {
                SootMethod sm = (SootMethod) mi.next();
                if (sm.isConcrete()) {
                    Body body = null;
                    try {
                        body = sm.retrieveActiveBody();
                    }
                    catch (Exception ex) {
                        continue;
                    }
                    for (Unit unit : body.getUnits()) {
                        Stmt stmt = (Stmt) unit;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr expr = stmt.getInvokeExpr();
                            SootMethod tgt = expr.getMethod();
                            if (tgt.getDeclaringClass() == WebViewClass) {
                                flag = true;
                            }
                        }
                        if (flag) break;
                    }
                }
                if (flag) {
                    webviewClasses.add(cls);
                    break;
                }
            }
        }

        HashSet<SootClass> notWebviewClasses = new HashSet<SootClass>();

        for(SootClass cls : Scene.v().getApplicationClasses()) {
            if(webviewClasses.contains(cls)) continue;
            notWebviewClasses.add(cls);
        }

        for(SootClass cls : notWebviewClasses) {
            cls.setLibraryClass();
        }

        appEntryPoints = Util.findEntryPoints();
        Scene.v().setEntryPoints(appEntryPoints);
        this.isPrepared = true;
    }

    public void runPTA() {
        if (!this.isPrepared) {
            Util.LOGGER.log(Level.WARNING, "App not perpared " + this.appFilePath);
            return;
        }
        PTA.runSparkPTA();
        this.pta = Scene.v().getPointsToAnalysis();
        this.isPTAFinished = true;

        return;
    }

    public void runJSA() {
        if (!this.isPrepared) {
            Util.LOGGER.log(Level.WARNING, "App not perpared " + this.appFilePath);
            return;
        }

        ArrayList<ValueBox> hotspots = new ArrayList<ValueBox>();

        for (SootClass cls : webviewClasses) {
            for (SootMethod m : cls.getMethods()) {
                if (!m.hasActiveBody()) continue;
                Body b = m.getActiveBody();
                for (Unit u : b.getUnits()) {
                    try {
                        Stmt stmt = (Stmt) u;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr expr = stmt.getInvokeExpr();
                            SootMethod tgt = expr.getMethod();
                            if (tgt == loadUrlMethod) {
                                ValueBox urlValue = expr.getArgBox(0);
                                hotspots.add(urlValue);
                            } else if (tgt == addJavascriptInterfaceMethod) {
                                ValueBox interfaceNameValue = expr.getArgBox(1);
                                hotspots.add(interfaceNameValue);
                            }
                        }
                    } catch (Exception e) {
                        Util.LOGGER.log(Level.WARNING, "error generating hotspots for: " + m + u);
                        e.printStackTrace();
                    }
                }
            }
        }

        this.jsa = JSA.run(hotspots);

        this.isJSAFinished = true;
    }

    public void dumpAllApplicationClasses(PrintStream os) {
        os.println("---Application Classes---");
        for (SootClass cls : Scene.v().getApplicationClasses()) {
            os.println(cls);
        }
        os.println("---end of Application Classes---");
    }

    public void generateBridges() {
        for (SootClass cls : webviewClasses) {
            if (cls.getSuperclass() == WebChromeClientClass) {
                for (SootMethod m : cls.getMethods()) {
                    if (m.getName().equals("onJsAlert")) {
                        VirtualWebview.v().addBridge(new EventBridge("alert", m));
                    }
                    else if (m.getName().equals("onJsConfirm")) {
                        VirtualWebview.v().addBridge(new EventBridge("confirm", m));
                    }
                    else if (m.getName().equals("onJsPrompt")) {
                        VirtualWebview.v().addBridge(new EventBridge("prompt", m));
                    }
                    else if (m.getName().equals("onConsoleMessage")) {
                        VirtualWebview.v().addBridge(new EventBridge("console", m));
                    }
                }
            }
            else if (cls.getSuperclass() == WebViewClientClass) {
                for (SootMethod m : cls.getMethods()) {
                    if (m.getName().equals("shouldOverrideUrlLoading")) {
                        VirtualWebview.v().addBridge(new EventBridge("url", m));
                    }
                }
            }

            for (SootMethod m : cls.getMethods()) {
                if (!m.hasActiveBody()) continue;
                Body b = m.getActiveBody();
                int unitid = 0;
                for (Unit u : b.getUnits()) {
                    try {
                        Stmt stmt = (Stmt) u;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr expr = stmt.getInvokeExpr();
                            SootMethod tgt = expr.getMethod();
                            if (tgt == loadUrlMethod) {
                                BridgeContext context = new BridgeContext(m, u, unitid);
                                ValueBox urlValue = expr.getArgBox(0);
                                Automaton urlAutomaton = this.jsa.getAutomaton(urlValue);
                                String urlStr = urlAutomaton.getShortestExample(true);
                                if (urlStr.startsWith("javascript:")) {
                                    VirtualWebview.v().addBridge(new JavascriptBridge(context, urlStr));
                                } else {
                                    VirtualWebview.v().addBridge(new UrlBridge(context, urlStr));
                                }
                            } else if (tgt == addJavascriptInterfaceMethod) {
                                PointsToSet interfaceClass = this.pta.reachingObjects((Local) expr.getArg(0));
                                ValueBox interfaceNameValue = expr.getArgBox(1);
                                Automaton interfaceNameAutomaton = this.jsa.getAutomaton(interfaceNameValue);
                                String interfaceNameStr = interfaceNameAutomaton.getShortestExample(true);
                                for (Type possibleType : interfaceClass.possibleTypes()) {
                                    VirtualWebview.v().addBridge(new JsInterfaceBridge(possibleType, interfaceNameStr));
                                }
                            }
                        }
                    } catch (Exception e) {
                        Util.LOGGER.log(Level.WARNING, "error generating bridges for: " + m + u);
                        e.printStackTrace();
                    }
                    unitid++;
                }
            }
        }
    }
}
