// Configurations of webview analysis tool
/*
 * This config class contains the important options
 * which might be used in both app and web analysis
 * For app analysis, it configures soot tool by operating on Options.v()
 * For web analysis, it stores the javascript bridges and temp js file paths
 *  generated during app analysis
 */

package com.lynnlyc;

import org.apache.commons.cli.Option;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by yuanchun on 5/4/15.
 * Package: webview-flow
 */
public class Config {
    // File path of apk
    public static String appFilePath = "";

    // Directory path to find android.jar
    public static String androidPlatformDir = "";

    // File path of android.jar which is forced to use by soot
    public static String forceAndroidJarPath = "";

    // Directory path of web pages
    public static String webDirPath = "";

    // A list of paths of web pages to analyse
    public static HashSet<String> htmlFilePaths = new HashSet();

    // Directory for result output
    public static String outputDirPath = "";

    public static boolean isInitialized = false;

    // printer of log output
    private static PrintStream ps;

    // noticeable webview methods
    public static String[] webview_methods = {
        "addJavascriptInterface",
        "loadUrl",
        "evaluateJavascript",
        "loadData",
        "loadDataWithBaseURL"
    };

    // possible app entries, used for generating appEntryPoints
    public static String[] possible_entries = {
        "onCreate",
        "onStart",
        "onCreateView",
        "onClick",
    };

    public static boolean parseArgs(String[] args) {
        int i;
        if (args.length == 0 || args.length % 2 == 1)
            return false;

        for (i = 0; i < args.length; i += 2) {
            String key = args[i];
            String value = args[i+1];
            switch (key) {
                case "-d": Config.outputDirPath = value; break;
                case "-app": Config.appFilePath = value; break;
                case "-android-jars": Config.androidPlatformDir = value; break;
                case "-force-android-jar": Config.forceAndroidJarPath = value; break;
                case "-web": Config.webDirPath = value; break;
                default: return false;
            }
        }

        if ("".equals(Config.androidPlatformDir) && "".equals(Config.forceAndroidJarPath)) {
            return false;
        }

        if ("".equals(Config.appFilePath) || "".equals(Config.outputDirPath)) {
            return false;
        }

        File workingDir = new File(String.format("%s/webviewflow_%s/", Config.outputDirPath, Util.getTimeString()));
        Config.outputDirPath = workingDir.getPath();
        if (!workingDir.exists()) workingDir.mkdirs();
        File logfile = new File(Config.outputDirPath + "/analysis.log");

        try {
            FileHandler fh = new FileHandler(logfile.getAbsolutePath());
            Util.LOGGER.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    };

    public static void init() {
        Util.LOGGER.log(Level.INFO, "initializing...");
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_output_dir(Config.outputDirPath);
        List<String> process_dirs = new ArrayList<>();
        process_dirs.add(Config.appFilePath);
        Options.v().set_process_dir(process_dirs);
        if (!("".equals(Config.androidPlatformDir)))
            Options.v().set_android_jars(Config.androidPlatformDir);
        if (!("".equals(Config.forceAndroidJarPath)))
            Options.v().set_force_android_jar(Config.forceAndroidJarPath);

        Config.isInitialized = true;
        Util.LOGGER.log(Level.INFO,  "initialization finished...");
    }


}
