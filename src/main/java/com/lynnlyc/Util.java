package com.lynnlyc;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import com.lynnlyc.app.AppManager;
import com.lynnlyc.app.FlowDroidCaller;
import com.lynnlyc.bridge.VirtualWebview;
import com.lynnlyc.HybridFlow.Merger;
import com.lynnlyc.web.HTMLTaintAnalysisCaller;
import org.apache.commons.io.FileUtils;
import soot.*;

public class Util {
    public static final Logger LOGGER = Logger.getLogger("Webview-flow");

	public static final String loadUrlSig =
			"<android.webkit.WebView: void loadUrl(java.lang.String)>";
	public static final String addJavascriptInterfaceSig =
			"<android.webkit.WebView: void addJavascriptInterface(java.lang.Object,java.lang.String)>";
	public static final String setWebViewClientSig =
			"<android.webkit.WebView: void setWebViewClient(android.webkit.WebViewClient)>";
    public static final String setWebChromeClientSig =
            "<android.webkit.WebView: void setWebChromeClient(android.webkit.WebChromeClient)>";

	public static List<SootMethod> findEntryPoints() {
		ArrayList<SootMethod> entries = new ArrayList<>();
		for (SootClass cls : Scene.v().getApplicationClasses()) {
			if (cls.isAbstract()) continue;

			for (SootMethod m : cls.getMethods()) {
				entries.add(m);
			}
		}
//		System.out.println(entries.size());
//		System.out.println(entries);
		return entries;
	}

    public static String getTimeString() {
        long timeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-hhmmss");
        Date date = new Date(timeMillis);
        return sdf.format(date);
    }

	public static void logException(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		Util.LOGGER.warning(sw.toString());
	}

	public static boolean isSimilarClass(SootClass c1, SootClass c2) {
		if (c1 == null || c2 == null) {
			return false;
		}
		if (c1 == c2) {
			return true;
		}
		while (c1.hasSuperclass()) {
			c1 = c1.getSuperclass();
			if (c1 == c2) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSimilarMethod(SootMethod m1, SootMethod m2) {
		if (m1 == null || m2 == null) {
			return false;
		}
		if (m1 == m2) {
			return true;
		}
		if ((m1.getSubSignature().equals(m2.getSubSignature())) && Util.isSimilarClass(m1.getDeclaringClass(), m2.getDeclaringClass())) {
			return true;
		}
		return false;
	}

	public static String trimQuotation(String value) {
		int len = value.length();
		int st = 0;
		char[] val = value.toCharArray();    /* avoid getfield opcode */

		while ((st < len) && (val[st] <= ' ' || val[st] == '"')) {
			st++;
		}
		while ((st < len) && (val[len - 1] <= ' ' || val[len - 1] == '"')) {
			len--;
		}
		return ((st > 0) || (len < value.length())) ? value.substring(st, len) : value;
	}

	public static void buildBridge() {
        if (!(Config.mode.equals(Config.modeAll) || Config.mode.equals(Config.modeBuildBridge)))
            return;
		if (!(Config.setUpFileStructure() && Config.readSourceAndSink()))
			return;
        Util.LOGGER.info("building bridge");
		Config.configSoot();
		AppManager appManager = AppManager.v();

        if (!appManager.isHybridApp()) {
            Util.LOGGER.warning("This is not a hybrid app!");
            return;
        }

		if (Config.runPTA)
			appManager.runPTA();
		if (Config.runJSA) {
			appManager.runJSA();
			G.reset();
			Config.configSoot();
			appManager.prepare();
		}

		appManager.generateBridges();

		VirtualWebview.v().generateJavaSideResult();
		VirtualWebview.v().generateHTMLSideResult();
		VirtualWebview.v().dump(Config.getBridgePs());
	}

	public static void runTaintAnalysis() {
        if (!(Config.mode.equals(Config.modeAll) || Config.mode.equals(Config.modeRunTaintAnalysis)))
            return;
        if (!Config.isHybridApp)
            return;

        Util.LOGGER.info("running taint analysis");
		String javaTargetDir = Config.workingDirPath + "/java";
		String htmlTargetDir = Config.workingDirPath + "/html";

		FlowDroidCaller.callWithTimeOut(javaTargetDir, Config.TAINT_ANALYSIS_TIMEOUT_SECONDS);
		HTMLTaintAnalysisCaller.callWithTimeOut(htmlTargetDir, Config.TAINT_ANALYSIS_TIMEOUT_SECONDS);
	}

	public static void mergeTaintFlow() {
        if (!(Config.mode.equals(Config.modeAll) || Config.mode.equals(Config.modeMergeTaintFlow)))
            return;
		if (!Config.isHybridApp)
			return;

        Util.LOGGER.info("merging taint flow");
		File outputFile = FileUtils.getFile(Config.workingDirPath, "AnalysisResult.md");
		try {
			Merger.v().merge(Config.workingDirPath, new PrintStream(new FileOutputStream(outputFile)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
