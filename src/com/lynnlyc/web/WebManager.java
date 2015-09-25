package com.lynnlyc.web;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.PropertyNameContextSelector;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction.CorrelatedPairExtractorFactory;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.loader.CAstAbstractLoader;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.lynnlyc.Util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yuanchun on 5/5/15.
 * Package: webview-flow
 */
public class WebManager {
    public HashSet<File> taint_js_files;
    public HashSet<File> html_files;

    public static WebManager v() {
        if (webManager == null) {
            webManager = new WebManager();
        }
        return webManager;
    }

    private static WebManager webManager = null;
    private WebManager() {
        taint_js_files = new HashSet<>();
        html_files = new HashSet<>();
    }

    public void addTaintJsFile(File taintJs) {
        taint_js_files.add(taintJs);
    }

    public void addHtmlFile(File htmlFile) {
        html_files.add(htmlFile);
    }

    public void runTaintAnalysis() {
        for (File htmlFile : html_files) {
            runTaintAnalysisOfOnePage(htmlFile, taint_js_files);
        }
    }

    public void runTaintAnalysisOfOnePage(File htmlFile, HashSet<File> jsFiles) {
        try {
            Util.LOGGER.info("Analyzing " + htmlFile.getName());
            JavaScriptTranslatorFactory javaScriptTranslatorFactory = new CAstRhinoTranslatorFactory();
            JSCallGraphUtil.setTranslatorFactory(javaScriptTranslatorFactory);
            URL htmlFileURL = htmlFile.toURI().toURL();
            JSCallGraphBuilderUtil.CGBuilderType builderType = JSCallGraphBuilderUtil.CGBuilderType.ZERO_ONE_CFA;
            IRFactory<IMethod> irFactory = AstIRFactory.makeDefaultFactory();
            CAstRewriterFactory preprocessor = new CorrelatedPairExtractorFactory(javaScriptTranslatorFactory, htmlFileURL);
            JavaScriptLoaderFactory loaders = new WebPageLoaderFactory(javaScriptTranslatorFactory, preprocessor);

            Set<SourceModule> scripts = HashSetFactory.make();
            JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
            scripts.add(JSCallGraphBuilderUtil.getPrologueFile("prologue.js"));
            scripts.add(JSCallGraphBuilderUtil.getPrologueFile("preamble.js"));
            try {
                scripts.addAll(WebUtil.extractScriptFromHTML(htmlFileURL, true).fst);
            } catch (TranslatorToCAst.Error e) {
                SourceModule dummy = new SourceURLModule(htmlFileURL);
                scripts.add(dummy);
                ((CAstAbstractLoader)loaders.getTheLoader()).addMessage(dummy, e.warning);
            }
            for (File jsFile : jsFiles) {
                scripts.add(CAstCallGraphUtil.makeSourceModule(jsFile.toURI().toURL(), jsFile.getName()));
            }
            SourceModule[] scriptsArray = scripts.toArray(new SourceModule[ scripts.size() ]);

            JSCFABuilder builder = JSCallGraphBuilderUtil.makeCGBuilder(loaders, scriptsArray, builderType, irFactory);
            builder.setContextSelector(new PropertyNameContextSelector(builder.getAnalysisCache(), 2, builder.getContextSelector()));
            builder.setBaseURL(htmlFileURL);
            CallGraph cg = builder.makeCallGraph(builder.getOptions());
            printIRs(cg);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WalaException e) {
            e.printStackTrace();
        } catch (CancelException e) {
            e.printStackTrace();
        }
    }

    public static void printIRs(CallGraph cg) {
        for(CGNode n : cg) {
            IR ir = n.getIR();
            System.out.println(ir);
        }
    }
}
