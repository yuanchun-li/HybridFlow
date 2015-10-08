package com.lynnlyc;

public class Main {
	public static void main(String[] args) {
		if (!Config.parseArgs(args)) {
            return;
        }

        Util.buildBridge();
        Util.runTaintAnalysis();
        Util.mergeTaintFlow();
	}

}
