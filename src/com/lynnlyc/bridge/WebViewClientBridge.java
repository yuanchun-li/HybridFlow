package com.lynnlyc.bridge;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Value;

import java.util.HashSet;

/**
 * Created by liyc on 9/24/15.
 */
public class WebViewClientBridge extends Bridge {
    public SootClass clientClass;
    public Value clientValue;
    private BridgeContext context;
    public HashSet<SootMethod> clientMethods;

    public WebViewClientBridge(SootClass clientClass, Value clientValue, BridgeContext context) {
        this.clientClass = clientClass;
        this.clientValue = clientValue;
        this.context = context;
        this.getClientMethods();
    }

    private void getClientMethods() {
        clientMethods = new HashSet<>();
        for (SootMethod m : clientClass.getMethods()) {
            String m_name = m.getName();
            if (m_name.equals("onJsAlert") || m_name.equals("onJsConfirm")
                    || m_name.equals("onJsPrompt") || m_name.equals("onConsoleMessage")
                    || m_name.equals("shouldOverrideUrlLoading")) {
                clientMethods.add(m);
            }
        }
    }

    public String toString() {
        String str = String.format("WebViewClientBridge:\n[context]%s,\n[ClientClass]%s,\n[ClientMethods]\n",
                this.context, this.clientClass);
        for (SootMethod m : clientMethods) {
            str += String.format("%s\n", m.getSignature());
        }
        return str;
    }

    @Override
    public void export2app() {
        SootField mockField = VirtualWebview.v().getMockField(this, clientValue, context);
        for (SootMethod m : clientMethods) {
            VirtualWebview.v().setJavaSourceMethod(m, mockField);
        }
    }

    @Override
    public void export2web() {
        for (SootMethod m : clientMethods) {
            String m_name = m.getName();
            String html_api = "";
            switch (m_name) {
                case "onJsAlert":
                    html_api = "alert";
                    break;
                case "onJsConfirm":
                    html_api = "confirm";
                    break;
                case "onJsPrompt":
                    html_api = "prompt";
                    break;
                case "onConsoleMessage":
                    html_api = "console";
                    break;
                case "shouldOverrideUrlLoading":
                    html_api = "load";
                    break;
            }
            VirtualWebview.v().addHTMLsink(String.format("ARGS window.%s", html_api));
        }
    }
}
