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
package com.codename1.impl.javase.fx;

import com.codename1.impl.javase.IBrowserComponent;
import com.codename1.impl.javase.JavaSEPort;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import com.codename1.util.AsyncResource;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.SnapshotParameters;
import javafx.scene.SnapshotResult;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javafx.scene.web.WebEngine;
//import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

/**
 *
 * Retina support note:  The browser component currently doesn't take advantage
 * of hidpi displays like the Video Component does.  This is because the *actual*
 * WebView needs to be the same visible dimensions the Swing Canvas for events to
 * work properly.  (Even if you intercept the events of the WebView and convert 
 * their coordinates, there are other aspects, such as tooltips, that will work
 * weirdly if the component doesn't take the acutal space on which it is rendered.
 * 
 * @author Chen
 */
public class SEBrowserComponent extends PeerComponent implements IBrowserComponent{
    private static boolean firstTime = true;
    private WebView web;
    private javafx.embed.swing.JFXPanel panel;
    private JFrame frm;
    private JavaSEPort instance;
    private String currentURL;
    private boolean init = false;
    private JPanel cnt;
    private boolean lightweightMode;
    private boolean lightweightModeSet;
    private  JScrollBar hSelector, vSelector;
    private AdjustmentListener adjustmentListener;
    private BrowserComponent browserComp;
    private boolean transparent;
    
    /**
     * A bridge to inject java methods into the webview.
     */
    public static class Bridge {
        final WeakReference<BrowserComponent> weakBrowserComp;
        
        Bridge(BrowserComponent cmp) {
            weakBrowserComp = new WeakReference(cmp);
        }
        
        /**
         * A method injected into the webview to provide direct access to the getBrowserNavigationCallback
         * that is registered.  This is kind of a workaround since there doesn't seem to be any way
         * to prevent the webview from loading a URL that is set via window.location.href and we need
         * the browser navigation callback to be executed for the Javascript bridge to work.  So we inject this
         * here, and the JavascriptContext checks for this hook when trying to send messages.
         * @param url
         * @return 
         */
        public boolean shouldNavigate(String url) {
            BrowserComponent browserComp = weakBrowserComp.get();
            if (browserComp != null) {
                return browserComp.fireBrowserNavigationCallbacks(url);
            }
            return true;
        } 
        
        public void log(String val) {
            System.out.println("[JS Console] "+val);
        }
    }

    private void makePageBackgroundTransparent() {
        try {
            Class WebPage = Class.forName("com.sun.webkit.WebPage", true, SEBrowserComponent.class.getClassLoader());
            Class Accessor = Class.forName("com.sun.javafx.webkit.Accessor", true, SEBrowserComponent.class.getClassLoader());
            Method getPageFor = Accessor.getMethod("getPageFor", new Class[]{WebEngine.class});
            Object webPage = getPageFor.invoke(null, web.getEngine());
            Method setBackgroundColor = WebPage.getMethod("setBackgroundColor", new Class[]{int.class});
            setBackgroundColor.invoke(webPage, 0);
        } catch (Exception ex){}
    }
    
    /**
     * Captures a browser screenshot using the JavaFX WebView snapshot() method.  The benefit
     * of this approach over {@link Component#toImage()} is that this can produce an image
     * with transparency.
     * @return AsyncResource resolving to an image.
     */
    public AsyncResource<Image> captureScreenshot() {
        
        final AsyncResource<Image> out = new AsyncResource<Image>();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                panel.repaint();
                Platform.runLater(new Runnable() {
                    public void run() {
                        SnapshotParameters params = new SnapshotParameters();
                        params.setFill(Color.TRANSPARENT);

                        web.snapshot(new javafx.util.Callback<SnapshotResult, Void>() {
                            @Override
                            public Void call(SnapshotResult p) {
                                WritableImage wi = p.getImage();
                                BufferedImage img = SwingFXUtils.fromFXImage(wi, null);

                                out.complete(Image.createImage(img));
                                return null;
                            }

                        }, params, null);

                    }
                });
            }
        });
        
        return out;
    }
    
    
    
    private static class InternalJPanel extends JPanel {
        private final JavaSEPort instance;
        private final SEBrowserComponent cmp;
        
        InternalJPanel(JavaSEPort instance, SEBrowserComponent cmp) {
            this.instance = instance;
            this.cmp = cmp;
            
        }
        
        /**
         * Paints native peer onto a buffered image
         * This buffered image will later be drawn to cn1 pipeline
         * in drawNativePeer()
         */
        public void paintOnBuffer() {
            if (getWidth() == 0 || getHeight() == 0) {
                return;
            }
            if (EventQueue.isDispatchThread()) {
                synchronized(cmp) {
                    paintOnBufferImpl();

                    // IMPORTANT:  Don't call any cn1 repaint() or paint() from here
                    // even using callSerially().  If you do you risk starting a cycle
                    // of cn1 paint -> awt paint -> cn1 paint -> awt paint -> etc...
                    // See paint(Graphics) below which calls paintOnBuffer() and then
                    // dispatches to cn1 to update itself.

                    // COROLARY: Don't call AWT repaint, etc from inside CN1 paint()
                    // Call this instead to prevent cycles.
                }
            } else if (!Display.getInstance().isEdt()) {
                try {
                    EventQueue.invokeAndWait(new Runnable() {
                        public void run() {
                            paintOnBuffer();
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                
            }
        }
        
        private void paintOnBufferImpl() {
            final BufferedImage buf = getBuffer();
            Graphics2D g2d = buf.createGraphics();
            g2d.scale(JavaSEPort.retinaScale / instance.zoomLevel, JavaSEPort.retinaScale / instance.zoomLevel);
            cmp.panel.paint(g2d);
            g2d.dispose();
            cmp.putClientProperty("__buffer", buf);
        }
        
        // The buffered image that AWT paints to and CN1 paints from
        BufferedImage buf;
        
        @Override
        public void paint(java.awt.Graphics g) {
            paintOnBuffer();
            
            // After painting buffer, we notify CN1 that it should 
            // draw the buffer as there are changes
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    cmp.repaint();
                }
            });
        }

        @Override
        protected void paintChildren(java.awt.Graphics g) {
            
        }

        @Override
        protected void paintBorder(java.awt.Graphics g) {
            
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            
        }

           
        // Gets the buffer that AWT draws to and CN1 reads from.   
        private BufferedImage getBuffer() {
            if (buf == null || buf.getWidth() != getWidth() * JavaSEPort.retinaScale / instance.zoomLevel || buf.getHeight() != getHeight() * JavaSEPort.retinaScale / instance.zoomLevel) {

                buf = new BufferedImage((int)(getWidth() * JavaSEPort.retinaScale / instance.zoomLevel), (int)(getHeight() * JavaSEPort.retinaScale / instance.zoomLevel), BufferedImage.TYPE_INT_ARGB);
            }
            return buf;
        }
        
    }
    /*
    private static EventHandler<WebErrorEvent> createOnErrorHandler() {
        return new EventHandler<WebErrorEvent>() {
            @Override
            public void handle(WebErrorEvent event) {
                Log.p("WebError: " + event.toString());
            }
        };
    }
    */
    
    private void setZoom(double zoom) {
        try {
            Method setZoom = WebView.class.getMethod("setZoom", new Class[]{double.class});
            setZoom.invoke(this, zoom);
            System.out.println("Set zoom to "+zoom);
        } catch (Exception ex) {}
    }
    
    private static void init(SEBrowserComponent self, BrowserComponent p) {
        final WeakReference<SEBrowserComponent> weakSelf = new WeakReference<SEBrowserComponent>(self);
        final WeakReference<BrowserComponent> weakP = new WeakReference<BrowserComponent>(p);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SEBrowserComponent self = weakSelf.get();
                if (self == null) {
                    return;
                }
                self.cnt = new InternalJPanel(self.instance, self);
                
                self.cnt.setOpaque(false); // <--- Important if container is opaque it will cause
                                        // all kinds of flicker due to painting conflicts with CN1 pipeline.
                self.cnt.setLayout(new BorderLayout());
                self.cnt.add(BorderLayout.CENTER, self.panel);
                //cnt.setVisible(false);
            }
        });
        
        
        self.web.getEngine().getLoadWorker().messageProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String t, String t1) {
                SEBrowserComponent self = weakSelf.get();
                BrowserComponent p = weakP.get();
                if (self == null || p == null) {
                    return;
                }
                if (t1.startsWith("Loading http:") || t1.startsWith("Loading file:") || t1.startsWith("Loading https:")) {
                    String url = t1.substring("Loading ".length());
                    if (!url.equals(self.currentURL)) {
                        p.fireWebEvent("onStart", new ActionEvent(url));
                    }
                    self.currentURL = url;
                } else if ("Loading complete".equals(t1)) {
                    
                }
            }
        });
        
        self.web.getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {

            @Override
            public void handle(WebEvent<String> t) {
                BrowserComponent p = weakP.get();
                if (p == null) {
                    return;
                }
                String msg = t.getData();
                if (msg.startsWith("!cn1_message:")) {
                    System.out.println("Receiving message "+msg);
                    p.fireWebEvent("onMessage", new ActionEvent(msg.substring("!cn1_message:".length())));
                }
            }
            
        });
        
        self.web.getEngine().getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
            @Override
            public void changed(ObservableValue<? extends Throwable> ov, Throwable t, Throwable t1) {
                System.out.println("Received exception: "+t1.getMessage());
                if (ov.getValue() != null) {
                    ov.getValue().printStackTrace();
                }
                if (t != ov.getValue() && t != null) {
                    t.printStackTrace();
                }
                if (t1 != ov.getValue() && t1 != t && t1 != null) {
                    t.printStackTrace();
                }
                
            }
        });

        self.web.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
            @Override
            public void changed(ObservableValue ov, State oldState, State newState) {
                SEBrowserComponent self = weakSelf.get();
                BrowserComponent p = weakP.get();
                try {
                    netscape.javascript.JSObject w = (netscape.javascript.JSObject)self.web.getEngine().executeScript("window");
                    if (w == null) {
                        System.err.println("Could not get window");
                    } else {
                        Bridge b = new Bridge(p);
                        self.putClientProperty("SEBrowserComponent.Bridge.jconsole", b);
                        w.setMember("jconsole", b);
                    }
                } catch (Throwable t) {
                    Log.e(t);
                }
                if (self == null || p == null) {
                    return;
                }
                /*
                // DISABLING TRANSPARENCY BECAUSE IT CAUSES A MESS WHEN SCROLLING
                // ORIGINALLY TRIED TO ADD THIS FOR CSS GENERATION, BUT LATER OPTED
                // TO ONLY USE CEF FOR THAT ANYWAYS
                // SINCE THIS FX COMPONENT IS DEPRECATED, WE'LL JUST LET IT DIE
                self.transparent = p.getStyle().getBgTransparency() == 0;
                if (self.transparent) {
                    try {
                        Class WebPage = Class.forName("com.sun.webkit.WebPage", true, SEBrowserComponent.class.getClassLoader());
                        Class Accessor = Class.forName("com.sun.javafx.webkit.Accessor", true, SEBrowserComponent.class.getClassLoader());
                        Method getPageFor = Accessor.getMethod("getPageFor", new Class[]{WebEngine.class});
                        Object webPage = getPageFor.invoke(null, self.web.getEngine());
                        Method setBackgroundColor = WebPage.getMethod("setBackgroundColor", new Class[]{int.class});
                        setBackgroundColor.invoke(webPage, 0);
                    } catch (Exception ex){}
                }
                */
                String url = self.web.getEngine().getLocation();
                if (newState == State.SCHEDULED) {
                    p.fireWebEvent("onStart", new ActionEvent(url));
                } else if (newState == State.RUNNING) {
                    p.fireWebEvent("onLoadResource", new ActionEvent(url));
                    
                } else if (newState == State.SUCCEEDED) {
                    
                    if (!p.isNativeScrollingEnabled()) {
                        self.web.getEngine().executeScript("document.body.style.overflow='hidden'");
                    }
                    
                    // Since I end of injecting firebug nearly every time I have to do some javascript code
                    // let's just add a client property to the BrowserComponent to enable firebug
                    if (Boolean.TRUE.equals(p.getClientProperty("BrowserComponent.firebug"))) {
                        self.web.getEngine().executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");
                    }
                    netscape.javascript.JSObject window = (netscape.javascript.JSObject)self.web.getEngine().executeScript("window");
                    Bridge b = new Bridge(p);
                    self.putClientProperty("SEBrowserComponent.Bridge.cn1application", b);
                    window.setMember("cn1application", b);
                    
                    self.web.getEngine().executeScript("while (window._cn1ready && window._cn1ready.length > 0) {var f = window._cn1ready.shift(); f();}");
                    //System.out.println("cn1application is "+self.web.getEngine().executeScript("window.cn1application && window.cn1application.shouldNavigate"));
                    self.web.getEngine().executeScript("window.addEventListener('unload', function(e){console.log('unloading...');return 'foobar';});");
                    p.fireWebEvent("onLoad", new ActionEvent(url));
                    
                }
                self.currentURL = url;
                self.repaint();
            }
        });
        self.web.getEngine().getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
            @Override
            public void changed(ObservableValue<? extends Throwable> ov, Throwable t, Throwable t1) {
                BrowserComponent p = weakP.get();
                if (p == null) {
                    return;
                }
                t1.printStackTrace();
                if(t1 == null) {
                    if(t == null) {
                        p.fireWebEvent("onError", new ActionEvent("Unknown error", -1));
                    } else {
                        p.fireWebEvent("onError", new ActionEvent(t.getMessage(), -1));
                    }
                } else {
                    p.fireWebEvent("onError", new ActionEvent(t1.getMessage(), -1));
                }
            }
        });

        // Monitor the location property so that we can send the shouldLoadURL event.
        // This allows us to cancel the loading of a URL if we want to handle it ourself.
        self.web.getEngine().locationProperty().addListener(new ChangeListener<String>(){
            @Override
            public void changed(ObservableValue<? extends String> prop, String before, String after) {
                SEBrowserComponent self = weakSelf.get();
                BrowserComponent p = weakP.get();
                if (self == null || p == null) {
                    return;
                }
                if ( !p.fireBrowserNavigationCallbacks(self.web.getEngine().getLocation()) ){
                    self.web.getEngine().getLoadWorker().cancel();
                }
            }
        });
        
        self.adjustmentListener = new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        SEBrowserComponent self = weakSelf.get();
                        if (self == null) {
                            return;
                        }
                        self.onPositionSizeChange(); 
                    }
                });
                
            }
            
        };
        
    }
    
    public SEBrowserComponent() {
        super(null);
    }
    
    public void SEBrowserComponent_init(final JavaSEPort instance, JPanel f, javafx.embed.swing.JFXPanel fx, final WebView web, final BrowserComponent p, final JScrollBar hSelector, JScrollBar vSelector) {
        
        //System.out.println("In SEBrowserComponent_init");
        this.web = web;
        this.instance = instance;
        this.frm = (JFrame)f.getTopLevelAncestor();
        this.transparent = p.getStyle().getBgTransparency() == 0;
        this.panel = fx;
        WebEngine we = web.getEngine();
        
        try {
            File home = new File(System.getProperty("user.home") + File.separator + JavaSEPort.getAppHomeDir());
            File userdata = new File(home, ".fxuserdata");
            if(!userdata.exists()){
                userdata.mkdirs();
            }
            Method mtd = we.getClass().getMethod("setUserDataDirectory",java.io.File.class); 
            mtd.invoke(we, userdata);
        } catch(Throwable t) {
            System.out.println("It looks like you are running on a version of Java older than Java 8. We recommend upgrading");
            t.printStackTrace();
        }
        
        //we.setOnError(createOnErrorHandler());
        
        init(this, p);
        
       
        
        this.hSelector = hSelector;
        this.vSelector = vSelector;
        

        
    }
    
    /**
     * Executes a javascript string and returns the result as a String if
     * appropriate.
     * @param js
     * @return
     */
    @Override
    public String executeAndReturnString(final String js){
        if (isFXThread()) {
            try {
                return ""+web.getEngine().executeScript(js);
            } catch (Throwable jse) {
                System.out.println("Error trying to execute js "+js);
                throw new RuntimeException(jse);
            }
        }
        
        final String[] result = new String[1];
        final boolean[] complete = new boolean[]{false};
        final Throwable[] error = new Throwable[1];
        Platform.runLater(new Runnable() {
            public void run() {
                try {
                    result[0] = ""+web.getEngine().executeScript(js);
                } catch (Throwable jse) {
                    System.out.println("Error trying to execute js "+js);
                    error[0] = jse;
                }
                synchronized(complete){
                    complete[0] = true;
                    complete.notify();
                }
            }
        });

        // We need to wait for the result of the javascript operation
        // but we don't want to block the entire EDT, so we use invokeAndBlock
        while (!complete[0]) {
            Display.getInstance().invokeAndBlock(new Runnable(){
                public void run() {
                    if ( !complete[0] ){
                        synchronized(complete){
                            try {
                                complete.wait(20);
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                }

            });
        }
        if (error[0] != null) {
            throw new RuntimeException(error[0]);
        }
        return result[0];
    }
    
    private void executeImpl(String js) {
        try {
            web.getEngine().executeScript(js);
        } catch (Throwable t) {
            Log.p("Javascript exception occurred while running expression: "+js);
            throw t;
        }
    }
    
    private static boolean isFXThread() {
        return Platform.isFxApplicationThread();
    }
    
    @Override
    public void execute(final String js) {
        if (isFXThread()) {
            executeImpl(js);
        } else {
            Platform.runLater(new Runnable() {
                public void run() {
                    executeImpl(js);
                }
            });
        }
    }

    
    private static final Object DEINIT_LOCK = new Object();
    @Override
    protected void deinitialize() {
        //lightweightMode = true;
        final boolean[] complete = new boolean[1];
        
        synchronized(imageLock) {
            peerImage = new BufferedImage(cnt.getWidth(), cnt.getHeight(), BufferedImage.TYPE_INT_ARGB);
            final Graphics2D imageG = (Graphics2D)peerImage.createGraphics();
            try {
                instance.drawingNativePeer = true;
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        cnt.paint(imageG);
                    }
                });
                
            } catch (Exception ex){
            } finally {
                instance.drawingNativePeer = false;
                imageG.dispose();
            }
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (init) {
                    
                    hSelector.removeAdjustmentListener(adjustmentListener);
                    vSelector.removeAdjustmentListener(adjustmentListener);
                    lastX=0;
                    lastY=0;
                    lastW=0;
                    lastH=0;
                    
                    frm.remove(cnt);
                    init = false;
                    complete[0] = true;
                    frm.repaint();
                    //SEBrowserComponent.this.repaint();
                    
                    
                } else {
                    complete[0] = true;
                }
                synchronized(DEINIT_LOCK) {
                    DEINIT_LOCK.notify();
                }
            }
        });
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                while(!complete[0]) {
                    synchronized(DEINIT_LOCK) {
                        try {
                            DEINIT_LOCK.wait(20);
                        } catch(InterruptedException er) {}
                    }
                }
            }
        });
        //getComponentForm().deregisterAnimated(this);
        super.deinitialize();
    }

    @Override
    protected void initComponent() {
        super.initComponent(); //To change body of generated methods, choose Tools | Templates.
        init();
        //getComponentForm().registerAnimated(this);
    }
    
    
    private static final Object INIT_LOCK = new Object();
    private final Object imageLock = new Object();
    private BufferedImage peerImage;
    private void init() {
        final boolean[] completed = new boolean[1];
        synchronized(imageLock) {
            peerImage = null;
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                if (!init) {
                    
                    init = true;
                    frm.add(cnt, 0);
                    completed[0] = true;
                    synchronized (INIT_LOCK) {
                        INIT_LOCK.notify();
                    }
                    onPositionSizeChange();
                    hSelector.addAdjustmentListener(adjustmentListener);
                    vSelector.addAdjustmentListener(adjustmentListener);
                    frm.repaint();
                    SEBrowserComponent.this.repaint();

                } else {
                    completed[0] = true;
                }

            }
        });
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                while (!completed[0]) {
                    synchronized (INIT_LOCK) {
                        try {
                            INIT_LOCK.wait(20);
                        } catch (InterruptedException er) {
                        }
                    }
                }
            }
        });
    }
    
    protected void setLightweightMode(final boolean l) {

    }

    
    
    protected boolean shouldRenderPeerImage() {
        return false;
    }
    
    @Override
    protected com.codename1.ui.geom.Dimension calcPreferredSize() {
        return new com.codename1.ui.geom.Dimension((int) (web.getWidth() * JavaSEPort.retinaScale / instance.zoomLevel), (int) (web.getHeight() * JavaSEPort.retinaScale / instance.zoomLevel));
    }
   
    int lastX, lastY, lastW, lastH;
    double lastZoom;
    
    @Override
    protected void onPositionSizeChange() {
        
        if(cnt == null) {
            return;
        }
        Form f = getComponentForm();
        if(cnt.getParent() == null && 
                f != null && 
                Display.getInstance().getCurrent() == f){
            //();
            return;
        }
        
        final int x = getAbsoluteX();
        final int y = getAbsoluteY();
        final int w = getWidth();
        final int h = getHeight();

        if (lastZoom == instance.zoomLevel && x==lastX && y==lastY && w==lastW && h==lastH) {
            return;
        }
        final int screenX;
        final int screenY;
        if(instance.getScreenCoordinates() != null) {
            screenX = instance.getScreenCoordinates().x;
            screenY = instance.getScreenCoordinates().y;
        } else {
            screenX = 0;
            screenY = 0;
        }
        
        lastX = x;
        lastY=y;
        lastW=w;
        lastH=h;
        lastZoom = instance.zoomLevel;
        final double zoom = lastZoom;
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    public void run() {
                        setZoom(instance.zoomLevel);
                    }
                });

                
                frm.doLayout();
                cnt.setBounds(
                    (int) ((x + screenX + instance.getCanvasX()) * zoom / JavaSEPort.retinaScale),
                    (int) ((y + screenY + instance.getCanvasY()) * zoom / JavaSEPort.retinaScale),
                    (int) (w * zoom / JavaSEPort.retinaScale),
                    (int) (h * zoom / JavaSEPort.retinaScale)
                );
                cnt.doLayout();
                ((InternalJPanel)cnt).paintOnBuffer();
                    
                
                
            }
        };
        
        if(SwingUtilities.isEventDispatchThread()) {
            r.run();
            return;
        }
        SwingUtilities.invokeLater(r);
    }

    int getScreenCoordinateX() {
        Rectangle r = instance.getScreenCoordinates();
        if(r == null) {
            return 0;
        }
        return r.x;
    }
    
    int getScreenCoordinateY() {
        Rectangle r = instance.getScreenCoordinates();
        if(r == null) {
            return 0;
        }
        return r.y;
    }
    
    @Override
    public void setProperty(String key, Object value) {
        if(key.equalsIgnoreCase("User-Agent")) {
            //web.getEngine().setUserAgent((String)value);
        }
    }

    @Override
    public String getTitle() {
        if (!Platform.isFxApplicationThread()) {
            final String[] out = new String[1];
            final boolean[] complete = new boolean[1];
            Platform.runLater(new Runnable() {
                public void run() {
                    synchronized (complete) {
                        out[0] = getTitle();
                        complete[0] = true;
                        complete.notifyAll();
                    }
                }
            });
            while (!complete[0]) {
                Util.wait(complete, 1000);
            }
            return out[0];
        }
        return web.getEngine().getTitle();
    }

    @Override
    public String getURL() {
        return web.getEngine().getLocation();
    }

    @Override
    public void setURL(String url) {
        web.getEngine().load(url);
    }

    @Override
    public void stop() {
    }

    @Override
    public void reload() {
        web.getEngine().reload();
    }

    public boolean hasBack() {
        return web.getEngine().getHistory().getCurrentIndex() > 0;
    }

    public boolean hasForward() {
        return web.getEngine().getHistory().getCurrentIndex() < web.getEngine().getHistory().getMaxSize() - 1;
    }

    @Override
    public void back() {
        web.getEngine().getHistory().go(-1);
    }

    public void forward() {
        web.getEngine().getHistory().go(1);
    }

    @Override
    public void clearHistory() {
    }

    @Override
    public void setPage(String html, String baseUrl) {
        web.getEngine().loadContent(html);
        repaint();
    }

    @Override
    public void exposeInJavaScript(Object o, String name) {
    }

    @Override
    public void paint(Graphics g) {
        if (!init) {
            synchronized(imageLock) {
                if (peerImage != null) {
                    Object nativeGraphics = Accessor.getNativeGraphics(g);
                    Graphics2D g2 = (Graphics2D)instance.getGraphics(nativeGraphics).create();
                    try {
                        g2.translate(getAbsoluteX(), getAbsoluteY());
                        if (instance.zoomLevel != 1) {
                            g2.scale(1/instance.zoomLevel, 1/instance.zoomLevel);
                        } else if (instance.takingScreenshot && instance.screenshotActualZoomLevel != 1) {
                            g2.scale(1/instance.screenshotActualZoomLevel, 1/instance.screenshotActualZoomLevel);
                        }
                        g2.drawImage(peerImage, 0,0, null);
                    } finally {
                        g2.dispose();
                    }
                    return;
                }
            }
        }
        
        onPositionSizeChange();
        instance.drawNativePeer(Accessor.getNativeGraphics(g), this, cnt);
        // If this paint is a result of an explicit repaint() call within
        // the AWT paint() method, then we don't reciprocate the call back
        // to AWT.  If, however, this paint came naturally from the CN1
        // paint pipeline, then we will propagate it back to AWT.
        // Trying to prevent infinite cycle here.
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                ((InternalJPanel)cnt).paintOnBuffer();
            }
        });
        
        
    }

    @Override
    public void runLater(Runnable r) {
        Platform.runLater(r);
    }

    @Override
    public boolean supportsExecuteAndReturnString() {
        return true;
    }
    
   
    
    
}