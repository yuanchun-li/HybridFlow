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

        AppManager appManager = AppManager.v();
//      appManager.dumpAllApplicationClasses(os);

        appManager.runPTA();
//        appManager.runJSA();

//        JSA.dumpJSAresults(os);

        appManager.generateBridges();
//        JSA.dumpJSAresults(os);
        //        Util.output();
        VirtualWebview.v().dump(Config.getBridgePs());
        VirtualWebview.v().instrumentBridgeToApp();

        VirtualWebview.v().dumpJavaSideResult();
	}
}
