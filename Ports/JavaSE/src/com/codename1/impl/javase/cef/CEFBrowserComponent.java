/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import com.codename1.impl.javase.IBrowserComponent;
import com.codename1.impl.javase.JavaSEPort.Peer;
import com.codename1.impl.javase.PeerComponentBuffer;
import com.codename1.impl.javase.cef.BrowserPanel;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.BrowserComponent.JSRef;
import com.codename1.ui.CN;
import com.codename1.util.SuccessCallback;
import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
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
        /*
        SystemBootstrap.setLoader(new SystemBootstrap.Loader() {
            @Override
            public void loadLibrary(String libname) {
                String res = System.getProperty("SystemBootstrap.loader."+libname);
                if (res != null) {
                    return;
                }
                System.setProperty("SystemBootstrap.loader."+libname, libname);
                System.loadLibrary(libname);
            }
        });
        */
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
    private boolean ready;
    private List<Runnable> readyCallbacks = new LinkedList<Runnable>();
    
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
            args.add("--touch-events=enabled");
            args.add("--disable-gpu");
            
        } else {
            throw new UnsupportedOperationException("CEF Not implemented on this platform yet");
        }
        return args.toArray(new String[args.size()]);
    }
    
    public static CEFBrowserComponent create(JFrame frame, BrowserComponent parent) {
        CefSettings settings = new CefSettings();
        
        String[] args = createArgs();
        // Perform startup initialization on platforms that require it.
        if (!"true".equals(System.getProperty("cef.started", "false"))) {
            if (!CefApp.startup(args)) {
                throw new RuntimeException("CEF Startup initialization failed!");

            }
            System.setProperty("cef.started", "true");
        }
        

        // OSR mode is enabled by default on Linux.
        // and disabled by default on Windows and Mac OS X.
        boolean osrEnabledArg = true;
        boolean transparentPaintingEnabledArg = true;
        boolean createImmediately = true;
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
        CEFPeerComponentBuffer buffer = new CEFPeerComponentBuffer();
        final BrowserPanel panel = new BrowserPanel(
                buffer, parent,  osrEnabledArg, transparentPaintingEnabledArg, createImmediately, args);
        panel.setBrowserComponent(parent);
        
        
        CEFBrowserComponent out =  new CEFBrowserComponent(frame, panel);
        out.setPeerComponentBuffer(buffer);
        panel.setCEFBrowserComponent(out);
        return out;
    }

    @Override
    public void back() {
        if (!ready) {
            readyCallbacks.add(new Runnable() {
                public void run() {
                    back();
                }
            });
            return;
        }
        panel.getBrowser().goBack();
    }

    @Override
    public void forward() {
        if (!ready) {
            readyCallbacks.add(new Runnable() {
                public void run() {
                    forward();
                }
            });
            return;
        }
        panel.getBrowser().goForward();
    }

    @Override
    public void setPage(String html, String baseUrl) {
        if (!ready) {
            readyCallbacks.add(new Runnable() {
                public void run() {
                    setPage(html, baseUrl);
                }
            });
            return;
        }
        String url = "data:text/html,"+com.codename1.io.Util.encodeUrl(html);
        setURL(url);
    }

    @Override
    public String getTitle() {
        if (!ready) {
            return title_;
        }
        return panel.getTitle();
    }

    @Override
    public String getURL() {
        if (!ready) {
            return url_;
        }
        return panel.getURL();
    }

    private String url_;
    private String title_;
    
    @Override
    public void setURL(String url) {
        if (!ready) {
            url_ = url;
            readyCallbacks.add(new Runnable() {
                public void run() {
                    setURL(url);
                }
            });
            return;
        }
        panel.getBrowser().loadURL(url);
    }

    @Override
    public void stop() {
        if (!ready) {
            readyCallbacks.add(new Runnable() {
                public void run() {
                    stop();
                }
            });
            return;
        }
        panel.getBrowser().stopLoad();
    }

    @Override
    public void reload() {
        if (!ready) {
            readyCallbacks.add(new Runnable() {
                public void run() {
                    reload();
                }
            });
            return;
        }
        panel.getBrowser().reload();
    }

    @Override
    public boolean hasBack() {
        if (!ready) {
            return false;
        }
        return panel.getBrowser().canGoBack();
    }

    @Override
    public boolean hasForward() {
        if (!ready) {
            return false;
        }
        return panel.getBrowser().canGoForward();
    }

    @Override
    public void execute(String js) {
        if (!ready) {
            readyCallbacks.add(new Runnable() {
                public void run() {
                    execute(js);
                }
            });
            return;
        }
        panel.getBrowser().executeJavaScript(js, getURL(), 0);
    }

    @Override
    public String executeAndReturnString(String js) {
        throw new UnsupportedOperationException("Not supported ."); 
       
        
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
    
    public void fireReady() {
        if (!CN.isEdt()) {
            CN.callSerially(new Runnable() {
               public void run() {
                  fireReady(); 
               } 
            });
            return;
        }
        if (!ready) {
            ready = true;
            while (!readyCallbacks.isEmpty()) {
                readyCallbacks.remove(0).run();
            }
            
        }
    }

    @Override
    public boolean supportsExecuteAndReturnString() {
        return false;
    }
    
    
    
    
}
