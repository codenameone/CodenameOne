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

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.events.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebView;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
    private JPanel cnt = new JPanel();
    private boolean lightweightMode;
    private boolean lightweightModeSet;
    public SEBrowserComponent(JavaSEPort instance, JPanel f, javafx.embed.swing.JFXPanel fx, final WebView web, final BrowserComponent p) {
        super(null);
        this.web = web;
        this.instance = instance;
        this.frm = f;
        this.panel = fx;

        cnt.setLayout(new BorderLayout());
        cnt.add(BorderLayout.CENTER, panel);
        cnt.setVisible(false);

        web.getEngine().getLoadWorker().messageProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String t, String t1) {
                if (t1.startsWith("Loading http:")) {
                    String url = t1.substring("Loading ".length());
                    if (!url.equals(currentURL)) {
                        p.fireWebEvent("onStart", new ActionEvent(url));
                    }
                    currentURL = url;
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
                    p.fireWebEvent("onLoad", new ActionEvent(url));
                }
                currentURL = url;
                repaint();
            }
        });
        web.getEngine().getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
            @Override
            public void changed(ObservableValue<? extends Throwable> ov, Throwable t, Throwable t1) {
                p.fireWebEvent("onError", new ActionEvent(t1.getMessage(), -1));
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
        
    @Override
    protected void deinitialize() {
        super.deinitialize();
        lightweightMode = true;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //panel.removeAll();
                //cnt.remove(panel);
                frm.remove(cnt);
                frm.repaint();
                init = false;
            }
        });
    }

    protected void setLightweightMode(final boolean l) {
        if(lightweightModeSet && lightweightMode == l) {
            return;
        }
        lightweightModeSet = true;
        lightweightMode = l;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                if (!l) {
                    if (!init) {
                        init = true;
                        cnt.setVisible(true);
                        frm.add(cnt, 0);
                        onPositionSizeChange();
                        frm.repaint();
                    } else {
                        cnt.setVisible(false);
                    }
                } else {
                    if (init) {
                        cnt.setVisible(false);
                    }
                }
            }
        });

    }

    protected com.codename1.ui.Image generatePeerImage() {
        final com.codename1.ui.Image[] img = new com.codename1.ui.Image[] {null};
        Platform.runLater(new Runnable() {
            public void run() {
                WritableImage w = new WritableImage(getWidth(), getHeight());
                web.snapshot(null, w);
                BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                SwingFXUtils.fromFXImage(w, bi);
                img[0] = com.codename1.ui.Image.createImage(bi);
                setPeerImage(img[0]);
                synchronized(img) {
                    img.notify();
                }
            }
        });
        if(firstTime) {
            // special case for first time
            firstTime = false;
            return com.codename1.ui.Image.createImage(5, 5);
        }
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                synchronized(img) {
                    while(img[0] == null) {
                        try {
                            img.wait(20);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(SEBrowserComponent.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        });
        return img[0];
    }
    
    protected boolean shouldRenderPeerImage() {
        return lightweightMode || !isInitialized();
    }
    
    @Override
    protected com.codename1.ui.geom.Dimension calcPreferredSize() {
        return new com.codename1.ui.geom.Dimension((int) web.getWidth(), (int) web.getHeight());
    }

    @Override
    protected void onPositionSizeChange() {
        if(SwingUtilities.isEventDispatchThread()) {
            final int x = getAbsoluteX();
            final int y = getAbsoluteY();
            final int w = getWidth();
            final int h = getHeight();

            cnt.setBounds((int) ((x + instance.getScreenCoordinates().getX() + instance.canvas.x) * instance.zoomLevel),
                    (int) ((y + instance.getScreenCoordinates().y + instance.canvas.y) * instance.zoomLevel),
                    (int) (w * instance.zoomLevel),
                    (int) (h * instance.zoomLevel));
            cnt.validate();
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final int x = getAbsoluteX();
                final int y = getAbsoluteY();
                final int w = getWidth();
                final int h = getHeight();

                cnt.setBounds((int) ((x + instance.getScreenCoordinates().getX() + instance.canvas.x) * instance.zoomLevel),
                        (int) ((y + instance.getScreenCoordinates().y + instance.canvas.y) * instance.zoomLevel),
                        (int) (w * instance.zoomLevel),
                        (int) (h * instance.zoomLevel));
                cnt.validate();
            }
        });
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
}
