/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import com.codename1.impl.javase.IBrowserComponent;
import com.codename1.impl.javase.JavaSEPort;
import com.codename1.impl.javase.JavaSEPort.Peer;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.BrowserNavigationCallback;
import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.ref.WeakReference;
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
    
    
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final boolean isWindows = isWindows();
    private static final boolean isMac = isMac();
    private static final boolean isUnix = isUnix();
    private static final boolean is64Bit = is64Bit();
    private static final String ARCH = System.getProperty("os.arch");
    private static final boolean is64Bit() {
        
        String model = System.getProperty("sun.arch.data.model",
                                          System.getProperty("com.ibm.vm.bitmode"));
        if (model != null) {
            return "64".equals(model);
        }
        if ("x86-64".equals(ARCH)
            || "ia64".equals(ARCH)
            || "ppc64".equals(ARCH) || "ppc64le".equals(ARCH)
            || "sparcv9".equals(ARCH)
            || "mips64".equals(ARCH) || "mips64el".equals(ARCH)
            || "amd64".equals(ARCH)
            || "aarch64".equals(ARCH)) {
            return true;
        }
        return false;
    }
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
    private boolean ready;
    private List<Runnable> readyCallbacks = new LinkedList<Runnable>();
    
    
    private com.codename1.ui.events.FocusListener focusListener = new com.codename1.ui.events.FocusListener() {
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


    };
    
    public CEFBrowserComponent(JFrame frame, BrowserPanel browserPanel) {
        super(frame, browserPanel);
        this.panel = browserPanel;
        setFocusable(true);

        
    }

    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        addFocusListener(focusListener);
    }

    @Override
    protected void deinitialize() {
        removeFocusListener(focusListener);
        super.deinitialize();
    }

    
    
    
    
    private static String getLibPath() {
        String out = System.getProperty("cef.libPath", null);
        if (out != null) {
            return out;
        }
        
        if (isMac) {
            String cefRoot = System.getProperty("user.home")+File.separator+".codenameone"+File.separator+"cef"+File.separator;
            return cefRoot + "macos64";
        } else if (isWindows) {
            String bitSuffix = is64Bit ? "64" : "32";
            String cefRoot = System.getProperty("user.home")+File.separator+".codenameone"+File.separator+"cef"+File.separator+"lib"+File.separator;
            return cefRoot + "win"+bitSuffix;
        } else if (isUnix && is64Bit) {
            
            String bitSuffix = is64Bit ? "64" : "32";
            String cefRoot = System.getProperty("user.home")+File.separator+".codenameone"+File.separator+"cef"+File.separator+"lib"+File.separator;
            return cefRoot + "linux"+bitSuffix;
        }else {
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
        } else if (isWindows) {
            // no extra stuff here
            //args.add(String.format("--browser-subprocess-path=%s\\jcef_helper.exer", getLibPath()));
            
            args.add("--disable-gpu");
            args.add("--disable-software-rasterizer");
            args.add("--disable-gpu-compositing");
        } else if (isUnix) {
            // no extra stuff here
            //args.add(String.format("--browser-subprocess-path=%s\\jcef_helper.exer", getLibPath()));
            
            args.add("--disable-gpu");
            args.add("--disable-software-rasterizer");
            args.add("--disable-gpu-compositing");
        } else {
            throw new UnsupportedOperationException("CEF Not implemented on this platform yet");
        }
        //args.add("--allow-file-access-from-files");
        args.add("--touch-events=enabled");
        args.add("--enable-media-stream");
        //args.add("--device-scale-factor=4");
        //args.add("--force-device-scale-factor=4");
        args.add("--autoplay-policy=no-user-gesture-required");
        args.add("--enable-usermedia-screen-capturing");
        //System.out.println("CEF Args: "+args);
        return args.toArray(new String[args.size()]);
    }
    
    public static CEFBrowserComponent create(BrowserComponent bc) {
        return create(new CEFBrowserComponentAdapter(bc));
    }
    public static CEFBrowserComponent create(CEFBrowserComponentListener parent) {
        return create(null, parent);
    }
    public static CEFBrowserComponent create(final String startingURL, final CEFBrowserComponentListener parent) {
        CefSettings settings = new CefSettings();
        
        String[] args = createArgs();
        // Perform startup initialization on platforms that require it.
        if (!"true".equals(System.getProperty("cef.started", "false"))) {
            if (!CefApp.startup(args)) {
                System.err.println("CEFStartup initialization failed");
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

        
        CEFPeerComponentBuffer buffer = new CEFPeerComponentBuffer();
        final WeakReference<CEFBrowserComponentListener> parentRef = new WeakReference<CEFBrowserComponentListener>(parent);
        BrowserNavigationCallback navigationCallback = new BrowserNavigationCallback() {
            private CEFBrowserComponentListener l = parentRef.get();
            @Override
            public boolean shouldNavigate(String url) {
                //System.out.println("in shouldNavigate "+url);
                //CEFBrowserComponentListener l = parentRef.get();
                if (l != null) {
                    return l.shouldNavigate(url);
                }
                return false;
            }
            
        };
        final BrowserPanel panel = new BrowserPanel(
                startingURL, buffer, navigationCallback,  osrEnabledArg, transparentPaintingEnabledArg, createImmediately, args) {
            
            private CEFBrowserComponentListener p = parentRef.get();
                    @Override
            protected void onError(ActionEvent l) {
                //CEFBrowserComponentListener p = parentRef.get();
                if (p != null) {
                    p.onError(l);
                }
            }

            @Override
            protected void onStart(ActionEvent l) {
                if (p != null) {
                    p.onStart(l);
                }
            }

            @Override
            protected void onLoad(ActionEvent l) {
                if (p != null) {
                    p.onLoad(l);
                }
            }
          
        };
        

        java.awt.Container cnt = JavaSEPort.instance.getCanvas().getParent();
        
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                System.err.println("CEFBrowserComponent requires a JFrame as an ancestor.  None found.  Returning null");
                return null;
            }
        }
        
        final CEFBrowserComponent out =  new CEFBrowserComponent((JFrame)cnt, panel);
        out.setPeerComponentBuffer(buffer);
        
        final WeakReference<CEFBrowserComponent> weakRef = new WeakReference<CEFBrowserComponent>(out);
        panel.setReadyCallback(new Runnable() {
            public void run() {
                CEFBrowserComponent callback = weakRef.get();
                if (callback != null) {
                    callback.fireReady();
                }
                
            }
        });
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
        //String url = "data:text/html,"+com.codename1.io.Util.encodeUrl(html);
        if (Display.getInstance().getProperty("cef.setPage.useDataURI", "false").equals("true")) {
            setURL("data:text/html,"+com.codename1.io.Util.encodeUrl(html));
            return;
        }
        try {
            byte[] bytes = html.getBytes("UTF-8");
            StreamWrapper stream = new StreamWrapper(new ByteArrayInputStream(bytes), "text/html" , bytes.length);
            
            String id = BrowserPanel.getStreamRegistry().registerStream(stream);
            
            String url = "https://cn1app/streams/"+id;
            setURL(url);
        } catch (Exception ex) {
            setURL("data:text/html,"+com.codename1.io.Util.encodeUrl(html));
        }
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
    private final Object readyLock = new Object();
    
    @Override
    public void setURL(final String url) {
        synchronized(readyLock) {
            if (!ready) {
                url_ = url;
                readyCallbacks.add(new Runnable() {
                    public void run() {
                        setURL(url);
                    }
                });
                return;
            }
        }
        url_ = url;
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
    public void execute(final String js) {
        if (!ready) {
            readyCallbacks.add(new Runnable() {
                public void run() {
                    execute(js);
                }
            });
            return;
        }
        panel.getBrowser().executeJavaScript(js, js, 0);
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
        List<Runnable> toRun = null;
        synchronized(readyLock) {
            if (!ready) {
                ready = true;
                if (!readyCallbacks.isEmpty()) {
                    toRun = new ArrayList<Runnable>(readyCallbacks);
                    readyCallbacks.clear();
                }
            }
        }
        if (toRun != null && !toRun.isEmpty()) {
            while (!toRun.isEmpty()) {
                toRun.remove(0).run();
            }
        }
       
    }

    @Override
    public boolean supportsExecuteAndReturnString() {
        return false;
    }
    
    
    public void cleanup() {
        if (panel != null) {
            final BrowserPanel fPanel = panel;
            panel = null;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if (fPanel != null) {
                        fPanel.cleanup();
                    }
                }
            });
        }
    }
   
}
