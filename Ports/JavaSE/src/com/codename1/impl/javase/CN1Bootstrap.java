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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A utility class which will dynamically add CEF to the classpath if it is available.  This is
 * used by the CSS compiler, and should also be used by other desktop CN1 apps that need to use
 * CEF.
 * @author shannah
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
     * Checks to see if this has already bootstrapped the classpath.  This doesn't necessarily
     * mean that CEF is on the classpath - it just means that this class has already attempted
     * to add it to the classpath.
     * @return 
     */
    public static boolean isBootstrapped() {
        return System.getProperty("CN1Bootstrap", null) != null;
    }
    
    /**
     * <p>Run the given mainclass with a bootstrapped classpath (i.e. it will first attempt to 
     * add CEF to the classpath, and then run the main() method of the given class using the
     * bootstrapped classloader.</p>
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
        return run(mainClass.getName(), argv);
    }
    
    /**
     * Variant of run that takes the name of the main class rather than the class reference.
     * This is useful for ensuring that the main class is loaded using the bootstrapped classloader.
     * @param mainClass THe name of the main class
     * @param argv Args to be passed to the main() method
     * @return
     * @throws Exception 
     */
    public static boolean run(String mainClass, String[] argv) throws Exception {
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
        
        StringTokenizer t = new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
        
        List<File> files = new ArrayList<File>();
        int len = t.countTokens();
        for (int iter = 0; iter < len; iter++) {
            files.add(new File(t.nextToken()));
        }
        File javase = new File("native" + File.separator + "javase");
        File libJavase = new File("lib" + File.separator + "impl" + File.separator + "native" + File.separator + "javase");
        for (File dir : new File[]{javase, libJavase}) {
            if (dir.exists()) {
                
                for (File jar : dir.listFiles()) {
                    if (jar.getName().endsWith(".jar")) {
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
        File cef = new File(System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "cef");
        if (cef.exists()) {
            if (isUnix && !is64Bit) {
                System.out.println("Found CEF, but not using because CEF is only supported on 64 bit platforms.  Try running inside a 64 bit JVM");
            } else {
                
            
                cefSupported = true;
                System.out.println("Adding CEF to classpath");
                String cn1LibPath = System.getProperty("cn1.library.path", ".");
                String bitSuffix = is64Bit ? "64" : "32";
                String nativeDir = isMac ? "macos64" : isWindows ? ("lib" + File.separator + "win"+bitSuffix) : ("lib" + File.separator + "linux"+bitSuffix);
                System.setProperty("cn1.library.path", cn1LibPath + File.pathSeparator + cef.getAbsolutePath() + File.separator + nativeDir);

                // Necessary to modify java.libary.path property on windows as it is used by CefApp to locate jcef_helper.exe
                System.setProperty("java.library.path", cef.getAbsolutePath()+File.separator+nativeDir+File.pathSeparator+System.getProperty("java.library.path", "."));
                for (File jar : cef.listFiles()) {
                    if (jar.getName().endsWith(".jar")) {
                        files.add(jar);
                    }
                }
            }
        }
        
        File jmf = new File(System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "jmf-2.1.1e.jar");
        if (jmf.exists()) {
            System.setProperty("java.class.path", System.getProperty("java.class.path") + File.pathSeparator + jmf.getAbsolutePath());
            files.add(jmf);
        }
        
        String implementation = System.getProperty("cn1.javase.implementation", "");

        
        if (implementation.equalsIgnoreCase("cef") && !cefSupported) {
            // We will use CEF
            System.err.println("cn1.javase.implementation=cef but CEF was not found.  Please update your Codename One libraries and try again.\nAlternatively, you can try using a different implementation.");
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
        
        //loadFXRuntime();
        ClassLoader ldr = rootClassLoader == null ? 
                new ClassPathLoader( files.toArray(new File[files.size()])) :
                new ClassPathLoader(rootClassLoader, files.toArray(new File[files.size()]));
        if (rootClassLoader == null) {
            rootClassLoader = (ClassPathLoader)ldr;
            
            ldr = new ClassPathLoader(rootClassLoader, files.toArray(new File[files.size()]));
            
        }
        ((ClassPathLoader)ldr).addExclude("org.cef.");
        
        final ClassLoader fLdr = ldr;
        Class c = Class.forName(mainClass, true, ldr);
        Method m = c.getDeclaredMethod("main", String[].class);
        
        m.invoke(null, new Object[]{argv});
        return true;
        
    }
    
     private static String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isWindows = (OS.indexOf("win") >= 0);
    

    private static boolean isMac =  (OS.indexOf("mac") >= 0);
    private static final String ARCH = System.getProperty("os.arch");

    private static boolean isUnix = (OS.indexOf("nux") >= 0);
    private static final boolean is64Bit = is64Bit();
    private static final boolean is64Bit() {
        
        String model = System.getProperty("sun.arch.data.model",
                                          System.getProperty("com.ibm.vm.bitmode"));
        if (model != null) {
            return "64".equals(model);
        }
        if ("x86-64".equals(ARCH)
            || "ia64".equals(ARCH)
            || "ppc64".equals(ARCH) || "ppc64le".equals(ARCH)
            || "sparcv9".equals(ARCH)
            || "mips64".equals(ARCH) || "mips64el".equals(ARCH)
            || "amd64".equals(ARCH)
            || "aarch64".equals(ARCH)) {
            return true;
        }
        return false;
    }
}
