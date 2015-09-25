package com.lynnlyc.bridge;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Value;

import java.util.HashSet;

/**
 * Created by yuanchun on 5/6/15.
 * Package: webview-flow
 */
public class JsInterfaceBridge extends Bridge {
    public SootClass interfaceClass;
    public Value interfaceValue;
    public String interfaceName;
    public HashSet<SootMethod> interfaceMethods;
    private BridgeContext context;
    public JsInterfaceBridge(SootClass interfaceClass, Value interfaceValue, String interfaceName, BridgeContext context) {
        this.interfaceClass = interfaceClass;
        this.interfaceValue = interfaceValue;
        this.interfaceName = interfaceName;
        this.context = context;
        this.getAllInterfaceMethods();
    }

    public String toString() {
        String str = String.format("JsInterfaceBridge:\n[context]%s,\n[Type]%s,\n[name]%s,\n[methods]\n",
                this.context, this.interfaceClass, this.interfaceName);
        for (SootMethod m : this.interfaceMethods) {
            str += String.format("%s\n", m.getSignature());
        }
        return str;
    }

    private void getAllInterfaceMethods() {
        interfaceMethods = new HashSet<>();
        for (SootMethod m : this.interfaceClass.getMethods()) {
            if (!m.isPublic() || m.isConstructor() || m.isAbstract())
                continue;
            interfaceMethods.add(m);
        }
    }

    @Override
    public void export2app() {
        SootField mockField = VirtualWebview.v().getMockField(this, interfaceValue, context);
        for (SootMethod m : interfaceMethods) {
            VirtualWebview.v().setJavaSourceMethod(m, mockField);
        }
    }

    @Override
    public void export2web() {
        for (SootMethod m : interfaceMethods) {
            VirtualWebview.v().addHTMLsink(String.format("ARGS %s.%s",
                    this.interfaceName, m.getName()));
            VirtualWebview.v().addHTMLsource(String.format("RET %s.%s",
                    this.interfaceName, m.getName()));
        }
    }
}
