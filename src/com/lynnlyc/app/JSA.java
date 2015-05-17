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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/*
 * analyze String argument of webview API
 * using Java string analyzer
 */
public class JSA {
	public static StringAnalysis jsa;

	public static ArrayList<SignatureWithArgOrRet> signatureWithArgOrRets = new ArrayList<SignatureWithArgOrRet>();
	public static HashMap<SignatureWithArgOrRet, List<ValueBox>> sig2values = new HashMap<SignatureWithArgOrRet, List<ValueBox>>();

	public static void init() {
		SignatureWithArgOrRet signatureWithArgOrRet = new SignatureWithArgOrRet(
				"<android.webkit.WebView: void loadUrl(java.lang.String)>", 0);
		signatureWithArgOrRets.add(signatureWithArgOrRet);

		signatureWithArgOrRet = new SignatureWithArgOrRet(
				"<android.webkit.WebView: void addJavascriptInterface(java.lang.Object,java.lang.String)>", 1);
		signatureWithArgOrRets.add(signatureWithArgOrRet);
	}

	public static void run() {
		init();

		Collection<ValueBox> hotspots = new ArrayList<ValueBox>();
		for (SignatureWithArgOrRet signatureWithArgOrRet : signatureWithArgOrRets) {
			List<ValueBox> values;
			if (signatureWithArgOrRet.position == -1)
				values = StringAnalysis.getReturnExpressions(signatureWithArgOrRet.signature);
			else
				values = StringAnalysis.getArgumentExpressions(signatureWithArgOrRet.signature, signatureWithArgOrRet.position);
			sig2values.put(signatureWithArgOrRet, values);
			hotspots.addAll(values);
		}

		jsa = new StringAnalysis(hotspots);
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
				os.println(automaton);
				os.println(automaton.getFiniteStrings());
			}
		}
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
