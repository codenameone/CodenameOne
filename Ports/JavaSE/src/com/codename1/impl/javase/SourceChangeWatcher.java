/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.sun.nio.file.SensitivityWatchEventModifier;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author shannah
 */
public class SourceChangeWatcher implements Runnable {
    private WatchService watchService;
    private List<File> watchDirectories = new ArrayList<File>();
    private List<Watch> watches = new ArrayList<Watch>();
    private boolean stopped;
    private Object app;
    
    public void setApp(Object obj) {
        this.app = obj;
    }
    
    private class Watch {
        private Path path;
        private WatchKey key;
        
        Watch(Path path, WatchKey key) {
            this.path = path;
            this.key = key;
        }
    }
    
    private Path getPathForKey(WatchKey key) {
        for (Watch w : watches) {
            if (key.equals(w.key)) {
                return w.path;
            }
        }
        return null;
    }
    
    
    private boolean recompile(Path path) throws IOException, InterruptedException {
        File f = path.toFile();
        File pom = findPom(f.getParentFile());
        if (pom == null) {
            System.out.println("Skipping recompile of "+path+" because no pom.xml was found");
            return false;
        }
        
        String mavenHome = System.getProperty("maven.home");
        if (mavenHome == null) {
            Log.p("Not recompiling path "+path+" because maven.home system property was not found.");
            return false;
        }
        
        String mavenPath = mavenHome + File.separator + "bin" + File.separator + "mvn";
        if (!new File(mavenPath).exists()) {
            if (new File(mavenPath+".exe").exists()) {
                mavenPath += ".exe";
            } else if (new File(mavenPath+".bat").exists()) {
                mavenPath += ".bat";
            } else if (new File(mavenPath+".cmd").exists()) {
                mavenPath += ".cmd";
            } else {
                Log.p("Not recompiling path "+path+" because " +mavenPath+" could not be found.");
                return false;
            }
        }
        
        ProcessBuilder pb = new ProcessBuilder(mavenPath, "compile", "-DskipComplianceCheck");
        pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
        pb.directory(pom.getParentFile());
        pb.inheritIO();
        Process p = pb.start();
        int result = p.waitFor();
        if (result != 0) {
            return false;
        }
        
        /// Sleep for a secont to allow the classloader to pick up the new classes.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            
        }
        
        CN.callSeriallyAndWait(new Runnable() {
            public void run() {
                try {
                    if (app != null) {
                        Method stop = app.getClass().getMethod("stop", new Class[0]);
                        stop.invoke(app, new Class[0]);

                        Method start = app.getClass().getMethod("start", new Class[0]);
                        start.invoke(app, new Class[0]);
                    }
                    
                    CN.restoreToBookmark();
                } catch (Exception ex) {
                    Log.e(ex); 
                }
            }
        });
        
        return true;
        
    }
    
    private File findPom(File startingPoint) {
        File pom = new File(startingPoint, "pom.xml");
        if (pom.exists()) return pom;
        File parent = startingPoint.getParentFile();
        if (parent != null) {
            return findPom(parent);
        }
        return null;
    }
    
    private void registerWatchRecursive(File directory) throws IOException {
        if (directory.isDirectory()) {
            WatchKey key = directory.toPath().register(watchService, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE}, SensitivityWatchEventModifier.HIGH);
            watches.add(new Watch(directory.toPath(), key));
            for (File child : directory.listFiles()) {
                registerWatchRecursive(child);
            }
        }
                    
    }
    private boolean requiresRecompile;
    @Override
    public void run() {
        try {
            System.out.println("SourceChangeWatcher running.  Watching directories "+watchDirectories);
            watchService = FileSystems.getDefault().newWatchService();
            for (File directory : watchDirectories) {
               registerWatchRecursive(directory);
                
            }
            
            while (!stopped && Display.isInitialized()) {
                try {
                    WatchKey key = watchService.take();
                    
                    if (stopped) {
                        return;
                    }
                    
                    Path path = getPathForKey(key);
                    requiresRecompile = false;
                    key.pollEvents().forEach(new Consumer<WatchEvent<?>>() {
                        @Override
                        public void accept(WatchEvent<?> evt) {
                            System.out.println("[Watcher " + SourceChangeWatcher.this + "] File changedL: " + evt.context() + " key=" + key);
                            if (evt.context().toString().endsWith(".java") || evt.context().toString().endsWith(".kt")) {
                                requiresRecompile = true;
                            }
                        }
                    });
                    if (requiresRecompile) {
                    
                        System.out.println("Changes detected in directory "+path);
                        recompile(path);
                    }
                   
                    // STEP8: Reset the watch key everytime for continuing to use it for further event polling
                    key.reset();
                } catch (InterruptedException ex) {
                    if (stopped) {
                        return;
                    }
                    Log.e(ex);
                }
            }
        } catch (IOException ex) {
            Log.e(ex);
        }

        
    }
    
    public void stop() {
        stopped = true;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (Exception ex){}
        
    }
    
    public SourceChangeWatcher() {
     
    }
    
    public void addWatchFolder(File path) {
        System.out.println("Adding watch folder "+path);
        watchDirectories.add(path);
    }
    
    public boolean hasWatchFolder(File path) {
        return watchDirectories.contains(path);
    }
    
}
