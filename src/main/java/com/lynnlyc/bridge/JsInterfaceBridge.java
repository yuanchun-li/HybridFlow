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
    public final SootClass interfaceClass;
    public final Value interfaceValue;
    public final String interfaceName;
    public final HashSet<SootMethod> interfaceMethods;
    private BridgeContext context;
    public JsInterfaceBridge(SootClass interfaceClass, Value interfaceValue, String interfaceName, BridgeContext context) {
        this.interfaceClass = interfaceClass;
        this.interfaceValue = interfaceValue;
        this.interfaceName = interfaceName;
        this.context = context;
        this.interfaceMethods = this.getAllInterfaceMethods();
    }

    public String toString() {
        String str = String.format("JsInterfaceBridge:\n[context]%s,\n[Type]%s,\n[name]%s,\n[methods]\n",
                this.context, this.interfaceClass, this.interfaceName);
        for (SootMethod m : this.interfaceMethods) {
            str += String.format("%s\n", m.getSignature());
        }
        SootField mockField = VirtualWebview.v().getMockField(this, interfaceValue, context);
        for (SootMethod m : this.interfaceMethods) {
            str += String.format("[bridgePath](J)(ARGS)%s --> (H)(RET)%s,%s\n",
                    VirtualWebview.v().getRetMock(m).getSignature(), this.interfaceName, m.getName());
            str += String.format("[bridgePath](H)(ARGS)%s,%s --> (J)(RET)%s\n",
                    this.interfaceName, m.getName(), VirtualWebview.v().getArgMock(m, mockField));
        }
        return str;
    }

    private HashSet<SootMethod> getAllInterfaceMethods() {
        HashSet<SootMethod> interfaceMethods = new HashSet<>();
        for (SootMethod m : this.interfaceClass.getMethods()) {
            if (!m.isPublic() || m.isConstructor() || m.isAbstract())
                continue;
            interfaceMethods.add(m);
        }
        return interfaceMethods;
    }

    @Override
    public void export2app() {
        SootField mockField = VirtualWebview.v().getMockField(this, interfaceValue, context);
        for (SootMethod m : interfaceMethods) {
            VirtualWebview.v().setJavaMethodArgsAsSource(m, mockField);
            VirtualWebview.v().setJavaMethodRetAsSink(m);
        }
    }

    @Override
    public void export2web() {
        for (SootMethod m : interfaceMethods) {
            VirtualWebview.v().setHTMLArgsAsSink(String.format("%s,%s",
                    this.interfaceName, m.getName()));
            VirtualWebview.v().setHTMLRetAsSource(String.format("%s,%s",
                    this.interfaceName, m.getName()));
        }
    }
}
