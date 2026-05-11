/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.util;

import com.codename1.io.Log;
import com.codename1.ui.Display;
import java.io.File;
import java.net.URL;

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

    public static File findJavac() {
        String javaHome = System.getProperty("java.home");
        File javac = new File(new File(javaHome), "bin" + File.separator + "javac");
        if (!javac.exists()) {
            javac = new File(javac.getParentFile(), "javac.exe");

        }
        if (!javac.exists()) {
            javac = new File(new File(javaHome).getParentFile(), "bin" + File.separator + "javac");
        }
        if (!javac.exists()) {
            javac = new File(javac.getParentFile(), "javac.exe");

        }
        if (!javac.exists()) {
            String PATH = System.getenv("PATH");
            if (PATH != null) {
                String[] parts = PATH.split(File.pathSeparator);
                for (String path : parts) {
                    javac = new File(path + File.separator + "javac");
                    if (!javac.exists()) {
                        javac = new File(javac.getParentFile(), "javac.exe");
                    }
                    if (javac.exists()) {
                        return javac;
                    }
                }
            }
        }
        if (javac.exists()) {
            return javac;
        }
        return null;
    }
    
    /**
     * Locate the codenameone-designer:jar-with-dependencies jar inside the local
     * Maven (~/.m2) repository, using the version of the codenameone-core jar that
     * is currently loaded into this JVM. Returns null if the running framework is
     * not loaded from m2 (e.g. running from a build directory) or if the matching
     * designer jar has not been resolved yet.
     *
     * <p>The Maven plugin declares codenameone-designer as a plugin dependency, so
     * any plugin invocation (cn1:run, mvn compile when bound to the css goal, etc.)
     * implicitly fetches the matching designer jar into m2. This lookup lets the
     * simulator runtime use that exact version even when codename1.designer.jar
     * isn't passed as a system property -- avoiding a stale ~/.codenameone/designer_1.jar
     * fallback.
     */
    public static File findDesignerJarInM2() {
        try {
            URL location = Display.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return null;
            }
            File coreJar = new File(location.toURI());
            // Expected layout: <repo>/com/codenameone/codenameone-core/<version>/codenameone-core-<version>.jar
            File versionDir = coreJar.getParentFile();
            if (versionDir == null) return null;
            File coreDir = versionDir.getParentFile();
            if (coreDir == null) return null;
            File codenameoneGroupDir = coreDir.getParentFile();
            if (codenameoneGroupDir == null) return null;
            if (!"codenameone-core".equals(coreDir.getName())) {
                return null;
            }
            String version = versionDir.getName();
            File designerVersionDir = new File(codenameoneGroupDir, "codenameone-designer" + File.separator + version);
            File designer = new File(designerVersionDir, "codenameone-designer-" + version + "-jar-with-dependencies.jar");
            if (designer.isFile()) {
                return designer;
            }
        } catch (Throwable t) {
            // Best-effort lookup. Any unexpected layout means we can't resolve via m2.
        }
        return null;
    }

    public static boolean isRunningInJDK() {
        if (!isRunningInJDKChecked) {
            isRunningInJDKChecked = true;
            String javaHome = System.getProperty("java.home");
            File javac = new File(new File(javaHome), "bin" + File.separator + "javac");
            if (!javac.exists()) {
                javac = new File(javac.getParentFile(), "javac.exe");

            }
            if (!javac.exists()) {
                javac = new File(new File(javaHome).getParentFile(), "bin" + File.separator + "javac");
            }
            if (!javac.exists()) {
                javac = new File(javac.getParentFile(), "javac.exe");

            }
            isRunningInJDK = javac.exists();
        }
        return isRunningInJDK;

    }
    
}
