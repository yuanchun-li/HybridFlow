package com.lynnlyc;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.spark.SparkTransformer;

public class PTA {
	public static void runSparkPTA() {
		HashMap<String, String> opt = new HashMap<String, String>();
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
		HashSet<String> methods = new HashSet<String>();
		for (String method_name : Util.webview_methods) {
			methods.add(method_name);
		}
		
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
								StringBuffer sb = new StringBuffer();
								sb.append("\nClass: " + cls);
								sb.append("\n\tMethod: " + m);
								sb.append("\n\t\tUnit: " + u);
								sb.append("\n\t\t\tBase: " + base);
								if (base instanceof Local) {
									PointsToSet reaching_objects = pta.reachingObjects((Local) base);
									sb.append(String.format("\n\t\t\t\tposible string constants: %s",
											reaching_objects.possibleStringConstants()));
									sb.append(String.format("\n\t\t\t\tposible types: %s",
											reaching_objects.possibleTypes()));
								}
								
								for (Value arg : args) {
									sb.append("\n\t\t\tArg: " + arg);
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
