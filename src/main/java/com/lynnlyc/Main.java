package com.lynnlyc;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.lynnlyc.app.AppManager;
import com.lynnlyc.app.FlowDroidCaller;
import com.lynnlyc.bridge.VirtualWebview;
import com.lynnlyc.merger.Merger;
import com.lynnlyc.web.HTMLTaintAnalysisCaller;
import com.lynnlyc.web.WebManager;
import org.apache.commons.io.FileUtils;
import soot.G;
import soot.PackManager;
import soot.Singletons;

public class Main {
	public static void main(String[] args) {
		if (!Config.parseArgs(args)) {
            return;
        }

        buildBridge();
        runTaintAnalysis();
	}

    public static void buildBridge() {
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
    }

    public static void runTaintAnalysis() {
        String javaTargetDir = Config.outputDirPath + "/java";
        String htmlTargetDir = Config.outputDirPath + "/html";

        FlowDroidCaller.v().run(javaTargetDir);
        HTMLTaintAnalysisCaller.v().run(htmlTargetDir);

        File outputFile = FileUtils.getFile(Config.outputDirPath, "AnalysisResult.txt");
        try {
            Merger.v().merge(Config.outputDirPath, new PrintStream(new FileOutputStream(outputFile)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
