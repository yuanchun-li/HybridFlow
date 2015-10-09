package com.lynnlyc.merger;

import com.lynnlyc.Util;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by liyc on 10/8/15.
 */
public class Merger {
    private static Merger merger;
    private Merger() {
        javaPathStrs = new HashSet<>();
        htmlPathStrs = new HashSet<>();
        bridgePathStrs = new HashSet<>();
        merger = this;
    }
    public static Merger v() {
        if (merger == null)
            merger = new Merger();
        return merger;
    }

    private Set<String> javaPathStrs;
    private Set<String> htmlPathStrs;
    private Set<String> bridgePathStrs;

    public void merge(String targetDir, PrintStream ps) {
        File javaTaintPathFile = new File(targetDir, "java/TaintAnalysis.log");
        File htmlTaintPathFile = new File(targetDir, "html/TaintAnalysis.log");
        File bridgeFile = new File(targetDir, "bridge/bridge.txt");

        try {
            javaPathStrs = parseJavaTaintPathFile(javaTaintPathFile);
            htmlPathStrs = parseHTMLTaintPathFile(htmlTaintPathFile);
            bridgePathStrs = parseBridgeFile(bridgeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ps.println("\njava taint path:");
        Util.printLines(ps, javaPathStrs);
        ps.println("\nhtml taint path:");
        Util.printLines(ps, htmlPathStrs);
        ps.println("\nbridge taint path:");
        Util.printLines(ps, bridgePathStrs);

        ps.println("\n\nmerged source to sink paths:");
    }

    private Set<String> parseJavaTaintPathFile(File javaTaintPathFile) throws IOException {
        List<String> lines = FileUtils.readLines(javaTaintPathFile);
        HashSet<String> paths = new HashSet<>();
        boolean foundFlow = false;
        for (String line : lines) {
            line = line.trim();
            if (foundFlow) {
                if (line.startsWith("on Path ")) {
                    paths.add(line.substring(8));
                }
            } else if (line.startsWith("Found a flow to sink")) {
                foundFlow = true;
            }
        }
        return paths;
    }

    private Set<String> parseHTMLTaintPathFile(File htmlTaintPathFile) throws IOException {
        List<String> lines = FileUtils.readLines(htmlTaintPathFile);
        HashSet<String> paths = new HashSet<>();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("tagged path: ")) {
                paths.add(line.substring(13));
            }
        }
        return paths;
    }

    private Set<String> parseBridgeFile(File bridgeFile) throws IOException {
        List<String> lines = FileUtils.readLines(bridgeFile);
        HashSet<String> paths = new HashSet<>();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("[bridgePath]")) {
                paths.add(line.substring(12));
            }
        }
        return paths;
    }

    private Set<String> mergeFlow(Set<String> bridgeFlows, Set<String> javaFlows, Set<String> htmlFlows) {
        Set<String> mergedFlows = new HashSet<>();

        for (String bridgeFlow : bridgeFlows) {
            String[] segs = bridgeFlow.split(" --> ");
            if (segs.length != 2) {
                Util.LOGGER.warning("error splitting bridgeFlow: " + bridgeFlow);
                continue;
            }
            String bridgeSource = segs[0];
            String bridgeSink = segs[1];


        }
        return mergedFlows;
    }

    private String getJavaFlowSource(String javaFlow) {
        return "";
    }
}
