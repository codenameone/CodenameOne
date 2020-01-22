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
package com.codename1.ui;

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.AsyncResource;

/**
 * Encapsulates a WebView that is contained in its own separate window when run on a Desktop (e.g. Simulator).  Platforms
 * that don't have "windows", will fall back to loading a Form with a BrowserComponent in it.
 * @author shannah
 * @since 7.0
 */
public class BrowserWindow {
    
    /**
     * Implementation for BrowserWindow provided by the platform implementation.  The default
     * implementation in the JavaSE port is a JavaFX Stage with a WebView in it.
     */
    private Object nativeWindow;
    
    /**
     * Fallback form to contain a webview if the platform implementation doesn't 
     * implement its own BrowserWindow.
     */
    private BrowserForm form;
    
    /**
     * Fallback webview to use when platform doesn't implement its own BrowserWindow.  This 
     * would be contained inside {@link #form}.
     */
    private BrowserComponent webview;
    
    /**
     * A fallback form implementation that contains a browser for platforms that don't
     * implement their own browser window.
     */
    private class BrowserForm extends Form {
        
        /**
         * The form to go "back" to when browser window is closed.
         */
        private Form backForm;
        
        /**
         * Flag that is set when closed() called.
         */
        private boolean closed;
        
        /**
         * Listeners to be notified when the browser is closed.
         */
        private final EventDispatcher closeListeners = new EventDispatcher();
        
        BrowserForm() {
            setLayout(new BorderLayout());
            webview = new BrowserComponent();
            add(BorderLayout.CENTER, webview);
            backForm = Display.getInstance().getCurrent();
            Toolbar tb = new Toolbar();
            setToolbar(tb);
            tb.addMaterialCommandToLeftBar("", FontImage.MATERIAL_ARROW_BACK_IOS, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (closed) {
                        return;
                    }
                    closed = true;
                    backForm.showBack();
                    closeListeners.fireActionEvent(evt);
                }
                
            });
            setBackCommand(new Command("") {
                public void actionPerformed(ActionEvent e) {
                    if (closed) {
                        return;
                    }
                    closed = true;
                    backForm.showBack();
                    closeListeners.fireActionEvent(e);
                }
            });
            
            
            
        }
        
        /**
         * Closes the browser window - navigating back to the previous form.
         */
        void close() {
            if (closed) {
                return;
            }
            closed = true;
            backForm.showBack();
            closeListeners.fireActionEvent(new ActionEvent(this));
            
        }
    }
    
    /**
     * Creates a new browser window with the given starting URL.
     * @param startURL The URL to start with.  
     */
    public BrowserWindow(String startURL) {
        nativeWindow = createNativeWindow(startURL);
        if (nativeWindow == null) {
            form = new BrowserForm();
            webview.setURL(startURL);
            
        }
    }
    
    /**
     * Creates a platform native Browser window with the given start URL.
     * @param startURL The URL to open in the browser window.
     * @return The native browser window.
     */
    private Object createNativeWindow(String startURL) {
        return Display.impl.createNativeBrowserWindow(startURL);
    }
    
    /**
     * Adds listeners to be notified when a page is loaded in the browser window.  ActionEvents
     * will have the URL of the page as a string as its "source" property.
     * @param l Listener to add.
     */
    public void addLoadListener(ActionListener l) {
        if (nativeWindow != null) {
            Display.impl.addNativeBrowserWindowOnLoadListener(nativeWindow, l);
        } else {
            webview.addWebEventListener("onLoad", l);
        }
    }
    
    /**
     * Removes listeners from being notified when page is loaded in the browser window.
     * @param l 
     */
    public void removeLoadListener(ActionListener l) {
        if (nativeWindow != null) {
            Display.impl.removeNativeBrowserWindowOnLoadListener(nativeWindow, l);
        } else {
            webview.removeWebEventListener("onLoad", l);
        }
    }
    
    /**
     * Sets the window title for the browser window.
     * @param title The title for the window.
     */
    public void setTitle(String title) {
        if (nativeWindow != null) {
            Display.impl.nativeBrowserWindowSetTitle(nativeWindow, title);
        } else {
            form.setTitle(title);
        }
    }
    
    /**
     * Sets the size in pixels of the browser window.
     * @param width The width in pixels
     * @param height The height in pixels
     */
    public void setSize(int width, int height) {
        if (nativeWindow != null) {
            Display.impl.nativeBrowserWindowSetSize(nativeWindow, width, height);
        }
    } 
    
    
    /**
     * Closes the browser window.
     */
    public void close() {
        if (nativeWindow != null) {
            Display.impl.nativeBrowserWindowHide(nativeWindow);
            Display.impl.nativeBrowserWindowCleanup(nativeWindow);
        } else {
            form.close();
        }
    }
    
    /**
     * Adds listener to be notified when the browser window is closed.
     * @param l 
     */
    public void addCloseListener(ActionListener l) {
        if (nativeWindow != null) {
            Display.impl.nativeBrowserWindowAddCloseListener(nativeWindow, l);
        } else {
            form.closeListeners.addListener(l);
        }
    }
    
    /**
     * Removes listener from being notified when the browser window is closed.
     * @param l 
     */
    public void removeCloseListener(ActionListener l) {
        if (nativeWindow != null) {
            Display.impl.nativeBrowserWindowRemoveCloseListener(nativeWindow, l);
        } else {
            form.closeListeners.removeListener(l);
        }
    }
    
    /**
     * Shows the browser window.
     */
    public void show() {
        if (nativeWindow != null) {
            Display.impl.nativeBrowserWindowShow(nativeWindow);
        } else {
            form.show();
        }
    }
    
    /**
     * A future that is returned from the eval() method.
     * @since 7.0
     */
    public static class EvalRequest extends AsyncResource<String> {
        private String js;
        
        /**
         * Sets the JS code to be executed
         * @param js 
         */
        public void setJS(String js) {
            this.js = js;
        }
        
        /**
         * Gets the JS code to be executed.
         * @return 
         */
        public String getJS() {
            return js;
        }
                
    
    }
}
