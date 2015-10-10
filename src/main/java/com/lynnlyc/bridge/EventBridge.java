package com.lynnlyc.bridge;

import soot.SootMethod;

/**
 * Created by yuanchun on 5/4/15.
 * Package: webview-
 * Deprecated!
 */
public class EventBridge extends Bridge {
    public final String eventType;
    public final SootMethod eventTarget;

    public EventBridge(String eventType, SootMethod eventTarget) {
        this.eventType = eventType;
        this.eventTarget = eventTarget;
    }

    public String toString() {
        return String.format("EventBridge:\n[eventType]%s,\n[eventTargat]%s\n[bridgePath](H)(ARGS)window,%s --> (J)(RET)%s\n",
                this.eventType, this.eventTarget, this.eventType, VirtualWebview.v().getArgMock(eventTarget, null));
    }

    @Override
    public void export2app() {
        VirtualWebview.v().setJavaMethodArgsAsSource(eventTarget, null);
    }

    @Override
    public void export2web() {
        VirtualWebview.v().setHTMLArgsAsSink(String.format("window,%s", this.eventType));
    }
}
