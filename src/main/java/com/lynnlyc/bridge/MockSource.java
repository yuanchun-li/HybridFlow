package com.lynnlyc.bridge;

import soot.SootMethod;

/**
 * Created by liyc on 9/24/15.
 * mock source method
 */
public class MockSource {
    public SootMethod sourceMethod;
    public int sourceId;

    public static int sourceCount = 0;
    public MockSource(SootMethod sourceMethod) {
        this.sourceMethod = sourceMethod;
        this.sourceId = sourceCount++;
    }
}
