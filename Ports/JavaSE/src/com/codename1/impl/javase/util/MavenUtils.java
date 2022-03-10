/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.util;

import com.codename1.io.Log;
import java.io.File;

/**
 *
 * @author shannah
 */
public class MavenUtils {
    private static boolean isRunningInJDK;
    private static boolean isRunningInMaven;
    private static boolean isRunningInJDKChecked;
    private static boolean isRunningInMavenChecked;
    public static boolean isRunningInMaven() {
        if (!isRunningInMavenChecked) {
            isRunningInMavenChecked = true;
        
            isRunningInMaven = System.getProperty("cn1.library.path", null) != null
                || System.getProperty("maven.home", null) != null
                || System.getProperty("codename1.designer.jar", null) != null;
        }
        return isRunningInMaven;
    }
    
    public static boolean isRunningInJDK() {
        if (!isRunningInJDKChecked) {
            isRunningInJDKChecked = true;
            String javaHome = System.getProperty("java.home");
            File javac = new File(new File(javaHome), "bin" + File.separator + "javac");
            if (!javac.exists()) {
                javac = new File(javac.getParentFile(), "javac.exe");

            }
            isRunningInJDK = javac.exists();
        }
        return isRunningInJDK;

    }
    
}
