package com.lynnlyc.app;

import com.lynnlyc.Config;
import com.lynnlyc.Util;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

/**
 * Created by liyc on 10/7/15.
 */
public class FlowDroidCaller {
    private String appFileName;
    private String androidPlatformHome;
    private File targetDirFile;
    private File flowDroidResult;
    private File flowdroidJar;
    private File callbacksTxt;
    private File wrapperTxt;

    private static String usage = "usage: FlowDroidCaller.main <targetDir> <androidPlatformDir>";

    private static FlowDroidCaller caller;
    private FlowDroidCaller() {
        appFileName = null;
        androidPlatformHome = null;
        targetDirFile = null;
        caller = this;
    }

    public static FlowDroidCaller v() {
        if (caller == null) {
            caller = new FlowDroidCaller();
        }
        return caller;
    }

    public boolean initWithDir(String targetDir) {
        this.targetDirFile = new File(targetDir);
        if (!(targetDirFile.exists() && targetDirFile.isDirectory())) {
            Util.LOGGER.warning("target dir not exist");
            return false;
        }

        Collection<File> apkFiles = FileUtils.listFiles(targetDirFile, new String[]{"apk"}, false);

        if (apkFiles.size() != 1) {
            Util.LOGGER.warning("cannot find apk in target dir");
            return false;
        }
        File apkFile = apkFiles.iterator().next();
        this.appFileName = apkFile.getName();

        Collection<File> txtFiles = FileUtils.listFiles(targetDirFile, new String[]{"txt"}, false);

        File sourceSinkFile = null;

        for (File txtFile : txtFiles) {
            if ("SourcesAndSinks.txt".equals(txtFile.getName()))
                sourceSinkFile = txtFile;
        }
        if (sourceSinkFile == null) {
            Util.LOGGER.warning("cannot find SourcesAndSinks.txt in target dir");
            return false;
        }

        this.flowDroidResult = FileUtils.getFile(this.targetDirFile, "TaintAnalysis.log");
        this.androidPlatformHome = Config.androidPlatformDir;

        this.flowdroidJar = new File(targetDirFile, "FlowDroid.jar");
        this.callbacksTxt = new File(targetDirFile, "AndroidCallbacks.txt");
        this.wrapperTxt = new File(targetDirFile, "EasyTaintWrapperSource.txt");

        try {
            FileUtils.copyURLToFile(getClass().getResource("/flowdroid/FlowDroid.jar"),
                    this.flowdroidJar);
            FileUtils.copyURLToFile(getClass().getResource("/flowdroid/AndroidCallbacks.txt"),
                    this.callbacksTxt);
            FileUtils.copyURLToFile(getClass().getResource("/flowdroid/EasyTaintWrapperSource.txt"),
                    this.wrapperTxt);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void cleanTargetDir() {
        FileUtils.deleteQuietly(this.flowdroidJar);
        FileUtils.deleteQuietly(this.callbacksTxt);
        FileUtils.deleteQuietly(this.wrapperTxt);
    }

    public boolean run(String targetDir) {
        if (!initWithDir(targetDir)) {
            Util.LOGGER.warning("initialization failed");
            return false;
        }
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "FlowDroid.jar", this.appFileName, this.androidPlatformHome);
        pb.directory(this.targetDirFile);
        pb.redirectOutput(flowDroidResult);
        try {
            Process flowdroid = pb.start();
            if (0 != flowdroid.waitFor())
                return false;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        this.cleanTargetDir();
        return true;
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println(usage);
            return;
        }
        System.out.println("running flowdroid on dir: " + args[0]);
        System.out.println("Android platforms dir: " + args[1]);
        Config.androidPlatformDir = args[1];
        FlowDroidCaller.v().run(args[0]);
    }
}