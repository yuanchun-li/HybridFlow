package com.lynnlyc;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class Util {
    public static final Logger LOGGER = Logger.getLogger("Webview-flow");
	
	public static void printIRsForHTML(String filename) throws IllegalArgumentException, MalformedURLException, IOException,
	CancelException, WalaException, Error {
		// use Rhino to parse JavaScript
		JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		// add model for DOM APIs
		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
		URL url = (new File(filename)).toURI().toURL();
		Pair<Set<MappedSourceModule>, File> p = WebUtil.extractScriptFromHTML(url, true);
		SourceModule[] scripts = p.fst.toArray(new SourceModule[] {});
		JavaScriptLoaderFactory loaders = new WebPageLoaderFactory(JSCallGraphUtil.getTranslatorFactory());
		CAstAnalysisScope scope = new CAstAnalysisScope(scripts, loaders, Collections.singleton(JavaScriptLoader.JS));
		IClassHierarchy cha = ClassHierarchy.make(scope, loaders, JavaScriptLoader.JS);
		com.ibm.wala.cast.js.util.Util.checkForFrontEndErrors(cha);
		printIRsForCHA(cha, new Predicate<String>() {

			@Override
			public boolean test(String t) {
				return t.startsWith("Lprologue.js") || t.startsWith("Lpreamble.js");
			}
		});
	}
	
	protected static void printIRsForCHA(IClassHierarchy cha, Predicate<String> exclude) {
		// for constructing IRs
		IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory();
		for (IClass klass : cha) {
			// ignore models of built-in JavaScript methods
			String name = klass.getName().toString();
			if (exclude.test(name)) continue;
			// get the IMethod representing the code
			IMethod m = klass.getMethod(AstMethodReference.fnSelector);
			if (m != null) {
				IR ir = factory.makeIR(m, Everywhere.EVERYWHERE, new SSAOptions());
				System.out.println(ir);
		        if (m instanceof AstMethod) {
		        	AstMethod astMethod = (AstMethod) m;
		        	System.out.println(astMethod.getSourcePosition());
		        }
		        System.out.println("===================================================\n");
			}
		}
	}

	public static List<SootMethod> findEntryPoints() {
		ArrayList<SootMethod> entries = new ArrayList<SootMethod>();
		for (SootClass cls : Scene.v().getClasses()) {
			if (!cls.isApplicationClass()) continue;
			if (cls.isAbstract()) continue;
			if (cls.getPackageName().startsWith("android.support")) continue;

			for (SootMethod m : cls.getMethods()) {
				for (String e : Config.possible_entries) {
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

    public static String getTimeString() {
        long timeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-hhmmss");
        Date date = new Date(timeMillis);
        return sdf.format(date);
    }

    public static void printUsage() {
        String usage = "Usage: java Main [options]\n" +
                "\t-app\tpath to the apk file\n" +
                "\t-web\tpath to webpage folder\n" +
                "\t-android-jars\tpath to sdk platforms\n" +
                "\t-force-android-jar\tpath to android.jar\n" +
                "\t-d\tpath to output\n" +
                "\t-f\toutput format, jimple or dex, default is dex" +
                "\texample: java Main -app XXX.apk -web path/to/webpage -android-jars path/to/sdk/platforms -d path/to/output\n";
        System.out.println(usage);
    }

    public static void output() {
        PackManager.v().writeOutput();
    }
}
