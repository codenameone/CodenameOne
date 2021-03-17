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
package com.codename1.designer.css;



import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;
import com.codename1.designer.css.CSSTheme.WebViewProvider;
import com.codename1.impl.javase.CN1Bootstrap;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.ComponentSelector.ComponentClosure;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.util.Resources;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JPanel;
//import org.apache.tools.ant.types.Path;

/**
 *
 * @author shannah
 */
public class CN1CSSCLI {
    public static String version = "7.0";
    static Object lock = new Object();
    static BrowserComponent web;
    
    public void start() throws Exception {
        //Platform.setImplicitExit(false);
        startImpl();
        //stage.hide();
        
    }
    
    private static void startImpl() throws Exception {
        web = new BrowserComponent();
        web.addWebEventListener(BrowserComponent.onError, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                 System.out.println("Received exception: "+evt.getSource());
            }
            
        });
        web.setPreferredW(400);
        web.setPreferredH(800);
        
        //Scene scene = new Scene(web, 400, 800, Color.web("#666670"));
        //stage.setScene(scene);
        //stage.show();
        synchronized(lock) {
            lock.notify();
        }
    }
    
    private static void relaunch() throws Exception {
        //Stage stage = new Stage();
        startImpl();
    }
    
    
    // It's getting to be a bit of a gong show in here with the CSS compiler trying to 
    // work nicely with the project files - e.g. automatically saving the theme.res in the
    // src directory.
    // Stateless mode is an attempt to make it simpler.  
    // Stateless mode can be invoked by adding "-stateless" to the parameters.
    public static boolean statelessMode;
    public static boolean mergeMode;
    public static boolean watchmode;
    private static Thread watchThread;
    
    
    
    private static String getArgByName(String[] args, String... names) {
        int len = args.length;
        List<String> namesList = Arrays.asList(names);
        for (int i=0; i<len; i++) {
            String arg = args[i];
            if (arg.length() > 0 && arg.charAt(0) == '-' && namesList.contains(arg.substring(1))) {
                if (i + 1 < len) {
                    String nextArg = args[i+1];
                    if (nextArg.length() > 0 && nextArg.charAt(0) == '-') {
                        // If the next arg is another flag, then just return value "true"
                        // to confirm that the flag exists.
                        return "true";
                    } else {
                        return nextArg;
                    }
                } else {
                    // If this is the last arg, then just return "true" to verify that the
                    // flag exists.
                    return "true";
                }
            }
        }
        return null;
    }

    
    private static String getInputFile(String[] args) {
        if (statelessMode) {
            return getArgByName(args, "i", "input");
            
        }
        if (args.length > 0) {
            return args[0];
        } else {
            return "test.css";
        }
    }
    
    private static Properties loadProjectProperties(File projectDir) throws IOException {
        
        Properties out = new Properties();
        try (FileInputStream fis = new FileInputStream(new File(projectDir, "codenameone_settings.properties"))) {
            out.load(fis);
        };
        return out;
    }
    
    private static boolean isStatelessMode(String[] args) {
        for (String arg : args) {
            if ("-stateless".equals(arg)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean isMergeMode(String[] args) {
        String inputFile = getInputFile(args);
        File f = new File(inputFile);
        if (!"theme.css".equals(f.getName())) {
            return false;
        }
        try {
            File projectDir = getProjectDir(f);
            Properties props = loadProjectProperties(projectDir);
            return "true".equals(props.getProperty("codename1.cssTheme"));
            
        } catch (IOException ex) {
            return false;
        }
        
    }
    
    private static String prefixUrls(String contents, String prefix) {
        
        contents = contents.replaceAll("url\\(\"(.*?)\"\\)", "url($1)");
        contents = contents.replaceAll("url\\('(.*?)'\\)", "url($1)");
        contents = contents.replaceAll("url\\((.*?://.*?)\\)", "url(\"$1\")");
        contents = contents.replaceAll("url\\((/.*?)\\)", "url(\"$1\")");
        //contents = contents.replaceAll("url\\(((?!(.*://)).*?)?\\)", "url("+prefix+"$1)");
        contents = contents.replaceAll("url\\(([^\\\"\'].*?)\\)", "url(\""+prefix+"$1\")");
        return contents;
    }
    
    private static String getRelativePath(File file, File relativeTo) throws IOException {
        File projectDir = getProjectDir(file);
        if (!relativeTo.getAbsolutePath().startsWith(projectDir.getAbsolutePath())) {
            throw new IllegalArgumentException("Relative file not in project: "+relativeTo);
        }
        if (!file.getAbsolutePath().startsWith(projectDir.getAbsolutePath())) {
            throw new IllegalArgumentException("File not in project: "+file);
        }
        
        List<String> fileAncestors = new ArrayList<String>();
        File tmp = file;
        while (tmp.getParentFile() != null) {
            tmp = tmp.getParentFile();
            fileAncestors.add(tmp.getAbsolutePath());
            if (tmp.getAbsolutePath().equals(projectDir.getAbsolutePath())) {
                break;
            }
            
        }
        List<String> relativeToAncestors = new ArrayList<String>();
        tmp = relativeTo;
        while (tmp.getParentFile() != null) {
            tmp = tmp.getParentFile();
            relativeToAncestors.add(tmp.getAbsolutePath());
            if (tmp.getAbsolutePath().equals(projectDir.getAbsolutePath())) {
                break;
            }
            
        }
        String commonAncestor = null;
        for (String filePath : fileAncestors) {
            int index = relativeToAncestors.indexOf(filePath);
            if (index > -1) {
                commonAncestor = filePath;
                break;
            }
        }
        if (commonAncestor == null) {
            throw new IOException("No common ancestors found between "+file+" and "+relativeTo+".  FileAncestors: "+fileAncestors+", relativeToAncestors:"+relativeToAncestors);
        }
        int distanceUpToCommonAncestor = relativeTo.getAbsolutePath().substring(commonAncestor.length()+1).split(Pattern.quote(File.separator)).length-1;
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<distanceUpToCommonAncestor; i++) {
            sb.append("../");
        }
        sb.append(file.getAbsolutePath().substring(commonAncestor.length()+1).replace('\\', '/'));
        return sb.toString();
    }
    
    private static void delTree(File dir) {
        for(File f : dir.listFiles()) {
            if(f.isDirectory()) {
                delTree(f);
            } else {
                f.delete();
            }
        }
    }
    private static void syncDirectories(File srcDir, File destDir) throws IOException {
        File canonicalSrc = srcDir.getCanonicalFile();
        File canonicalDest = destDir.getCanonicalFile();
        
        if (canonicalSrc.equals(canonicalDest)) {
            return;
        }
        
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        
        if (contains(srcDir, destDir) || contains(destDir, srcDir)) {
            throw new IllegalArgumentException("Cannot sync dir "+srcDir+" to "+destDir+" because one contains the other");
        }
        HashSet<String> destChildren = new HashSet<String>();
        String[] children = destDir.list();
        if (children != null) {
            for (String child : children) {
                destChildren.add(child);
            }
        }
        for (File child : srcDir.listFiles()) {
            
            String childName = child.getName();
            File destChild = new File(destDir, childName);
            if (destChildren.contains(childName)) {
                
                if (child.isDirectory()) {
                    if (destChild.isDirectory()) {
                        syncDirectories(child, destChild);
                    } else {
                        destChild.delete();
                        destChild.mkdir();
                        syncDirectories(child, destChild);
                    }
                } else {
                    if (destChild.isDirectory()) {
                        delTree(destChild);
                        Files.copy(child.toPath(), destChild.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }  else {
                        long destMtime = destChild.lastModified();
                        long srcMTime = child.lastModified();
                        if (destMtime < srcMTime) {
                            Files.copy(child.toPath(), destChild.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            } else {
                
                if (child.isDirectory()) {
                    destChild.mkdir();
                    syncDirectories(child, destChild);
                } else {
                    Files.copy(child.toPath(), destChild.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        
        
    }
    
    private static String getMd5(String input) 
    { 
        try { 
  
            // Static getInstance method is called with hashing MD5 
            MessageDigest md = MessageDigest.getInstance("MD5"); 
  
            // digest() method is called to calculate message digest 
            //  of an input digest() return array of byte 
            byte[] messageDigest = md.digest(input.getBytes()); 
  
            // Convert byte array into signum representation 
            BigInteger no = new BigInteger(1, messageDigest); 
  
            // Convert message digest into hex value 
            String hashtext = no.toString(16); 
            while (hashtext.length() < 32) { 
                hashtext = "0" + hashtext; 
            } 
            return hashtext; 
        }  
  
        // For specifying wrong message digest algorithms 
        catch (NoSuchAlgorithmException e) { 
            throw new RuntimeException(e); 
        } 
    } 
    
    /**
     * Checks if directory 1 contains directory 2
     * @param directory1
     * @param directory2
     * @return true if directory 1 contains directory 2
     */
    private static boolean contains(File directory1, File directory2) throws IOException {
        File canonical1 = directory1.getCanonicalFile();
        File canonical2 = directory2.getCanonicalFile();
        File parent2 = canonical2.getParentFile();
        if (parent2 == null) {
            return false;
        }
        if (canonical1.equals(parent2)) {
            return true;
        }
        return contains(directory2, parent2);
    }
    
    /**
     * Updates the given merged CSS file with the contents of the given input files.  This is only used when
     * running in stateless mode.
     * @param inputFiles List of input CSS files
     * @param mergedFile Output CSS file
     * @throws IOException If there is a problem writing the merged file.
     */
    private static void updateMergeFileStateless(File[] inputFiles, File mergedFile) throws IOException {
        boolean changed = false;
        
        long mergedFileLastModified = mergedFile.exists() ? mergedFile.lastModified() : 0l;
        for (File inputFile : inputFiles) {
            if (mergedFileLastModified < inputFile.lastModified()) {
                changed = true;
                break;
            }
        }
        
        if (!changed) {
            return;
        }
        
        File mergeRoot = mergedFile.getParentFile();
        File incDir = new File(mergeRoot, "cn1-merged-files");
        incDir.mkdir();
        
        
        StringBuilder buf = new StringBuilder();
        
        //File libCSSDir = getLibCSSDirectory(inputFile);
        //String relativePathToLibCSSDir = getRelativePath(libCSSDir, mergedFile);
        
        for (File f : inputFiles) {

            File canonicalFile = f.getCanonicalFile();
            String md5 = getMd5(canonicalFile.getAbsolutePath());
            File destDir = new File(incDir, md5);
            syncDirectories(canonicalFile.getParentFile(), destDir);
            
            String contents;
            try (FileInputStream fis = new FileInputStream(f)) {
                byte[] buffer = new byte[(int)f.length()];
                fis.read(buffer);
                contents = new String(buffer, "UTF-8");
            }
            
            contents = prefixUrls(contents, "cn1-merged-files/"+md5+"/");
            buf.append("\n/* "+f.getAbsolutePath()+" */\n").append(contents).append("\n/* end "+f.getAbsolutePath()+"*/\n");
        }
       
        try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
            fos.write(buf.toString().getBytes("UTF-8"));
        }
        
        
    }
    
    private static void updateMergeFile(File[] inputFiles, File mergedFile) throws IOException {
        if (statelessMode) {
            updateMergeFileStateless(inputFiles, mergedFile);
            return;
        } 
        File inputFile = inputFiles[0];
        List<File> libCSSFiles = findLibCSSFiles(inputFile);
        boolean changed = false;
        if (!mergedFile.exists() || mergedFile.lastModified() < inputFile.lastModified()) {
            changed = true;
        }
        if (!changed) {
            for (File f : libCSSFiles) {
                if (mergedFile.lastModified() < f.lastModified()) {
                    changed = true;
                    break;
                }
            }
        }
        if (!changed) {
            return;
        }
        StringBuilder buf = new StringBuilder();
        
        File libCSSDir = getLibCSSDirectory(inputFile);
        String relativePathToLibCSSDir = getRelativePath(libCSSDir, mergedFile);
        
        for (File f : libCSSFiles) {
            String contents;
            try (FileInputStream fis = new FileInputStream(f)) {
                byte[] buffer = new byte[(int)f.length()];
                fis.read(buffer);
                contents = new String(buffer, "UTF-8");
            }
            contents = prefixUrls(contents, relativePathToLibCSSDir+"/"+f.getParentFile().getName()+"/");
            buf.append("\n/* "+f.getAbsolutePath()+" */\n").append(contents).append("\n/* end "+f.getAbsolutePath()+"*/\n");
        }
        
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            buf.append("\n/* ").append(inputFile.getAbsolutePath()).append(" */\n");
            byte[] buffer = new byte[(int)inputFile.length()];
            fis.read(buffer);
            String fileContents = new String(buffer, "UTF-8");
            if (!mergedFile.getParentFile().getAbsolutePath().equals(inputFile.getParentFile().getAbsolutePath())) {
                // Merged file is in a different directory than the theme.css file.
                // We need to update the relative URL paths.
                String relativePathToInputFileDir = getRelativePath(inputFile.getParentFile(), mergedFile);
                fileContents = prefixUrls(fileContents, relativePathToInputFileDir+"/");

            }
            buf.append(fileContents);
            buf.append("\n/* End ").append(inputFile.getAbsolutePath()).append(" */\n");
        }
        
        try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
            fos.write(buf.toString().getBytes("UTF-8"));
        }
        
        
        
        
    }
    
   
    
    private static  File getProjectDir(File start) {
        File f = new File(start, "codenameone_settings.properties");
        
        while (!f.exists() && f.getParentFile().getParentFile() != null) {
            f = new File(f.getParentFile().getParentFile(), "codenameone_settings.properties");
            if (f.exists()) {
                return f.getParentFile();
            }
        }
        return f.exists() ? f.getParentFile() : null;
        
    }
    
    private static File getLibCSSDirectory(File inputFile) throws IOException {
        if (System.getProperty("cn1.libCSSDir", null) != null) {
            return new File(System.getProperty("cn1.libCSSDir"));
        }
        if (isMavenProject(inputFile)) {
            return new File(getProjectDir(inputFile), "target" + File.separator + "css");
        }
        return new File(getProjectDir(inputFile), 
                "lib" + File.separator + "impl" +File.separator + "css"
        );
    }
    
    private static List<File> findLibCSSFiles(File inputFile) throws IOException {
        ArrayList<File> out = new ArrayList<>();
        File cssDir = getLibCSSDirectory(inputFile);
        if (cssDir.exists() && cssDir.isDirectory()) {
            for (File child : cssDir.listFiles()) {
                if (child.isDirectory()) {
                    File themeCss = new File(child, "theme.css");
                    if (themeCss.exists()) {
                        out.add(themeCss);
                    }
                }
            }
        }
        
        return out;
       
    }
    
    private static boolean isMavenProject(File inputFile) throws IOException {
        return new File(getProjectDir(inputFile), "pom.xml").exists();
    }
    
    private static String getMergedFile(String inputPath) throws IOException {
        if (System.getProperty("cn1.cssMergeFile") != null) {
            System.getProperty("cn1.cssMergeFile");
        }
        File inputFile = new File(inputPath);
        if (isMavenProject(inputFile)) {
            return new File(getLibCSSDirectory(inputFile), inputFile.getName()+".merged").getAbsolutePath();
        }
        return inputPath + ".merged";
    }
    
    private static boolean checkWatchMode(String[] args) {
        for (int i=0; i<args.length; i++) {
            if ("-watch".equals(args[i])) {
                return true;
            }
        }
        return false;
    }
    
    private static File[] getInputFiles(String inputPath) {
        List<File> out = new ArrayList<File>();
        if (inputPath.contains(",")) {
            String[] paths = inputPath.split(",");
            for (String path : paths) {
                if (path.trim().isEmpty()) {
                    continue;
                }
                File f = new File(path);
                out.add(f);
            }
        } else {
            if (!inputPath.trim().isEmpty()) {
                out.add(new File(inputPath));
            }
        }
        return out.toArray(new File[out.size()]);
    }
            
    
    
    public static void main(String[] args) throws Exception {
        if (getArgByName(args, "help") != null) {
            System.out.println("Codename One CSS Compiler\nversion "+version);
            System.out.println("Usage Instructions:\n");
            System.out.println(" java -jar designer.jar -Dcli=true -css -input [inputfile.css] -output [outputfile.res] OPTIONS...\n");
            System.out.println("Options:");
            System.out.println(" -i, -input         Input CSS file path.  Multiple files separated by commas.");
            System.out.println(" -o, -output        Output res file path.");
            System.out.println(" -m, -merge         Path to merge file, used in  case there are multipl input files.");
            System.out.println(" -w, -watch         Run in watch mode.");
            System.out.println("                    Watches input files for changes and automatically recompiles.");
            System.out.println("\nSystem Properties:");
            System.out.println(" cef.dir            The path to the CEF directory.");
            System.out.println("                    Required for generation of image borders.");
            System.out.println(" parent.port        The port number to connect to the parent process for watch mode so that it knows ");
            System.out.println("                    to exit if the parent process ends.");
            return;
            
            
        }
        statelessMode = getArgByName(args, "i", "input") != null;
        String inputPath;
        String outputPath;
        String mergedFile;
        if (statelessMode) {
            System.out.println("Using stateless mode");
            inputPath = getArgByName(args, "i", "input");
            outputPath = getArgByName(args, "o", "output");
            if (outputPath == null) {
                throw new IllegalArgumentException("Output path is required.  Use -o [filepath] or -output [filepath]");
            }
            watchmode = "true".equals(getArgByName(args, "watch"));
            if (watchmode && !CN1Bootstrap.isBootstrapped() && !CN1Bootstrap.isCEFLoaded()) {
                // In watch mode we require CEF to be loaded
                throw new MissingNativeBrowserException();
            }
            mergedFile = getArgByName(args, "m", "merge");
            
            mergeMode = inputPath.contains(",") || mergedFile != null;
            if (mergeMode && mergedFile == null) {
                throw new IllegalArgumentException("When compiling multiple files, you must specify a merge path.  Use -m [filepath] or -merge [filepath]");
            }
           
            
        } else {
            System.out.println("Using stateful mode. Use -help flag to see options for new stateless mode.");
            // We don't want media queries while editing CSS because we need to be 
            // editing the rules raw.
            if (checkWatchMode(args) && !CN1Bootstrap.isBootstrapped() && !CN1Bootstrap.isCEFLoaded()) {
                // In watch mode we require 
                throw new MissingNativeBrowserException();
            }
            Resources.setEnableMediaQueries(false);
            mergeMode = isMergeMode(args);
            inputPath = getInputFile(args);

            mergedFile = mergeMode ? getMergedFile(inputPath) : inputPath;
            

            outputPath = inputPath+".res";

            if (args.length > 1) {
                if ("-watch".equals(args[1])) {
                    watchmode = true;
                    File tmpF = new File(inputPath).getParentFile().getParentFile();
                    tmpF = new File(tmpF, "src");
                    tmpF = new File(tmpF, new File(inputPath).getName()+".res");
                    outputPath = tmpF.getAbsolutePath();
                } else {
                    outputPath = args[1];
                }
            } else {
                File tmpF = new File(inputPath).getParentFile().getParentFile();
                tmpF = new File(tmpF, "src");
                tmpF = new File(tmpF, new File(inputPath).getName()+".res");
                outputPath = tmpF.getAbsolutePath();

            }
            if (args.length > 2 && "-watch".equals(args[2])) {
                watchmode = true;
            }
        }
        
        
        if (!Display.isInitialized()) {
            JavaSEPort.setShowEDTViolationStacks(false);
            JavaSEPort.blockMonitors();
            JavaSEPort.setShowEDTWarnings(false);
            JFrame f = new JFrame();
            Display.init(f.getContentPane());
            int w = 640;
            int h = 480;
            f.getContentPane().setPreferredSize(new java.awt.Dimension(w, h));
            f.getContentPane().setMinimumSize(new java.awt.Dimension(w, h));
            f.getContentPane().setMaximumSize(new java.awt.Dimension(w, h));
            f.pack();
            //f.setVisible(true);
            
        }
        
        File[] inputFiles = getInputFiles(inputPath);
        if (inputFiles.length == 0) {
            throw new IllegalArgumentException("CSS Compiler requires at least one input file");
        }
        if (mergeMode) {
            System.out.println("Updating merge file "+mergedFile);
            updateMergeFile(inputFiles, new File(mergedFile));
        }

        File outputFile = new File(outputPath);
        if (watchmode && watchThread == null) {
            System.out.println("Starting watch thread to watch "+Arrays.toString(inputFiles));
            watchThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    
                    // When run in watch mode, a parent process can provide a port
                    // number that the cli compiler can connect to in order to detect
                    // if the parent process is "dead".  If this socket disconnects 
                    // for any reason, we know the parent process is dead and we can
                    // exit.
                    final String parentPort = System.getProperty("parent.port", null);
                    if (parentPort != null) {
                        Thread pulseThread = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    Socket sock = new Socket("127.0.0.1", Integer.parseInt(parentPort));
                                    sock.setKeepAlive(true);
                                    InputStream is = sock.getInputStream();
                                    while (is.read() >= 0) {
                                        // Still alive
                                    }
                                    
                                } catch (IOException ex) {
                                    Logger.getLogger(CN1CSSCLI.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                System.exit(0);
                            }
                            
                        });
                        pulseThread.setDaemon(true);
                        pulseThread.start();
                    }
                    
                    
                    PollingFileWatcher watcher = new PollingFileWatcher(inputFiles, 1000);
                    while (true) {
                        try {
                            watcher.poll();
                            try {
                                System.out.println("Changed detected in "+Arrays.toString(inputFiles)+".  Recompiling");
                                if (mergeMode) {
                                    updateMergeFile(inputFiles, new File(mergedFile));
                                    compile(new File(mergedFile), outputFile);
                                } else {
                                    compile(inputFiles[0], outputFile);
                                }

                                System.out.println("::refresh::"); // Signal to CSSWatcher in Simulator that it should refresh
                            } catch (Throwable t) {
                                System.err.println("Compile of "+Arrays.toString(inputFiles)+" failed");
                                t.printStackTrace();    
                            }
                        } catch (InterruptedException ex) {
                            Logger.getLogger(CN1CSSCLI.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }

                    
                    
                }
                
            });
            
        }

        try {
            if (mergeMode) {
                System.out.println("Compiling "+mergedFile+" to "+outputFile);
                compile(new File(mergedFile), outputFile);
            } else {
                compile(inputFiles[0], outputFile);
            }
            System.out.println("CSS file successfully compiled.  "+outputFile);
            
        } catch (Throwable t) {
            if (!CN1Bootstrap.isBootstrapped() && t instanceof MissingNativeBrowserException) {
                // Rethrow MissingNativeBrowserException so that it can be handled in main
                // to attempt to re-add
                throw t;
            }
            t.printStackTrace();
            if (!watchmode) {
                System.exit(1);
            }
        }
        
        if (!watchmode) {
            System.exit(0);
        } else {
            if (watchThread != null && !watchThread.isAlive()) {
                watchThread.start();
                watchThread.join();
            }
            
        }
        
    }
    
    
    
    
    private static void compile(File inputFile, File outputFile) throws IOException {
        File baseDir = inputFile.getParentFile().getParentFile();
        File checksumsFile = getChecksumsFile(baseDir);
        if (!checksumsFile.exists()) {
            saveChecksums(baseDir, new HashMap<String,String>());
        }
        if (!checksumsFile.exists()) {
            throw new RuntimeException("Failed to create checksums file");
        }
        FileChannel channel = new RandomAccessFile(checksumsFile, "rw").getChannel();
        FileLock lock = channel.lock();
        try {
            Map<String,String> checksums = loadChecksums(baseDir);
            if (outputFile.exists() && !isMavenProject(inputFile)) {
                String outputFileChecksum = getMD5Checksum(outputFile.getAbsolutePath());
                String previousChecksum = checksums.get(inputFile.getName());
                if (previousChecksum == null || !previousChecksum.equals(outputFileChecksum)) {
                    File backups = new File(inputFile.getParentFile(), ".backups");
                    backups.mkdirs();
                    File bak = new File(backups, outputFile.getName()+"."+System.currentTimeMillis()+".bak");
                    Files.copy(outputFile.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println(outputFile+" has been modified since it was last compiled.  Making copy at "+bak);
                    outputFile.delete();
                }
                
            }
           
            if (outputFile.exists() && inputFile.lastModified() <= outputFile.lastModified()) {
                System.out.println("File has not changed since last compile.");
                return;
            }
                    

            try {
                URL url = inputFile.toURI().toURL();
                //CSSTheme theme = CSSTheme.load(CSSTheme.class.getResource("test.css"));
                CSSTheme theme = CSSTheme.load(url);
                theme.cssFile = inputFile;
                theme.resourceFile = outputFile;
                JavaSEPort.setBaseResourceDir(outputFile.getParentFile());
                WebViewProvider webViewProvider = new WebViewProvider() {

                    @Override
                    public BrowserComponent getWebView() {
                        if (web == null) {
                            if (!CN1Bootstrap.isCEFLoaded()/* && !CN1Bootstrap.isJavaFXLoaded()*/) {
                                // In theory having JavaFX should work, but I'm having problems getting the snapshotter
                                // to output the correct bounds so we are killing FX support.  CEF only.
                                throw new MissingNativeBrowserException();
                            }
                            if (!CN.isEdt()) {
                                CN.callSerially(()->{
                                    getWebView();
                                });
                                int counter = 0;
                                while (web == null && counter++ < 50) {
                                    Util.sleep(100);
                                }
                                return web;
                            }
                            web = new BrowserComponent();
                            ComponentSelector.select("*", web).add(web, true).selectAllStyles()
                                    .setBgTransparency(0)
                                    .setMargin(0)
                                    .setPadding(0)
                                    .setBorder(Border.createEmpty())
                                    .each(new ComponentClosure() {
                                @Override
                                public void call(Component c) {
                                    c.setOpaque(false);
                                }
                                        
                                
                                    });
                            web.setOpaque(false);
                            Form f = new Form();
                            f.getContentPane().getStyle().setBgColor(0xff0000);
                            f.getContentPane().getStyle().setBgTransparency(0xff);
                            if (f.getToolbar() == null) {
                                f.setToolbar(new com.codename1.ui.Toolbar());
                            }
                            f.getToolbar().hideToolbar();
                            f.setLayout(new com.codename1.ui.layouts.BorderLayout());
                            f.add(CN.CENTER, web);
                            f.show();
                            
                            
                            
                            
                        } 
                        
                        return web;
                    }

                };


                File cacheFile = new File(theme.cssFile.getParentFile(), theme.cssFile.getName()+".checksums");
                if (outputFile.exists() && cacheFile.exists()) {
                    theme.loadResourceFile();
                    theme.loadSelectorCacheStatus(cacheFile);
                }

                theme.createImageBorders(webViewProvider);
                theme.updateResources();
                theme.save(outputFile);
                
                theme.saveSelectorChecksums(cacheFile);
                
                String checksum = getMD5Checksum(outputFile.getAbsolutePath());
                checksums.put(inputFile.getName(), checksum);
                saveChecksums(baseDir, checksums);
            
            } catch (MalformedURLException ex) {
                Logger.getLogger(CN1CSSCLI.class.getName()).log(Level.SEVERE, null, ex);
            } 
        } finally {
            if (lock != null) {
                lock.release();
            }
            if (channel != null) {
                channel.close();
            }
            
            
        }
    }
    
    private static byte[] createChecksum(String filename) throws IOException  {
        try {
            InputStream fis =  new FileInputStream(filename);
            
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;
            
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            
            fis.close();
            return complete.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
   }
    // see this How-to for a faster way to convert
   // a byte array to a HEX string
   private static String getMD5Checksum(String filename) throws IOException {
       byte[] b = createChecksum(filename);
       String result = "";

       for (int i=0; i < b.length; i++) {
           result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
       }
       return result;
   }
   
   private static Map<String,String> loadChecksums(File baseDir) {
       File checkSums = getChecksumsFile(baseDir);
       if (!checkSums.exists()){
           return new HashMap<String,String>();
       }
       HashMap<String,String> out = new HashMap<String,String>();
       try (FileInputStream fis = new FileInputStream(checkSums)){
            Scanner scanner = new Scanner(fis);

            //now read the file line by line...
            int lineNum = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lineNum++;
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    out.put(parts[0], parts[1]);
                }
            }
            return out;
        } catch(Exception e) { 
            //handle this
            return out;
        }
       
   }
   
   private static File getChecksumsFile(File baseDir) {
        try {
            if (isMavenProject(baseDir)) {
                return new File(getProjectDir(baseDir), "target" + File.separator + ".cn1_css_checksums");
            }
        } catch (Exception ex) {
            Log.e(ex);
        }
        
       return new File(baseDir, ".cn1_css_checksums");
   }
   
   private static void saveChecksums(File baseDir, Map<String,String> map) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileOutputStream(getChecksumsFile(baseDir)))) {
            for (String key : map.keySet()) {
                out.println(key+":"+map.get(key));
            }
        }
       
   }
}
