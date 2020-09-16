/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;


import com.codename1.impl.javase.UnzipUtility;
import com.codename1.io.Log;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 *
 * @author shannah
 */
public class CN1Console {
    
    private static final String GROOVY_HOME=System.getProperty("user.home") + File.separator + ".codenameone" + File.separator + "groovy";
    private static final String GROOVY_URL="https://github.com/codenameone/cn1-binaries/raw/master/apache-groovy-binary-2.5.8.zip";
    private static boolean downloading;
    
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
    
    private void downloadGroovy() throws IOException {
        try {
            String url = GROOVY_URL;
            if (url == null) {
                throw new RuntimeException("No Groovy URL found for this platform");
            }
            File groovyDir = new File(GROOVY_HOME);
            if (groovyDir.exists()) {
                delTree(groovyDir);
            }
            groovyDir.getParentFile().mkdirs();
            File groovyZip = new File(groovyDir.getParentFile(), "groovy.zip");
            downloadToFile(url, groovyZip);
            File tmpDir = new File(groovyZip.getParentFile(), "groovy.tmp."+System.currentTimeMillis());
            try {
                new UnzipUtility().unzip(groovyZip.getAbsolutePath(), tmpDir.getAbsolutePath());
                groovyDir.mkdir();
                File libDirTmp = findDir(tmpDir, "lib");
                File legalDirTmp = findDir(tmpDir, "licenses");
                
                if (libDirTmp == null || !libDirTmp.exists()) {
                    throw new IOException("No lib dir found within Groovy zip");
                }
                if (legalDirTmp == null || !legalDirTmp.exists()) {
                    throw new IOException("No legal dir found within Groovy zip");
                }
                libDirTmp.renameTo(new File(groovyDir, "lib"));
                legalDirTmp.renameTo(new File(groovyDir, "licenses"));
                
                
                
            } finally {
                delTree(tmpDir);
                groovyZip.delete();
            }
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(CN1Console.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    
    private boolean download(final Component src) throws IOException {
        File groovyHome = new File(GROOVY_HOME);
        if (!groovyHome.exists()) {
            final JFrame f = new JFrame();
            f.setUndecorated(true);
            JLabel l = new JLabel("<html>Downloading Components...<br>Please Wait...</html>");
            f.getContentPane().setLayout(new BorderLayout());
            f.getContentPane().add(l, BorderLayout.CENTER);
            f.pack();
            
            if (Window.getWindows().length > 0) {
                Window w = Window.getWindows()[0];
                f.setBounds(w.getX() + w.getWidth()/2 - f.getWidth()/2, w.getY() + w.getHeight()/2 - f.getHeight()/2, f.getWidth(), f.getHeight());
            }
            f.setVisible(true);
            new Thread(new Runnable() {
                    public void run() {
                        try {
                            downloadGroovy();
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
    
                                     open(src);
                                }
                            });
                           
                        } catch (Exception ex) {
                            Log.e(ex);
                        } finally {
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                     f.setVisible(false);
                                     
                                }
                            });
                        }
                    }
            }).start();
            return true;
        }
        return false;
    }
    
    public void open(Component src) {
        try {
            
            if (download(src)) {
                return;
            }
            File lib = new File(GROOVY_HOME + File.separator + "lib");
            List<URL> jars = new ArrayList<>();
            for (File jar : lib.listFiles()) {
                if (jar.getName().endsWith(".jar")) {
                    jars.add(jar.toURI().toURL());
                } else if (jar.isDirectory()) {
                    for (File j2 : jar.listFiles()) {
                        if (j2.getName().endsWith(".jar")) {
                            jars.add(j2.toURI().toURL());
                        }
                    }
                }
            }
            URLClassLoader cl = new URLClassLoader(jars.toArray(new URL[0]), com.codename1.ui.Component.class.getClassLoader());
            //Thread.currentThread().setContextClassLoader(cl);
            //Class groovyClassLoaderClass = cl.loadClass("groovy.lang.GroovyClassLoader");
            //Constructor groovyClassLoaderConstructor = groovyClassLoaderClass.getConstructor(ClassLoader.class);
            //ClassLoader groovyClassLoader = (ClassLoader)groovyClassLoaderConstructor.newInstance(cl);
            //System.out.println("Classloader: "+cl);
            //System.out.println("Parent classloader: "+com.codename1.ui.Component.class.getClassLoader());
            
            //Class frame1 = getClass().getClassLoader().loadClass("com.codename1.ui.Form");
            //Class frame = cl.loadClass("com.codename1.ui.Form");
            //System.out.println("Frame 1 = "+frame+", frame2="+frame1+" (frame1==frame2? "+(frame==frame1));
            Class consoleClass = cl.loadClass("groovy.ui.Console");
            Constructor constructor = consoleClass.getConstructor(ClassLoader.class);
            Object console = constructor.newInstance(cl);
            Method runMethod = consoleClass.getMethod("run", new Class[0]);
            runMethod.invoke(console, new Object[0]);
            
            File scriptFile = File.createTempFile("tmpScript", ".groovy");
            scriptFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(scriptFile)) {
                fos.write("import com.codename1.ui.*\nimport static com.codename1.ui.ComponentSelector.$\nform=CN.currentForm\n".getBytes("UTF-8"));
            }
            
            Method loadScriptFile = consoleClass.getMethod("loadScriptFile", File.class);
            loadScriptFile.invoke(console, scriptFile);
        } catch (Exception ex) {
            Log.e(ex);
        }
    }
}
