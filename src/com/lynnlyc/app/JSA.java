package com.lynnlyc.app;

import com.ibm.wala.cast.tree.CAstType;
import com.lynnlyc.Config;
import com.lynnlyc.Util;
import dk.brics.automaton.Automaton;
import dk.brics.string.StringAnalysis;
import dk.brics.string.intermediate.Hotspot;
import soot.Scene;
import soot.ValueBox;

import java.io.PrintStream;
import java.security.cert.PKIXRevocationChecker;
import java.util.*;

/*
 * analyze String argument of webview API
 * using Java string analyzer
 */
public class JSA {
	public static StringAnalysis jsa;

	private static HashSet<ValueBox> hotspots = null;
	public static ArrayList<SignatureWithArgOrRet> signatureWithArgOrRets = new ArrayList<SignatureWithArgOrRet>();
	public static HashMap<SignatureWithArgOrRet, List<ValueBox>> sig2values = new HashMap<SignatureWithArgOrRet, List<ValueBox>>();

	public static void init() {
		SignatureWithArgOrRet signatureWithArgOrRet = new SignatureWithArgOrRet(
				Util.loadUrlSig, 0);
		signatureWithArgOrRets.add(signatureWithArgOrRet);

		signatureWithArgOrRet = new SignatureWithArgOrRet(
				Util.addJavascriptInterfaceSig, 1);
		signatureWithArgOrRets.add(signatureWithArgOrRet);
	}

	public static StringAnalysis run() {
		jsa = new StringAnalysis(hotspots);
		return jsa;
	}
	
	public static void dumpJSAresults(PrintStream os) {
		if (jsa == null) {
			os.println("No JSA result.");
			return;
		}

		os.println("JSA result:");
		for (SignatureWithArgOrRet signatureWithArgOrRet : sig2values.keySet()) {
			os.println(signatureWithArgOrRet);
			for (ValueBox value : sig2values.get(signatureWithArgOrRet)) {
				os.println("[ValueBox]");
				os.println(value);
				Automaton automaton = jsa.getAutomaton(value);
				os.println("[Automation]");
				// os.println(automaton);
				os.println(automaton.getShortestExample(true));
			}
		}
	}

	public static void setHotspots(HashSet<ValueBox> hotspots1) {
		hotspots = hotspots1;
	}

	public static HashSet<ValueBox> getHotspots() {
		return hotspots;
	}
}

class SignatureWithArgOrRet {
	public String signature;

	// position = -1 means the it is ret value
	public int position;

	public SignatureWithArgOrRet(String signature, int position) {
		this.signature = signature;
		this.position = position;
	}

	public String toString() {
		return String.format("[Signature]%s [arg]%d", signature, position);
	}
}
