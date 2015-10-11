package com.lynnlyc.web;

import com.lynnlyc.Util;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by liyc on 10/7/15.
 * run taint analysis of given directory
 */
public class HTMLTaintAnalysisCaller implements Callable<Boolean> {
    private File targetDirFile;
    private File analysisResult;
    private URL dummyHTMLURL;

    private static HTMLTaintAnalysisCaller caller;
    private HTMLTaintAnalysisCaller() {
        targetDirFile = null;
        caller = this;
    }

    public static HTMLTaintAnalysisCaller v() {
        if (caller == null) {
            caller = new HTMLTaintAnalysisCaller();
        }
        return caller;
    }

    private String targetDir;
    public HTMLTaintAnalysisCaller(String targetDir) {
        this.targetDir = targetDir;
        targetDirFile = null;
        caller = this;
    }

    private boolean initWithDir(String targetDir) {
        this.targetDirFile = new File(targetDir);
        if (!(targetDirFile.exists() && targetDirFile.isDirectory())) {
            Util.LOGGER.warning("target dir not exist");
            return false;
        }

        Collection<File> jsFiles = FileUtils.listFiles(targetDirFile, new String[]{"js"}, false);
        WebManager.v().setTaintJsFiles(jsFiles);

        File sourceSinkFile = null, possibleURLsFile = null;
        Collection<File> txtFiles = FileUtils.listFiles(targetDirFile, new String[]{"txt"}, false);
        for (File txtFile : txtFiles) {
            if (txtFile.getName().equals("SourcesAndSinks.txt"))
                sourceSinkFile = txtFile;
            else if (txtFile.getName().equals("possibleURLs.txt"))
                possibleURLsFile = txtFile;
        }

        if (sourceSinkFile == null) {
            Util.LOGGER.warning("cannot find SourcesAndSinks.txt in target dir");
            return false;
        }
        if (possibleURLsFile == null) {
            Util.LOGGER.warning("cannot find possibleURLs.txt in target dir");
            return false;
        }

        try {
            List<String> sourceSinkLines = FileUtils.readLines(sourceSinkFile);
            WebManager.v().setSourceSinks(sourceSinkLines);

            List<String> possibleURLLines = FileUtils.readLines(possibleURLsFile);
            for (String URLLine : possibleURLLines) {
                URL possibleURL = new URL(URLLine);
                WebManager.v().addPossibleURL(possibleURL);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        File dummyHTMLcp = new File(targetDir, "dummy.html");
        this.analysisResult = FileUtils.getFile(this.targetDirFile, "TaintAnalysis.log");

        try {
            FileUtils.copyURLToFile(getClass().getResource("/dummy.html"), dummyHTMLcp);
            this.dummyHTMLURL = dummyHTMLcp.toURI().toURL();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean run(String targetDir) {
        if (!initWithDir(targetDir)) {
            Util.LOGGER.warning("initialization failed");
            return false;
        }
        try {
            PrintStream ps = new PrintStream(new FileOutputStream(this.analysisResult));
            WebManager.v().runTaintAnalysis(dummyHTMLURL, ps);
            WebManager.v().runTaintAnalysis(ps);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            String usage = "usage: HTMLTaintAnalysisCaller.main <targetDir>";
            System.out.println(usage);
            return;
        }
        System.out.println("running HTML taint analysis on dir: " + args[0]);
        HTMLTaintAnalysisCaller.v().run(args[0]);
    }

    public static void callWithTimeOut(String targetDir, int timeoutSeconds) {
        HTMLTaintAnalysisCaller htmlTaintAnalysisCaller = new HTMLTaintAnalysisCaller(targetDir);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(htmlTaintAnalysisCaller);

        try {
            Util.LOGGER.info("HTML taint analysis started!");
            if (future.get(timeoutSeconds, TimeUnit.SECONDS)) {
                Util.LOGGER.info("HTML taint analysis finished!");
            }
            else {
                Util.LOGGER.info("HTML taint analysis failed");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            Util.LOGGER.info("HTML taint analysis failed!");
            future.cancel(true);
        } catch (TimeoutException e) {
            Util.LOGGER.info("HTML taint analysis timeout!");
            future.cancel(true);
        }
        executor.shutdownNow();
    }

    @Override
    public Boolean call() throws Exception {
        return run(this.targetDir);
    }
}
