package com.lynnlyc.app;

import com.lynnlyc.Config;
import com.lynnlyc.Util;
import soot.PackManager;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by yuanchun on 5/5/15.
 * Package: webview-flow
 */
public class AppManager {
    public String appFilePath;
    private boolean isPrepared = false;
    private boolean isPTAFinished = false;
    private PointsToAnalysis pta = null;

    public AppManager(String appFilePath) {
        this.appFilePath = appFilePath;
        this.prepare();
    }

    // entry points of soot-based app analysis
    public List<SootMethod> appEntryPoints = new ArrayList<SootMethod>();

    private void prepare() {
        if (!Config.isInitialized) {
            Util.LOGGER.log(Level.WARNING, "Configuration not initialized");
            return;
        }
        Scene.v().loadNecessaryClasses();
        appEntryPoints = Util.findEntryPoints();
        Scene.v().setEntryPoints(appEntryPoints);
        this.isPrepared = true;
    }

    public void runPTA() {
        if (!this.isPrepared) {
            Util.LOGGER.log(Level.WARNING, "App not perpared " + this.appFilePath);
            return;
        }
        PTA.runSparkPTA();
        this.pta = Scene.v().getPointsToAnalysis();
        this.isPTAFinished = true;
    }

    public void generateApp2WebBridge() {

    }

    public void outputJimple() {
        Options.v().set_output_format(Options.output_format_jimple);
    }
}
