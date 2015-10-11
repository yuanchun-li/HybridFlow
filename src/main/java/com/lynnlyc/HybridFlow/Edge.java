package com.lynnlyc.HybridFlow;

import com.lynnlyc.Util;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Created by yuanchun on 10/10/15.
 * Package: hybridflow
 */
public class Edge {
    public final Node source;
    public final Node sink;
    public final String pathStr;

    public Edge(Node source, Node sink, String pathStr) {
        this.source = source;
        this.sink = sink;
        this.pathStr = pathStr;
    }

    public static final int PATH_TYPE_FLOWDROID = 0;
    public static final int PATH_TYPE_TAGGED = 1;
    public static final int PATH_TYPE_BRIDGE = 2;

    public static Edge buildFromPathStr(String pathStr, int pathType) {
        Node sourceNode = null, sinkNode = null;
        if (pathType == PATH_TYPE_FLOWDROID) {
            pathStr = StringUtils.removeStart(pathStr, "[");
            pathStr = StringUtils.removeEnd(pathStr, "]");

            String[] segs = StringUtils.splitByWholeSeparator(pathStr, " --> ");
            String sourceStr = segs[0].trim();
            String sinkStr = segs[segs.length - 1].trim();

            sourceNode = Node.buildFromFlowNode(sourceStr, Node.NODE_FLOWDROID, true);
            sinkNode = Node.buildFromFlowNode(sinkStr, Node.NODE_FLOWDROID, false);
        }
        else if (pathType == PATH_TYPE_TAGGED) {
            String[] segs = StringUtils.splitByWholeSeparator(pathStr, " --> ");
            String sourceStr = segs[0].trim();
            String sinkStr = segs[segs.length - 1].trim();

            sourceNode = Node.buildFromFlowNode(sourceStr, Node.NODE_TAGGED, true);
            sinkNode = Node.buildFromFlowNode(sinkStr, Node.NODE_TAGGED, false);
        }
        else if (pathType == PATH_TYPE_BRIDGE) {
            String[] segs = StringUtils.splitByWholeSeparator(pathStr, " --> ");
            String sourceStr = segs[0].trim();
            String sinkStr = segs[segs.length - 1].trim();

            sourceNode = Node.buildFromFlowNode(sourceStr, Node.NODE_BRIDGE, true);
            sinkNode = Node.buildFromFlowNode(sinkStr, Node.NODE_BRIDGE, false);
        }

        if (sourceNode != null && sinkNode != null)
            return new Edge(sourceNode, sinkNode, pathStr);
        Util.LOGGER.warning("invalid path str: " + pathStr);
        return null;
    }

    public static Edge combineEdgesToOne(List<Edge> edges) {
        Node first = edges.get(0).source;
        Node last = edges.get(edges.size() - 1).sink;
        String signature = StringUtils.join(edges, " --> ");
        return new Edge(first, last, signature);
    }

    @Override
    public String toString() {
        return pathStr;
    }

    public String toShortString() {
        return source.toString() + "-->" + sink.toString();
    }

    public String toLongString() {
        return String.format("***\n\n**Source**: %s\n\n**Sink**: %s\n\n**Path**:%s\n\n***\n",
                source, sink, getBeautifiedPathStr());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public String getBeautifiedPathStr() {
        String[] pathNodes = StringUtils.splitByWholeSeparator(this.pathStr, " --> ");
        String beautifiedPathStr = "";
        for (String pathNode : pathNodes) {
            beautifiedPathStr += String.format("\n\n\t--> `%s`", pathNode);
        }
        return beautifiedPathStr;
    }
}
