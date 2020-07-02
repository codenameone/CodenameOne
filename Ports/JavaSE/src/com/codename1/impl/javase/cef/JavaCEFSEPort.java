/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import com.codename1.impl.javase.AbstractBrowserWindowSE;
import com.codename1.impl.javase.BrowserWindowFactory;
import com.codename1.impl.javase.fx.FXBrowserWindowSE;
import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import java.awt.EventQueue;
import java.util.Map;
import javax.swing.JFrame;

/**
 *
 * @author shannah
 */
public class JavaCEFSEPort extends JavaSEPort {
    
    private static boolean cefExists;

    @Override
    public void init(Object m) {
        super.init(m);
        try {
            Class.forName("org.cef.CefApp");
            
            cefExists = true;
        } catch (Throwable ex) {
        }
    }
    
    
    
    
    public boolean isNativeBrowserComponentSupported() {
        return cefExists && !blockNativeBrowser;
        //return false;
    }
    
    
    public PeerComponent createBrowserComponent(final Object parent) {
        boolean useWKWebView = "true".equals(Display.getInstance().getProperty("BrowserComponent.useWKWebView", "false"));
        if (useWKWebView) {
            if (!useWKWebViewChecked) {
                useWKWebViewChecked = true;
                Map<String, String> m = Display.getInstance().getProjectBuildHints();
                if(m != null) {
                    if(!m.containsKey("ios.useWKWebView")) {
                        Display.getInstance().setProjectBuildHint("ios.useWKWebView", "true");
                    }
                }
            }
        }
        return createCEFBrowserComponent(parent);
        
    }
    
    protected BrowserWindowFactory createBrowserWindowFactory() {
        return new BrowserWindowFactory() {
            @Override
            public AbstractBrowserWindowSE createBrowserWindow(String startURL) {
                return new FXBrowserWindowSE(startURL);
            }

       };
    }
    
    public PeerComponent createCEFBrowserComponent(final Object parent) {
        final PeerComponent[] out = new PeerComponent[1];
        if (!EventQueue.isDispatchThread()) {
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        
                        out[0] = createCEFBrowserComponent(parent);
                    }
                });
            } catch (Throwable ex) {
                throw new RuntimeException("Failed to create CEF browser", ex);
            }
            
            return out[0];
        } else {
            java.awt.Container cnt = canvas.getParent();
            while (!(cnt instanceof JFrame)) {
                cnt = cnt.getParent();
                if (cnt == null) {
                    return null;
                }
            }
            return CEFBrowserComponent.create((JFrame)cnt, (BrowserComponent)parent);
        }
    }
    
}
