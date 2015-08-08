package com.lynnlyc.bridge;

import soot.SootMethod;

/**
 * Created by yuanchun on 5/4/15.
 * Package: webview-flow
 */
public class EventBridge extends Bridge {
    public String eventType;
    public SootMethod eventTarget;

    public EventBridge(String eventType, SootMethod eventTarget) {
        this.eventType = eventType;
        this.eventTarget = eventTarget;
    }

    public String toString() {
        return String.format("EventBridge:\n[eventType]%s,\n[eventTargat]%s\n", this.eventType, this.eventTarget);
    }
}
