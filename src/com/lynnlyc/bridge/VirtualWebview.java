package com.lynnlyc.bridge;

import com.lynnlyc.Config;
import com.lynnlyc.Util;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.internal.*;

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
    private HashSet<JsInterfaceBridge> jsInterfaceBridges;
    private HashSet<JavascriptBridge> javascriptBridges;
    private HashSet<UrlBridge> urlBridges;
    private HashSet<EventBridge> eventBridges;

    private VirtualWebview() {
        webviewClasses = new HashSet<SootClass>();
        jsInterfaceBridges = new HashSet<JsInterfaceBridge>();
        javascriptBridges = new HashSet<JavascriptBridge>();
        urlBridges = new HashSet<UrlBridge>();
        eventBridges = new HashSet<EventBridge>();
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
    private void setSourceMethod(SootMethod method) {
        List<Type> para_types = method.getParameterTypes();
        List<Value> paras = new ArrayList<>();
        for (Type t : para_types) {
            paras.add(getTaintedValue(t));
        }

        if (method.isStatic()) {
            mockMainBody.getUnits().addLast(Jimple.v().newInvokeStmt(
                    Jimple.v().newStaticInvokeExpr(method.makeRef(), paras)));
        }
        else {
            Value base = getTaintedValue(method.getDeclaringClass().getType());
            Local baseLocal = getNewLocal(base);
            mockMainBody.getUnits().addLast(Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(baseLocal, method.makeRef(), paras)));
        }

    }

    private SootClass webViewBridgeClass;
    private SootMethod mockMain;
    private JimpleBody mockMainBody;
    private SootMethod mockSource;

    private static int localCount = 0;
    private Local getNewLocal(Value rvalue) {
        String localName = "local_" + localCount++;
        JimpleLocal local = new JimpleLocal(localName, rvalue.getType());
        mockMainBody.getLocals().addLast(local);
        mockMainBody.getUnits().addLast(Jimple.v().newAssignStmt(local, rvalue));
        return local;
    }

    private Value getTaintedValue(Type t) {
        List<Value> sourcePara = new ArrayList<>();
        JStaticInvokeExpr sourceExpr = new JStaticInvokeExpr(mockSource.makeRef(), sourcePara);
        return new JCastExpr(sourceExpr, t);
    }

    public void addBridgeToApp() {
//        File hybridAppFile = new File(Config.appFilePath);
//        File javaAppFile = new File(Config.javaDirPath + "/javaSide.apk");
//        try {
//            FileUtils.copyFile(hybridAppFile, javaAppFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        webViewBridgeClass = new SootClass(Config.projectName, Modifier.PUBLIC);
        webViewBridgeClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        ArrayList<Type> paras = new ArrayList<>();
        mockMain = new SootMethod("main", paras, VoidType.v(),
                Modifier.PUBLIC | Modifier.STATIC);
        SootClass objectClass = Scene.v().getSootClass("java.lang.Object");
        mockSource = new SootMethod("mockSource", paras, objectClass.getType(),
                Modifier.PUBLIC | Modifier.STATIC);
        webViewBridgeClass.addMethod(mockMain);
        webViewBridgeClass.addMethod(mockSource);

        mockMainBody = Jimple.v().newBody(mockMain);
        mockMain.setActiveBody(mockMainBody);

        for (JsInterfaceBridge jsInterfaceBridge : jsInterfaceBridges) {
            for (SootMethod m : jsInterfaceBridge.interfaceClass.getMethods()) {
                if (!m.isPublic() || m.isConstructor() || m.isAbstract())
                    continue;
                String line = String.format("%s -> _SOURCE__", m.getSignature());
                Config.javaSourceAndSinks.add(line);
            }
        }

        for (EventBridge eventBridge : eventBridges) {
            SootMethod target = eventBridge.eventTarget;
        }

        for (JavascriptBridge javascriptBridge : javascriptBridges) {
            SootMethod invokedMethod = javascriptBridge.context.getInvokedMethod();
            if (invokedMethod == null) continue;
            String line = String.format("%s -> _SINK__", invokedMethod.getSignature());
            Config.javaSourceAndSinks.add(line);
        }

        for (UrlBridge urlBridge : urlBridges) {
            SootMethod invokedMethod = urlBridge.context.getInvokedMethod();
            if (invokedMethod == null) continue;
            String line = String.format("%s -> _SINK__", invokedMethod.getSignature());
            Config.javaSourceAndSinks.add(line);
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
