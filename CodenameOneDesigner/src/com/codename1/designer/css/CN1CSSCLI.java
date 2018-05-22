/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.designer.css;



import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;
import com.codename1.designer.css.CSSTheme.WebViewProvider;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javax.swing.JFrame;
//import org.apache.tools.ant.types.Path;

/**
 *
 * @author shannah
 */
public class CN1CSSCLI extends Application {
    static Object lock = new Object();
    static WebView web;
    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("Opening JavaFX Webview to render some CSS styles");
        web = new WebView();
        web.getEngine().getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
            @Override
            public void changed(ObservableValue<? extends Throwable> ov, Throwable t, Throwable t1) {
                System.out.println("Received exception: "+t1.getMessage());
            }
        });
        Scene scene = new Scene(web, 400, 800, Color.web("#666670"));
        stage.setScene(scene);
        stage.show();
        synchronized(lock) {
            lock.notify();
        }
        //stage.hide();
        
    }
    
    public static boolean watchmode;
    private static Thread watchThread;
    public static void main(String[] args) throws Exception {
        
        String inputPath = "test.css";
        
        if (args.length > 0) {
            inputPath = args[0];
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
            Container cnt = new Container();
            cnt.setLayout(new BorderLayout());
            cnt.setSize(new Dimension(640, 480));
            Display.init(cnt);
            
            
        }
        
        //Thread.sleep(5000);
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
                                        compile(inputFile, outputFile);
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
                
            });
            
        }
        try {
            compile(inputFile, outputFile);
            System.out.println("CSS file successfully compiled.  "+outputFile);
        } catch (Throwable t) {
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
                    public WebView getWebView() {
                        if (web == null) {
                            new Thread(()->{
                                launch(CN1CSSCLI.class, new String[0]);
                            }).start();
                        }
                        while (web == null) {
                            System.out.println("Waiting for web browser");
                            synchronized(lock) {
                                try {
                                    lock.wait(1000l);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(CN1CSSCLI.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                        System.out.println("Web browser is available");
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
       try {
            Scanner scanner = new Scanner(checkSums);

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
