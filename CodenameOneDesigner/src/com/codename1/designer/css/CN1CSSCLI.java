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
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
//import org.apache.tools.ant.types.Path;

/**
 *
 * @author shannah
 */
public class CN1CSSCLI {
    static Object lock = new Object();
    static BrowserComponent web;
    
    public void start() throws Exception {
        //Platform.setImplicitExit(false);
        startImpl();
        //stage.hide();
        
    }
    
    private static void startImpl() throws Exception {
        //System.out.println("Opening JavaFX Webview to render some CSS styles");
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
    
    public static boolean mergeMode;
    public static boolean watchmode;
    private static Thread watchThread;
    
    private static String getInputFile(String[] args) {
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
    
    private static void updateMergeFile(File inputFile, File mergedFile) throws IOException {
        System.out.println("Updating merge file "+mergedFile);
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
        for (File f : libCSSFiles) {
            String contents;
            System.out.println("Merging "+f);
            try (FileInputStream fis = new FileInputStream(f)) {
                byte[] buffer = new byte[(int)f.length()];
                fis.read(buffer);
                contents = new String(buffer, "UTF-8");
            }
            contents = prefixUrls(contents, "../lib/impl/css/"+f.getParentFile().getName()+"/");
            buf.append("\n/* "+f.getAbsolutePath()+" */\n").append(contents).append("\n/* end "+f.getAbsolutePath()+"*/\n");
        }
        
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            buf.append("\n/* ").append(inputFile.getAbsolutePath()).append(" */\n");
            byte[] buffer = new byte[(int)inputFile.length()];
            fis.read(buffer);
            String fileContents = new String(buffer, "UTF-8");
            buf.append(fileContents);
            buf.append("\n/* End ").append(inputFile.getAbsolutePath()).append(" */\n");
        }
        
        try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
            fos.write(buf.toString().getBytes("UTF-8"));
        }
        
        
        
    }
    
    private static File getProjectDir(File inputFile) throws IOException {
        return inputFile.getCanonicalFile().getParentFile().getParentFile();
        
    }
    
    private static File getLibCSSDirectory(File inputFile) throws IOException {
        
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
    
    public static void main(String[] args) throws Exception {
        // We don't want media queries while editing CSS because we need to be 
        // editing the rules raw.
        Resources.setEnableMediaQueries(false);
        mergeMode = isMergeMode(args);
        String inputPath = getInputFile(args);
        
        String mergedFile = mergeMode ? inputPath + ".merged" : inputPath;
        if (mergeMode) {
            updateMergeFile(new File(inputPath), new File(mergedFile));
        }
        
        String outputPath = inputPath+".res";
        
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
        System.out.println("Input: "+inputPath);
        System.out.println("Output: "+outputPath);
        if (args.length > 2 && "-watch".equals(args[2])) {
            watchmode = true;
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
        
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        
        if (watchmode && watchThread == null) {
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
                    
                    boolean usePollingFileWatcher = true;
                    
                    if (usePollingFileWatcher) {
                        PollingFileWatcher watcher = new PollingFileWatcher(inputFile, 1000);
                        while (true) {
                            try {
                                watcher.poll();
                                try {
                                    System.out.println("Changed detected in "+inputFile+".  Recompiling");
                                    if (mergeMode) {
                                        updateMergeFile(new File(inputPath), new File(mergedFile));
                                        compile(new File(mergedFile), outputFile);
                                    } else {
                                        compile(inputFile, outputFile);
                                    }
                                    
                                    System.out.println("CSS file successfully compiled.  "+outputFile);
                                    System.out.println("::refresh::"); // Signal to CSSWatcher in Simulator that it should refresh
                                } catch (Throwable t) {
                                    System.err.println("Compile of "+inputFile+" failed");
                                    t.printStackTrace();
                                }
                            } catch (InterruptedException ex) {
                                Logger.getLogger(CN1CSSCLI.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                        }
                    } else {
                        final Path path = inputFile.getParentFile().toPath();
                    
                        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
                            System.out.println("Watching file "+inputFile+" for changes...");

                            final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                            while (true) {
                                final WatchKey wk = watchService.take();
                                for (WatchEvent<?> event : wk.pollEvents()) {
                                    //we only register "ENTRY_MODIFY" so the context is always a Path.
                                    final Path changed = (Path) event.context();
                                    //System.out.println("Change detected at path "+changed);
                                    File changedFile = new File(inputFile.getParentFile(), changed.toString());
                                    if (inputFile.equals(changedFile)) {
                                        try {
                                            System.out.println("Changed detected in "+inputFile+".  Recompiling");
                                            if (mergeMode) {
                                                updateMergeFile(new File(inputPath), new File(mergedFile));
                                                compile(new File(mergedFile), outputFile);
                                            } else {
                                                compile(inputFile, outputFile);
                                            }
                                            System.out.println("CSS file successfully compiled.  "+outputFile);
                                            System.out.println("::refresh::"); // Signal to CSSWatcher in Simulator that it should refresh
                                        } catch (Throwable t) {
                                            System.err.println("Compile of "+inputFile+" failed");
                                            t.printStackTrace();
                                        }
                                    }

                                }
                                // reset the key
                                boolean valid = wk.reset();
                                if (!valid) {
                                    System.out.println("Key has been unregisterede");
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(CN1CSSCLI.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    
                    
                }
                
            });
            
        }
        try {
            if (mergeMode) {
                compile(new File(mergedFile), outputFile);
            } else {
                compile(inputFile, outputFile);
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
        System.out.println("Acquiring lock on CSS checksums file "+checksumsFile+"...");
        FileLock lock = channel.lock();
        System.out.println("Lock obtained");
        try {
            Map<String,String> checksums = loadChecksums(baseDir);
            //System.out.println("Loaded checksums["+baseDir+"]: "+checksums);
            if (outputFile.exists()) {
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
                //System.out.println("Cache file: "+cacheFile+" [Exists="+(cacheFile.exists()+"]"));
                if (outputFile.exists() && cacheFile.exists()) {
                    theme.loadResourceFile();
                    //System.out.println("Loading cache file: "+cacheFile);
                    theme.loadSelectorCacheStatus(cacheFile);
                }

                theme.createImageBorders(webViewProvider);
                theme.updateResources();
                theme.save(outputFile);
                
                theme.saveSelectorChecksums(cacheFile);
                
                String checksum = getMD5Checksum(outputFile.getAbsolutePath());
                checksums.put(inputFile.getName(), checksum);
                //System.out.println("Saving checksums ["+baseDir+"]: "+checksums);
                saveChecksums(baseDir, checksums);
            
            } catch (MalformedURLException ex) {
                Logger.getLogger(CN1CSSCLI.class.getName()).log(Level.SEVERE, null, ex);
            } 
        } finally {
            if (lock != null) {
                System.out.println("Releasing lock");
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
