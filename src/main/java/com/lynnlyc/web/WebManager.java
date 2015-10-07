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
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.lynnlyc.Util;
import com.lynnlyc.web.taintanalysis.JSTaintAnalysis;
import com.lynnlyc.web.taintanalysis.HTMLSourceSink;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yuanchun on 5/5/15.
 * Package: webview-flow
 */
public class WebManager {
    private Collection<File> taintJsFiles;
    private Collection<URL> possibleURLs;
    private Collection<HTMLSourceSink> sourceSinks;

    public static WebManager v() {
        if (webManager == null) {
            webManager = new WebManager();
        }
        return webManager;
    }

    private static WebManager webManager = null;
    private WebManager() {
        taintJsFiles = new HashSet<>();
        possibleURLs = new HashSet<>();
        sourceSinks = new HashSet<>();
    }

    public void setTaintJsFiles(Collection<File> taintJsFiles) {
        this.taintJsFiles = taintJsFiles;
    }

    public void addPossibleURL(URL possible_url) {
        possibleURLs.add(possible_url);
    }

    public void setSourceSinks(Collection<String> htmlSourceSinks) {
        for (String htmlSourceSinkStr : htmlSourceSinks) {
            this.sourceSinks.add(new HTMLSourceSink(htmlSourceSinkStr));
        }
    }

    public void runTaintAnalysis(PrintStream ps) {
        ps.println("analysis result:");
        for (URL possible_url : possibleURLs) {
            try {
                CallGraph cg = generateCG(possible_url, taintJsFiles);
                ps.println("\n\nrunning taint analysis on URL: " + possible_url.toString());
                runTaintAnalysis(cg, ps);
            } catch (Exception e) {
                Util.LOGGER.warning("analysis failed on URL: " + possible_url.toString());
            }
        }
    }

    public void runTaintAnalysis(CallGraph cg, PrintStream ps) {
        JSTaintAnalysis jsTaint = new JSTaintAnalysis(cg, sourceSinks);
        jsTaint.analyze();
        jsTaint.dumpResult(ps);
//        printIRs(cg);
    }

    public static void printIRs(CallGraph cg) {
        IClassHierarchy cha = cg.getClassHierarchy();
        for(CGNode n : cg) {
            IR ir = n.getIR();
            System.out.println(ir);
        }
    }

    public CallGraph generateCG(URL possible_url, Collection<File> jsFiles) {
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
