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
package com.codename1.impl.javase.fx;

import com.codename1.impl.javase.UnzipUtility;
import com.codename1.impl.javase.fx.JavaFXLoader.JavaFXNotLoadedException;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A Simple utilty class that will add JavaFX to the classpath at runtime.  How it works is simple.  Just add the following to the
 * first line of your main() method:
 * 
 * <p><pre>{@code 
 * if (JavaFXLoader.main(MyMainClass.class, args)) { return };
 * }</pre></p>
 * 
 * <p>This will check to see if JavaFX is loaded.  If so, it will call your main method again (don't worry about recursion as JavaFXLoader.main()
 * will only run once.. The second run you can view it like a NOP).  If JavaFX is not loaded, it will download it (if necessary), add it to the classpath, 
 * and re-run your main method using this new ClassLoader which includes JavafX.
 * </p>
 * @author Steve Hannah
 */
public class JavaFXLoader {
    private static String OS = System.getProperty("os.name").toLowerCase();
    private File javafxDir = new File(System.getProperty("javafx.install.dir", new File(System.getProperty("user.home"), ".codenameone" + File.separator + "javafx" + getJavaFXVersionStr()).getAbsolutePath()));
    private String winUrl = System.getProperty("javafx.win.url", "https://github.com/codenameone/cn1-binaries/raw/master/javafx"+getJavaFXVersionStr()+"-win.zip");
    private String macUrl = System.getProperty("javafx.mac.url", "https://github.com/codenameone/cn1-binaries/raw/master/javafx"+getJavaFXVersionStr()+"-mac.zip");
    private String linuxUrl = System.getProperty("javafx.linux.url", "https://github.com/codenameone/cn1-binaries/raw/master/javafx"+getJavaFXVersionStr()+"-linux.zip");
    
    
    private static boolean delTree(File f) throws IOException {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                if (!delTree(child)) {
                    return false;
                }
            }
        }
        if (f.exists()) {
            if (!f.delete()) {
                return false;
            }
        }
        return true;
    }
    
    private static void downloadToFile(String url, File f) throws IOException {
        URL u = new URL(url);
        URLConnection conn = u.openConnection();
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection)conn;
            http.setInstanceFollowRedirects(true);
            http.setDefaultUseCaches(false);
            
        }
        
        try (InputStream input = conn.getInputStream()) {
            try (FileOutputStream output = new FileOutputStream(f)) {
                byte[] buf = new byte[128 * 1024];
                int len;
                while ((len = input.read(buf)) >= 0) {
                    output.write(buf, 0, len);
                }
            }
        }
    }
    
    private static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    private static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    private static boolean isUnix() {
        return (OS.indexOf("nux") >= 0);
    }
    
    private String getJavaFXURL() {
        if (isWindows()) {
            return winUrl;
        }
        if (isMac()) {
            return macUrl;
        }
        
        if (isUnix()) {
            return linuxUrl;
        }
        
        return null;
    }
    
    private static File findDir(File root, String dirName) {
        if (root.getName().equals(dirName)) {
            return root;
        }
        if (root.isDirectory()) {
            for (File child : root.listFiles()) {
                File found = findDir(child, dirName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    private void downloadJavaFX() throws IOException {
        try {
            String url = getJavaFXURL();
            if (url == null) {
                throw new RuntimeException("No JavaFX URL found for this platform");
            }
            if (javafxDir.exists()) {
                delTree(javafxDir);
            }
            javafxDir.getParentFile().mkdirs();
            File javafxZip = new File(javafxDir.getParentFile(), "javafx.zip");
            downloadToFile(url, javafxZip);
            System.out.println("Downladed "+javafxZip+" "+javafxZip.length()+" bytes");
            File tmpDir = new File(javafxZip.getParentFile(), "javafx.tmp."+System.currentTimeMillis());
            try {
                new UnzipUtility().unzip(javafxZip.getAbsolutePath(), tmpDir.getAbsolutePath());
                javafxDir.mkdir();
                File libDirTmp = findDir(tmpDir, "lib");
                
                File legalDirTmp = findDir(tmpDir, "legal");
                File binDirTmp = findDir(tmpDir, "bin");
                if (libDirTmp == null || !libDirTmp.exists()) {
                    throw new IOException("No lib dir found within JavaFX zip");
                }
                //System.out.println("Files:");
                //for (File f : libDirTmp.listFiles()) {
                //    System.out.println(f);
                //    if (f.isDirectory()) {
                //        for (File child : f.listFiles()) {
                //            System.out.println("  "+child);
                 //       }
                //    }
                //}
                //if (legalDirTmp == null || !legalDirTmp.exists()) {
                //    throw new IOException("No legal dir found within JavaFX zip");
                //}
                
                libDirTmp.renameTo(new File(javafxDir, "lib"));
                if (legalDirTmp != null && legalDirTmp.exists()) {
                    legalDirTmp.renameTo(new File(javafxDir, "legal"));
                }
                if (binDirTmp != null && binDirTmp.exists()) {
                    binDirTmp.renameTo(new File(javafxDir, "bin"));
                }
                
                
                
            } finally {
                delTree(tmpDir);
                javafxZip.delete();
            }
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(JavaFXLoader.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    public URL[] getJavaFXJars() throws IOException {
        if (!javafxDir.exists()) {
            downloadJavaFX();
        }
        if (!javafxDir.exists()) {
            throw new RuntimeException("Failed to download JavaFX");
        }
        
        File javafxLibDir = new File(javafxDir, "lib");
        if (!javafxLibDir.exists()) {
            throw new RuntimeException("JavaFX is missing.  This application requires a JDK with JavaFX.");
        }
        File jarsDir = javafxLibDir;
        if ("8".equals(getJavaFXVersionStr())) {
            // JavaFX 8 jar files are in the lib/ext directory
            jarsDir = new File(javafxLibDir, "ext");
        }
        
        java.util.List<java.net.URL> javafxUrls = new ArrayList<java.net.URL>();
        for (File f : jarsDir.listFiles()) {
            if (!f.getName().endsWith(".jar")) {
                continue;
            }
            try {
                java.net.URL u = f.toURI().toURL();
                javafxUrls.add(u);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }

        }
        if (javafxUrls.isEmpty()) {
            throw new RuntimeException("JavaFX is missing.  This application requires a JDK with JavaFX.");
        }

        try {
            javafxUrls.add(javafxLibDir.toURI().toURL()); // Necessary for loading native libs
        } catch (MalformedURLException mex) {
            throw new RuntimeException(mex);
        }
        return javafxUrls.toArray(new java.net.URL[javafxUrls.size()]);
    }
    
    public File[] getJavaFXJarFiles() throws IOException, URISyntaxException {
        URL[] urls = getJavaFXJars();
        File[] files = new File[urls.length];
        for (int i=0; i<urls.length; i++) {
            files[i] = new File(urls[i].toURI());
        }
        return files;
    }
    
    public String getJavaFXClassPath() throws IOException, URISyntaxException {
        StringBuilder sb = new StringBuilder();
        for (File f : getJavaFXJarFiles()) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(f.getAbsolutePath());
        }
        return sb.toString();
    }
    
   
    
    
    public static class JavaFXNotLoadedException extends Exception {
        public JavaFXNotLoadedException(Throwable cause) {
            super(cause);
        }
    }
    
    public static boolean isJavaFXLoaded() {
        boolean isJavaFX8 = "8".equals(getJavaFXVersionStr());
        if (!isJavaFX8 && containsJavaFX8(System.getProperty("java.class.path"))) {
            // We're using javafx8 in jdk9 or higher...
            // we should claim that javafx isn't loaded so that we can fix it.
            return false;
        }
        try {
            ClassLoader.getSystemClassLoader().loadClass("javafx.scene.web.WebEngine");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        } catch (UnsupportedClassVersionError er) {
            
            return false;
        
        }
    }
    
    private File findEclipseLaunchFile() {
        
        for (File child : new File(".").listFiles()) {
            if (child.getName().startsWith("Simulator_") && child.getName().endsWith(".launch")) {
                return child;
            }
        }
        return null;
    }
    
    
    
    private void updateEclipseLaunchClasspath() {
        File launchFile = findEclipseLaunchFile();
        if (launchFile == null || !launchFile.exists()) {
            return;
        }
        String contents = null;
            try {
                try (FileInputStream fos = new FileInputStream(launchFile)) {
                    byte[] buf = new byte[(int)launchFile.length()];
                    fos.read(buf);
                    contents = new String(buf, "UTF-8");
                }
                boolean isJavaFX8 = "8".equals(getJavaFXVersionStr());
                String javafxListEntry = "<listEntry value=\"&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot; standalone=&quot;no&quot;?&gt;&#10;&lt;runtimeClasspathEntry id=&quot;org.eclipse.jdt.launching.classpathentry.variableClasspathEntry&quot;&gt;&#10;    &lt;memento path=&quot;5&quot; variableString=&quot;${system_property:user.home}/.codenameone/javafx/lib/*&quot;/&gt;&#10;&lt;/runtimeClasspathEntry&gt;&#10;\"/>";
                String javafx8ListEntry = "<listEntry value=\"&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot; standalone=&quot;no&quot;?&gt;&#10;&lt;runtimeClasspathEntry id=&quot;org.eclipse.jdt.launching.classpathentry.variableClasspathEntry&quot;&gt;&#10;    &lt;memento path=&quot;5&quot; variableString=&quot;${system_property:user.home}/.codenameone/javafx8/lib/ext/*&quot;/&gt;&#10;&lt;/runtimeClasspathEntry&gt;&#10;\"/>";
                boolean changed = false;
                
                if (isJavaFX8 && containsJavaFX(contents)) {
                    // This is javafx 8 but we have javafx11 on the classpath - REMOVE THEM
                    System.out.println("Detected Incompatible version of JavaFX. Removing incompatible libs from the classpath");
                    contents = contents.replaceAll("<listEntry .*\\.codenameone/javafx/lib.*/>", "");
                    changed = true;
                } else if (!isJavaFX8 && containsJavaFX8(contents)) {
                    System.out.println("Detected Incompatible version of JavaFX.  Removing incompatible libs from the classpath");
                    // This is javafx11 but we have javafx8 on the classpath - REMOVE THEM
                    contents = contents.replaceAll("<listEntry .*\\.codenameone/javafx8/lib.*/>", "");
                    changed = true;
                }
                
                if (!isJavaFX8 && !containsJavaFX(contents)) {
                    // This is JDK9 or higher and the javafx libs havent' been added to the classpath yet.
                    System.out.println("Adding OpenJFX11 to the classpath");
                    int pos = contents.indexOf("<listAttribute key=\"org.eclipse.jdt.launching.CLASSPATH\">");
                    if (pos < 0) {
                        return;
                    }
                    String endTag = "</listAttribute>";
                    int closingPos = contents.indexOf(endTag, pos);
                    contents = contents.substring(0, closingPos)
                            + "    " + javafxListEntry + "\n    " + endTag
                            + contents.substring(closingPos + endTag.length());
                    changed = true;
                   
                } else if (isJavaFX8 && !containsJavaFX8(contents)) {
                    System.out.println("Adding OpenJFX8 to the classpath");
                    // This is JDK 8 and javafx libs haven't been added to the classpath yet.
                    int pos = contents.indexOf("<listAttribute key=\"org.eclipse.jdt.launching.CLASSPATH\">");
                    if (pos < 0) {
                        return;
                    }
                    String endTag = "</listAttribute>";
                    int closingPos = contents.indexOf(endTag, pos);
                    contents = contents.substring(0, closingPos)
                            + "    " + javafx8ListEntry + "\n    " + endTag
                            + contents.substring(closingPos + endTag.length());
                    changed = true;
                }
                
                if (changed) {
                    System.out.println("Adding JavaFX to your Eclipse launch classpath at "+launchFile);
                    System.out.println("JavaFX should be correctly loaded the next time you run this project.");
                     try (FileOutputStream fos = new FileOutputStream(launchFile)) {
                        fos.write(contents.getBytes("UTF-8"));
                    }
                    
                }
                
            } catch (IOException ex) {
                System.err.println("Failed to update "+launchFile+" with JavaFX path");
                ex.printStackTrace(System.err);
            }
        
    }
    
    private void updateNbProjectProperties() {
        // Update the nbProject properties file so that we don't have to do this every time.
        File nbProjectProperties = new File("nbproject" + File.separator + "project.properties");
        if (nbProjectProperties.exists()) {
            String contents = null;
            try {
                try (FileInputStream fos = new FileInputStream(nbProjectProperties)) {
                    byte[] buf = new byte[(int)nbProjectProperties.length()];
                    fos.read(buf);
                    contents = new String(buf, "UTF-8");
                }
                //System.out.println("Starting contents="+contents);
                boolean isJavaFX8 = "8".equals(getJavaFXVersionStr());
                String jfxPath = isJavaFX8 ? "${user.home}/.codenameone/javafx8/lib/ext/*" :
                        "${user.home}/.codenameone/javafx/lib/*";
                
                boolean changed = false;
                if (contents != null) {


                    if (contents.contains("cn1.javafx.path=")) {
                        int pos = contents.indexOf("cn1.javafx.path=");
                        String newContents = contents;
                        if (pos > -1) {
                            int eqPos = contents.indexOf("=", pos);
                            int newlinePos = contents.indexOf("\n", pos);
                            if (newlinePos < 0) {
                                newlinePos = contents.length();
                            }
                            newContents = contents.substring(0, pos) + contents.substring(newlinePos);
                        }
                        
                        if (!newContents.equals(contents)) {
                            contents = newContents;
                            changed = true;
                        }
                    } else {
                        String sep = System.getProperty("line.separator");
                        if (!contents.endsWith(sep)) {
                            contents += sep;
                        }
                        contents += "cn1.javafx.path="+jfxPath;
                        changed = true;
                    }
                    if (!contents.contains("${cn1.javafx.path}")) {
                        int runClassPathPos = contents.indexOf("run.classpath=");

                        if (runClassPathPos > 0) {
                            int pos = contents.indexOf("${build.classes.dir}", runClassPathPos);
                            if (pos > 0) {
                                String before = contents.substring(0, pos);
                                String after = contents.substring(pos + "${build.classes.dir}".length());
                                contents = before + "${build.classes.dir}:${cn1.javafx.path}" + after;
                                contents = contents.replace("${cn1.javafx.path}:${cn1.javafx.path}", "${cn1.javafx.path}");
                                changed = true;

                            }
                        }
                        runClassPathPos = contents.indexOf("run.test.classpath=");

                        if (runClassPathPos > 0) {
                            int pos = contents.indexOf("${build.classes.dir}", runClassPathPos);
                            if (pos > 0) {
                                String before = contents.substring(0, pos);
                                String after = contents.substring(pos + "${build.classes.dir}".length());
                                contents = before + "${build.classes.dir}:${cn1.javafx.path}" + after;
                                contents = contents.replace("${cn1.javafx.path}:${cn1.javafx.path}", "${cn1.javafx.path}");
                                changed = true;

                            }
                        }


                    }
                }
                if (changed) {
                    System.out.println("Adding JavaFX to your project properties file at "+nbProjectProperties);
                    System.out.println("JavaFX should be correctly loaded the next time you run this project.");
                     try (FileOutputStream fos = new FileOutputStream(nbProjectProperties)) {
                        fos.write(contents.getBytes("UTF-8"));
                    }
                }

            } catch (IOException ex) {
                System.err.println("Failed to update "+nbProjectProperties+" with JavaFX path");
                ex.printStackTrace(System.err);
            }
        }
    }
    
    private static int cachedJavaVersion=-1;
    /**
     * Returns the Java version as an int value.
     *
     * @return the Java version as an int value (8, 9, etc.)
     * @since 12130
     */
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
    
    /**
     * Because of the historical order in which we supported sideloading JavaFX, JDK9, 10, and 11
     * have an empty string for the version string. And JDK 8 has "8".  The javafx libs 
     * are downloaded to ${user.home}/.codenameone/javafx${versionstr}.  E.g.
     * on JDK 8 it would be ${user.home}/.codenameone/javafx8, and oh 9, 10, 11, it would 
     * just be ${user.home}/.codenameone/javafx
     * @return 
     */
    private static String getJavaFXVersionStr() {
        return (getJavaVersion() == 8) ? "8" : "";
    }
    
    private static boolean containsJavaFX8(String classpath) {
        return classpath.contains(".codenameone/javafx8/lib/ext") || classpath.contains(".codenameone\\javafx8\\lib\\ext");
    }

    private static boolean containsJavaFX(String classpath) {
        return classpath.contains(".codenameone/javafx/lib") || classpath.contains(".codenameone\\javafx\\lib");
    }

    private static String p(String path) {
        if ("\\".equals(File.separator)) {
            return path.replace("/", "\\");
        } else {
            return path.replace("\\", "/");
        }
    }

    public boolean runWithJavaFX(Class launchClass, Class mainClass, String[] args) throws JavaFXNotLoadedException, InvocationTargetException {
        if (!JavaFXLoader.isJavaFXLoaded()) {
            System.out.println("JavaFX Not loaded.  Classpath="+System.getProperty("java.class.path")+" . Adding to classpath");
            try {
                getJavaFXJarFiles();
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            Properties props = new Properties();
            boolean isJavaFX8 = "8".equals(getJavaFXVersionStr());
            try {
                String cp = System.getProperty("java.class.path");
                List<String> cpParts = new ArrayList<String>(Arrays.asList(cp.split(Pattern.quote(File.pathSeparator))));
                if (isJavaFX8 && containsJavaFX(cp)) {
                    //cp = cp.replace(".codenameone/javafx/lib/*", ".codenameone/javafx8/lib/ext/*");
                    ListIterator<String> lit = cpParts.listIterator();
                    while (lit.hasNext()) {
                        String nex = lit.next();
                        if (containsJavaFX(nex)) {
                            lit.remove();
                        }
                        
                    }
                    cp = String.join(File.pathSeparator, cpParts.toArray(new String[cpParts.size()]));
                    cp += File.pathSeparator + System.getProperty("user.home")+p("/.codenameone/javafx8/lib/ext/*");
                    
                } else if (!isJavaFX8 && containsJavaFX8(cp)) {
                   // cp = cp.replace(".codenameone/javafx8/lib/ext/*", ".codenameone/javafx/lib/*");
                   ListIterator<String> lit = cpParts.listIterator();
                    while (lit.hasNext()) {
                        String nex = lit.next();
                        if (containsJavaFX8(nex)) {
                            lit.remove();
                        }
                    }
                    cp = String.join(File.pathSeparator, cpParts.toArray(new String[cpParts.size()]));
                    cp += File.pathSeparator + System.getProperty("user.home")+p("/.codenameone/javafx/lib/*");
                } else if (isJavaFX8 && !containsJavaFX8(cp)) {
                    cp += File.pathSeparator + System.getProperty("user.home")+p("/.codenameone/javafx8/lib/ext/*");
                    
                } else if (!isJavaFX8 && !containsJavaFX(cp)) {
                    cp += File.pathSeparator + System.getProperty("user.home") + p("/.codenameone/javafx/lib/*");
                } else {
                    String javafxPath = isJavaFX8 ?
                            System.getProperty("user.home") + p("/.codenameone/javafx8") :
                            System.getProperty("user.home") + p("/.codenameone/javafx");
                    System.err.println("Project could not be run because JavaFX is missing.  It already has JavaFX in the class path so something else must be wrong.  Ensure that the "+javafxPath+" directory exists and contains the proper files.  You may want to try just deleting the entire directory and try running this project again, as it should autonmatically re-download it.");
                    System.exit(1);
                }
                props.setProperty("java.class.path", cp);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load JavaFX", ex);
            }
            
            updateNbProjectProperties();
            updateEclipseLaunchClasspath();
            
            System.out.println("Restarting JVM with JavaFX in the classpath.");
            System.out.println("NOTE: If you are trying to debug the project, you'll need to cancel this run and try running debug on the project again.  JavaFX should now be in your classpath.");
            restartJVM(launchClass, props, args);
            return true;
            
        }
        System.out.println("JavaFX is loaded");
        return false;
    }
    
    
    
    
    public static boolean main(Class launchClass, Class mainClass, String[] argv) throws JavaFXNotLoadedException, InvocationTargetException {
        return new JavaFXLoader().runWithJavaFX(launchClass, mainClass, argv);
    }
    
    public static boolean restartJVM(Class launchClass, Properties props, String[] args) {
      
      String osName = System.getProperty("os.name");
      
      
      // get current jvm process pid
      String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
      // get environment variable on whether XstartOnFirstThread is enabled
      String env = System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + pid);
      
      // restart jvm with -XstartOnFirstThread
      String separator = System.getProperty("file.separator");
      String classpath = props.getProperty("java.class.path", System.getProperty("java.class.path"));
      String mainClass = System.getenv("JAVA_MAIN_CLASS_" + pid);
      if (mainClass == null) {
          mainClass = launchClass.getName();
      }
      String jvmPath = System.getProperty("java.home") + separator + "bin" + separator + "java";
      
      List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
      
      ArrayList<String> jvmArgs = new ArrayList<String>();
      
      jvmArgs.add(jvmPath);
      //jvmArgs.add("-XstartOnFirstThread");
      jvmArgs.addAll(inputArguments);
      jvmArgs.add("-cp");
      jvmArgs.add(classpath);
      jvmArgs.add(mainClass);
      for (String arg : args) {
          jvmArgs.add(arg);
      }
      try {
         ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
         processBuilder.inheritIO();
         Process process = processBuilder.start();
         process.waitFor();
         int exitCode = process.exitValue();
         System.exit(exitCode);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return true;
   }
    
    
    public static void main(String[] args) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        PrintStream stdOut = System.out;
        PrintStream stdErr = System.err;
        System.setOut(out);
        System.setErr(out);
        JavaFXLoader jfxLoader = new JavaFXLoader();
        if (!isJavaFXLoaded()) {
            try {
                jfxLoader.getJavaFXClassPath();
                boolean isJavaFX8 = "8".equals(getJavaFXVersionStr());
                String path = isJavaFX8 ?  System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "javafx8" + File.separator + "lib" + File.separator + "ext" + File.separator + "*":
                        System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "javafx" + File.separator + "lib" + File.separator + "*";
                stdOut.print(path) ;
            } catch (Exception ex){
                stdErr.print("Failed to load JavaFX jars");
                ex.printStackTrace(stdErr);
                System.exit(1);
            }
            
        } else {
            //stdOut.print(".");
        }
        System.exit(0);
        
    }
    


    
    
    
}
