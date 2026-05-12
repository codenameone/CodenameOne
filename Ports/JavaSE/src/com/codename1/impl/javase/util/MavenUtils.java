/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.util;

import com.codename1.io.Log;
import com.codename1.ui.Display;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
            return findDesignerJarInM2(new File(location.toURI()));
        } catch (Throwable t) {
            // Best-effort lookup. Any unexpected layout means we can't resolve via m2.
        }
        return null;
    }

    /**
     * Test seam for {@link #findDesignerJarInM2()}: takes the codenameone-core jar
     * path explicitly so the resolution logic can be exercised against a fake m2
     * layout in a temp directory.
     */
    static File findDesignerJarInM2(File coreJar) {
        try {
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
            // The published jar-with-dependencies artifact is *not* directly runnable:
            // maven/designer/pom.xml's antrun step renames the shaded jar to
            // designer_1.jar and re-zips it, so this file is a zip wrapper containing
            // a single designer_1.jar entry with no top-level Main-Class manifest.
            // AbstractCN1Mojo.getDesignerJar (in the maven plugin) unzips it on demand
            // and returns the inner jar; we mirror that here so the CSSWatcher
            // fallback path receives a path that `java -jar` can actually launch.
            File wrapperZip = new File(designerVersionDir, "codenameone-designer-" + version + "-jar-with-dependencies.jar");
            if (!wrapperZip.isFile()) {
                return null;
            }
            File extracted = new File(wrapperZip.getParentFile(), wrapperZip.getName() + "-extracted");
            File innerJar = new File(extracted, "designer_1.jar");
            if (!innerJar.isFile() || innerJar.lastModified() < wrapperZip.lastModified()) {
                extractInnerJar(wrapperZip, extracted);
            }
            if (innerJar.isFile()) {
                return innerJar;
            }
        } catch (Throwable t) {
            // Best-effort lookup. Any unexpected layout means we can't resolve via m2.
        }
        return null;
    }

    private static final String INNER_JAR_NAME = "designer_1.jar";

    /**
     * Extracts the single expected inner jar from the designer wrapper artifact.
     *
     * <p>The wrapper produced by {@code maven/designer/pom.xml} contains exactly
     * one entry named {@code designer_1.jar} at the root. To stay safe against
     * Zip Slip even if an unexpected artifact is dropped in m2, this method:
     * (1) writes only to a single, fixed destination path under {@code destDir}
     * (never derived from the archive's entry name), and (2) skips any entry
     * whose name isn't the literal expected filename. A malicious entry like
     * {@code ../../etc/passwd} therefore never participates in path
     * construction; in the worst case the loop finds no match and throws.</p>
     */
    private static void extractInnerJar(File wrapperZip, File destDir) throws IOException {
        if (!destDir.exists() && !destDir.mkdirs() && !destDir.isDirectory()) {
            throw new IOException("Could not create designer extraction directory: " + destDir.getAbsolutePath());
        }
        File innerJar = new File(destDir, INNER_JAR_NAME);
        InputStream in = new FileInputStream(wrapperZip);
        try {
            ZipInputStream zis = new ZipInputStream(in);
            try {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    if (!INNER_JAR_NAME.equals(entry.getName())) {
                        // Unexpected entry. Skip it rather than materialize a
                        // file path derived from untrusted archive metadata.
                        continue;
                    }
                    OutputStream fos = new FileOutputStream(innerJar);
                    try {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = zis.read(buf)) > 0) {
                            fos.write(buf, 0, n);
                        }
                    } finally {
                        fos.close();
                    }
                    return;
                }
                throw new IOException("Wrapper zip does not contain a " + INNER_JAR_NAME
                        + " entry: " + wrapperZip.getAbsolutePath());
            } finally {
                zis.close();
            }
        } finally {
            in.close();
        }
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
