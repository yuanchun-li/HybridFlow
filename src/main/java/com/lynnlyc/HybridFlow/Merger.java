package com.lynnlyc.HybridFlow;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by liyc on 10/8/15.
 * merge hybrid edges to hybrid taint flows
 */
public class Merger {
    private static Merger merger;
    private Merger() {
        javaFlowEdges = new HashSet<>();
        htmlFlowEdges = new HashSet<>();
        bridgeFlowEdges = new HashSet<>();

        edges = new HashSet<>();
        sourceNodes = new HashSet<>();
        sinkNodes = new HashSet<>();

        mergedFlows = new HashSet<>();

        merger = this;
    }
    public static Merger v() {
        if (merger == null)
            merger = new Merger();
        return merger;
    }

    private Set<Edge> javaFlowEdges;
    private Set<Edge> htmlFlowEdges;
    private Set<Edge> bridgeFlowEdges;

    // problem is:
    // given a set of edges of graph
    // find all paths from source nodes to sink nodes
    private Set<Node> sourceNodes;
    private Set<Node> sinkNodes;
    private Set<Edge> edges;

    // result
    private Set<Edge> mergedFlows;

    public void merge(String targetDir, PrintStream ps) {
        File javaTaintPathFile = new File(targetDir, "java/TaintAnalysis.log");
        File htmlTaintPathFile = new File(targetDir, "html/TaintAnalysis.log");
        File bridgeFile = new File(targetDir, "bridge/bridge.txt");
        File sourceSinkFile = new File(targetDir, "SourcesAndSinks.txt");

        try {
            parseFlowdroidTaintPathFile(javaTaintPathFile);
            parseTaggedTaintPathFile(htmlTaintPathFile);
            parseBridgeFile(bridgeFile);
            parseSourceSinkFile(sourceSinkFile);

        } catch (IOException e) {
            e.printStackTrace();
        }

        edges.addAll(javaFlowEdges);
        edges.addAll(htmlFlowEdges);
        edges.addAll(bridgeFlowEdges);
        this.mergeFlows();

        ps.println("\n# Analysis Result:\n");

        ps.println("\n## Merged taint paths:\n");
        printEdges(ps, mergedFlows);

        ps.println("\n## Respective taint edges:\n");
        ps.println("\n### FlowDroid edges:\n");
        printEdges(ps, javaFlowEdges);
        ps.println("\n### HTML edges:\n");
        printEdges(ps, htmlFlowEdges);
        ps.println("\n### Hybrid bridges:\n");
        printEdges(ps, bridgeFlowEdges);

    }

    private static final String flowdroidLogPrefix = "[main] INFO soot.jimple.infoflow.Infoflow - ";
    private static final String flowdroidPathLogPrefix = flowdroidLogPrefix + "\ton Path:";
    private static final String flowdroidNodeLogPrefix = flowdroidLogPrefix + "\t\t -> ";
    private void parseFlowdroidTaintPathFile(File javaTaintPathFile) throws IOException {
        List<String> lines = FileUtils.readLines(javaTaintPathFile);
//        Set<String> paths = new HashSet<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            line = line.trim();
            if (line.startsWith(flowdroidPathLogPrefix)) {
                // handle callback source
                List<String> nodes = new ArrayList<>();
                String sourceLine = StringUtils.removeStart(lines.get(i - 1), flowdroidLogPrefix);
                if (sourceLine.contains("@parameter")) {
                    nodes.add(sourceLine);
                }

                int j;
                for (j = i+1; j < lines.size(); j++) {
                    String nodeLine = lines.get(j);
                    if (nodeLine.startsWith(flowdroidNodeLogPrefix)) {
                        nodes.add(StringUtils.removeStart(nodeLine, flowdroidNodeLogPrefix).trim());
                    }
                    else break;
                }
                i = j;

                String pathStr = StringUtils.join(nodes, " --> ");
//                paths.add(pathStr);
                Edge e = Edge.buildFromPathStr(pathStr, Edge.PATH_TYPE_FLOWDROID);
                if (e != null) {
                    if (sourceLine.contains("@parameter")) sourceNodes.add(e.source);
                    javaFlowEdges.add(e);
                }
            }
        }
    }

    private Set<Edge> parseTaggedTaintPathFile(File htmlTaintPathFile) throws IOException {
        List<String> lines = FileUtils.readLines(htmlTaintPathFile);
//        HashSet<String> paths = new HashSet<>();
        Set<Edge> edges = new HashSet<>();

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("tagged path: ")) {
                String pathStr = line.substring(13).trim();
//                paths.add(pathStr);
                Edge e = Edge.buildFromPathStr(pathStr, Edge.PATH_TYPE_TAGGED);
                if (e != null) htmlFlowEdges.add(e);
            }
        }
        return edges;
    }

    private Set<Edge> parseBridgeFile(File bridgeFile) throws IOException {
        List<String> lines = FileUtils.readLines(bridgeFile);
//        HashSet<String> paths = new HashSet<>();
        Set<Edge> edges = new HashSet<>();

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("[bridgePath]")) {
                String pathStr = line.substring(12).trim();
//                paths.add(pathStr);
                Edge e = Edge.buildFromPathStr(pathStr, Edge.PATH_TYPE_BRIDGE);
                if (e != null) bridgeFlowEdges.add(e);
            }
        }
        return edges;
    }

    private void parseSourceSinkFile(File sourceSinkFile) throws IOException {
        List<String> lines = FileUtils.readLines(sourceSinkFile);
        for (String line : lines) {
            if (line.length() == 0 || line.startsWith("%")) continue;
            Node node = Node.buildFromSourceSinkLine(line);
            if (node == null) continue;
            if (node.isHead) sourceNodes.add(node);
            else sinkNodes.add(node);
        }
    }

    private void mergeFlows() {
        Set<List<Edge>> hybridPaths = findHybridPathsFromSourcesToSinks(sourceNodes, sinkNodes, edges);
        for (List<Edge> hybridPath : hybridPaths) {
            Edge hybridFlow = Edge.combineEdgesToOne(hybridPath);
            mergedFlows.add(hybridFlow);
        }
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

    public static void printEdges(PrintStream ps, Set<Edge> edges) {
        for (Edge e : edges) {
            ps.println(e.toLongString());
        }
    }
}
