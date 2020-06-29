/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import com.codename1.impl.javase.JavaSEPort.Peer;
import com.codename1.impl.javase.cef.BrowserPanel;
import com.codename1.ui.PeerComponent;
import java.awt.EventQueue;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.browser.CN1CefBrowserFactory;
import org.cef.browser.CefBrowserFactory;

/**
 *
 * @author shannah
 */
public class CEFBrowserComponent extends Peer implements IBrowserComponent  {
    private static String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isWindows = isWindows();
    private static boolean isMac = isMac();
    private static boolean isUnix = isUnix();
    static {
       CefBrowserFactory.setInstance(new CN1CefBrowserFactory());
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
    
    private BrowserPanel panel;
    
    public CEFBrowserComponent(JFrame frame, BrowserPanel browserPanel) {
        super(frame, browserPanel);
        this.panel = browserPanel;
        setFocusable(true);
        addFocusListener(new com.codename1.ui.events.FocusListener() {
            @Override
            public void focusGained(com.codename1.ui.Component cmp) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        panel.requestFocus();
                    }
                });
                
            }

            @Override
            public void focusLost(com.codename1.ui.Component cmp) {
                
            }
            
            
        });
        
    }
    
    
    
    private static String getLibPath() {
        String out = System.getProperty("cef.libPath", null);
        if (out != null) {
            return out;
        }
        if (isMac) {
            String cefRoot = System.getProperty("user.home")+File.separator+".codenameone"+File.separator+"cef"+File.separator;
            return cefRoot + "macos64";
        } else {
            throw new UnsupportedOperationException("CEF Not implemented on this platform yet");
        }
    }
    
    private static String[] createArgs() {
        List<String> args = new ArrayList<String>();
        if (isMac) {
            args.add(String.format("--framework-dir-path=%s/Chromium Embedded Framework.framework", getLibPath()));
            args.add(String.format("--main-bundle-path=%s/jcef Helper.app", getLibPath()));
            args.add(String.format("--browser-subprocess-path=%s/jcef Helper.app/Contents/MacOS/jcef Helper", getLibPath()));
            args.add("--disable-gpu");
        } else {
            throw new UnsupportedOperationException("CEF Not implemented on this platform yet");
        }
        return args.toArray(new String[args.size()]);
    }
    
    public static CEFBrowserComponent create(JFrame frame) {
        CefSettings settings = new CefSettings();
        
        String[] args = createArgs();
        // Perform startup initialization on platforms that require it.
        if (!CefApp.startup(args)) {
            throw new RuntimeException("CEF Startup initialization failed!");
            
        }

        // OSR mode is enabled by default on Linux.
        // and disabled by default on Windows and Mac OS X.
        boolean osrEnabledArg = true;
        boolean transparentPaintingEnabledArg = true;
        boolean createImmediately = false;
        for (String arg : args) {
            arg = arg.toLowerCase();
            if (arg.equals("--off-screen-rendering-enabled")) {
                osrEnabledArg = true;
            } else if (arg.equals("--transparent-painting-enabled")) {
                transparentPaintingEnabledArg = true;
            } else if (arg.equals("--create-immediately")) {
                createImmediately = true;
            }
        }

        System.out.println("Offscreen rendering " + (osrEnabledArg ? "enabled" : "disabled"));
        PeerComponentBuffer buffer = new PeerComponentBuffer();
        final BrowserPanel panel = new BrowserPanel(
                buffer, osrEnabledArg, transparentPaintingEnabledArg, createImmediately, args);
        
        CEFBrowserComponent out =  new CEFBrowserComponent(frame, panel);
        out.setPeerComponentBuffer(buffer);
        return out;
    }

    @Override
    public void back() {
        panel.getBrowser().goBack();
    }

    @Override
    public void forward() {
        panel.getBrowser().goForward();
    }

    @Override
    public void setPage(String html, String baseUrl) {
        throw new UnsupportedOperationException("setPage not implemented yet");
    }

    @Override
    public String getTitle() {
        return panel.getTitle();
    }

    @Override
    public String getURL() {
        return panel.getURL();
    }

    @Override
    public void setURL(String url) {
        System.out.println("Loading URL "+url);
        panel.getBrowser().loadURL(url);
    }

    @Override
    public void stop() {
        panel.getBrowser().stopLoad();
    }

    @Override
    public void reload() {
        panel.getBrowser().reload();
    }

    @Override
    public boolean hasBack() {
        return panel.getBrowser().canGoBack();
    }

    @Override
    public boolean hasForward() {
        return panel.getBrowser().canGoForward();
    }

    @Override
    public void execute(String js) {
        panel.getBrowser().executeJavaScript(js, getURL(), 0);
    }

    @Override
    public String executeAndReturnString(String js) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void setProperty(String key, Object value) {
        if(key.equalsIgnoreCase("User-Agent")) {
            //panel.getBrowser().getClient().
        }
    }

    @Override
    public void runLater(Runnable r) {
        EventQueue.invokeLater(r);
    }

    @Override
    public void clearHistory() {
        
    }

    @Override
    public void exposeInJavaScript(Object o, String name) {
        
    }
    
    
    
    
}
