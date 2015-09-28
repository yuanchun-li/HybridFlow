package com.lynnlyc;
import java.io.PrintStream;

import com.lynnlyc.app.AppManager;
import com.lynnlyc.bridge.VirtualWebview;
import com.lynnlyc.web.WebManager;
import soot.G;
import soot.PackManager;
import soot.Singletons;

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
        if (Config.runJSA) {
            appManager.runJSA();
            G.reset();
            Config.reinit();
            appManager.prepare();
        }

        appManager.generateBridges();

        VirtualWebview.v().dump(Config.getBridgePs());
        VirtualWebview.v().generateJavaSideResult();
        VirtualWebview.v().generateHTMLSideResult();

        WebManager.v().setSourceSinks(Config.htmlSourcesAndSinks);
        WebManager.v().runTaintAnalysis();
	}
}
