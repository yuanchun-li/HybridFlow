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
        for (SootMethod m : clientMethods) {
            String mName = m.getName();
            String tags = this.getHTMLAPIByName(mName);
            if (mName.equals("shouldOverrideUrlLoading"))
                str += String.format("[bridgePath](H)(PUT)%s --> (J)(ARGS)%s\n", tags, m.getSignature());
            else
                str += String.format("[bridgePath](H)(ARGS)%s --> (J)(ARGS)%s\n", tags, m.getSignature());

        }
        return str;
    }

    @Override
    public void export2app() {
        SootField mockField = VirtualWebview.v().getMockField(this, clientValue, context);
        for (SootMethod m : clientMethods) {
            VirtualWebview.v().setJavaMethodArgsAsSource(m, mockField);
        }
    }

    @Override
    public void export2web() {
        for (SootMethod m : clientMethods) {
            String mName = m.getName();
            String tags = this.getHTMLAPIByName(mName);
            if (mName.equals("shouldOverrideUrlLoading"))
                VirtualWebview.v().setHTMLFieldPutAsSink(tags);
            else
                VirtualWebview.v().setHTMLArgsAsSink(tags);
        }
    }

    private String getHTMLAPIByName(String mName) {
        String html_api = "";
        switch (mName) {
            case "onJsAlert":
                html_api = "window,alert";
                break;
            case "onJsConfirm":
                html_api = "window,confirm";
                break;
            case "onJsPrompt":
                html_api = "window,prompt";
                break;
            case "onConsoleMessage":
                html_api = "window,console";
                break;
            case "shouldOverrideUrlLoading":
                html_api = "window,location,href";
                break;
        }
        return html_api;
    }
}
