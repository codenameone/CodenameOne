/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shannah
 */
public class CSSWatcher implements Runnable {
    private int simulatorReloadVersion = Integer.parseInt(System.getProperty("reload.simulator.count", "0"));
    private Thread watchThread, pulseThread;
    private ServerSocket pulseSocket;
    private Process childProcess;
    private boolean closing;
    private static final int MIN_DESIGNER_VERSION=6;
    
    public CSSWatcher() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                if (childProcess != null && childProcess.isAlive()) {
                    try {
                        closing = true;
                        childProcess.destroyForcibly();
                    } catch (Throwable t){}
                }
            }
            
        }));
    }

    public void stop() {
        closing = true;
        if (childProcess != null && childProcess.isAlive()) {
            try {
                childProcess.destroyForcibly();
            } catch (Throwable t){}
        }
        if (pulseSocket != null && !pulseSocket.isClosed()) {
            try {
                pulseSocket.close();
            } catch (Exception ex){}
        }
    }
    
    /**
     * Checks whether the CSS watcher is supported currently. This will check the
     * update project's properties to see the current version of the designer. 
     * CSS watcher is only supported for designer version 5 or higher.
     * 
     * <p>This can be overridden by setting {@literal csswatcher.enabled=true|false} in 
     * the codenameone_settings.properties file</p>
     * @return 
     */
    public static boolean isSupported() {
        File userHome = new File(System.getProperty("user.home"));
        File cn1Home = new File(userHome, ".codenameone");
        File updateStatusProps = new File(cn1Home, "UpdateStatus.properties");
        
        File cn1Props = new File("codenameone_settings.properties");
        if (cn1Props.exists()) {
            java.util.Properties cn1Properties = new Properties();
            try (InputStream input = new FileInputStream(cn1Props)) {
                cn1Properties.load(input);
                String cssWatcherEnabled = cn1Properties.getProperty("csswatcher.enabled", null);
                if (cssWatcherEnabled != null) {
                    return "true".equals(cssWatcherEnabled.toLowerCase());
                }
            } catch (IOException ex) {
                Logger.getLogger(CSSWatcher.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        
        if (updateStatusProps.exists()) {
            java.util.Properties updateStatusProperties = new Properties();
            try (InputStream input = new FileInputStream(updateStatusProps)){
                updateStatusProperties.load(input);
                String designerVersionStr = updateStatusProperties.getProperty("designer", "0");
                Double designerVersionDbl = Double.parseDouble(designerVersionStr);
                if (designerVersionDbl >= MIN_DESIGNER_VERSION) {
                    return true;
                }
            } catch (IOException ex) {
                Logger.getLogger(CSSWatcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return false;
    }
    
    
    public String unescapeXSI(final String s) throws IOException {
        StringBuilder sb = new StringBuilder();


        int segmentStart = 0;
        int searchOffset = 0;
        while (true) {
            final int pos = s.indexOf('\\', searchOffset);
            if (pos == -1) {
                if (segmentStart < s.length()) {
                    sb.append(s.substring(segmentStart));
                }
                break;
            }
            if (pos > segmentStart) {
                sb.append(s.substring(segmentStart, pos));
            }
            segmentStart = pos + 1;
            searchOffset = pos + 2;
        }

        return sb.toString();
    }

    private void watch() throws IOException {
        if (pulseSocket == null || pulseSocket.isClosed()) {
            // If the the Simulator is killed then the shutdown hook doesn't run
            // so we need an alternative way for the ResourceEditorApp to know that
            // the parent is dead so that it will close itself.
            // So we create a ServerSocket to serve as a "pulse".  
            // We pass the port to the ResourceEditorApp so that it can connect.
            // When the socket is disconnected for any reason, the ResourceEditor app will exit.
            pulseSocket = new ServerSocket(0);
            pulseThread = new Thread(new Runnable() {
                public void run() {
                    while (!closing) {
                        try {
                            Socket clientSocket = pulseSocket.accept();
                            
                        } catch (IOException ex) {
                            Logger.getLogger(CSSWatcher.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                }
            });
            pulseThread.setDaemon(true);
            
        }
        File javaBin = new File(System.getProperty("java.home"), "bin/java");
        final File srcFile = new File("css", "theme.css");
        String overrideInputs = System.getProperty("codename1.css.compiler.args.input", null);
        
        if (!srcFile.exists() && overrideInputs == null) {
            //System.out.println("No theme.css file found.  CSSWatcher canceled");
            return;
        } else {
            if (overrideInputs == null) {
                System.out.println("Found theme.css file.  Watching for changes...");
            } else {
                if (overrideInputs.trim().isEmpty()) {
                    System.out.println("CSS file "+overrideInputs+" does not exist.  Not activating CSS watcher");
                    return;
                }
                System.out.println("Watching CSS files for changes: ["+overrideInputs+"]");
            }
        }
        File resDir = JavaSEPort.instance.getSourceResourcesDir();
        
        File destFile = new File(resDir, "theme.res");
        
        String overrideOutputs = System.getProperty("codename1.css.compiler.args.output", null);
        if (overrideOutputs != null) {
            destFile = new File(overrideOutputs);
        }
        File userHome = new File(System.getProperty("user.home"));
        File cn1Home = new File(userHome, ".codenameone");
        File designerJar = new File(cn1Home, "designer_1.jar");
        if (System.getProperty("codename1.designer.jar", null) != null) {
            designerJar = new File(System.getProperty("codename1.designer.jar", null));
        }
        String cefDir = System.getProperty("cef.dir", cn1Home + File.separator + "cef");
        
        //List<String> args = new ArrayList<String>();
        ProcessBuilder pb = new ProcessBuilder(
                javaBin.getAbsolutePath(),
                "-jar", "-Dcli=true", 
                "-Dcef.dir="+cefDir, 
                "-Dparent.port="+pulseSocket.getLocalPort(), 
                designerJar.getAbsolutePath(), 
                "-css"    
        );
        List<String> args = pb.command();
        if (overrideInputs != null) {
            args.add("-input");
            args.add(overrideInputs);
            args.add("-output");
            args.add(overrideOutputs);
            args.add("-merge");
            args.add(System.getProperty("codename1.css.compiler.args.merge", null));
            args.add("-watch");
            
        } else {
            args.add(srcFile.getAbsolutePath());
            args.add(destFile.getAbsolutePath());
            args.add("-watch");
            args.add("-Dprism.order=sw");
        }
        
        
        
        Process p = pb.start();
        
        if (childProcess != null) {
            try {
                childProcess.destroyForcibly();
            } catch (Throwable t){}
        }
        childProcess = p;
        String line;
       
        OutputStream stdin = p.getOutputStream();
        final InputStream stderr = p.getErrorStream();
        final BufferedReader errorReader = new BufferedReader(new InputStreamReader(stderr));
        new Thread(new Runnable() {
            public void run() {

                while (true) {
                    int reloadVersion = Integer.parseInt(System.getProperty("reload.simulator.count", "0"));
                    if (reloadVersion != simulatorReloadVersion) {
                        stop();
                        break;
                    }
                    try {
                        String l = errorReader.readLine();
                        if (l != null) {
                            System.err.println("CSS> "+l);
                        } else {
                            break;
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(CSSWatcher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();
        
        InputStream stdout = p.getInputStream();
        

        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
        final File fDestFile = destFile;
        final String fOverrideInputs = overrideInputs;
        while (true) {
            try {
                int reloadVersion = Integer.parseInt(System.getProperty("reload.simulator.count", "0"));
                if (reloadVersion != simulatorReloadVersion) {
                    stop();
                    return;
                }
                String l = reader.readLine();
                if (l == null) {
                    if (!p.isAlive()) {
                        watchThread = null;
                        start();
                        break;
                    }
                }
                System.out.println("CSS> "+l);
                if ("::refresh::".equals(l)) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (fOverrideInputs != null) {
                                    System.out.println("CSS File "+fOverrideInputs+" has been updated.  Reloading styles from "+fDestFile);
                                } else {
                                    System.out.println("CSS File "+srcFile+" has been updated.  Reloading styles from "+fDestFile);
                                }
                                Resources res = Resources.open(new FileInputStream(fDestFile));
                                UIManager.getInstance().addThemeProps(res.getTheme(res.getThemeResourceNames()[0]));
                                Form f = CN.getCurrentForm();
                                if (f != null) {
                                    f.refreshTheme();
                                    f.revalidate();
                                }
                            } catch(Exception err) {
                                err.printStackTrace();
                            }
                        }
                    });
                }
            } catch (Throwable t) {
                if (closing) {
                    return;
                }
                t.printStackTrace();
                
                if (!p.isAlive()) {
                    watchThread = null;
                    start();
                    break;
                }
            }
        }
        
        
    }

    @Override
    public void run() {
        try {
            watch();
        } catch (Throwable t) {
            System.err.println("CSS watching failed");
            t.printStackTrace();
            watchThread = null;
        }
    }
    
    public void start() {
        if (watchThread == null) {
            watchThread = new Thread(this);
            watchThread.setDaemon(true);
            watchThread.start(); 
        }
    }
    
    
    
}
