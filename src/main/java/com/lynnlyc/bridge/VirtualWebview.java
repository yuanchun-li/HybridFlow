package com.lynnlyc.bridge;

import com.lynnlyc.Config;
import com.lynnlyc.Util;
import com.lynnlyc.app.AppManager;
import org.apache.commons.io.FileUtils;
import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JReturnStmt;
import soot.toolkits.scalar.Pair;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by yuanchun on 5/4/15.
 * webview based hybrid bridges
 */
public class VirtualWebview {
    private static VirtualWebview virtualWebview;
    public static VirtualWebview v() {
        if (virtualWebview == null) {
            virtualWebview = new VirtualWebview();
        }
        return virtualWebview;
    }

//    private HashSet<SootClass> webviewClasses;
    private HashSet<BridgeContext> loadUrlContexts;
    public void addLoadUrlContext(BridgeContext context) {
        loadUrlContexts.add(context);
    }

    private HashSet<Bridge> bridges;

    private VirtualWebview() {
//        webviewClasses = new HashSet<>();
        loadUrlContexts = new HashSet<>();
        bridges = new HashSet<>();
        mockFields = new HashMap<>();
        argMocks = new HashMap<>();
        retMocks = new HashMap<>();
    }

    public void dump(PrintStream os) {
        for (Bridge bridge : bridges) {
            os.println(bridge);
        }
    }

    // set and get a local as a mock field
    private static int mockFieldId = 0;
    private HashMap<Bridge, SootField> mockFields;
    public SootField getMockField(Bridge bridge, Value v, BridgeContext context) {
        if (mockFields.containsKey(bridge)) return mockFields.get(bridge);

        SootField mockField = new SootField(String.format("mockField%d", mockFieldId++),
                v.getType(), Modifier.PUBLIC | Modifier.STATIC);
        webViewBridgeClass.addField(mockField);
        mockFields.put(bridge, mockField);

        context.method.getActiveBody().getUnits().insertAfter(
                Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(mockField.makeRef()), v),
                context.unit);

        return mockField;
    }

    // set parameters of a method as sources
    private HashMap<Pair<SootMethod, SootField>, SootMethod> argMocks;
    public SootMethod getArgMock(SootMethod m, SootField base) {
        Pair<SootMethod, SootField> mockKey = new Pair<>(m, base);
        if (argMocks.containsKey(mockKey)) return argMocks.get(mockKey);
        SootMethod argMock = this.createMockSource(m.getName());
        argMocks.put(mockKey, argMock);
        return argMock;
    }
    public void setJavaMethodArgsAsSource(SootMethod method, SootField baseField) {
        SootMethod argMock = this.getArgMock(method, baseField);
//        Local taintedLocal = newLocalTaintedByMethod(objectClass.getType(), argMock);

        List<Type> para_types = method.getParameterTypes();
        List<Value> paras = new ArrayList<>();
        for (Type t : para_types) {
            paras.add(newLocalTaintedByMethod(t, argMock));
        }

        if (method.isStatic()) {
            mockMainBody.getUnits().addLast(Jimple.v().newInvokeStmt(
                    Jimple.v().newStaticInvokeExpr(method.makeRef(), paras)));
        }
        else {
            Local base = newLocal(baseField.getType());
            mockMainBody.getUnits().addLast(Jimple.v().newAssignStmt(
                    base, Jimple.v().newStaticFieldRef(baseField.makeRef())));
            mockMainBody.getUnits().addLast(Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(base, method.makeRef(), paras)));
        }
    }

    public void setJavaMethodRetAsSource(SootMethod method) {
        String line = String.format("%s -> _SOURCE_", method.getSignature());
        Config.javaSourcesAndSinks.add(line);
    }

    public void setJavaMethodArgsAsSink(SootMethod method) {
        String line = String.format("%s -> _SINK_", method.getSignature());
        Config.javaSourcesAndSinks.add(line);
    }

    private HashMap<SootMethod, SootMethod> retMocks;
    public SootMethod getRetMock(SootMethod mockKey) {
        if (retMocks.containsKey(mockKey)) return retMocks.get(mockKey);
        SootMethod retMock = this.createMockSink(mockKey.getName());
        retMocks.put(mockKey, retMock);
        return retMock;
    }
    public void setJavaMethodRetAsSink(SootMethod method) {
        if (!method.hasActiveBody()) {
            return;
        }

        SootMethod retMock = this.getRetMock(method);

        Set<Pair<Unit, Value>> mockSinkSites = new HashSet<>();
        Body b = method.getActiveBody();
        for (Unit u : b.getUnits()) {
            if (u instanceof JReturnStmt) {
                JReturnStmt retStmt = (JReturnStmt) u;
                for (ValueBox retValueBox : retStmt.getUseBoxes()) {
                    Value retValue = retValueBox.getValue();
                    mockSinkSites.add(new Pair<>(u, retValue));
                }
            }
        }

        for (Pair<Unit, Value> mockSinkSite : mockSinkSites) {
            Unit u = mockSinkSite.getO1();
            Value v = mockSinkSite.getO2();
            List<Value> paras = new ArrayList<>();
            paras.add(v);
            Unit mockSinkStmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(retMock.makeRef(), paras));
            b.getUnits().insertAfter(mockSinkStmt, u);
        }
    }

    private SootClass webViewBridgeClass;
    private SootMethod mockMain;
    private JimpleBody mockMainBody;
    private SootClass objectClass;
//    private Local taintedObject;

    private static int localCount = 0;
    private Local newLocal(Type t) {
        String localName = "local_" + localCount++;
        Local local = Jimple.v().newLocal(localName, t);
        mockMainBody.getLocals().addLast(local);
        return local;
    }

//    private Local getTaintedLocal(Type t) {
//        return taintedObject;
//    }

    private Local newLocalTaintedByMethod(Type t, SootMethod mockSource) {
        Local taintedLocal = newLocal(t);
        mockMainBody.getUnits().addLast(
                Jimple.v().newAssignStmt(taintedLocal,
                        Jimple.v().newStaticInvokeExpr(
                                mockSource.makeRef(), new ArrayList<Value>())));
        return taintedLocal;
    }

    private static int mockMethodId = 0;
    // create a mock method whose argument is SINK
    private SootMethod createMockSink(String name) {
        name = String.format("mockSink%d_%s", mockMethodId++, name);
        List<Type> paras = new ArrayList<>();
        paras.add(objectClass.getType());
        SootMethod m = new SootMethod(name, paras, VoidType.v(),
                Modifier.PUBLIC | Modifier.STATIC);
        webViewBridgeClass.addMethod(m);
        JimpleBody mockSinkBody = Jimple.v().newBody(m);
        mockSinkBody.getUnits().addLast(Jimple.v().newReturnVoidStmt());
        m.setActiveBody(mockSinkBody);
        this.setJavaMethodArgsAsSink(m);
        return m;
    }

    // create a mock method whose ret is SOURCE
    private SootMethod createMockSource(String name) {
        name = String.format("mockSource%d_%s", mockMethodId++, name);
        List<Type> paras = new ArrayList<>();
        SootMethod m = new SootMethod(name, paras, objectClass.getType(),
                Modifier.PUBLIC | Modifier.STATIC);
        webViewBridgeClass.addMethod(m);
        JimpleBody mockSourceBody = Jimple.v().newBody(m);
        mockSourceBody.getUnits().addLast(
                Jimple.v().newReturnStmt(
                        Jimple.v().newLocal("taint", objectClass.getType())));
        m.setActiveBody(mockSourceBody);
        this.setJavaMethodRetAsSource(m);
        return m;
    }

    public void instrumentBridgeToApp() {
        Scene.v().loadClassAndSupport("java.lang.Object");
        objectClass = Scene.v().getSootClass("java.lang.Object");

        SootClass threadClass = Scene.v().getSootClass("java.lang.Thread");

        webViewBridgeClass = new SootClass(Config.projectName, Modifier.PUBLIC);
        webViewBridgeClass.setSuperclass(threadClass);

        List<Type> parasTypes = new ArrayList<>();
        parasTypes.add(threadClass.getType());
        mockMain = new SootMethod("main", parasTypes, VoidType.v(), Modifier.PUBLIC|Modifier.STATIC);
        mockMainBody = Jimple.v().newBody(mockMain);
        mockMain.setActiveBody(mockMainBody);
        webViewBridgeClass.addMethod(mockMain);

        // a demo mockSource --> mockSink flow
        SootMethod mockSource = this.createMockSource("source");
        SootMethod mockSink = this.createMockSink("sink");

        Local taintedObject = newLocalTaintedByMethod(objectClass.getType(), mockSource);
        List<Value> paraValues = new ArrayList<>();
        paraValues.add(taintedObject);
        mockMainBody.getUnits().addLast(Jimple.v().newInvokeStmt(
                Jimple.v().newStaticInvokeExpr(mockSink.makeRef(), paraValues)));

//        this.setJavaMethodRetAsSink(mockSource);
//        this.setJavaMethodArgsAsSource(mockSink, null);

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

    private HashSet<String> possibleURLs = new HashSet<>();
    public void addPossibleURL(String url_str) {
        try {
            URL url = new URL(url_str);
            possibleURLs.add(url.toString());
        } catch (MalformedURLException e) {
            Util.LOGGER.warning("incorrect URL: " + url_str);
        }
    }

    public void setHTMLArgsAsSource(String tags) {
        setHTMLSourceSink(tags, "ARGS", true);
    }

    public void setHTMLArgsAsSink(String tags) {
        setHTMLSourceSink(tags, "ARGS", false);
    }

    public void setHTMLRetAsSource(String tags) {
        setHTMLSourceSink(tags, "RET", true);
    }

    public void setHTMLRetAsSink(String tags) {
        setHTMLSourceSink(tags, "RET", false);
    }

    public void setJSCodeAsSource(String tags) {
        setHTMLSourceSink(tags, "CODE", true);
    }

    public void setHTMLFieldGetAsSource(String tags) {
        setHTMLSourceSink(tags, "GET", true);
    }

    public void setHTMLFieldPutAsSink(String tags) {
        setHTMLSourceSink(tags, "PUT", false);
    }

    private void setHTMLSourceSink(String tags, String typeTag, boolean isSource) {
        String line = String.format("HTML <(%s) %s> -> _%s_", typeTag, tags, isSource?"SOURCE":"SINK");
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

//    public void setWebviewClasses(HashSet<SootClass> webviewClasses) {
//        this.webviewClasses = webviewClasses;
//    }
}
