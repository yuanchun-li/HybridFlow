package com.lynnlyc.bridge;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.TypeReference;
import soot.SootMethod;
import soot.Type;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by yuanchun on 5/10/15.
 * Package: webview-flow
 */
public class WALA2Soot {
    private static final String[] excludeClasses = {
            "LFunction",
            "Lprologue.js",
            "Lpreamble.js",
            "LObject",
            "LString",
            "LArray",
            "LStringObject"
    };

    private static final HashSet<String> excludeClassesSet = new HashSet<String>();

    public static void init() {
        for (String excludeClass : excludeClasses) {
            excludeClassesSet.add(excludeClass);
        }
    }

    public static SootMethod convertCGnode(CGNode walanode) {
        IMethod walaMethod = walanode.getMethod();
        IClass declaringClass = walaMethod.getDeclaringClass();
        String declaringClassName = declaringClass.getName().toString();
        String declaringClassNamePrefix = declaringClassName;
        if (declaringClassName.indexOf('/') != -1)
            declaringClassNamePrefix = declaringClassName.substring(0, declaringClassName.indexOf('/'));

        if (excludeClassesSet.contains(declaringClassNamePrefix)) {
            return null;
        }

//        if (!declaringClassName.startsWith("Lwebview")) {
//            return null;
//        }

        System.out.println(walanode.getIR());

        String name = walaMethod.getName().toString();
        String signature = walanode.getMethod().getSignature();
        int paracount = walaMethod.getNumberOfParameters();
        ArrayList<soot.Type> paratypes = new ArrayList<>();
        for (int i = 0; i < paracount; i++) {
            TypeReference paratype = walaMethod.getParameterType(i);
            String paratypeName = paratype.getName().toString();
        }
        Type returnType = null;

//        SootMethod sootnode = new SootMethod(name, paratypes, returnType);
        return null;
    }
}
