package com.lynnlyc.bridge;

import soot.SootMethod;

/**
 * Created by liyc on 9/24/15.
 */
public class MockSink {
    public SootMethod sinkMethod;
    public int sinkId;

    public static int sinkCount = 0;
    public MockSink(SootMethod sourceMethod) {
        this.sinkMethod = sourceMethod;
        this.sinkId = sinkCount++;
    }
}
