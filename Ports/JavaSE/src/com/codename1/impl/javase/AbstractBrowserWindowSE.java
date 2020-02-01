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

import com.codename1.ui.BrowserWindow;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;


/**
 * A base class for JavaSE browser window implementations.
 * @author shannah
 * @since 7.0
 */
public abstract class AbstractBrowserWindowSE {

    private EventDispatcher loadListeners = new EventDispatcher();
    private EventDispatcher closeListeners = new EventDispatcher();
    
    
    public AbstractBrowserWindowSE() {
        
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
    public abstract void show();

    
    /**
     * Sets the window size.
     * @param width
     * @param height 
     */
    public abstract void setSize(final int width, final int height);
    
    /**
     * Sets the window title.
     * @param title 
     */
    public abstract void setTitle(final String title);

    /**
     * Hides the window.
     */
    public abstract void hide();

    
    /**
     * Cleans up window resources.
     */
    public abstract void cleanup() ;

    
    /**
     * Evaluates Javascript
     * @param req 
     */
    public abstract void eval(BrowserWindow.EvalRequest req);
    
    
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
