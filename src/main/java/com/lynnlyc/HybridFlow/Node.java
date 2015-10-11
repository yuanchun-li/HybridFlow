package com.lynnlyc.HybridFlow;

import com.lynnlyc.Util;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yuanchun on 10/10/15.
 * Package: hybridflow
 */
public class Node {
    public final int languageType;
    public static final int LANGUAGE_JAVA = 0;
    public static final int LANGUAGE_HTML = 1;

    public final String tag;
    public final String signature;

    public final boolean isHead;

    public Node(int languageType, String tag, String signature, boolean isHead) {
        this.languageType = languageType;
        this.tag = tag;
        this.signature = signature;
        this.isHead = isHead;
    }

    public static final int NODE_FLOWDROID = 0;
    public static final int NODE_TAGGED = 1;
    public static final int NODE_BRIDGE = 2;

    private static final String flowdroidNodePatternStr = "^(.+)invoke (.*)(<.+>)\\((.*)\\)$";
    private static final Pattern flowdroidNodePattern = Pattern.compile(flowdroidNodePatternStr);
    private static final String taggedNodePatternStr = "^\\{\\{HTML <\\((.+)\\) (.+)> -> _(SOURCE|SINK)_\\}\\}$";
    private static final Pattern taggedNodePattern = Pattern.compile(taggedNodePatternStr);
    private static final String bridgeNodePatternStr = "^\\((J|H)\\)\\(([A-Za-z0-9]+)\\)(.*)$";
    private static final Pattern bridgeNodePattern = Pattern.compile(bridgeNodePatternStr);
    public static Node buildFromFlowNode(String nodeStr, int nodeType, boolean isHead) {
        if (nodeType == NODE_FLOWDROID) {
            Matcher m = flowdroidNodePattern.matcher(nodeStr);
            if (m.find()) {
                return new Node(LANGUAGE_JAVA, isHead? "RET":"ARGS", m.group(3), isHead);
            }
        }
        else if (nodeType == NODE_TAGGED) {
            Matcher m = taggedNodePattern.matcher(nodeStr);
            if (m.find()) {
                return new Node(LANGUAGE_HTML, m.group(1), m.group(2), isHead);
            }
        }
        else if (nodeType == NODE_BRIDGE) {
            Matcher m = bridgeNodePattern.matcher(nodeStr);
            if (m.find()) {
                int languageType = m.group(1).equals("J")?LANGUAGE_JAVA:LANGUAGE_HTML;
                return new Node(languageType, m.group(2), m.group(3), isHead);
            }
        }
        Util.LOGGER.warning("invalid flow node: " + nodeStr);
        return null;
    }

    private static final String javaSourceSinkNodePatternStr = "^(<.+>)(.*) -> _(SOURCE|SINK)_$";
    private static final Pattern javaSourceSinkNodePattern = Pattern.compile(javaSourceSinkNodePatternStr);
    private static final String htmlSourceSinkNodePatternStr = "^HTML <\\((.+)\\) (.+)> -> _(SOURCE|SINK)_$";
    private static final Pattern htmlSourceSinkNodePattern = Pattern.compile(htmlSourceSinkNodePatternStr);
    public static Node buildFromSourceSinkLine(String sourceSinkLine) {
        sourceSinkLine = sourceSinkLine.trim();

        Matcher javaMatcher = javaSourceSinkNodePattern.matcher(sourceSinkLine);
        Matcher htmlMatcher = htmlSourceSinkNodePattern.matcher(sourceSinkLine);

        if (javaMatcher.find()) {
            boolean isHead = javaMatcher.group(3).equals("SOURCE");
            String tag = isHead?"RET":"ARGS";
            String signature = javaMatcher.group(1);
            return new Node(LANGUAGE_JAVA, tag, signature, isHead);
        }
        else if (htmlMatcher.find()) {
            boolean isHead = htmlMatcher.group(3).equals("SOURCE");
            String tag = htmlMatcher.group(1);
            String signature = htmlMatcher.group(2);
            return new Node(LANGUAGE_HTML, tag, signature, isHead);
        }

        Util.LOGGER.warning("invalid source/sink line: " + sourceSinkLine);
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return  (o instanceof Node && this.languageType == ((Node) o).languageType &&
                this.tag.equals(((Node) o).tag) && this.signature.equals(((Node) o).signature));
    }

    @Override
    public String toString() {
        String languageTag = this.languageType == LANGUAGE_JAVA?"J":(this.languageType == LANGUAGE_HTML?"H":"I");
        return String.format("(%s)(%s)`%s`", languageTag, tag, signature);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public static void main(String[] args) {
        String testStr1 = "$r4 = virtualinvoke $r3.<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)>(\"network\")";
        Node testNode1 = Node.buildFromFlowNode(testStr1, Node.NODE_FLOWDROID, true);

        String testStr2 = "{{HTML <(RET) webviewdemo,getSystemResource> -> _SOURCE_}}";
        Node testNode2 = Node.buildFromFlowNode(testStr2, Node.NODE_TAGGED, true);

        String testStr3 = "(H)(ARGS)window,confirm";
        Node testNode3 = Node.buildFromFlowNode(testStr3, Node.NODE_BRIDGE, true);

        String testStr4 = "<android.telephony.TelephonyManager: java.lang.String getDeviceId()> android.permission.READ_PHONE_STATE -> _SOURCE_";
        Node testNode4 = Node.buildFromSourceSinkLine(testStr4);

        String testStr5 = "HTML <(RET) document,getElementById> -> _SOURCE_";
        Node testNode5 = Node.buildFromSourceSinkLine(testStr5);

        System.out.println(testNode1);
        System.out.println(testNode2);
        System.out.println(testNode3);
        System.out.println(testNode4);
        System.out.println(testNode5);

    }
}
