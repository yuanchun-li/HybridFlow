package com.lynnlyc.HybridFlow;

import com.lynnlyc.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by liyc on 10/8/15.
 */
public class Merger {
    private static Merger merger;
    private Merger() {
        javaPathStrs = new HashSet<>();
        htmlPathStrs = new HashSet<>();
        bridgePathStrs = new HashSet<>();
        sourceSinkStrs = new HashSet<>();
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
    private Set<String> sourceSinkStrs;

    public void merge(String targetDir, PrintStream ps) {
        File javaTaintPathFile = new File(targetDir, "java/TaintAnalysis.log");
        File htmlTaintPathFile = new File(targetDir, "html/TaintAnalysis.log");
        File bridgeFile = new File(targetDir, "bridge/bridge.txt");
        File sourceSinkFile = new File(targetDir, "SourcesAndSinks.txt");

        try {
            javaPathStrs = parseFlowdroidTaintPathFile(javaTaintPathFile);
            htmlPathStrs = parseTaggedTaintPathFile(htmlTaintPathFile);
            bridgePathStrs = parseBridgeFile(bridgeFile);
            sourceSinkStrs = parseSourceSinkFile(sourceSinkFile);

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
        Util.printLines(ps, this.mergeFlow(sourceSinkStrs, bridgePathStrs, javaPathStrs, htmlPathStrs));
    }

    private static final String flowdroidPathLogPrefix = "[main] INFO soot.jimple.infoflow.Infoflow - \ton Path:";
    private static final String flowdroidNodeLogPrefix = "[main] INFO soot.jimple.infoflow.Infoflow - \t\t -> ";
    private Set<String> parseFlowdroidTaintPathFile(File javaTaintPathFile) throws IOException {
        List<String> lines = FileUtils.readLines(javaTaintPathFile);
        HashSet<String> paths = new HashSet<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            line = line.trim();
            if (line.startsWith(flowdroidPathLogPrefix)) {
                List<String> nodes = new ArrayList<>();
                int j;
                for (j = i+1; j < lines.size(); j++) {
                    String nodeLine = lines.get(j);
                    if (nodeLine.startsWith(flowdroidNodeLogPrefix)) {
                        nodes.add(StringUtils.removeStart(nodeLine, flowdroidNodeLogPrefix));
                    }
                    else break;
                }
                i = j;
                paths.add(StringUtils.join(nodes, " --> "));
            }
        }
        return paths;
    }

    private Set<String> parseTaggedTaintPathFile(File htmlTaintPathFile) throws IOException {
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

    private Set<String> parseSourceSinkFile(File sourceSinkFile) throws IOException {
        List<String> lines = FileUtils.readLines(sourceSinkFile);
        return new HashSet<>(lines);
    }

    private Set<String> mergeFlow(Set<String> originalSourceSinkStrs, Set<String> bridgePathStrs,
                                  Set<String> javaPathStrs, Set<String> htmlPathStrs) {
        Set<Node> originalSources = new HashSet<>();
        Set<Node> originalSinks = new HashSet<>();
        Set<Edge> edges = new HashSet<>();
        Set<String> mergedPathStrs = new HashSet<>();
        for (String line : originalSourceSinkStrs) {
            if (line.length() == 0 || line.startsWith("%")) continue;
            Node node = Node.buildFromSourceSinkLine(line);
            if (node == null) continue;
            if (node.isHead) originalSources.add(node);
            else originalSinks.add(node);
        }
        for (String pathStr : bridgePathStrs) {
            Edge flow = Edge.buildFromPathStr(pathStr, Edge.PATH_TYPE_BRIDGE);
            if (flow != null) edges.add(flow);
        }
        for (String pathStr : javaPathStrs) {
            Edge flow = Edge.buildFromPathStr(pathStr, Edge.PATH_TYPE_FLOWDROID);
            if (flow != null) edges.add(flow);
        }
        for (String pathStr : htmlPathStrs) {
            Edge flow = Edge.buildFromPathStr(pathStr, Edge.PATH_TYPE_TAGGED);
            if (flow != null) edges.add(flow);
        }

        Set<List<Edge>> hybridPaths = findHybridPathsFromSourcesToSinks(originalSources, originalSinks, edges);
        for (List<Edge> hybridPath : hybridPaths) {
            Edge hybridFlow = Edge.combineEdgesToOne(hybridPath);
            mergedPathStrs.add(hybridFlow.toLongString());
        }
        return mergedPathStrs;
    }

    public static Set<List<Edge>> findHybridPathsFromSourcesToSinks(Set<Node> sources,
                                                                   Set<Node> sinks,
                                                                   Set<Edge> edges) {
        Set<List<Edge>> hybridPaths = new HashSet<>();

        for (Node source : sources) {
            Set<List<Edge>> pathsFromSource = findHybridPathsFrom(source, new HashSet<Node>(), edges);
            for (List<Edge> path : pathsFromSource) {
                Node lastNode = path.get(path.size() - 1).sink;
                if (sinks.contains(lastNode))
                    hybridPaths.add(path);
            }
        }
        return hybridPaths;
    }

    private static Map<Node, Set<Edge>> succEdgesMap = new HashMap<>();
    private static Set<Edge> getSuccEdges(Node node, Set<Edge> edges) {
        if (succEdgesMap.containsKey(node)) return succEdgesMap.get(node);
        Set<Edge> succEdges = new HashSet<>();
        if (node == null) return succEdges;
        for (Edge e : edges) {
            if (node.equals(e.source)) succEdges.add(e);
        }
        succEdgesMap.put(node, succEdges);
        return succEdges;
    }

    private static Set<List<Edge>> findHybridPathsFrom(Node node,
                                                       Set<Node> reachedNodes,
                                                       Set<Edge> edges) {
        Set<List<Edge>> hybridPaths = new HashSet<>();
        Set<Edge> succEdges = getSuccEdges(node, edges);
        for (Edge succEdge : succEdges) {
            Node succNode = succEdge.sink;
            if (reachedNodes.contains(succNode)) continue;
            Set<Node> succReachedNodes = new HashSet<>();
            succReachedNodes.addAll(reachedNodes);
            succReachedNodes.add(succNode);
            Set<List<Edge>> hybridFlowsFromSucc = findHybridPathsFrom(succNode, succReachedNodes, edges);
            if (hybridFlowsFromSucc.size() == 0) {
                List<Edge> hybridPath = new ArrayList<>();
                hybridPath.add(succEdge);
                hybridPaths.add(hybridPath);
            }
            else {
                for (List<Edge> hybridFlowFromSucc : hybridFlowsFromSucc) {
                    hybridFlowFromSucc.add(0, succEdge);
                    hybridPaths.add(hybridFlowFromSucc);
                }
            }
        }
        return hybridPaths;
    }
}
