package com.lynnlyc.web.taintanalysis;

import com.lynnlyc.Util;

/**
 * Created by liyc on 9/27/15.
 */
public class SourceSink {
    public String signature;
    public String methodName;
    public boolean isArgs;
    public boolean isSource;

    public SourceSink(String signature) {
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

        this.methodName = methodSig.substring(methodSig.indexOf(' ') + 1);
    }
}
