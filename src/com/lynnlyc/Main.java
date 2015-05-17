package com.lynnlyc;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.lynnlyc.app.AppManager;
import com.lynnlyc.app.JSA;
import com.lynnlyc.app.PTA;
import dk.brics.string.StringAnalysis;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class Main {
	public static void main(String[] args) {

		if (!Config.parseArgs(args)) {
            Util.printUsage();
            return;
        }

        Config.init();

        AppManager appManager = new AppManager(Config.appFilePath);
//		appManager.runPTA();
        appManager.runJSA();
        JSA.dumpJSAresults(System.out);
////        Util.output();

//        try {
//            jsaTest();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

	}

    public static void jsaTest() throws IOException {
        JSA.init();
        StringAnalysis.loadClass("com.lynnlyc.pta_android_sample.MainActivity");
        StringAnalysis jsa = new StringAnalysis();
        JSA.dumpJSAresults(System.out);
    }
}
