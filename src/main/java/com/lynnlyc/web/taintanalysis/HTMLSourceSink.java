package com.lynnlyc.web.taintanalysis;

import com.lynnlyc.Util;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by liyc on 9/27/15.
 */
public class HTMLSourceSink {
    public String signature;
    public String methodName;
    public boolean isArgs;
    public boolean isSource;
    public String[] tags;

    public HTMLSourceSink(String signature) {
        this.signature = signature.trim();

        if (this.signature.endsWith("_SOURCE_")) {
            this.isSource = true;
        }
        else if (this.signature.endsWith("_SINK_")) {
            this.isSource = false;
        }
        else {
            Util.LOGGER.warning("identification error: source or sink? " + this.signature);
            this.isSource = true;
        }

        String methodSig = this.signature.substring(this.signature.indexOf('<') + 1, this.signature.indexOf('>'));
        this.methodName = methodSig.substring(methodSig.indexOf(' ') + 1);
        this.tags = this.methodName.split("\\.");

        if (methodSig.startsWith("ARGS")) {
            this.isArgs = true;
        }
        else if (methodSig.startsWith("RET")) {
            this.isArgs = false;
        }
        else {
            Util.LOGGER.warning("identification error: args or ret? " + this.signature);
            this.isArgs = true;
        }
    }

    public boolean matches(HashSet<String> matchTags) {
        if (this.tags == null || matchTags == null)
            return false;
        for (String tag : tags) {
            boolean matched = false;
            for (String matchTag : matchTags) {
                if (matchTag.contains(tag)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return true;
    }
}
