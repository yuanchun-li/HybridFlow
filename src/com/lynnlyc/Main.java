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

        if (Config.runPTA)
            appManager.runPTA();
        if (Config.runJSA)
            appManager.runJSA();

        appManager.generateBridges();

        VirtualWebview.v().dump(Config.getBridgePs());
        VirtualWebview.v().instrumentBridgeToApp();
        VirtualWebview.v().generateHTMLSideResult();

        VirtualWebview.v().dumpJavaSideResult();
        VirtualWebview.v().dumpHTMLSideResult();
	}
}
