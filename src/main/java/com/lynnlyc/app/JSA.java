package com.lynnlyc.app;

import com.lynnlyc.bridge.BridgeContext;
import dk.brics.automaton.Automaton;
import dk.brics.string.StringAnalysis;
import soot.ValueBox;

import java.util.HashMap;
import java.util.HashSet;

/*
 * analyze String argument of webview API
 * using Java string analyzer
 */
public class JSA {
	public static StringAnalysis jsa;

	public HashSet<ValueBox> hotspots;
	public HashMap<String, ValueBox> context2ValueBox;
	public HashMap<String, String> context2String;

//    public static ArrayList<SignatureWithArgOrRet> signatureWithArgOrRets = new ArrayList<>();
//    public static HashMap<SignatureWithArgOrRet, List<ValueBox>> sig2values = new HashMap<>();
//	public static void init() {
//		SignatureWithArgOrRet signatureWithArgOrRet = new SignatureWithArgOrRet(
//				Util.loadUrlSig, 0);
//		signatureWithArgOrRets.add(signatureWithArgOrRet);
//
//		signatureWithArgOrRet = new SignatureWithArgOrRet(
//				Util.addJavascriptInterfaceSig, 1);
//		signatureWithArgOrRets.add(signatureWithArgOrRet);
//	}

    public JSA() {
        hotspots = new HashSet<>();
        context2ValueBox = new HashMap<>();
        context2String = new HashMap<>();
    }

    public void run() {
        jsa = new StringAnalysis(hotspots);
        for (String context : context2ValueBox.keySet()) {
            Automaton automaton = jsa.getAutomaton(context2ValueBox.get(context));
            context2String.put(context, automaton.getShortestExample(true));
        }
    }

    public String getStringAnalysisResult(BridgeContext context) {
        String contextStr = context.toString();
		if (context2String.containsKey(contextStr)) {
			return context2String.get(contextStr);
		}
        return null;
    }
	
//	public static void dumpJSAresults(PrintStream os) {
//		if (jsa == null) {
//			os.println("No JSA result.");
//			return;
//		}
//
//		os.println("JSA result:");
//		for (SignatureWithArgOrRet signatureWithArgOrRet : sig2values.keySet()) {
//			os.println(signatureWithArgOrRet);
//			for (ValueBox value : sig2values.get(signatureWithArgOrRet)) {
//				os.println("[ValueBox]");
//				os.println(value);
//				Automaton automaton = jsa.getAutomaton(value);
//				os.println("[Automation]");
//				// os.println(automaton);
//				os.println(automaton.getShortestExample(true));
//			}
//		}
//	}
//	public static void setHotspots(HashSet<ValueBox> hotspots1) {
//		hotspots = hotspots1;
//	}

    public void addHotspots(BridgeContext context, ValueBox valueBox) {
        hotspots.add(valueBox);
        context2ValueBox.put(context.toString(), valueBox);
    }
}

//class SignatureWithArgOrRet {
//	public String signature;
//
//	// position = -1 means the it is ret value
//	public int position;
//
//	public SignatureWithArgOrRet(String signature, int position) {
//		this.signature = signature;
//		this.position = position;
//	}
//
//	public String toString() {
//		return String.format("[Signature]%s [arg]%d", signature, position);
//	}
//}
