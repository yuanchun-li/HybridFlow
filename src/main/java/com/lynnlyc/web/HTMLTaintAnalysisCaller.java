package com.lynnlyc.web;

import com.lynnlyc.Util;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;

/**
 * Created by liyc on 10/7/15.
 * run taint analysis of given directory
 */
public class HTMLTaintAnalysisCaller {
    private File targetDirFile;
    private File analysisResult;

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

    public boolean initWithDir(String targetDir) {
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
            WebManager.v().addPossibleURL(dummyHTMLcp.toURI().toURL());
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
            WebManager.v().runTaintAnalysis(new PrintStream(new FileOutputStream(this.analysisResult)));
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

}
