package com.lynnlyc.web;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.*;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction.CorrelatedPairExtractorFactory;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.loader.CAstAbstractLoader;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.lynnlyc.Config;
import com.lynnlyc.Util;
import com.lynnlyc.web.taintanalysis.JSTaintAnalysis;
import com.lynnlyc.web.taintanalysis.SourceSink;

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
    public HashSet<URL> possible_urls;
    public HashSet<SourceSink> sourceSinks;

    public static WebManager v() {
        if (webManager == null) {
            webManager = new WebManager();
        }
        return webManager;
    }

    private static WebManager webManager = null;
    private WebManager() {
        taint_js_files = new HashSet<>();
        possible_urls = new HashSet<>();
        sourceSinks = new HashSet<>();
    }

    public void addTaintJsFile(File taintJs) {
        taint_js_files.add(taintJs);
    }

    public void addPossibleURL(URL possible_url) {
        possible_urls.add(possible_url);
    }

    public void setSourceSinks(HashSet<String> htmlSourceSinks) {
        for (String htmlSourceSinkStr : htmlSourceSinks) {
            this.sourceSinks.add(new SourceSink(htmlSourceSinkStr));
        }
    }

    public void runTaintAnalysis() {
        for (URL possible_url : possible_urls) {
            CallGraph cg = generateCG(possible_url, taint_js_files);
            runTaintAnalysis(cg);
        }
    }

    public void runTaintAnalysis(CallGraph cg) {
        JSTaintAnalysis jsTaint = new JSTaintAnalysis(cg, sourceSinks);
        jsTaint.analyze();
//        printIRs(cg);
    }

    public static void printIRs(CallGraph cg) {
        IClassHierarchy cha = cg.getClassHierarchy();
        for(CGNode n : cg) {
            IR ir = n.getIR();
            System.out.println(ir);
        }
    }

    public CallGraph generateCG(URL possible_url, HashSet<File> jsFiles) {
        CallGraph cg = null;
        try {
            Util.LOGGER.info("Analyzing " + possible_url.toString());
            JavaScriptTranslatorFactory javaScriptTranslatorFactory = new CAstRhinoTranslatorFactory();
            JSCallGraphUtil.setTranslatorFactory(javaScriptTranslatorFactory);
            JSCallGraphBuilderUtil.CGBuilderType builderType = JSCallGraphBuilderUtil.CGBuilderType.ZERO_ONE_CFA;
            IRFactory<IMethod> irFactory = AstIRFactory.makeDefaultFactory();
            CAstRewriterFactory preprocessor = new CorrelatedPairExtractorFactory(javaScriptTranslatorFactory, possible_url);
            JavaScriptLoaderFactory loaders = new WebPageLoaderFactory(javaScriptTranslatorFactory, preprocessor);

            Set<SourceModule> scripts = HashSetFactory.make();
            JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
            scripts.add(JSCallGraphBuilderUtil.getPrologueFile("prologue.js"));
            scripts.add(JSCallGraphBuilderUtil.getPrologueFile("preamble.js"));
            for (File jsFile : jsFiles) {
                try {
                    scripts.add(CAstCallGraphUtil.makeSourceModule(jsFile.toURI().toURL(), jsFile.getName()));
                } catch (Exception ignored) {
                }
            }
            try {
                scripts.addAll(WebUtil.extractScriptFromHTML(possible_url, true).fst);
            } catch (TranslatorToCAst.Error e) {
                SourceModule dummy = new SourceURLModule(possible_url);
                scripts.add(dummy);
                ((CAstAbstractLoader) loaders.getTheLoader()).addMessage(dummy, e.warning);
            }
            SourceModule[] scriptsArray = scripts.toArray(new SourceModule[scripts.size()]);

            JSCFABuilder builder = JSCallGraphBuilderUtil.makeCGBuilder(loaders, scriptsArray, builderType, irFactory);
            builder.setContextSelector(new PropertyNameContextSelector(builder.getAnalysisCache(), 2, builder.getContextSelector()));
            builder.setBaseURL(possible_url);
            AnalysisOptions options = builder.getOptions();
//            options.setTraceStringConstants(true);
//            options.setUseLexicalScopingForGlobals(true);
            cg = builder.makeCallGraph(options);
        } catch (IOException | WalaException | CancelException e) {
            e.printStackTrace();
        }
        return cg;
    }
}
