package com.lynnlyc;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.lynnlyc.app.AppManager;
import com.lynnlyc.app.PTA;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class Main {
	public static void main(String[] args) {

		if (!Config.parseArgs(args)) {
            Util.printUsage();
            return;
        }

        Config.init();

        AppManager appManager = new AppManager(Config.appFilePath);
//		appManager.runPTA();
        appManager.outputJimple();

	}
}
