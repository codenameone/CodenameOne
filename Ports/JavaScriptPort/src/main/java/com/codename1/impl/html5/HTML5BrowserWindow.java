/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.io.Log;
import com.codename1.ui.BrowserWindow;
import com.codename1.ui.CN;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import java.util.Objects;
import java.util.Timer;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.browser.TimerHandler;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;


/**
 * A base class for JavaSE browser window implementations.
 * @author shannah
 * @since 7.0
 */
public class HTML5BrowserWindow {

    private EventDispatcher loadListeners = new EventDispatcher();
    private EventDispatcher closeListeners = new EventDispatcher();
    private Window win;
    private String url;
    private String name;
    private boolean closed;
    private int intervalHandle;
    public HTML5BrowserWindow(String url, String title) {
        this.url=url;
        name=title;
    }
    
    
    /**
     * Adds listener to be notified on page load.
     * @param l 
     */
    public void addLoadListener(ActionListener l) {
        loadListeners.addListener(l);
    }
    
    /**
     * Removes page load listener.
     * @param l 
     */
    public void removeLoadListener(ActionListener l) {
        loadListeners.removeListener(l);
    }
    
    
    
    /**
     * Shows the window
     */
    public void show() {
        
        win = Window.current().open("", "");
        win.addEventListener("close", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                if (closed) {
                    return;
                }
                closed = true;
                HTML5Implementation.getInstance().callSerially(new Runnable() {
                    public void run() {
                        fireCloseEvent(new ActionEvent(this));
                    }
                });
            }
        });
        win.addEventListener("load", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                
                String newUrl;
                try {
                    newUrl = win.getLocation().getFullURL();
                } catch (Throwable t) {
                    
                    newUrl = url;
                }
                final String fNewUrl = newUrl;
                url = newUrl;
                HTML5Implementation.getInstance().callSerially(new Runnable() {
                    public void run() {
                        
                        fireLoadEvent(new ActionEvent(fNewUrl));
                    }
                });
                
            }
              
        });
        
        win.setName(name);
        win.getLocation().setFullURL(url);
        intervalHandle = Window.setInterval(new TimerHandler() {
            @Override
            public void onTimer() {
                if (win == null || isClosed(win)) {
                    Window.clearInterval(intervalHandle);
                    intervalHandle = 0;
                    
                    if (closed) {
                        return;
                    }
                    closed = true;
                    HTML5Implementation.callSerially(new Runnable() {
                        public void run() {
                             fireCloseEvent(new ActionEvent(null));
                        }

                    });
                    
                    return;
                }
                 try {
                    String newUrl = win.getLocation().getFullURL();
                    if (!Objects.equals(newUrl, url)) {
                        url = newUrl;
                        final String fNewUrl = newUrl;
                        HTML5Implementation.callSerially(new Runnable() {
                            public void run() {
                                 fireLoadEvent(new ActionEvent(fNewUrl));
                            }
                           
                        });
                        
                        
                    }
                } catch (Throwable t) {}
            }
        }, 200);
        
    }

    @JSBody(params={"win"}, script="return win.closed")
    private native static boolean isClosed(Window win);
    
    /**
     * Sets the window size.
     * @param width
     * @param height 
     */
    public void setSize(final int width, final int height) {
        if (win == null) {
            return;
        }
        win.resizeTo(width, width);
    }
    
    /**
     * Sets the window title.
     * @param title 
     */
    public void setTitle(final String title) {
        this.name = title;
        if (win != null) {
            win.setName(title);
        }
    }

    /**
     * Hides the window.
     */
    public void hide() {
        if (win == null) {
            return;
        }
        win.close();
    }

    
    /**
     * Cleans up window resources.
     */
    public void cleanup() {
        hide();
    }

    
    /**
     * Evaluates Javascript
     * @param req 
     */
    public void eval(BrowserWindow.EvalRequest req) {
        throw new RuntimeException("eval() not supported yet");
    }
    
    
    /**
     * Adds listener to be notified when window is closed.
     * @param l 
     */
    public void addCloseListener(ActionListener l) {
        closeListeners.addListener(l);
    }
    
    /**
     * Removes window close listener.
     * @param l 
     */
    public void removeCloseListener(ActionListener l) {
        closeListeners.removeListener(l);
    }
    
    /**
     * Deliver event on close.
     * @param evt 
     */
    protected void fireCloseEvent(ActionEvent evt) {
        closeListeners.fireActionEvent(evt);
    }
    
    /**
     * Deliver event on load. The source of the event should be a string URL
     * of the page that was loaded.
     * @param evt 
     */
    protected void fireLoadEvent(ActionEvent evt) {
        loadListeners.fireActionEvent(evt);
    }
}
