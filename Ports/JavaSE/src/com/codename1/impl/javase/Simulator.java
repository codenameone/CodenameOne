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

import com.codename1.impl.javase.util.MavenUtils;
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
    
    private static boolean hasFFmpeg() {
        String path = System.getProperty("ffmpeg.dir");
        if (path == null || path.isEmpty()) {
            return false;
        }
        File dir = new File(path);
        String suffix = isWindows ? ".exe" : "";
        return new File(dir, "ffmpeg" + suffix).exists() && new File(dir, "ffprobe" + suffix).exists();
    }


    /**
     * Loads properties from the target/codenameone/simulator.properties file into the System properties.
     * This file is created by the PrepareSimulatorClasspathMojo
     * @param projectDir
     */
    private static void loadSimulatorProperties(File projectDir) {
       if (!MavenUtils.isRunningInMaven()) {
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

        // Register framework-level BuildHintEditor schema defaults (see
        // Ports/JavaSE/src/com/codename1/impl/javase/BuildHintSchemaDefaults)
        // before any code reads codename1.arg.{{*}} system properties.
        BuildHintSchemaDefaults.register();

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
                files.add(commonClasses.getAbsoluteFile());
            }
            loadSimulatorProperties(cn1Props.getParentFile());
            publishAnnotationBuildHints(cn1Props.getParentFile(), classPathStr);
        }
        if (isDebug && usingHotswapAgent) { 
            HotswapProperties hotswapProperties = new HotswapProperties();
            files.addAll(hotswapProperties.getExtraClasses());
        }
        int len = t.countTokens();
        for (int iter = 0; iter < len; iter++) {
            files.add(new File(t.nextToken()).getAbsoluteFile());
        }
        File javase = new File("native" + File.separator + "javase");
        File libJavase = new File("lib" + File.separator + "impl" + File.separator + "native" + File.separator + "javase");
        for (File dir : new File[]{javase, libJavase}) {
            if (dir.exists()) {
                
                for (File jar : dir.listFiles()) {
                    if (jar.getName().endsWith(".jar")) {
                        if (!files.contains(jar)) {
                            files.add(jar.getAbsoluteFile());
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
        
        try {
            Class cefRuntime = Class.forName("org.cef.CN1JcefRuntime");
            cefSupported = Boolean.TRUE.equals(cefRuntime.getMethod("isSupported").invoke(null));
        } catch (Throwable ex) {
            cefSupported = false;
        }
        
        File jmf = new File(System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "jmf-2.1.1e.jar");
        if (jmf.exists()) {
            System.setProperty("java.class.path", System.getProperty("java.class.path") + File.pathSeparator + jmf.getAbsolutePath());
            files.add(jmf.getAbsoluteFile());
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
        StringBuilder filesPath = new StringBuilder();
        for (File f : files) {
            if (filesPath.length() > 0) {
                filesPath.append(File.pathSeparator);
            }
            filesPath.append(f.getAbsolutePath());
        }
        System.setProperty("cn1.classPathLoader.Path", filesPath.toString());
        ((ClassPathLoader)ldr).addExclude("org.cef.");
        ((ClassPathLoader)ldr).addExclude("me.friwi.jcefmaven.");
        
        final ClassLoader fLdr = ldr;
        Thread.currentThread().setContextClassLoader(fLdr);
        final Class c = Class.forName("com.codename1.impl.javase.Executor", true, ldr);
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
                            Method cleanup = c.getDeclaredMethod("cleanupForSimulatorReload");
                            cleanup.invoke(null);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
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
                files.add(new File(part).getAbsoluteFile());
            }
            return files;
        }
    }
    

    /**
     * Publishes build hints declared as annotations so the simulator sees them.
     *
     * <p>The simulator never runs {@code cn1:build}, so it never sees the build
     * request the annotations feed. Without this, moving a hint like
     * {@code desktop.titleBar} or {@code nativeTheme} out of
     * {@code codenameone_settings.properties} and onto the main class would
     * silently stop it working under {@code cn1:run} -- the build would still be
     * right and only the simulator would be wrong, which is the hardest kind of
     * discrepancy to track down.</p>
     *
     * <p>Published as system properties rather than added as another source to
     * {@code JavaSEPort.buildHint} because several readers bypass that method
     * and call {@code System.getProperty("codename1.arg....")} directly. Setting
     * the property fixes those, and every future one, with no change to them.</p>
     *
     * <p>An existing value always wins, which is what preserves {@code -D}: the
     * JVM has already applied the command line by the time this runs.</p>
     *
     * <p>Read straight off disk, not through {@code getResourceAsStream}: at this
     * point in {@code main} the application classes are not on any classloader
     * yet -- the loader is built from {@code files} further down.</p>
     */
    private static void publishAnnotationBuildHints(File projectDir, String classPathStr) {
        // A reload re-enters main() in the SAME JVM, so anything published last
        // time is still set. Withdraw it before deciding anything: otherwise the
        // "existing value wins" rule that protects -D also protects the previous
        // build's annotation value, and an edited @Desktop(titleBar = ...) -- or
        // a deleted annotation, which takes an early return below -- keeps
        // showing the old setting until the process is restarted.
        //
        // Only what THIS method installed is withdrawn. A -D was never installed
        // here, because a key already set is skipped, so it is never a candidate.
        withdrawPublishedHints();
        if (projectDir == null) {
            return;
        }
        String expectedMain = configuredMainClass(projectDir);
        FoundManifest found = findAnnotationManifest(projectDir, classPathStr, expectedMain);
        if (found == null) {
            return;
        }
        java.util.Properties p = found.hints;
        File f = found.file;
        String stampedFor = p.getProperty("cn1.buildHints.mainClass");
        if (expectedMain != null && !expectedMain.equals(stampedFor)) {
            // Somebody else's configuration. codename1.mainName changing without a
            // clean build leaves the old class and its manifest together in the
            // output directory, and the timestamp check finds that pair perfectly
            // consistent -- so without the stamp the simulator runs the previous
            // application's hints. The native merge already refuses this.
            //
            // No stamp is refused on the same terms rather than trusted: the
            // resource is written by process-annotations, which always records
            // one, so a file without it was written by something else. And
            // staleManifestReason cannot judge it either -- with no main class
            // named there is nothing to compare the manifest against, so it
            // reports "not stale" and the hints would be published unchecked.
            System.err.println("Warning: " + found.where + (stampedFor == null
                            ? " does not say which application it was generated for"
                            : " was generated for " + stampedFor + ", not " + expectedMain)
                    + ", so its build hints were NOT applied.");
            return;
        }
        // Sharing an archive does not mean sharing a build. Nothing deletes an
        // old manifest from target/classes, so a recompiled main class and last
        // week's resource are packaged into the same jar.
        String staleAgainst = staleManifestReason(p, found);
        if (staleAgainst != null) {
            // Nothing removes target/classes between builds, so a project that ran
            // process-annotations once and then stopped -- goal unbound, skipped,
            // or bound to a phase that no longer runs -- keeps a manifest that
            // looks entirely valid while the annotations beside it have moved on.
            // The device build refuses this outright; the simulator would
            // otherwise run on the previous values of hints it can actually see,
            // such as desktop.titleBar and nativeTheme, and show the wrong thing
            // with no indication why.
            //
            // Judged on timestamps rather than the manifest's own fingerprint:
            // recomputing that means parsing the class file's annotation table,
            // and the simulator has no bytecode reader. The comparison is sound in
            // the direction that matters -- process-classes always follows compile
            // within a build, so a main class newer than the manifest cannot have
            // produced it.
            System.err.println("Warning: " + found.where + " " + staleAgainst
                    + ", so it was produced by an earlier build "
                    + "and its build hints were NOT applied.");
            System.err.println("         Rebuild the project so the cn1 process-annotations "
                    + "goal regenerates it.");
            return;
        }
        // A hint declared BOTH ways is a build error, and the native merge says
        // so. Publishing it here instead would bury it: buildHint() reads the
        // system property before the settings file, so the line the developer
        // just added to codenameone_settings.properties would be silently
        // ignored by the simulator while the device build refused to run at all.
        // Reachable because editing the properties file does not touch the class,
        // so the timestamp check above still finds the manifest current.
        java.util.Properties declared = loadProjectSettings(projectDir);
        int applied = 0;
        for (String key : p.stringPropertyNames()) {
            if (!key.startsWith("codename1.arg.")) {
                continue;
            }
            String conflict = declaredInPropertiesToo(p, declared, key);
            if (conflict != null) {
                System.err.println("Warning: " + conflict + " is declared both as an annotation "
                        + "and in codenameone_settings.properties, so the annotation value was "
                        + "NOT applied. Delete one of them -- a build will refuse this.");
                continue;
            }
            if (System.getProperty(key) == null) {
                System.setProperty(key, p.getProperty(key));
                PUBLISHED_HINTS.add(key);
                applied++;
            }
        }
        if (applied > 0) {
            System.out.println("Applied " + applied + " build hint(s) from annotations");
        }
    }

    /**
     * Keys this class installed into the system properties, so a reload can take
     * them back out.
     *
     * <p>Static because a reload re-enters {@code main} in the same JVM rather
     * than starting a process.</p>
     */
    private static final java.util.Set<String> PUBLISHED_HINTS =
            new java.util.HashSet<String>();

    /** Removes what a previous launch published, leaving anything else alone. */
    private static void withdrawPublishedHints() {
        if (PUBLISHED_HINTS.isEmpty()) {
            return;
        }
        for (String key : PUBLISHED_HINTS) {
            System.clearProperty(key);
        }
        PUBLISHED_HINTS.clear();
    }

    /**
     * The properties key that declares the same setting as {@code key} in the
     * settings file, or null when the file declares none of its spellings.
     *
     * <p>An alias and its target name one setting -- the builder reads
     * {@code android.captureRecord} and then lets {@code and.captureRecord}
     * override it -- so either spelling in the file collides. The spellings come
     * out of the manifest, which the annotation processor writes them into,
     * because the catalog that knows about aliases is a build-time artifact and
     * this port cannot reach it.</p>
     */
    private static String declaredInPropertiesToo(java.util.Properties manifest,
                                                  java.util.Properties declared, String key) {
        if (declared == null) {
            return null;
        }
        if (declared.getProperty(key) != null) {
            return key;
        }
        String name = key.substring("codename1.arg.".length());
        String aliases = manifest.getProperty("cn1.buildHints.alias." + name);
        if (aliases == null) {
            return null;
        }
        for (String alias : aliases.split(",")) {
            String other = "codename1.arg." + alias.trim();
            if (alias.trim().length() > 0 && declared.getProperty(other) != null) {
                return other;
            }
        }
        return null;
    }

    /** The project's settings file, or null when it cannot be read. */
    private static java.util.Properties loadProjectSettings(File projectDir) {
        File settings = new File(projectDir, "codenameone_settings.properties");
        if (!settings.isFile()) {
            return null;
        }
        java.util.Properties p = new java.util.Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(settings);
            p.load(in);
            return p;
        } catch (IOException ex) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // read-only stream; nothing useful to do
                }
            }
        }
    }

    /**
     * The main class this project is configured to launch, or null.
     *
     * <p>The launcher's answer first. {@code process-annotations} stamps the
     * manifest with the EFFECTIVE identity -- the settings file with any
     * {@code -Dcodename1.mainName} applied -- and {@code SimulatorMojo} forwards
     * that same pair. Reading only the file disagreed with the stamp on an
     * overridden build, so the simulator refused the manifest and silently ran
     * without the hints the device build applies.</p>
     *
     * <p>The pair is taken from one source or the other, never mixed: a
     * forwarded main class with no package is a project that has none, not one
     * whose package should be read out of the file.</p>
     */
    static String configuredMainClass(File projectDir) {
        String main = System.getProperty("codename1.mainName");
        String pkg = System.getProperty("codename1.packageName");
        if (main == null || main.trim().length() == 0) {
            java.util.Properties p = loadProjectSettings(projectDir);
            if (p == null) {
                return null;
            }
            main = p.getProperty("codename1.mainName");
            pkg = p.getProperty("codename1.packageName");
        }
        if (main == null || main.trim().length() == 0) {
            return null;
        }
        return (pkg == null || pkg.trim().length() == 0)
                ? main.trim() : pkg.trim() + "." + main.trim();
    }

    /** The manifest's path inside a jar, which is its resource path with / separators. */
    private static final String ANNOTATION_HINTS_ENTRY =
            "META-INF/codenameone/build-hints.properties";

    /**
     * A manifest that was found, and where.
     *
     * <p>Not a File, because it can live inside a jar: the javase-only nested
     * build the simulator runs resolves the application's own common module as a
     * dependency artifact, so its manifest has no path of its own.</p>
     */
    static final class FoundManifest {
        final java.util.Properties hints;
        /** The file on disk, or null when it came out of a jar. */
        final File file;
        /** The jar it came out of, or null when it is a file on disk. */
        final File jar;
        final String where;

        FoundManifest(java.util.Properties hints, File file, File jar, String where) {
            this.hints = hints;
            this.file = file;
            this.jar = jar;
            this.where = where;
        }
    }

    /**
     * The emitted build hint manifest, or null when there is none.
     *
     * <p>{@code target/classes} is only the default: a module may configure
     * {@code build/outputDirectory}, and the annotation processor writes where
     * that says. Hard-coding the conventional path meant the device build applied
     * the annotated hints and {@code cn1:run} silently ignored them, which is the
     * asymmetry this whole publishing step exists to remove.</p>
     *
     * <p>The configured directory is on the simulator's own classpath, so the
     * classpath is searched rather than the layout guessed at. The conventional
     * path is tried last, so a project that moved its output without running
     * clean cannot have the leftover tree answer for it.</p>
     */
    static FoundManifest findAnnotationManifest(File projectDir, String classPathStr,
                                               String expectedMain) {
        String resource = "META-INF" + File.separator + "codenameone"
                + File.separator + "build-hints.properties";
        String entryName = ANNOTATION_HINTS_ENTRY;
        // The classpath first, because it is the output the build is ACTUALLY
        // using. Trying the conventional path first looked harmless and is not:
        // a project that moves to a configured output directory without running
        // clean leaves the old target/classes in place, complete with its old
        // manifest and the old class beside it, so the staleness check compares
        // two obsolete files against each other, finds them consistent, and
        // publishes last week's hints while the real ones sit on the classpath.
        FoundManifest stale = null;
        if (classPathStr != null) {
            for (String entry
                    : classPathStr.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (entry.length() == 0) {
                    continue;
                }
                File dir = new File(entry);
                if (dir.isDirectory()) {
                    File candidate = new File(dir, resource);
                    if (candidate.isFile()) {
                        java.util.Properties loaded = readProperties(candidate);
                        if (loaded != null) {
                            // The stamp decides here too. A reactor dependency or
                            // a stale JavaSE output directory earlier on the
                            // classpath can carry ANOTHER application's manifest,
                            // and taking it ended the search: the caller then saw
                            // a main class that was not this one and published
                            // nothing at all, so cn1:run silently dropped
                            // desktop.titleBar and nativeTheme while this
                            // application's own manifest sat in a later entry.
                            String stamp = loaded.getProperty("cn1.buildHints.mainClass");
                            FoundManifest found =
                                    new FoundManifest(loaded, candidate, null,
                                            candidate.toString());
                            if (expectedMain == null || expectedMain.equals(stamp)) {
                                // Right application, but possibly the wrong
                                // build: a leftover output directory earlier on
                                // the classpath carries a manifest stamped for
                                // this same main class, and taking it ended the
                                // search -- the caller then reported it stale and
                                // published nothing, while the current manifest
                                // sat in a later entry. The first stale one is
                                // kept so that finding NO current manifest still
                                // says why.
                                if (staleManifestReason(loaded, found) == null) {
                                    return found;
                                }
                                if (stale == null) {
                                    stale = found;
                                }
                            }
                            // A manifest with no stamp at all is passed over
                            // when there IS a main class to compare against. It
                            // was kept as a last resort "in case it predates the
                            // stamp", but nothing predates it: the whole
                            // resource is written by process-annotations, which
                            // has always stamped it. What that leniency really
                            // did was let an unstamped file in a dependency's
                            // output directory -- or one a project keeps in
                            // src/main/resources -- outrank this application's
                            // own manifest, because it was returned before the
                            // conventional target/classes lookup below and the
                            // caller accepted a null stamp.
                        }
                    }
                    continue;
                }
                // A jar too. The javase-only nested build the simulator runs
                // resolves the application's OWN common module as a dependency
                // artifact, so its manifest has no directory anywhere -- skipping
                // jars meant desktop.titleBar and nativeTheme were simply absent
                // under cn1:run while the device build applied them.
                //
                // The stamp is what tells that jar from a library's: a jar is
                // accepted only when it was generated for THIS application's main
                // class, so a dependency carrying its own manifest is passed over
                // rather than picked and then rejected.
                if (dir.isFile() && entry.endsWith(".jar") && expectedMain != null) {
                    java.util.Properties loaded = readJarEntry(dir, entryName);
                    if (loaded != null
                            && expectedMain.equals(loaded.getProperty("cn1.buildHints.mainClass"))) {
                        FoundManifest found = new FoundManifest(loaded, null, dir,
                                entryName + " in " + dir.getName());
                        if (staleManifestReason(loaded, found) == null) {
                            return found;
                        }
                        if (stale == null) {
                            stale = found;
                        }
                    }
                }
            }
        }
        // Only when the classpath carries none: a launch that did not pass the
        // module's output directory at all still finds a conventional build.
        File conventional = new File(projectDir, "target" + File.separator + "classes"
                + File.separator + resource);
        java.util.Properties loaded = conventional.isFile() ? readProperties(conventional) : null;
        if (loaded != null) {
            FoundManifest found =
                    new FoundManifest(loaded, conventional, null, conventional.toString());
            if (staleManifestReason(loaded, found) == null) {
                return found;
            }
            if (stale == null) {
                stale = found;
            }
        }
        // Nothing current anywhere. The stale one is returned rather than
        // nothing, so the caller can say which file it is and why it was not
        // used instead of silently applying no hints at all.
        return stale;
    }

    /** Loads a properties file, or null when it cannot be read. */
    private static java.util.Properties readProperties(File f) {
        java.util.Properties p = new java.util.Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            p.load(in);
            return p;
        } catch (IOException ex) {
            System.err.println("Warning: could not read " + f + ": " + ex.getMessage());
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // read-only stream; nothing useful to do
                }
            }
        }
    }

    /** Loads one entry of a jar as properties, or null when it is not there. */
    private static java.util.Properties readJarEntry(File jar, String entryName) {
        java.util.zip.ZipFile zip = null;
        try {
            zip = new java.util.zip.ZipFile(jar);
            java.util.zip.ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            java.util.Properties p = new java.util.Properties();
            java.io.InputStream in = zip.getInputStream(entry);
            try {
                p.load(in);
            } finally {
                in.close();
            }
            return p;
        } catch (IOException ex) {
            return null;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
                    // read-only archive
                }
            }
        }
    }

    /**
     * The compiled main class when it is newer than the manifest, or null.
     *
     * <p>Null whenever the question cannot be answered -- no main class recorded,
     * no class file for it, no readable timestamps -- so the manifest is taken at
     * face value rather than discarded on a guess.</p>
     */
    /**
     * Why the manifest was left behind by an earlier build, or null when it is
     * current.
     *
     * <p>By the class file's own contents when the manifest records them, and
     * only otherwise by timestamps. Timestamps are not always available to
     * compare: a jar records entry times to two-second granularity, and a build
     * configured for reproducible output stamps every entry identically, which
     * makes the comparison inert rather than merely coarse. A manifest with no
     * recorded digest is one an older plugin wrote, so it falls back rather than
     * being refused.</p>
     */
    static String staleManifestReason(java.util.Properties manifest,
                                      FoundManifest found) {
        String main = manifest.getProperty("cn1.buildHints.mainClass");
        if (main == null || main.trim().length() == 0) {
            return null;
        }
        String entry = main.trim().replace('.', '/') + ".class";
        String recorded = manifest.getProperty("cn1.buildHints.classDigest");
        if (recorded != null && recorded.length() > 0) {
            String actual = found.jar != null
                    ? digestOfJarEntry(found.jar, entry)
                    : digestOfFile(classFileBeside(found.file, main));
            if (actual != null) {
                return recorded.equals(actual) ? null
                        : "does not describe the compiled " + entry;
            }
        }
        String older = found.jar != null
                ? classNewerThanManifestInJar(manifest, found.jar)
                : (found.file == null ? null : classNewerThanManifest(manifest, found.file));
        return older == null ? null : "is older than " + older;
    }

    /** The compiled main class in the output directory the manifest sits in. */
    private static File classFileBeside(File manifestFile, String main) {
        if (manifestFile == null) {
            return null;
        }
        File classes = manifestFile.getParentFile();                  // .../codenameone
        classes = classes == null ? null : classes.getParentFile();   // .../META-INF
        classes = classes == null ? null : classes.getParentFile();   // the output dir
        if (classes == null) {
            return null;
        }
        File f = new File(classes, main.trim().replace('.', File.separatorChar) + ".class");
        return f.isFile() ? f : null;
    }

    /** SHA-256 of a file, hex, or null when it cannot be read. */
    private static String digestOfFile(File f) {
        if (f == null) {
            return null;
        }
        try {
            java.io.InputStream in = new FileInputStream(f);
            try {
                return digestOf(in);
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            return null;
        }
    }

    /** SHA-256 of one jar entry, hex, or null when it is absent or unreadable. */
    static String digestOfJarEntry(File jar, String entryName) {
        java.util.zip.ZipFile zip = null;
        try {
            zip = new java.util.zip.ZipFile(jar);
            java.util.zip.ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            java.io.InputStream in = zip.getInputStream(entry);
            try {
                return digestOf(in);
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            return null;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
                    // read-only archive
                }
            }
        }
    }

    private static String digestOf(java.io.InputStream in) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                md.update(buf, 0, n);
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : md.digest()) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            return null;
        }
    }

    /**
     * As above, for a manifest read out of a jar: the two entries' own
     * timestamps, since being in one archive proves nothing about which build
     * wrote them.
     *
     * <p>Zip stores times to two-second granularity, so the comparison is
     * deliberately strict -- a class newer by less than that reads as
     * consistent. It only has to catch a manifest from an EARLIER build, which
     * is not a near thing.</p>
     */
    static String classNewerThanManifestInJar(java.util.Properties manifest, File jar) {
        String main = manifest.getProperty("cn1.buildHints.mainClass");
        if (main == null || main.trim().length() == 0) {
            return null;
        }
        String classEntry = main.trim().replace('.', '/') + ".class";
        java.util.zip.ZipFile zip = null;
        try {
            zip = new java.util.zip.ZipFile(jar);
            java.util.zip.ZipEntry cls = zip.getEntry(classEntry);
            java.util.zip.ZipEntry res = zip.getEntry(ANNOTATION_HINTS_ENTRY);
            if (cls == null || res == null) {
                return null;
            }
            long classTime = cls.getTime();
            long manifestTime = res.getTime();
            if (classTime < 0L || manifestTime < 0L) {
                return null;
            }
            return classTime > manifestTime ? classEntry : null;
        } catch (IOException ex) {
            return null;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
                    // read-only archive
                }
            }
        }
    }

    private static String classNewerThanManifest(java.util.Properties manifest,
                                                 File manifestFile) {
        String main = manifest.getProperty("cn1.buildHints.mainClass");
        if (main == null || main.trim().length() == 0) {
            return null;
        }
        // Beside the manifest, whatever directory that turned out to be -- the
        // two are written by the same build into the same output directory.
        File classes = manifestFile.getParentFile();          // .../codenameone
        classes = classes == null ? null : classes.getParentFile();   // .../META-INF
        classes = classes == null ? null : classes.getParentFile();   // the output dir
        if (classes == null) {
            return null;
        }
        File classFile = new File(classes,
                main.trim().replace('.', File.separatorChar) + ".class");
        if (!classFile.isFile()) {
            return null;
        }
        long classTime = classFile.lastModified();
        long manifestTime = manifestFile.lastModified();
        if (classTime == 0L || manifestTime == 0L) {
            return null;
        }
        return classTime > manifestTime ? classFile.getName() : null;
    }
}
