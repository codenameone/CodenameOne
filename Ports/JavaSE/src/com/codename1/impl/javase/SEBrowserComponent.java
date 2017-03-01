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
package com.codename1.impl.javase;

import com.codename1.io.Log;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuDragMouseEvent;

/**
 *
 * @author Chen
 */
public class SEBrowserComponent extends PeerComponent {
    private static boolean firstTime = true;
    private WebView web;
    private javafx.embed.swing.JFXPanel panel;
    private JPanel frm;
    private JavaSEPort instance;
    private String currentURL;
    private boolean init = false;
    private JPanel cnt;
    private boolean lightweightMode;
    private boolean lightweightModeSet;
    private final JScrollBar hSelector, vSelector;
    private AdjustmentListener adjustmentListener;
    private BrowserComponent browserComp;
    
    
    /**
     * A bridge to inject java methods into the webview.
     */
    public class Bridge {
        
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
            if (browserComp.getBrowserNavigationCallback() != null) {
                return browserComp.getBrowserNavigationCallback().shouldNavigate(url);
            }
            return true;
        }        
    }
    
    public SEBrowserComponent(JavaSEPort instance, JPanel f, javafx.embed.swing.JFXPanel fx, final WebView web, final BrowserComponent p, final JScrollBar hSelector, JScrollBar vSelector) {
        super(null);
        this.web = web;
        this.instance = instance;
        this.frm = f;
        this.panel = fx;
        final JavaSEPort inst = instance;
        browserComp = p;
        WebEngine we = web.getEngine();
        try {
            Method mtd = we.getClass().getMethod("setUserDataDirectory",java.io.File.class); 
            mtd.invoke(we, new File(JavaSEPort.getAppHomeDir()));
        } catch(Throwable t) {
            System.out.println("It looks like you are running on a version of Java older than Java 8. We recommend upgrading");
            t.printStackTrace();
        }
        
        we.setOnError(new EventHandler<WebErrorEvent>() {
            @Override
            public void handle(WebErrorEvent event) {
                Log.p("WebError: " + event.toString());
            }
        });
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                cnt = new JPanel() {
                    public void paint(java.awt.Graphics g) {
                        // We want the native component to be hidden unless
                        // it is being drawn by Codename One via drawNativePeer()
                        // This allows the component to be present (respond to events)
                        // but gives us the flexibility to draw the component
                        // at the correct depth with the rest of the Codename One
                        // components.
                        if (SEBrowserComponent.this.instance.drawingNativePeer) {
                            super.paint(g);
                        } else {
                            
                        }
                    }
                };
                
                cnt.setOpaque(false); // <--- Important if container is opaque it will cause
                                        // all kinds of flicker due to painting conflicts with CN1 pipeline.
                cnt.setLayout(new BorderLayout());
                cnt.add(BorderLayout.CENTER, panel);
                //cnt.setVisible(false);
            }
        });

        web.getEngine().getLoadWorker().messageProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String t, String t1) {
                if (t1.startsWith("Loading http:") || t1.startsWith("Loading file:") || t1.startsWith("Loading https:")) {
                    String url = t1.substring("Loading ".length());
                    if (!url.equals(currentURL)) {
                        p.fireWebEvent("onStart", new ActionEvent(url));
                    }
                    currentURL = url;
                } else if ("Loading complete".equals(t1)) {
                    
                }
            }
        });
        
        web.getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {

            @Override
            public void handle(WebEvent<String> t) {
                String msg = t.getData();
                if (msg.startsWith("!cn1_message:")) {
                    System.out.println("Receiving message "+msg);
                    p.fireWebEvent("onMessage", new ActionEvent(msg.substring("!cn1_message:".length())));
                }
            }
            
        });
        
        web.getEngine().getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
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

        web.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
            @Override
            public void changed(ObservableValue ov, State oldState, State newState) {
                String url = web.getEngine().getLocation();
                if (newState == State.SCHEDULED) {
                    p.fireWebEvent("onStart", new ActionEvent(url));
                } else if (newState == State.RUNNING) {
                    p.fireWebEvent("onLoadResource", new ActionEvent(url));
                    
                } else if (newState == State.SUCCEEDED) {
                    if (!p.isNativeScrollingEnabled()) {
                        web.getEngine().executeScript("document.body.style.overflow='hidden'");
                    }
                    
                    // Since I end of injecting firebug nearly every time I have to do some javascript code
                    // let's just add a client property to the BrowserComponent to enable firebug
                    if (Boolean.TRUE.equals(p.getClientProperty("BrowserComponent.firebug"))) {
                        web.getEngine().executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");
                    }
                    netscape.javascript.JSObject window = (netscape.javascript.JSObject)web.getEngine().executeScript("window");
                    window.setMember("cn1application", new Bridge());
                    //web.getEngine().executeScript("window.addEventListener('unload', function(e){console.log('unloading...');return 'foobar';});");
                    p.fireWebEvent("onLoad", new ActionEvent(url));
                    
                }
                currentURL = url;
                repaint();
            }
        });
        web.getEngine().getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
            @Override
            public void changed(ObservableValue<? extends Throwable> ov, Throwable t, Throwable t1) {
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
        web.getEngine().locationProperty().addListener(new ChangeListener<String>(){
            @Override
            public void changed(ObservableValue<? extends String> prop, String before, String after) {
                if ( !p.getBrowserNavigationCallback().shouldNavigate(web.getEngine().getLocation()) ){
                    web.getEngine().getLoadWorker().cancel();
                }
            }
        });
        
        adjustmentListener = new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        onPositionSizeChange(); 
                    }
                });
                
            }
            
        };
        
        this.hSelector = hSelector;
        this.vSelector = vSelector;

        
    }
    
    /**
     * Executes a javascript string and returns the result as a String if
     * appropriate.
     * @param js
     * @return
     */
    public String executeAndReturnString(final String js){
        final String[] result = new String[1];
        final boolean[] complete = new boolean[]{false};
        Platform.runLater(new Runnable() {
            public void run() {
                result[0] = ""+web.getEngine().executeScript(js);
                synchronized(complete){
                    complete[0] = true;
                    complete.notify();
                }
            }
        });

        // We need to wait for the result of the javascript operation
        // but we don't want to block the entire EDT, so we use invokeAndBlock
        Display.getInstance().invokeAndBlock(new Runnable(){
            public void run() {
                while ( !complete[0] ){
                    synchronized(complete){
                        try {
                            complete.wait(200);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }

        });
        return result[0];
    }
     
    public void execute(final String js) {
        Platform.runLater(new Runnable() {
            public void run() {
                web.getEngine().executeScript(js);
            }
        });
    }

    
    private static final Object DEINIT_LOCK = new Object();
    @Override
    protected void deinitialize() {
        //lightweightMode = true;
        final boolean[] complete = new boolean[1];
        
        synchronized(imageLock) {
            peerImage = new BufferedImage(cnt.getWidth(), cnt.getHeight(), BufferedImage.TYPE_INT_ARGB);
            System.out.println("PI width: "+peerImage.getWidth()+ "PI height "+peerImage.getHeight());
            System.out.println("Creating peer image");
            Graphics2D imageG = (Graphics2D)peerImage.createGraphics();
            try {
                instance.drawingNativePeer = true;
                cnt.paint(imageG);
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
        super.deinitialize();
    }

    @Override
    protected void initComponent() {
        super.initComponent(); //To change body of generated methods, choose Tools | Templates.
        init();
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
        return new com.codename1.ui.geom.Dimension((int) web.getWidth(), (int) web.getHeight());
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
        lastX = x;
        lastY=y;
        lastW=w;
        lastH=h;
        lastZoom = instance.zoomLevel;
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    public void run() {
                        try {
                            Method setZoom = web.getClass().getMethod("setZoom", new Class[]{double.class});
                            setZoom.invoke(web, instance.zoomLevel);
                        } catch (NoSuchMethodException ex) {
                            Logger.getLogger(SEBrowserComponent.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (SecurityException ex) {
                            Logger.getLogger(SEBrowserComponent.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalAccessException ex) {
                            Logger.getLogger(SEBrowserComponent.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(SEBrowserComponent.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InvocationTargetException ex) {
                            Logger.getLogger(SEBrowserComponent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                
                cnt.setBounds((int) ((x + getScreenCoordinateX() + instance.canvas.x) * instance.zoomLevel),
                        (int) ((y + getScreenCoordinateY() + instance.canvas.y) * instance.zoomLevel),
                        (int) (w * instance.zoomLevel),
                        (int) (h * instance.zoomLevel));
                cnt.validate();
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
    
    void setProperty(String key, Object value) {
    }

    String getTitle() {
        return web.getEngine().getTitle();
    }

    String getURL() {
        return web.getEngine().getLocation();
    }

    void setURL(String url) {
        web.getEngine().load(url);
    }

    void stop() {
    }

    void reload() {
        web.getEngine().reload();
    }

    boolean hasBack() {
        return web.getEngine().getHistory().getCurrentIndex() > 0;
    }

    boolean hasForward() {
        return web.getEngine().getHistory().getCurrentIndex() < web.getEngine().getHistory().getMaxSize() - 1;
    }

    void back() {
        web.getEngine().getHistory().go(-1);
    }

    void forward() {
        web.getEngine().getHistory().go(1);
    }

    void clearHistory() {
    }

    void setPage(String html, String baseUrl) {
        web.getEngine().loadContent(html);
        repaint();
    }

    void exposeInJavaScript(Object o, String name) {
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
                        g2.drawImage(peerImage, 0,0, null);
                    } finally {
                        g2.dispose();
                    }
                    return;
                }
            }
        }
        instance.drawNativePeer(Accessor.getNativeGraphics(g), this, cnt);
        onPositionSizeChange();
        
    }
   
}