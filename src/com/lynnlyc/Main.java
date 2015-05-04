package com.lynnlyc;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.EntryPoints;
import soot.Local;
import soot.PackManager;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.util.Chain;

public class Main {
	public static HashSet<String> methods = new HashSet<String>();
	
	private static PrintStream ps;
	
	public static void main(String[] args) throws FileNotFoundException {
		prepare(args);
		PTA.runSparkPTA();
		PTA.dumpPTAresult(ps);
		
//		runJSA();
		
		shutdown();
	}
	
	private static void shutdown() {
		ps.close();
	}

	private static void prepare(String[] args) throws FileNotFoundException {
		Options.v().parse(args);
		Options.v().set_prepend_classpath(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_whole_program(true);
		Options.v().set_src_prec(Options.src_prec_apk);
		
		String output_dir = Options.v().output_dir();
		File file = new File(output_dir + "/webview_PTA_result.txt");
		ps = new PrintStream(file);
		
		Scene.v().loadNecessaryClasses();
		Scene.v().setEntryPoints(findEntryPoints());
	}
	
	private static List<SootMethod> findEntryPoints() {
		ArrayList<SootMethod> entries = new ArrayList<SootMethod>();
		for (SootClass cls : Scene.v().getClasses()) {
			if (!cls.isApplicationClass()) continue;
			if (cls.isAbstract()) continue;
			if (cls.getPackageName().startsWith("android.support")) continue;
			
			for (SootMethod m : cls.getMethods()) {
				for (String e : Util.possible_entries) {
					if (m.getName().contains(e)) {
//						System.out.println(String.format("[%s]->[%s]", cls, m));
						entries.add(m);
					}
				}
			}
		}
//		System.out.println(entries.size());
//		System.out.println(entries);
		return entries;
	}
	
}
