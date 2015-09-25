package com.lynnlyc.web;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.modref.JavaScriptModRef;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.IntSetAction;
import com.lynnlyc.bridge.WALA2Soot;

/*
 * convert web page to Jimple
 * using wala js
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
		String page = "/Users/yuanchun/temp/dummy.html";
		try {
			File htmlFile = new File(page);
			url = htmlFile.toURI().toURL();
			url = new URL("http://lynnblog.sinaapp.com/webview/");
			PropagationCallGraphBuilder b = page2CGbuilder(url);
			CallGraph cg = CGbuilder2CG(b);
			WALA2Soot.init();
			convertCGnodes(cg);
//			printIRs(cg);
//			CGbuilder2Slice(b);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		} catch (WalaException e) {
			e.printStackTrace();
		}
		
//		try {
//			Util.printIRsForHTML(page);
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	private static void convertCGnodes(CallGraph cg) {
		for(CGNode n : cg) {
			WALA2Soot.convertCGnode(n);
		}
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
