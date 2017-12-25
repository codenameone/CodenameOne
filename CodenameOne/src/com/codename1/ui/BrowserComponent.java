/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.events.BrowserNavigationCallback;
import com.codename1.ui.plaf.Style;
import java.io.IOException;
import java.util.Hashtable;

/**
 * <p>The browser component is an interface to an embeddable native platform browser on platforms
 * that support embedding the native browser in place, if you need wide compatibility and flexibility
 * you should check out the HTMLComponent which provides a lightweight 100% cross platform
 * web component.<br>
 * This component will only work on platforms that support embedding a native browser which
 * exclude earlier versions of Blackberry devices and J2ME devices.<br>
 * Its recommended that you place this component in a fixed position (none scrollable) on the screen without other
 * focusable components to prevent confusion between focus authority and allow the component to scroll
 * itself rather than CodenameOne making that decision for it.</p>
 *
 *  <p>On Android this component might show a native progress indicator dialog. You can disable that functionality
 * using the {@Display.getInstance().setProperty("WebLoadingHidden", "true");} call.</p>
 * 
 * <p>
 * The following code shows the basic usage of the {@code BrowserComponent}:
 * </p>
 * <script src="https://gist.github.com/codenameone/20b6a17463152f90ebbb.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-browsercomponent.png" alt="Simple usage of BrowserComponent" />
 * 
 * 
 * @author Shai Almog
 */
public class BrowserComponent extends Container {
    private Hashtable listeners;
    private PeerComponent internal;
    private boolean pinchToZoom = true;
    private boolean nativeScrolling = true;
    
    /**
     * String constant for web event listener {@link #addWebEventListener(java.lang.String, com.codename1.ui.events.ActionListener)}
     */
    public static final String onStart = "onStart";

    /**
     * String constant for web event listener {@link #addWebEventListener(java.lang.String, com.codename1.ui.events.ActionListener)}
     */
    public static final String onLoad = "onLoad";

    /**
     * String constant for web event listener {@link #addWebEventListener(java.lang.String, com.codename1.ui.events.ActionListener)}
     */
    public static final String onError = "onError";
    
    private BrowserNavigationCallback browserNavigationCallback = new BrowserNavigationCallback(){
        public boolean shouldNavigate(String url) {
            return true;
        }
    };

    /**
     * Set the browser navigation callback which allows handling a case where 
     * a URL invocation can be delegated to Java code. This allows binding 
     * Java side functionality to JavaScript functionality in the same
     * way PhoneGap/Cordova work
     * @param callback the callback interface
     */
    public void setBrowserNavigationCallback(BrowserNavigationCallback callback){
        this.browserNavigationCallback = callback;
    }

    /**
     * The browser navigation callback interface allows handling a case where 
     * a URL invocation can be delegated to Java code. This allows binding 
     * Java side functionality to JavaScript functionality in the same
     * way PhoneGap/Cordova work
     * 
     * @return the callback interface
     */
    public BrowserNavigationCallback getBrowserNavigationCallback(){
        return this.browserNavigationCallback;
    }


    /**
     * This constructor will work as expected when a browser component is supported, see isNativeBrowserSupported()
     */
    public BrowserComponent() {
        setUIID("BrowserComponent");
        PeerComponent c = Display.impl.createBrowserComponent(this);
        setLayout(new BorderLayout());
        addComponent(BorderLayout.CENTER, c);
        internal = c;
        Style s = internal.getUnselectedStyle();
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
        s.setBgTransparency(255);
    }

    /**
     * Returns true if the platform supports embedding a native browser component
     *
     * @return true if native browsing is supported
     */
    public static boolean isNativeBrowserSupported() {
        return Display.impl.isNativeBrowserComponentSupported();
    }

    /**
     * This method allows customizing the properties of a web view in various ways including platform specific settings.
     * When a property isn't supported by a specific platform it is just ignored.
     *
     * @param key see the documentation with the CodenameOne Implementation for further details
     * @param value see the documentation with the CodenameOne Implementation for further details
     */
    public void setProperty(String key, Object value) {
        Display.impl.setBrowserProperty(internal, key, value);
    }

    /**
     * The page title
     * @return the title
     */
    public String getTitle() {
        return Display.impl.getBrowserTitle(internal);
    }

    /**
     * The page URL
     * @return the URL
     */
    public String getURL() {
        return Display.impl.getBrowserURL(internal);
    }

    /**
     * Sets the page URL, jar: URL's must be supported by the implementation
     * @param url  the URL
     */
    public void setURL(String url) {
        Display.impl.setBrowserURL(internal, url);
    }


    /**
     * Sets the page URL while respecting the hierarchy of the html
     * @param url  the URL
     */
    public void setURLHierarchy(String url) throws IOException {
        Display.impl.setBrowserPageInHierarchy(internal, url);
    }

    /**
     * Reload the current page
     */
    public void reload() {
        Display.impl.browserReload(internal);
    }

    /**
     * Indicates whether back is currently available
     * @return true if back should work
     */
    public boolean hasBack() {
        return Display.impl.browserHasBack(internal);
    }

    /**
     * Indicates whether forward is currently available
     * @return true if forward should work
     */
    public boolean hasForward() {
        return Display.impl.browserHasForward(internal);
    }

    /**
     * Navigates back in the history
     */
    public void back() {
        Display.impl.browserBack(internal);
    }

    /**
     * Navigates forward in the history
     */
    public void forward() {
        Display.impl.browserForward(internal);
    }

    /**
     * Clears navigation history
     */
    public void clearHistory() {
        Display.impl.browserClearHistory(internal);
    }

    /**
     * Some platforms require that you enable pinch to zoom explicitly. This method has no
     * effect if pinch to zoom isn't supported by the platform
     * 
     * @param e true to enable pinch to zoom, false to disable it
     */
    public void setPinchToZoomEnabled(boolean e) {
        pinchToZoom = e;
        Display.impl.setPinchToZoomEnabled(internal, e);
    }

    /**
     * This method is unreliable and is only here for consistency with setPinchToZoomEnabled,
     * it will not return whether the platform supports pinch since this is very hard to detect
     * properly.
     * @return the last value for setPinchToZoomEnabled
     */
    public boolean isPinchToZoomEnabled() {
        return pinchToZoom;
    }
    
    /**
     * This flag allows disabling the native browser scrolling on platforms that support it
     * @param b true to enable native scrolling, notice that non-native scrolling might be problematic
     */
    public void setNativeScrollingEnabled(boolean b) {
        nativeScrolling = b;
        Display.impl.setNativeBrowserScrollingEnabled(internal, b);
    }
    
    /**
     * This method is unreliable and is only here for consistency with setNativeScrollingEnabled.
     * 
     * @return the last value for setNativeScrollingEnabled
     */
    public boolean isNativeScrollingEnabled() {
        return nativeScrolling;
    }
    
    /**
     * Shows the given HTML in the native viewer
     *
     * @param html HTML web page
     * @param baseUrl base URL to associate with the HTML
     */
    public void setPage(String html, String baseUrl) {
        Display.impl.setBrowserPage(internal, html, baseUrl);
    }

    private EventDispatcher getEventDispatcher(String type, boolean autoCreate) {
        if(listeners == null) {
            if(!autoCreate) {
                return null;
            }
            listeners = new Hashtable();
            EventDispatcher ev = new EventDispatcher();
            listeners.put(type, ev);
            return ev;
        }
        EventDispatcher ev = (EventDispatcher)listeners.get(type);
        if(ev == null) {
            if(autoCreate) {
                ev = new EventDispatcher();
                listeners.put(type, ev);
            }
        }
        return ev;
    }
    
    /**
     * Adds a listener to the given event type name, event type names are platform specific but some 
     * must be fired for all platforms and will invoke the action listener when the appropriate event loads
     * 
     * @param type platform specific but must support: onStart, onLoad, onError
     * @param listener callback for the event
     */
    public void addWebEventListener(String type, ActionListener listener) {
        getEventDispatcher(type, true).addListener(listener);
    }

    /**
     * Removes the listener, see addWebEventListener for details
     *
     * @param type see addWebEventListener for details
     * @param listener see addWebEventListener for details
     */
    public void removeWebEventListener(String type, ActionListener listener) {
        EventDispatcher e = getEventDispatcher(type, false);
        if(e != null) {
            e.removeListener(listener);
            if(!e.hasListeners()) {
                listeners.remove(type);
            }
        }
    }
    
    /**
     * Cancel the loading of the current page
     */
    public void stop() {
        Display.impl.browserStop(internal);
    }

    /**
     * Release native resources of this Browser Component
     */ 
    public void destroy() {
        Display.impl.browserDestroy(internal);        
    }
    
    /**
     * Used internally by the implementation to fire an event from the native browser widget
     * 
     * @param type the type of the event
     * @param ev the event
     */
    public void fireWebEvent(String type, ActionEvent ev) {
        EventDispatcher e = getEventDispatcher(type, false);
        if(e != null) {
            e.fireActionEvent(ev);
        }
    }

    /**
     * Executes the given JavaScript string within the current context
     * 
     * @param javaScript the JavaScript string
     */
    public void execute(String javaScript) {
        Display.impl.browserExecute(internal, javaScript);
    }

    /**
     * Executes the given JavaScript and returns a result string from the underlying platform
     * where applicable
     * @param javaScript the JavaScript code to execute
     * @return the string returned from the Javascript call
     */
    public String executeAndReturnString(String javaScript){
        return Display.impl.browserExecuteAndReturnString(internal, javaScript);
    }

    /**
     * Allows exposing the given object to JavaScript code so the JavaScript code can invoke methods
     * and access fields on the given object. Notice that on RIM devices which don't support reflection
     * this object must implement the propriatery Scriptable interface
     * http://www.blackberry.com/developers/docs/5.0.0api/net/rim/device/api/script/Scriptable.html
     *
     * @param o the object to invoke, notice all public fields and methods would be exposed to JavaScript
     * @param name the name to expose within JavaScript
     * @deprecated this doesn't work in most platforms see issue 459 for details, use the setBrowserNavigationCallback
     * method instead
     */
    public void exposeInJavaScript(Object o, String name) {
        Display.impl.browserExposeInJavaScript(internal, o, name);
    }

    /**
     * Toggles debug mode for the browser component which helps detect coding errors in the JavaScript
     * bridge logic
     * @param mode true to debug false otherwise, this might have no effect in some platforms
     */
    public void setDebugMode(boolean mode) {
        if(mode) {
            putClientProperty("BrowserComponent.firebug", Boolean.TRUE);
        } else {
            putClientProperty("BrowserComponent.firebug", null);
        }
    }
    
    /**
     * Indicates if debug mode is set (might have no effect though)
     * @return true if debug mode was activated
     */
    public boolean isDebugMode() {
        return getClientProperty("BrowserComponent.firebug") == Boolean.TRUE;
    }
}
