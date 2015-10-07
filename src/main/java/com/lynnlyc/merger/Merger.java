package com.lynnlyc.merger;

import java.io.PrintStream;

/**
 * Created by liyc on 10/8/15.
 */
public class Merger {
    private static Merger merger;
    private Merger() {
        merger = this;
    }
    public static Merger v() {
        if (merger == null)
            merger = new Merger();
        return merger;
    }

    public void merge(String targetDir, PrintStream ps) {
        ps.println("merged source to sink paths:");
    }
}
