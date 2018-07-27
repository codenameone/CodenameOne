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

import com.codename1.impl.javase.JavaFXLoader.JavaFXNotLoadedException;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.ArrayList;
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
    private File javafxDir = new File(System.getProperty("javafx.install.dir", new File(System.getProperty("user.home"), ".codenameone" + File.separator + "javafx").getAbsolutePath()));
    private String winUrl = System.getProperty("javafx.win.url", "https://github.com/codenameone/cn1-binaries/raw/master/javafx-win.zip");
    private String macUrl = System.getProperty("javafx.mac.url", "https://github.com/codenameone/cn1-binaries/raw/master/javafx-mac.zip");
    private String linuxUrl = System.getProperty("javafx.linux.url", "https://github.com/codenameone/cn1-binaries/raw/master/javafx-linux.zip");
    
    
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
            File tmpDir = new File(javafxZip.getParentFile(), "javafx.tmp."+System.currentTimeMillis());
            try {
                new UnzipUtility().unzip(javafxZip.getAbsolutePath(), tmpDir.getAbsolutePath());
                javafxDir.mkdir();
                File libDirTmp = findDir(tmpDir, "lib");
                File legalDirTmp = findDir(tmpDir, "legal");
                if (libDirTmp == null || !libDirTmp.exists()) {
                    throw new IOException("No lib dir found within JavaFX zip");
                }
                if (legalDirTmp == null || !legalDirTmp.exists()) {
                    throw new IOException("No legal dir found within JavaFX zip");
                }
                libDirTmp.renameTo(new File(javafxDir, "lib"));
                legalDirTmp.renameTo(new File(javafxDir, "legal"));
                
            } finally {
                delTree(tmpDir);
                javafxZip.delete();
            }
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(JavaFXLoader.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    private ClassLoader addJavaFXToClassPath() throws IOException  {
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
        java.util.List<java.net.URL> javafxUrls = new ArrayList<java.net.URL>();
        for (File f : javafxLibDir.listFiles()) {
            if (!f.getName().endsWith(".jar")) {
                continue;
            }
            try {
                java.net.URL u = f.toURL();
                javafxUrls.add(u);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }

        }
        if (javafxUrls.isEmpty()) {
            throw new RuntimeException("JavaFX is missing.  This application requires a JDK with JavaFX.");
        }

        try {
            javafxUrls.add(javafxLibDir.toURL()); // Necessary for loading native libs
        } catch (MalformedURLException mex) {
            throw new RuntimeException(mex);
        }
        return addToClassPath(javafxUrls.toArray(new java.net.URL[javafxUrls.size()]));
    }
    
    
    public static class JavaFXNotLoadedException extends Exception {
        public JavaFXNotLoadedException(Throwable cause) {
            super(cause);
        }
    }
    
    public boolean runWithJavaFX(Class mainClass, String[] args) throws JavaFXNotLoadedException, InvocationTargetException {
        if (System.getProperty("JavaFXLoader.loaded", null) != null) {
            // Make sure that we don't cause infinite recursion.
            return false;
        }
        System.setProperty("JavaFXLoader.loaded", "1");
        ClassLoader cl;
        try {
            Class webEngineClass = mainClass.getClassLoader().loadClass("javafx.scene.web.WebEngine");
            cl = mainClass.getClassLoader();
        } catch (ClassNotFoundException ex) {
            
            try {
                cl = addJavaFXToClassPath();
            } catch (IOException ex1) {
                throw new JavaFXNotLoadedException(ex1);
            }
        }
        Class executorClass;
        try {
            executorClass = cl.loadClass(mainClass.getName());
        } catch (ClassNotFoundException ex1) {
            Logger.getLogger(JavaFXLoader.class.getName()).log(Level.SEVERE, null, ex1);
            throw new JavaFXNotLoadedException(ex1);
        }
        Method mainMethod;
        try {
            mainMethod = executorClass.getDeclaredMethod("main", new Class[]{String[].class});
        } catch (Exception ex1) {
            throw new RuntimeException("Main class supplied to runWithJavaFX has no main method; or failed to load the main method", ex1);
        }
        try {
            mainMethod.invoke(null, new Object[]{args});
        } catch (IllegalAccessException ex1) {
            Logger.getLogger(JavaFXLoader.class.getName()).log(Level.SEVERE, null, ex1);
            throw new RuntimeException("main() method doesn't seem to allow access", ex1);
        } catch (IllegalArgumentException ex1) {
            Logger.getLogger(JavaFXLoader.class.getName()).log(Level.SEVERE, null, ex1);
            throw new RuntimeException("main() method invalid argument", ex1);
        } 
        
        return true;
    }
    
    private ClassLoader addToClassPath(java.net.URL... paths) {
        String[] parts = System.getProperty("java.class.path", "").split(File.pathSeparator);
        java.util.List<java.net.URL> urls = new java.util.ArrayList<java.net.URL>();
        urls.addAll(Arrays.asList(paths));
        for (String part : parts) {
            try {
                java.net.URL url = new File(part).toURL();
                urls.add(url);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
        StringBuilder newCp = new StringBuilder();
        boolean first = true;
        for (java.net.URL url : urls) {
            try {
                if (first) {
                    first = false;
                } else {
                    newCp.append(File.pathSeparator);
                }
                newCp.append(new File(url.toURI()).getAbsolutePath());
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
        System.setProperty("java.class.path", newCp.toString());
        
        return new URLClassLoader(urls.toArray(new java.net.URL[urls.size()])) {
            @Override
            public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("java") || name.startsWith("com.sun") || name.startsWith("org.jdesktop")) {
                    return super.loadClass(name, resolve);
                }
                Class cls = findLoadedClass(name);
                if (cls != null) {
                    return cls;
                }
                try {
                    return findClass(name);
                } catch (ClassNotFoundException ex) {
                    return super.loadClass(name, resolve); //To change body of generated methods, choose Tools | Templates.
                }
            }
            
        };
    }
    public static boolean main(Class mainClass, String[] argv) throws JavaFXNotLoadedException, InvocationTargetException {
        return new JavaFXLoader().runWithJavaFX(mainClass, argv);
    }
}

class UnzipUtility {
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    public void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
    
    
}
