package com.lynnlyc;
import java.io.PrintStream;

import com.lynnlyc.app.AppManager;
import com.lynnlyc.bridge.VirtualWebview;

public class Main {
	public static void main(String[] args) {
        PrintStream os = System.out;

		if (!Config.parseArgs(args)) {
            return;
        }

        Config.init();

        AppManager appManager = new AppManager(Config.appFilePath);
        appManager.dumpAllApplicationClasses(os);

		appManager.runPTA();
        appManager.runJSA();

//        JSA.dumpJSAresults(os);

        appManager.generateBridges();

//        JSA.dumpJSAresults(os);
////        Util.output();
        VirtualWebview.v().dump(Config.getBridgePs());

	}
}
