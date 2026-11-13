/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.impl.javase;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.cef.CN1JcefRuntime;

/**
 * Bootstraps the reloadable JavaSE classpath while keeping native-backed
 * libraries such as JCEF in the parent classloader. This is used by the CSS
 * compiler and desktop Codename One applications.
 */
public class CN1Bootstrap {
    private static ClassPathLoader rootClassLoader;
    
    /**
     * Checks if JavaFX is available on the classpath.
     * @return 
     */
    public static boolean isJavaFXLoaded() {
        try {
            Class.forName("javafx.embed.swing.JFXPanel");
            return true;
        } catch (Throwable ex) {}
        return false;
    }
    
    /**
     * Checks if CEF is available on the classpath.
     * @return 
     */
    public static boolean isCEFLoaded() {
        try {
            Class.forName("org.cef.CefApp");
            return true;
        } catch (Throwable ex){}
        return false;
    }

    /**
     * Checks whether JCEF Maven provides a native runtime for this platform.
     * The runtime itself is installed lazily when the first BrowserComponent
     * is created.
     *
     * @return true if JCEF is available for this platform
     */
    public static boolean isCEFSupported() {
        try {
            return CN1JcefRuntime.isSupported();
        } catch (Throwable ex) {
            return false;
        }
    }

    private static boolean hasFFmpeg() {
        File ffmpegDir = getFFmpegDir();
        if (ffmpegDir == null || !ffmpegDir.exists()) {
            return false;
        }
        String suffix = isWindows ? ".exe" : "";
        return new File(ffmpegDir, "ffmpeg" + suffix).exists() && new File(ffmpegDir, "ffprobe" + suffix).exists();
    }

    private static File getFFmpegDir() {
        String path = System.getProperty("ffmpeg.dir");
        if (path == null || path.isEmpty()) {
            return null;
        }
        return new File(path);
    }
    
    /**
     * Checks to see if this has already bootstrapped the classpath.  This doesn't necessarily
     * mean that CEF is on the classpath - it just means that this class has already attempted
     * to add it to the classpath.
     * @return 
     */
    public static boolean isBootstrapped() {
        return System.getProperty("CN1Bootstrap", null) != null;
    }
    
    /**
     * <p>Run the given main class with a bootstrapped classpath, keeping JCEF
     * in the parent classloader and running the main method with the reloadable
     * child classloader.</p>
     * 
     * <p>NOTE: This will only execute the main class if bootstrapping had not already occurred.  This is
     * is to allow you to call this method inside your class's main() method without infinite recursion, as follows:</p>
     * 
     * <pre>{@code 
     * public void main(String[] args) {
     *  if (CNBootstrap.run(MyClass.class, args)) return;
     *  // rest of main.. method
     * }
     * }</pre>
     * 
     * @param mainClass The main class to run.
     * @param argv Args to pass to the main() method.
     * @return True If this method triggered a bootstrapping and ran the main method of the given class.  False, if
     *  bootstrapping had already occurred and the main method was not executed.
     * @throws Exception 
     */
    public static boolean run(Class mainClass, String[] argv) throws Exception {
        return run(mainClass.getName(), mainClass.getClassLoader(), argv);
    }
    
    /**
     * Variant of run that takes the name of the main class rather than the class reference.
     * This is useful for ensuring that the main class is loaded using the bootstrapped classloader.
     * @param mainClass THe name of the main class
     * @param argv Args to be passed to the main() method
     * @return false if CN1 was already bootstrapped.  true if CN1 was not yet bootstrapped, and this bootstrapped it.
     * @throws Exception 
     */
    public static boolean run(String mainClass, String[] argv) throws Exception {
        return run(mainClass, Thread.currentThread().getContextClassLoader(), argv);
    }

    private static boolean run(String mainClass, ClassLoader origin, String[] argv) throws Exception {
        if (isBootstrapped()) {
            return false;
        }
        System.setProperty("CN1Bootstrap", "true");
        try {
            // Load the sqlite database Engine JDBC driver in the top level classloader so it's shared
            // this works around the exception: java.lang.UnsatisfiedLinkError: Native Library sqlite already loaded in another classloader
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
        }
        System.setProperty("NSHighResolutionCapable", "true");
        
        List<File> files = bootstrapClasspath(System.getProperty("java.class.path"), origin);
        // The simulator reads java.class.path itself later on, so it has to name what we load from
        StringBuilder cp = new StringBuilder();
        for (File f : files) {
            if (cp.length() > 0) {
                cp.append(File.pathSeparator);
            }
            cp.append(f.getPath());
        }
        System.setProperty("java.class.path", cp.toString());
        for (File dir : SimulatorProject.legacyNativeJarDirs()) {
            if (dir.exists()) {
                
                for (File jar : dir.listFiles()) {
                    if (jar.getName().endsWith(".jar") || jar.getName().endsWith(".zip")) {
                        if (!files.contains(jar)) {
                            files.add(jar);
                            System.setProperty("java.class.path", System.getProperty("java.class.path")+File.pathSeparator+jar.getAbsolutePath());
                        }
                    }
                }
            }
        }
        boolean cefSupported = false;
        boolean fxSupported = false;
        try {
            Class.forName("javafx.embed.swing.JFXPanel");
            fxSupported = true;
        } catch (Throwable ex) {}
        boolean fxOnSystemPath = fxSupported;
        cefSupported = isCEFSupported();
        
        File jmf = new File(System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "jmf-2.1.1e.jar");
        if (jmf.exists()) {
            System.setProperty("java.class.path", System.getProperty("java.class.path") + File.pathSeparator + jmf.getAbsolutePath());
            files.add(jmf);
        }
        
        String implementation = System.getProperty("cn1.javase.implementation", "");

        
        if (implementation.equalsIgnoreCase("cef") && !cefSupported) {
            // We will use CEF
            System.err.println("cn1.javase.implementation=cef but JCEF Maven does not support "
                    + "this platform. Please try a different JavaSE implementation.");
            System.exit(1);
        }
        if (implementation.equalsIgnoreCase("fx") && !fxSupported) {
            System.err.println("cn1.javase.implementation=fx but JavaFX was not found.  Please use a JDK that has JavaFX such as ZuluFX.  https://www.azul.com/downloads/zulu-community/");
            System.exit(1);
        }
        if ("".equals(implementation)) {
            if (cefSupported) {
                System.setProperty("cn1.javase.implementation", "cef");
            } else if (fxSupported) {
                System.setProperty("cn1.javase.implementation", "fx");
            } else {
                System.setProperty("cn1.javase.implementation", "jmf");
            }
        }
        String mediaImplementation = System.getProperty("cn1.javase.mediaImplementation", "");
        if ("".equals(mediaImplementation)) {
            if (hasFFmpeg()) {
                System.setProperty("cn1.javase.mediaImplementation", "ffmpeg");
            } else if (fxSupported) {
                System.setProperty("cn1.javase.mediaImplementation", "fx");
            } else {
                System.setProperty("cn1.javase.mediaImplementation", "jmf");
            }
        }
        
        //loadFXRuntime();
        ClassLoader ldr = rootClassLoader == null ? 
                new ClassPathLoader( files.toArray(new File[files.size()])) :
                new ClassPathLoader(rootClassLoader, files.toArray(new File[files.size()]));
        if (rootClassLoader == null) {
            rootClassLoader = (ClassPathLoader)ldr;
            
            ldr = new ClassPathLoader(rootClassLoader, files.toArray(new File[files.size()]));
            
        }
        ((ClassPathLoader)ldr).addExclude("org.cef.");
        ((ClassPathLoader)ldr).addExclude("me.friwi.jcefmaven.");
        
        Class c;
        try {
            c = Class.forName(mainClass, true, ldr);
        } catch (ClassNotFoundException ex) {
            ClassNotFoundException cnf = new ClassNotFoundException("Codename One could not reload the main class "
                    + mainClass + " in its simulator class loader. It searched: " + files
                    + ". The class must be on java.class.path or reachable through a URLClassLoader that loaded it.");
            cnf.initCause(ex);
            throw cnf;
        }
        Method m = c.getDeclaredMethod("main", String[].class);
        
        m.invoke(null, new Object[]{argv});
        return true;
        
    }
    
    /// The class path the reloadable class loader is built from: `java.class.path`, followed by
    /// the URLs of every `URLClassLoader` from `origin` up to the system loader.
    ///
    /// `java.class.path` alone is not enough. `mvn -Prun-desktop` runs the stub with `exec:java`,
    /// inside Maven's own JVM: there `java.class.path` is Maven's launcher, and the application,
    /// the port and core are reachable only through the loader the plugin built (issue #6203).
    static List<File> bootstrapClasspath(String javaClassPath, ClassLoader origin) {
        List<File> files = new ArrayList<File>();
        if (javaClassPath != null) {
            StringTokenizer t = new StringTokenizer(javaClassPath, File.pathSeparator);
            while (t.hasMoreTokens()) {
                files.add(new File(t.nextToken()));
            }
        }
        List<URLClassLoader> chain = new ArrayList<URLClassLoader>();
        for (ClassLoader l = origin; l != null && l != ClassLoader.getSystemClassLoader(); l = l.getParent()) {
            if (l instanceof URLClassLoader) {
                chain.add(0, (URLClassLoader) l);
            }
        }
        // Parents first, so a class both loaders see resolves as it did before the reload
        for (URLClassLoader l : chain) {
            for (URL u : l.getURLs()) {
                if (!"file".equals(u.getProtocol())) {
                    continue;
                }
                File f = urlToFile(u);
                if (!files.contains(f)) {
                    files.add(f);
                }
            }
        }
        return files;
    }

    /// A `file:` URL as a file. `toURI()` rejects the unescaped spaces of the deprecated
    /// `File.toURL()`, and `File(URI)` rejects a UNC authority, so both fall back to the decoded
    /// path, without the leading slash `getPath()` puts before a Windows drive letter.
    static File urlToFile(URL u) {
        try {
            return new File(u.toURI());
        } catch (URISyntaxException ex) {
            // fall through to the decoded path
        } catch (IllegalArgumentException ex) {
            // fall through to the decoded path
        }
        String path;
        try {
            path = URLDecoder.decode(u.getPath().replace("+", "%2B"), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            path = u.getPath();
        }
        String host = u.getHost();
        if (host != null && host.length() > 0) {
            return new File("\\\\" + host + path.replace('/', '\\'));
        }
        if (path.length() > 2 && path.charAt(0) == '/' && path.charAt(2) == ':') {
            path = path.substring(1);
        }
        return new File(path);
    }

     private static String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isWindows = (OS.indexOf("win") >= 0);
    

}
