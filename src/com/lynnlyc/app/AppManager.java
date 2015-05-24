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
import soot.jimple.internal.ImmediateBox;

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

    private SootClass WebViewClass = null;
    private SootClass WebChromeClientClass = null;
    private SootClass WebViewClientClass = null;
    private SootMethod loadUrlMethod = null;
    private SootMethod addJavascriptInterfaceMethod = null;

    private HashSet<SootClass> webviewClasses = new HashSet<SootClass>();

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

        try {
            WebViewClass = Scene.v().getSootClass("android.webkit.WebView");
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find WebView class");
            e.printStackTrace(Config.getLogPs());
        }
        try {
            WebChromeClientClass = Scene.v().getSootClass("android.webkit.WebChromeClient");
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find WebChromeClient class");
            e.printStackTrace(Config.getLogPs());
        }
        try {
            WebViewClientClass = Scene.v().getSootClass("android.webkit.WebViewClient");
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find WebViewClient class");
            e.printStackTrace(Config.getLogPs());
        }
        try {
            loadUrlMethod = Scene.v().getMethod(Util.loadUrlSig);
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find loadUrl method");
            e.printStackTrace(Config.getLogPs());
        }
        try {
            addJavascriptInterfaceMethod = Scene.v().getMethod(Util.addJavascriptInterfaceSig);
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find addJavascriptInterface method");
            e.printStackTrace(Config.getLogPs());
        }
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
            if (WebChromeClientClass != null && cls.getSuperclass() == WebChromeClientClass) {
                webviewClasses.add(cls);
                continue;
            }
            if (webviewClasses != null && cls.getSuperclass() == WebViewClientClass) {
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
        try {
            PTA.runSparkPTA();
        }
        catch (Exception e) {
            Util.LOGGER.warning("Spark PTA failed");
            e.printStackTrace(Config.getLogPs());
        }
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
                            if (loadUrlMethod != null && tgt == loadUrlMethod) {
                                ValueBox urlValue = expr.getArgBox(0);
                                hotspots.add(urlValue);
                            } else if (addJavascriptInterfaceMethod != null && tgt == addJavascriptInterfaceMethod) {
                                ValueBox interfaceNameValue = expr.getArgBox(1);
                                hotspots.add(interfaceNameValue);
                            }
                        }
                    } catch (Exception e) {
                        Util.LOGGER.warning("error generating hotspots for: " + m + u);
                        e.printStackTrace(Config.getLogPs());
                    }
                }
            }
        }
        try {
            this.jsa = JSA.run(hotspots);
        }
        catch (Exception e) {
            Util.LOGGER.warning("JSA failed");
        }
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
                            BridgeContext context = new BridgeContext(m, u, unitid);
                            InvokeExpr expr = stmt.getInvokeExpr();
                            SootMethod tgt = expr.getMethod();
                            if (loadUrlMethod != null && tgt == loadUrlMethod) {
                                ValueBox urlValue = expr.getArgBox(0);
                                String urlStr;
                                if (this.jsa == null) {
                                    urlStr = urlValue.toString();
                                }
                                else {
                                    Automaton urlAutomaton = this.jsa.getAutomaton(urlValue);
                                    urlStr = urlAutomaton.getShortestExample(true);
                                }
                                if (urlStr.contains("javascript:")) {
                                    VirtualWebview.v().addBridge(new JavascriptBridge(context, urlStr));
                                } else {
                                    VirtualWebview.v().addBridge(new UrlBridge(context, urlStr));
                                }
                            } else if (addJavascriptInterfaceMethod != null && tgt == addJavascriptInterfaceMethod) {
                                ValueBox interfaceNameValue = expr.getArgBox(1);
                                String interfaceNameStr;
                                if (this.jsa == null) {
                                    interfaceNameStr = interfaceNameValue.toString();
                                }
                                else {
                                    Automaton interfaceNameAutomaton = this.jsa.getAutomaton(interfaceNameValue);
                                    interfaceNameStr = interfaceNameAutomaton.getShortestExample(true);
                                }

                                Value interfaceObj = expr.getArg(0);
                                HashSet<Type> possibleTypes = new HashSet<Type>();

                                if (this.pta == null) {
                                    possibleTypes.add(interfaceObj.getType());
                                }
                                else {
                                    PointsToSet interfaceClass = this.pta.reachingObjects((Local) interfaceObj);
                                    possibleTypes.addAll(interfaceClass.possibleTypes());
                                }
                                for (Type possibleType : possibleTypes) {
                                    VirtualWebview.v().addBridge(new JsInterfaceBridge(possibleType, interfaceNameStr, context));
                                }
                            }
                        }
                    } catch (Exception e) {
                        Util.LOGGER.warning("error generating bridges for: " + m + u);
                        e.printStackTrace(Config.getLogPs());
                    }
                    unitid++;
                }
            }
        }
    }
}
