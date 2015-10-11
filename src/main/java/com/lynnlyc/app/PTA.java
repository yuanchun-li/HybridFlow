package com.lynnlyc.app;

import com.lynnlyc.Config;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.spark.SparkTransformer;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class PTA {
	public static void runSparkPTA() {
		HashMap<String, String> opt = new HashMap<>();
		opt.put("enabled", "true");
//		opt.put("verbose", "true");
		opt.put("set-impl", "double");
		opt.put("double-set-old", "hybrid");
		opt.put("double-set-new", "hybrid");
		opt.put("propagator", "worklist");
		opt.put("string-constants", "true");
		SparkTransformer.v().transform("", opt);
	}
	
	public static void dumpPTAresult(PrintStream os) {
		os.println("PTA results:");
		HashSet<String> methods = new HashSet<>();
		Collections.addAll(methods, Config.webview_methods);
		
		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		for (SootClass cls : Scene.v().getClasses()) {
			for (SootMethod m : cls.getMethods()) {
				if (!m.hasActiveBody()) continue;
				Body b = m.getActiveBody();
				for (Unit u : b.getUnits()) {
					if (u instanceof JInvokeStmt) {
						InvokeExpr expr = ((JInvokeStmt) u).getInvokeExpr();
						if (expr instanceof JVirtualInvokeExpr) {
							JVirtualInvokeExpr virtual_expr = (JVirtualInvokeExpr) expr;
							
							if (methods.contains(virtual_expr.getMethod().getName())){
								Value base = virtual_expr.getBase();
								List<Value> args = virtual_expr.getArgs();
								StringBuilder sb = new StringBuilder();
								sb.append("\nClass: ").append(cls);
								sb.append("\n\tMethod: ").append(m);
								sb.append("\n\t\tUnit: ").append(u);
								sb.append("\n\t\t\tBase: ").append(base);
								if (base instanceof Local) {
									PointsToSet reaching_objects = pta.reachingObjects((Local) base);
									sb.append(String.format("\n\t\t\t\tposible string constants: %s",
											reaching_objects.possibleStringConstants()));
									sb.append(String.format("\n\t\t\t\tposible types: %s",
											reaching_objects.possibleTypes()));
								}
								
								for (Value arg : args) {
									sb.append("\n\t\t\tArg: ").append(arg);
									if (arg instanceof Local) {
										PointsToSet reaching_objects = pta.reachingObjects((Local) arg);
										sb.append(String.format("\n\t\t\t\tposible string constants: %s",
												reaching_objects.possibleStringConstants()));
										sb.append(String.format("\n\t\t\t\tposible types: %s",
												reaching_objects.possibleTypes()));
									}
								}
								sb.append("\n");
								os.print(sb.toString());
								os.flush();
								break;
							}
						}
					}
				}
			}
		}
	}
}
