/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.javase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * A simple class that can invoke a lifecycle object to allow it to run a
 * Codename One application. Classes are loaded with a classloader so the UI
 * skin can be updated and the lifecycle objects reloaded.
 *
 * @author Shai Almog
 */
public class Simulator {
    
    private static final String DEFAULT_SKIN="/iPhoneX.skin";
    private static ClassPathLoader rootClassLoader;


    /**
     * Loads properties from the target/codenameone/simulator.properties file into the System properties.
     * This file is created by the PrepareSimulatorClasspathMojo
     * @param projectDir
     */
    private static void loadSimulatorProperties(File projectDir) {
       if (System.getProperty("maven.home") == null) {
           // simulator.properties file is only for maven.
           // The PrepareSimulatorClassPathMojo writes the simulator.properties file in the target/codenameone folder.
           return;
       }
       if (System.getProperty("cn1.simulator.properties.loaded") != null) {
           // properties are already loaded.
           return;
       }
       System.setProperty("cn1.simulator.properties.loaded", "true");
       File simulatorProperties = new File(projectDir, "target" + File.separator + "codenameone" + File.separator + "simulator.properties");
       if (simulatorProperties.exists()) {
           Properties props = new Properties();
           try (FileInputStream fis = new FileInputStream(simulatorProperties)) {
               props.load(fis);
           } catch (IOException ex) {
               System.err.println("Failed to load simulator.properties file");
               ex.printStackTrace();
           }
           for (Object key : props.keySet()) {
               String stringKey = (String)key;
               if (stringKey.isEmpty()) continue;
               System.setProperty(stringKey, props.getProperty(stringKey));
           }


       }

   }


    private static void setCWD() {
        try {
            File currDir = new File(System.getProperty("user.dir")).getCanonicalFile();
            File codenameOneSettings = new File(currDir, "codenameone_settings.properties");
            if (codenameOneSettings.exists()) {
                return;
            }
            currDir = new File(currDir, "common");
            codenameOneSettings = new File(currDir, codenameOneSettings.getName());
            if (codenameOneSettings.exists()) {
                System.setProperty("user.dir", currDir.getAbsolutePath());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Accepts the classname to launch
     */
    public static void main(final String[] argv) throws Exception {

        try {
            // Load the sqlite database Engine JDBC driver in the top level classloader so it's shared
            // this works around the exception: java.lang.UnsatisfiedLinkError: Native Library sqlite already loaded in another classloader
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
        }
        System.setProperty("NSHighResolutionCapable", "true");
        String skin = System.getProperty("dskin");
        if (skin == null) {
            System.setProperty("dskin", DEFAULT_SKIN);
        }
        
        for (int i = 0; i < argv.length; i++) {
            String argv1 = argv[i];
            if(argv1.equals("resetSkins")){
                System.setProperty("resetSkins", "true");
                System.setProperty("skin", DEFAULT_SKIN);
                System.setProperty("dskin", DEFAULT_SKIN);            
            }
        }
        
        if (System.getenv("CN1_SIMULATOR_SKIN") != null) {
            System.setProperty("skin", System.getenv("CN1_SIMULATOR_SKIN"));
        }
        
        
        String classPathStr = System.getProperty("java.class.path");
        if (System.getProperty("cn1.class.path") != null) {
            classPathStr += File.pathSeparator + System.getProperty("cn1.class.path");
        }
        StringTokenizer t = new StringTokenizer(classPathStr, File.pathSeparator);
        if(argv.length > 0) {
            System.setProperty("MainClass", argv[0]);
        }
        List<File> files = new ArrayList<File>();
        // Support for instant reload:
        // If running with HotswapAgent (https://github.com/HotswapProjects/HotswapAgent) in debug mode
        // we add special support for instant refresh when source files are changed.
        // The easiest way to enable this is to install DCEVM JDK https://github.com/TravaOpenJDK/trava-jdk-11-dcevm/releases
        // and use that as the project JDK.  Then add "-XX:HotswapAgent=core" to the java VM options.
        // 
        List<String> inputArgs = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
        final boolean isDebug = inputArgs.toString().indexOf("-agentlib:jdwp") > 0;
        final boolean usingHotswapAgent = inputArgs.toString().indexOf("-XX:HotswapAgent") > 0;
        File cn1Props = new File("codenameone_settings.properties");
        if (!cn1Props.exists()) {
            cn1Props = new File("common" + File.separator + "codenameone_settings.properties");

        }
        if (!cn1Props.exists()) {
            cn1Props = new File(".." + File.separator + "common" + File.separator + "codenameone_settings.properties").getAbsoluteFile();

        }
        if (cn1Props.exists()) {
            File commonClasses = new File(cn1Props.getParentFile(), "target" + File.separator + "classes");
            if (commonClasses.exists()) {
                files.add(commonClasses);
            }
            loadSimulatorProperties(cn1Props.getParentFile());
        }
        if (isDebug && usingHotswapAgent) { 
            HotswapProperties hotswapProperties = new HotswapProperties();
            files.addAll(hotswapProperties.getExtraClasses());
        }
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
        
        File cef = System.getProperty("cef.dir") != null ? new File(System.getProperty("cef.dir")) : new File(System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "cef");
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
                    if (jar.getName().endsWith(".jar") && !jar.getName().endsWith("-tests.jar")) {
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
        StringBuilder filesPath = new StringBuilder();
        for (File f : files) {
            if (filesPath.length() > 0) {
                filesPath.append(File.pathSeparator);
            }
            filesPath.append(f.getAbsolutePath());
        }
        System.setProperty("cn1.classPathLoader.Path", filesPath.toString());
        ((ClassPathLoader)ldr).addExclude("org.cef.");
        
        final ClassLoader fLdr = ldr;
        Thread.currentThread().setContextClassLoader(fLdr);
        Class c = Class.forName("com.codename1.impl.javase.Executor", true, ldr);
        Method m = c.getDeclaredMethod("main", String[].class);
        m.invoke(null, new Object[]{argv});
        new Thread() {
            public void run() {
                setContextClassLoader(fLdr);
                while (true) {
                    try {
                        sleep(500);
                    } catch (InterruptedException ex) {
                    }
                    String r = System.getProperty("reload.simulator");
                    if (r != null && r.equals("true")) {
                        System.setProperty("reload.simulator", "");
                        int version = Integer.parseInt(System.getProperty("reload.simulator.count", "0"));
                        System.setProperty("reload.simulator.count", String.valueOf(version+1));
                        try {
                            main(argv);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        return;
                    }
                }
            }
        }.start();
    }

    private static void addToSystemClassLoader(File f) {
        ClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(sysloader, new Object[]{f.toURI().toURL()});
        } catch (Throwable t) {
            t.printStackTrace();
        }//end try catch    
    }
    
    static void loadFXRuntime() {
        String javahome = System.getProperty("java.home");
        String fx = javahome + "/lib/jfxrt.jar";
        File f = new File(fx);
        if (f.exists()) {
            addToSystemClassLoader(f);

        } 
    }
    
    private static String getJavaFXVersionStr() {
        return (getJavaVersion() == 8) ? "8" : "";
    }
    
    private static int cachedJavaVersion = -1;

    private static int getJavaVersion() {
        if (cachedJavaVersion < 0) {

            String version = System.getProperty("java.version");
            if (version.startsWith("1.")) {
                version = version.substring(2);
            }
            // Allow these formats:
            // 1.8.0_72-ea
            // 9-ea
            // 9
            // 9.0.1
            int dotPos = version.indexOf('.');
            int dashPos = version.indexOf('-');
            if (dotPos < 0 && dashPos < 0) {
                cachedJavaVersion = Integer.parseInt(version);
                return cachedJavaVersion;
            }
            cachedJavaVersion = Integer.parseInt(version.substring(0,
                    dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : 1));
            return cachedJavaVersion;
        }
        return cachedJavaVersion;
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
    
    /**
     * Encapsulates the hotswap-agent.properties file that is used when running with HotswapAgent.
     * See https://github.com/HotswapProjects/HotswapAgent
     */
    private static class HotswapProperties {
        Properties props;
        
        /**
         * Finds the hotswap-agent.properties file.
         * @return 
         */
        private File findHotswapPropertiesFile() {
        
            try {
                File currDir = new File(System.getProperty("user.dir")).getCanonicalFile();
                while (!new File(currDir, "javase").exists()) {
                    currDir = currDir.getParentFile();
                    if (currDir == null) {
                        return null;
                    }
                }

                //System.out.println("Curr Directory is "+currDir);
                File hotswapProps = new File(currDir, "javase" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "hotswap-agent.properties");
                if (hotswapProps.exists()) {
                    return hotswapProps;
                }
            } catch (IOException ex){
                ex.printStackTrace();
            }

            return null;

        }


        /**
         * The hotswap-agent.properties file may be added to the root of the
         * classpath to tune the Hotswap Agent to support enhanced live
         * class-reloading.
         *
         * https://github.com/HotswapProjects/HotswapAgent
         *
         * @return
         */
        private Properties loadHotswapProperties() {
            Properties out = new Properties();
            File hotswapProps = findHotswapPropertiesFile();
            if (hotswapProps != null) {
                FileInputStream fis = null;
                try {
                   fis = new FileInputStream(hotswapProps);
                   out.load(fis);

                } catch (IOException ex) {
                    System.err.println("Failed to load hotswap properties file from "+hotswapProps);
                    ex.printStackTrace();
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Exception ex){}
                    }
                }
            }
            return out;

        }
        
        private Properties getProperties() {
            if (props == null) {
                props = loadHotswapProperties();
            }
            return props;
        }
        
        /**
         * Gets the extraClasspath from the hotswap-agent.properties file.  These paths
         * are prepended to the classpath of the classloader to allow for live code refresh.
         * 
         * @return 
         */
        private List<File> getExtraClasses() {
            String extraClasspath = getProperties().getProperty("extraClasspath");
            // NOTE: The hotswap-agent.properties file  uses semicolon to separate entries in extraClasspath on all
            // platforms - not just windows.
            if (extraClasspath == null || extraClasspath.trim().isEmpty()) {
                return new ArrayList();
            }
            String[] parts = extraClasspath.split(";");
            List<File> files = new ArrayList<File>();
            for (String part : parts) {
                part = part.trim();
                files.add(new File(part));
            }
            return files;
        }
    }
    
}