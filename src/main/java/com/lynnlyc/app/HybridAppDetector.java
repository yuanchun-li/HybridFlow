package com.lynnlyc.app;

import com.lynnlyc.Config;

/**
 * Created by liyc on 10/13/15.
 *
 */
public class HybridAppDetector {
    public static void main(String[] args) {
        if (args.length != 2) {
            String usage = "usage: HybridAppDetector.main <apk> <android-jar>";
            System.out.println(usage);
            return;
        }
        Config.appFilePath = args[0];
        Config.forceAndroidJarPath = args[1];
        Config.configSoot();

        String result = "no";
        if (AppManager.v().getWebViewClasses() != null && AppManager.v().getWebViewClasses().size() != 0)
            result = "yes";
        else result = "no";
        System.out.println(String.format("%s: %s %s", HybridAppDetector.class.getName(), result, Config.appFilePath));
    }

}
