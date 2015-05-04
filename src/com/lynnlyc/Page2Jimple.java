package com.lynnlyc;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.modref.JavaScriptModRef;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.viz.DotUtil;

/*
 * convert web page to Jimple
 * using wala js front-end
 */
public class Page2Jimple {
	public static PropagationCallGraphBuilder page2CGbuilder(URL url)
			throws IOException, WalaException {
		JSCallGraphUtil.setTranslatorFactory(
				new CAstRhinoTranslatorFactory());
		PropagationCallGraphBuilder b = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
		return b;
	}
	
	public static CallGraph CGbuilder2CG(PropagationCallGraphBuilder b) 
			throws IllegalArgumentException, CancelException {
		CallGraph cg = b.makeCallGraph(b.getOptions());
		return cg;
	}
	
	public static Collection<Statement> CGbuilder2Slice(PropagationCallGraphBuilder b)
			throws IllegalArgumentException, CancelException {
		CallGraph cg = b.makeCallGraph(b.getOptions());
		final Collection<Statement> ss = findTargetStatement(cg);
		 
	    SDG sdg = new SDG(cg, b.getPointerAnalysis(), new JavaScriptModRef(),
	    		DataDependenceOptions.FULL, ControlDependenceOptions.FULL);
	    Collection<Statement> slice = Slicer.computeBackwardSlice(sdg, ss);
	    System.out.println("Slicing result:");
	    for (Statement s : slice) {
	        System.out.println(s);
	      }
	    return slice;
	}
	
	public static void printIRs(CallGraph cg) {
		for(CGNode n : cg) {
			IR ir = n.getIR();
			System.out.println(ir);
		}
	}
	
	public static void main(String args[]) {
		URL url;
		String page = "/home/liyc/temp/dummy.html";
		try {
			File htmlFile = new File(page);
			url = htmlFile.toURI().toURL();
//			url = new URL("http://www.baidu.com");
			PropagationCallGraphBuilder b = page2CGbuilder(url);
			CallGraph cg = CGbuilder2CG(b);
			printIRs(cg);
//			CGbuilder2Slice(b);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CancelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (WalaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		try {
//			Util.printIRsForHTML(page);
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
	}
	
	private static class ApplicationLoaderFilter extends Predicate<CGNode> {

	    @Override public boolean test(CGNode o) {
	      if (o instanceof CGNode) {
	        CGNode n = (CGNode) o;
	        return n.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
	      } else if (o instanceof LocalPointerKey) {
	        LocalPointerKey l = (LocalPointerKey) o;
	        return test(l.getNode());
	      } else {
	        return false;
	      }
	    }
	  }
	
	  private static Collection<Statement> findTargetStatement(CallGraph CG) {
		    final Collection<Statement> ss = HashSetFactory.make();
		    for(CGNode n : JSCallGraphUtil.getNodes(CG, "suffix:bridge")) {
		      for(Iterator<CGNode> callers = CG.getPredNodes(n); callers.hasNext(); ) {
		        final CGNode caller = callers.next();
		        for(Iterator<CallSiteReference> sites = CG.getPossibleSites(caller, n); sites.hasNext(); ) {
		          caller.getIR().getCallInstructionIndices(sites.next()).foreach(new IntSetAction() {
		            public void act(int x) {
		              ss.add(new NormalStatement(caller, x));
		            }
		          });
		        }
		      }
		    }
		    return ss;
		  }
}
