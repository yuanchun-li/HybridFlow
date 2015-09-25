package com.lynnlyc.bridge;

import com.lynnlyc.Config;
import com.lynnlyc.Util;
import com.lynnlyc.app.AppManager;
import org.apache.commons.io.FileUtils;
import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;

import javax.swing.plaf.synth.SynthEditorPaneUI;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    private HashSet<SootClass> webviewClasses;
    private HashSet<BridgeContext> loadUrlContexts;
    public void addLoadUrlContext(BridgeContext context) {
        loadUrlContexts.add(context);
    }

    private HashSet<Bridge> bridges;

//    private HashSet<JsInterfaceBridge> jsInterfaceBridges;
//    private HashSet<JavascriptBridge> javascriptBridges;
//    private HashSet<UrlBridge> urlBridges;
//    private HashSet<EventBridge> eventBridges;
//    private HashSet<WebViewClientBridge> webViewClientBridges;

    private VirtualWebview() {
        webviewClasses = new HashSet<>();
        loadUrlContexts = new HashSet<>();
        bridges = new HashSet<>();
        mockFields = new HashMap<>();
//        jsInterfaceBridges = new HashSet<>();
//        javascriptBridges = new HashSet<>();
//        urlBridges = new HashSet<>();
//        eventBridges = new HashSet<>();
//        webViewClientBridges = new HashSet<>();
    }

    public void dump(PrintStream os) {
//        os.println("======Virtual Webview Bridges======");
//        os.println("------    WebViewClasses     ------");
//        for (SootClass sootClass : this.webviewClasses) {
//            os.println(sootClass);
//        }
//        os.println("------   JsInterfaceBridges  ------");
//        for (JsInterfaceBridge jsInterfaceBridge : jsInterfaceBridges) {
//            os.println(jsInterfaceBridge);
//        }
//
//        os.println("------   JavascriptBridges   ------");
//        for (JavascriptBridge javascriptBridge : javascriptBridges) {
//            os.println(javascriptBridge);
//        }
//
//        os.println("------      UrlBridges       ------");
//        for (UrlBridge urlBridge : urlBridges) {
//            os.println(urlBridge);
//        }
//        os.println("-----    WebViewClientBridges   -----");
//        for (WebViewClientBridge webViewClientBridge : webViewClientBridges) {
//            os.println(webViewClientBridge);
//        }
//        os.println("------     EventBridges      ------");
//        for (EventBridge eventBridge : eventBridges) {
//            os.println(eventBridge);
//        }
//        os.println("======     End of Bridges    ======");
        for (Bridge bridge : bridges) {
            os.println(bridge);
        }
    }

    // set and get a local as a mock field
    public static int mockFieldId = 0;
    public HashMap<Bridge, SootField> mockFields;
    public SootField getMockField(Bridge bridge, Value v, BridgeContext context) {
        if (mockFields.containsKey(bridge)) return mockFields.get(bridge);

        SootField mockField = new SootField(String.format("mock_field_%d", mockFieldId++),
                v.getType(), Modifier.PUBLIC | Modifier.STATIC);
        webViewBridgeClass.addField(mockField);
        mockFields.put(bridge, mockField);

        context.method.getActiveBody().getUnits().insertAfter(
                Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(mockField.makeRef()), v),
                context.unit);

        return mockField;
    }

    // set parameters of a method as sources
    public void setJavaSourceMethod(SootMethod method, SootField baseField) {
        List<Type> para_types = method.getParameterTypes();
        List<Value> paras = new ArrayList<>();
        for (Type t : para_types) {
            paras.add(getTaintedLocal(t));
        }

        if (method.isStatic()) {
            mockMainBody.getUnits().addLast(Jimple.v().newInvokeStmt(
                    Jimple.v().newStaticInvokeExpr(method.makeRef(), paras)));
        }
        else {
            Local base = getNewLocal(baseField.getType());
            mockMainBody.getUnits().addLast(Jimple.v().newAssignStmt(
                    base, Jimple.v().newStaticFieldRef(baseField.makeRef())));
            mockMainBody.getUnits().addLast(Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(base, method.makeRef(), paras)));
        }
    }

    public void setJavaSinkMethod(SootMethod method) {
        String line = String.format("%s -> _SINK_", method.getSignature());
        Config.javaSourcesAndSinks.add(line);
    }

    private SootClass webViewBridgeClass;
    private SootMethod mockMain;
    private JimpleBody mockMainBody;
    private SootClass objectClass;
    private Local taintedObject;

    private static int localCount = 0;
    private Local getNewLocal(Type t) {
        String localName = "local_" + localCount++;
        Local local = Jimple.v().newLocal(localName, t);
        mockMainBody.getLocals().addLast(local);
        return local;
    }

    private Local getTaintedLocal(Type t) {
        return taintedObject;
//        Local castedTaint = getNewLocal(t);
//
//        mockMainBody.getUnits().addLast(
//                Jimple.v().newAssignStmt(castedTaint, taintedObject));
//
//        return castedTaint;
    }

    public void instrumentBridgeToApp() {
        Scene.v().loadClassAndSupport("java.lang.Object");
        objectClass = Scene.v().getSootClass("java.lang.Object");

//        Scene.v().loadClassAndSupport("android.app.Service");
//        SootClass serviceClass = Scene.v().getSootClass("android.app.Service");
//        SootClass bundleClass = Scene.v().getSootClass("android.os.Bundle");
        SootClass threadClass = Scene.v().getSootClass("java.lang.Thread");

        webViewBridgeClass = new SootClass(Config.projectName, Modifier.PUBLIC);
        webViewBridgeClass.setSuperclass(threadClass);

        List<Type> paras = new ArrayList<>();
        paras.add(threadClass.getType());
        mockMain = new SootMethod("main", paras, VoidType.v(), Modifier.PUBLIC|Modifier.STATIC);
        mockMainBody = Jimple.v().newBody(mockMain);
        mockMain.setActiveBody(mockMainBody);

        paras = new ArrayList<>();
        SootMethod mockSource = new SootMethod("mockSource", paras, objectClass.getType(),
                Modifier.PUBLIC | Modifier.STATIC);

        paras = new ArrayList<>();
        paras.add(objectClass.getType());
        SootMethod mockSink = new SootMethod("mockSink", paras, VoidType.v(),
                Modifier.PUBLIC | Modifier.STATIC);

        webViewBridgeClass.addMethod(mockMain);
        webViewBridgeClass.addMethod(mockSource);
        webViewBridgeClass.addMethod(mockSink);

        JimpleBody mockSourceBody = Jimple.v().newBody(mockSource);
        mockSourceBody.getUnits().addLast(
                Jimple.v().newReturnStmt(
                        Jimple.v().newLocal("taint", objectClass.getType())));
        mockSource.setActiveBody(mockSourceBody);

        JimpleBody mockSinkBody = Jimple.v().newBody(mockSink);
        mockSinkBody.getUnits().addLast(Jimple.v().newReturnVoidStmt());
        mockSink.setActiveBody(mockSinkBody);

        taintedObject = getNewLocal(objectClass.getType());
        mockMainBody.getUnits().addLast(
                Jimple.v().newAssignStmt(taintedObject,
                        Jimple.v().newStaticInvokeExpr(
                                mockSource.makeRef(), new ArrayList<Value>())));

        String line = String.format("%s -> _SOURCE_", mockSource.getSignature());
        Config.javaSourcesAndSinks.add(line);

//        SootMethod sinkExample = Scene.v().getMethod("<com.lynnlyc.webview.WebviewDemoInterface: void logInApp(java.lang.String)>");
//        setJavaSourceMethod(sinkExample);

        setJavaSinkMethod(mockSink);
        setJavaSourceMethod(mockSink, null);

        for (Bridge bridge : bridges) {
            bridge.export2app();
        }

        // add invocation of mockMain to app, so that other java-side analysis can reach mockMain
        for (BridgeContext c : loadUrlContexts)
            this.addMockMainToContext(c);

        Scene.v().addClass(webViewBridgeClass);
        webViewBridgeClass.setApplicationClass();
    }

    private void addMockMainToMethod(SootMethod m) {
        if (!m.hasActiveBody()) return;
        Body b = m.getActiveBody();
        b.getUnits().addFirst(Jimple.v().newInvokeStmt(
                Jimple.v().newStaticInvokeExpr(mockMain.makeRef())));
    }

    private void addMockMainToContext(BridgeContext c) {
        if (c == null || c.method == null) return;

        Body b = c.method.getActiveBody();
        b.getUnits().insertAfter(Jimple.v().newInvokeStmt(
                Jimple.v().newStaticInvokeExpr(mockMain.makeRef())), c.unit);
    }

    public void generateJavaSideResult() {
        this.instrumentBridgeToApp();
        AppManager.v().outputInstrumentedApp();
        File javaSourceAndSink = new File(Config.javaDirPath + "/SourcesAndSinks.txt");
        try {
            FileUtils.writeLines(javaSourceAndSink, Config.javaSourcesAndSinks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashSet<String> possibleURLs = new HashSet<>();
    public void addPossibleURL(String url_str) {
        try {
            URL url = new URL(url_str);
            possibleURLs.add(url.toString());
        } catch (MalformedURLException e) {
            Util.LOGGER.warning("incorrect URL: " + url_str);
        }
    }

    public void addHTMLsource(String source) {
        String line = String.format("HTML <%s> -> _SOURCE_", source);
        Config.htmlSourcesAndSinks.add(line);
    }

    public void addHTMLsink(String sink) {
        String line = String.format("HTML <%s> -> _SINK_", sink);
        Config.htmlSourcesAndSinks.add(line);
    }

    public void generateHTMLSideResult() {
        for (Bridge bridge : bridges) {
            bridge.export2web();
        }
        File possibleURLsFile = new File(Config.htmlDirPath + "/possibleURLs.txt");
        File htmlSourceAndSink = new File(Config.htmlDirPath + "/SourcesAndSinks.txt");
        try {
            FileUtils.writeLines(possibleURLsFile, possibleURLs);
            FileUtils.writeLines(htmlSourceAndSink, Config.htmlSourcesAndSinks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addBridge(Bridge bridge) {
        this.bridges.add(bridge);
    }

    public void setWebviewClasses(HashSet<SootClass> webviewClasses) {
        this.webviewClasses = webviewClasses;
    }
}
