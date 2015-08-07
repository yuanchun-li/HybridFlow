package com.lynnlyc.bridge;

import com.lynnlyc.Config;
import com.lynnlyc.Util;
import com.lynnlyc.app.AppManager;
import org.apache.commons.io.FileUtils;
import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
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
    public HashSet<SootMethod> loadUrlMethods;
    private HashSet<JsInterfaceBridge> jsInterfaceBridges;
    private HashSet<JavascriptBridge> javascriptBridges;
    private HashSet<UrlBridge> urlBridges;
    private HashSet<EventBridge> eventBridges;

    private VirtualWebview() {
        webviewClasses = new HashSet<>();
        loadUrlMethods = new HashSet<>();
        jsInterfaceBridges = new HashSet<>();
        javascriptBridges = new HashSet<>();
        urlBridges = new HashSet<>();
        eventBridges = new HashSet<>();
    }

    public void dump(PrintStream os) {
        os.println("======Virtual Webview Bridges======");
//        os.println("------    WebViewClasses     ------");
//        for (SootClass sootClass : this.webviewClasses) {
//            os.println(sootClass);
//        }

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

    // set parameters of a method as sources
    private void setJavaSourceMethod(SootMethod method) {
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
            Local base = getTaintedLocal(method.getDeclaringClass().getType());
            mockMainBody.getUnits().addLast(Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(base, method.makeRef(), paras)));
        }

    }

    private void setJavaSinkMethod(SootMethod method) {
        String line = String.format("%s -> _SINK__", method.getSignature());
        Config.javaSourcesAndSinks.add(line);
    }

    private SootClass webViewBridgeClass;
    private SootMethod mockMain;
    private JimpleBody mockMainBody;
    private SootMethod mockSource;
    private JimpleBody mockSourceBody;
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
        Local castedTaint = getNewLocal(t);

        mockMainBody.getUnits().addLast(
                Jimple.v().newAssignStmt(castedTaint,
                        Jimple.v().newCastExpr(taintedObject, t)));

        return castedTaint;
    }

    public void instrumentBridgeToApp() {
        Scene.v().loadClassAndSupport("java.lang.Object");
        objectClass = Scene.v().getSootClass("java.lang.Object");

        webViewBridgeClass = new SootClass(Config.projectName, Modifier.PUBLIC);
        webViewBridgeClass.setSuperclass(objectClass);
        List<Type> paras = new ArrayList<>();
        mockMain = new SootMethod("main", paras, VoidType.v(),
                Modifier.PUBLIC | Modifier.STATIC);
        mockSource = new SootMethod("mockSource", paras, objectClass.getType(),
                Modifier.PUBLIC | Modifier.STATIC);
        webViewBridgeClass.addMethod(mockMain);
        webViewBridgeClass.addMethod(mockSource);

        mockMainBody = Jimple.v().newBody(mockMain);
        mockMain.setActiveBody(mockMainBody);

        mockSourceBody = Jimple.v().newBody(mockSource);
        mockSource.setActiveBody(mockSourceBody);

        taintedObject = getNewLocal(objectClass.getType());
        mockMainBody.getUnits().addLast(
                Jimple.v().newAssignStmt(taintedObject,
                        Jimple.v().newStaticInvokeExpr(
                                mockSource.makeRef(), new ArrayList<Value>())));

        String line = String.format("%s -> _SOURCE__", mockSource.getSignature());
        Config.javaSourcesAndSinks.add(line);

        for (JsInterfaceBridge jsInterfaceBridge : jsInterfaceBridges) {
            for (SootMethod m : jsInterfaceBridge.interfaceClass.getMethods()) {
                if (!m.isPublic() || m.isConstructor() || m.isAbstract())
                    continue;
                setJavaSourceMethod(m);
            }
        }

        for (EventBridge eventBridge : eventBridges) {
            SootMethod target = eventBridge.eventTarget;
            setJavaSourceMethod(target);
        }

        for (JavascriptBridge javascriptBridge : javascriptBridges) {
            SootMethod invokedMethod = javascriptBridge.context.getInvokedMethod();
            if (invokedMethod == null) continue;
            setJavaSinkMethod(invokedMethod);
        }

        for (UrlBridge urlBridge : urlBridges) {
            SootMethod invokedMethod = urlBridge.context.getInvokedMethod();
            if (invokedMethod == null) continue;
            setJavaSinkMethod(invokedMethod);
        }

        // add invocation of mockMain to app, so that other java-side analysis can reach mockMain
        for (SootMethod m : loadUrlMethods)
            this.addMockMainToMethod(m);

        Scene.v().addClass(webViewBridgeClass);
        webViewBridgeClass.setApplicationClass();
    }

    private void addMockMainToMethod(SootMethod m) {
        if (!m.hasActiveBody()) return;
        Body b = m.getActiveBody();
        b.getUnits().addFirst(Jimple.v().newInvokeStmt(
                Jimple.v().newStaticInvokeExpr(mockMain.makeRef())));
    }

    public void dumpJavaSideResult() {
        AppManager.v().outputInstrumentedApp();
        File javaSourceAndSink = new File(Config.javaDirPath + "/SourcesAndSinks.txt");
        try {
            FileUtils.writeLines(javaSourceAndSink, Config.javaSourcesAndSinks);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void setWebviewClasses(HashSet<SootClass> webviewClasses) {
        this.webviewClasses = webviewClasses;
    }
}
