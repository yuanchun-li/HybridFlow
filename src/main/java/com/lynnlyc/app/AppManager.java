package com.lynnlyc.app;

import com.lynnlyc.Util;
import com.lynnlyc.bridge.*;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by yuanchun on 5/5/15.
 * Package: webview-flow
 */
public class AppManager {
    private boolean isPrepared = false;
    private PointsToAnalysis pta = null;
    private JSA jsa = null;

    private SootClass WebViewClass = null;
    private SootClass WebChromeClientClass = null;
    private SootClass WebViewClientClass = null;

    private SootMethod loadUrlMethod = null;
    private SootMethod addJavascriptInterfaceMethod = null;
    private SootMethod setWebViewClientMethod = null;
    private SootMethod setWebChromeClientMethod = null;

    private HashSet<SootClass> webviewClasses;
    public List<SootClass> originApplicationClasses;

    public static AppManager v() {
        if (appManager == null) {
            appManager = new AppManager();
        }
        return appManager;
    }

    private static AppManager appManager = null;
    private AppManager() {
        this.prepare();
    }

    // entry points of soot-based app analysis
    public List<SootMethod> appEntryPoints = new ArrayList<>();

    public void prepare() {
        Util.LOGGER.info("preparing app analysis");

        Scene.v().loadNecessaryClasses();

        originApplicationClasses = new ArrayList<>();
        for (SootClass cls : Scene.v().getApplicationClasses()) {
            originApplicationClasses.add(cls);
        }
        try {
            WebViewClass = Scene.v().getSootClass("android.webkit.WebView");
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find WebView class");
            Util.logException(e);
        }
        try {
            WebChromeClientClass = Scene.v().getSootClass("android.webkit.WebChromeClient");
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find WebChromeClient class");
            Util.logException(e);
        }
        try {
            WebViewClientClass = Scene.v().getSootClass("android.webkit.WebViewClient");
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find WebViewClient class");
            Util.logException(e);
        }
        try {
            loadUrlMethod = Scene.v().getMethod(Util.loadUrlSig);
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find loadUrl method");
            Util.logException(e);
        }
        try {
            addJavascriptInterfaceMethod = Scene.v().getMethod(Util.addJavascriptInterfaceSig);
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find addJavascriptInterface method");
            Util.logException(e);
        }
        try {
            setWebViewClientMethod = Scene.v().getMethod(Util.setWebViewClientSig);
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find setWebViewClient method");
            Util.logException(e);
        }
        try {
            setWebChromeClientMethod = Scene.v().getMethod(Util.setWebChromeClientSig);
        } catch (Exception e) {
            Util.LOGGER.warning("Can not find setWebChromeClient method");
            Util.logException(e);
        }

        // filter all support classes and R classes
        HashSet<SootClass> androidLibClasses = new HashSet<>();
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
        webviewClasses = new HashSet<>();
        for(SootClass cls : Scene.v().getApplicationClasses()) {
            if (Util.isSimilarClass(cls, WebViewClientClass)) {
                webviewClasses.add(cls);
                continue;
            }
            if (Util.isSimilarClass(cls, WebChromeClientClass)) {
                webviewClasses.add(cls);
                continue;
            }
            Iterator mi = cls.getMethods().iterator();
            boolean flag = false;
            while(mi.hasNext()) {
                SootMethod sm = (SootMethod) mi.next();
                if (sm.isConcrete()) {
                    Body body;
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

        HashSet<SootClass> notWebviewClasses = new HashSet<>();

        for(SootClass cls : Scene.v().getApplicationClasses()) {
            if(webviewClasses.contains(cls)) continue;
            notWebviewClasses.add(cls);
        }

        for(SootClass cls : notWebviewClasses) {
            cls.setLibraryClass();
        }

        appEntryPoints = Util.findEntryPoints();
        Scene.v().setEntryPoints(appEntryPoints);

//        VirtualWebview.v().setWebviewClasses(webviewClasses);

        this.isPrepared = true;
    }

    public Set<SootClass> getWebViewClasses() {
        return webviewClasses;
    }

    public void runPTA() {
        if (!this.isPrepared) {
            Util.LOGGER.warning("App not perpared");
            return;
        }
        Util.LOGGER.info("running PTA of app");
        try {
            PTA.runSparkPTA();
            this.pta = Scene.v().getPointsToAnalysis();
        }
        catch (Exception e) {
            Util.LOGGER.warning("Spark PTA failed");
            Util.logException(e);
        }
    }

    public void runJSA() {
        if (!this.isPrepared) {
            Util.LOGGER.log(Level.WARNING, "App not perpared");
            return;
        }
        Util.LOGGER.info("running JSA of app");

        this.jsa = new JSA();
        for (SootClass cls : webviewClasses) {
            for (SootMethod m : cls.getMethods()) {
                if (!m.isConcrete()) continue;
                Body b;
                if (m.hasActiveBody()) b = m.getActiveBody();
                else {
                    try {
                        b = m.retrieveActiveBody();
                    }
                    catch (Exception e) {
                        continue;
                    }
                }

                int unitid = 0;
                for (Unit u : b.getUnits()) {
                    try {
                        Stmt stmt = (Stmt) u;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr expr = stmt.getInvokeExpr();
                            SootMethod tgt = expr.getMethod();
                            if (Util.isSimilarMethod(tgt, loadUrlMethod)) {
                                ValueBox urlValue = expr.getArgBox(0);
                                BridgeContext context = new BridgeContext(m, u, unitid);
                                this.jsa.addHotspots(context, urlValue);
                            } else if (Util.isSimilarMethod(tgt, addJavascriptInterfaceMethod)) {
                                ValueBox interfaceNameValue = expr.getArgBox(1);
                                BridgeContext context = new BridgeContext(m, u, unitid);
                                this.jsa.addHotspots(context, interfaceNameValue);
                            }
                        }
                    } catch (Exception e) {
                        Util.LOGGER.warning("error generating hotspots for: " + m + u);
                        Util.logException(e);
                    }
                    unitid++;
                }
            }
        }

        try {
            this.jsa.run();
        }
        catch (Exception e) {
            Util.LOGGER.warning("JSA failed");
        }
    }

    public void dumpAllApplicationClasses(PrintStream os) {
        os.println("---Application Classes---");
        for (SootClass cls : Scene.v().getApplicationClasses()) {
            os.println(cls);
        }
        os.println("---end of Application Classes---");
    }

    public void generateBridges() {
        Util.LOGGER.info("generating webview bridge of app");

        for (SootClass cls : webviewClasses) {
//            if (Util.isSimilarClass(cls, WebChromeClientClass)) {
//                for (SootMethod m : cls.getMethods()) {
//                    if (m.getName().equals("onJsAlert")) {
//                        VirtualWebview.v().addBridge(new EventBridge("alert", m));
//                    }
//                    else if (m.getName().equals("onJsConfirm")) {
//                        VirtualWebview.v().addBridge(new EventBridge("confirm", m));
//                    }
//                    else if (m.getName().equals("onJsPrompt")) {
//                        VirtualWebview.v().addBridge(new EventBridge("prompt", m));
//                    }
//                    else if (m.getName().equals("onConsoleMessage")) {
//                        VirtualWebview.v().addBridge(new EventBridge("console", m));
//                    }
//                }
//            }
//            else if (Util.isSimilarClass(cls, WebViewClientClass)) {
//                for (SootMethod m : cls.getMethods()) {
//                    if (m.getName().equals("shouldOverrideUrlLoading")) {
//                        VirtualWebview.v().addBridge(new EventBridge("url", m));
//                    }
//                }
//            }

            for (SootMethod m : cls.getMethods()) {
                if (!m.isConcrete()) continue;
                Body b;
                if (m.hasActiveBody()) b = m.getActiveBody();
                else {
                    try {
                        b = m.retrieveActiveBody();
                    }
                    catch (Exception e) {
                        continue;
                    }
                }

                int unitid = 0;
                for (Unit u : b.getUnits()) {
                    try {
                        Stmt stmt = (Stmt) u;
                        if (stmt.containsInvokeExpr()) {
                            BridgeContext context = new BridgeContext(m, u, unitid);
                            InvokeExpr expr = stmt.getInvokeExpr();
                            SootMethod tgt = expr.getMethod();
                            if (Util.isSimilarMethod(tgt, loadUrlMethod)) {
                                VirtualWebview.v().addLoadUrlContext(context);
                                ValueBox urlValue = expr.getArgBox(0);
                                String urlStr = null;
                                if (this.jsa != null) {
                                    urlStr = this.jsa.getStringAnalysisResult(context);
                                }
                                if (urlStr == null) {
                                    urlStr = urlValue.getValue().toString();
                                    urlStr = Util.trimQuotation(urlStr);
                                }

                                if (urlStr.contains("javascript:")) {
                                    VirtualWebview.v().addBridge(new JavascriptBridge(context, urlStr));
                                } else {
                                    VirtualWebview.v().addBridge(new UrlBridge(context, urlStr));
                                }
                            } else if (Util.isSimilarMethod(tgt, addJavascriptInterfaceMethod)) {
                                ValueBox interfaceNameValue = expr.getArgBox(1);
                                String interfaceNameStr = null;

                                if (this.jsa != null) {
                                    interfaceNameStr = this.jsa.getStringAnalysisResult(context);
                                }
                                if (interfaceNameStr == null) {
                                    interfaceNameStr = interfaceNameValue.getValue().toString();
                                    interfaceNameStr = Util.trimQuotation(interfaceNameStr);
                                }

                                Value interfaceObj = expr.getArg(0);
                                HashSet<Type> possibleTypes = new HashSet<>();

                                if (this.pta != null) {
                                    PointsToSet interfaceClass = this.pta.reachingObjects((Local) interfaceObj);
                                    possibleTypes.addAll(interfaceClass.possibleTypes());
                                }
                                if (possibleTypes.isEmpty()) {
                                    possibleTypes.add(interfaceObj.getType());
                                }

                                for (Type possibleType : possibleTypes) {
                                    if (!(possibleType instanceof RefType))
                                        continue;
                                    VirtualWebview.v().addBridge(new JsInterfaceBridge(
                                            ((RefType) possibleType).getSootClass(), interfaceObj,
                                            interfaceNameStr, context));
                                }
                            } else if (Util.isSimilarMethod(tgt, setWebViewClientMethod) ||
                                    Util.isSimilarMethod(tgt, setWebChromeClientMethod)) {
                                Value webViewClientValue = expr.getArg(0);

                                HashSet<Type> possibleTypes = new HashSet<>();

                                if (this.pta == null) {
                                    possibleTypes.add(webViewClientValue.getType());
                                }
                                else {
                                    PointsToSet interfaceClass = this.pta.reachingObjects((Local) webViewClientValue);
                                    possibleTypes.addAll(interfaceClass.possibleTypes());
                                }
                                for (Type possibleType : possibleTypes) {
                                    if (!(possibleType instanceof RefType))
                                        continue;
                                    VirtualWebview.v().addBridge(new WebViewClientBridge(
                                            ((RefType) possibleType).getSootClass(), webViewClientValue, context));
                                }
                            }
                        }
                    } catch (Exception e) {
                        Util.LOGGER.warning(String.format("error generating bridges.\n[method] %s\n[unit] %s\n", m, u));
                        Util.logException(e);
                    }
                    unitid++;
                }
            }
        }
    }

    public void outputInstrumentedApp() {
        for (SootClass cls : originApplicationClasses) {
            cls.setApplicationClass();
        }
        try {
            PackManager.v().writeOutput();
        }
        catch (Exception e) {
            Util.LOGGER.warning("exception during outputing");
            e.printStackTrace();
        }
    }

    public boolean isHybridApp() {
        return  (AppManager.v().getWebViewClasses() != null && AppManager.v().getWebViewClasses().size() != 0);
    }
}
