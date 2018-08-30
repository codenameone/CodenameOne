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

import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.processing.Result;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.events.BrowserNavigationCallback;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.UITimer;
import com.codename1.util.Callback;
import com.codename1.util.CallbackAdapter;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import com.codename1.util.regex.StringReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.Vector;

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
    private boolean ready = false;
    private boolean fireCallbacksOnEdt=true;
    
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
     * Sets whether javascript callbacks should be run on the EDT.  Default is {@literal true}.
     * @param edt True if callbacks should be run on EDT.  False if they should be run on the platform's main thread.
     * @since 5.0
     */
    public void setFireCallbacksOnEdt(boolean edt) {
        this.fireCallbacksOnEdt = edt;
    }
    
    /**
     * Checks if javascript callbacks are run on the EDT.
     * @return True if javascript callbacks are run on the EDT.
     * @since 5.0
     * @see #setFireCallbacksOnEdt(boolean) 
     */
    public boolean isFireCallbacksOnEdt() {
        return fireCallbacksOnEdt;
    }
    
    /**
     * Set the browser navigation callback which allows handling a case where 
     * a URL invocation can be delegated to Java code. This allows binding 
     * Java side functionality to JavaScript functionality in the same
     * way PhoneGap/Cordova work
     * @param callback the callback interface
     * @deprecated Use {@link #addBrowserNavigationCallback(com.codename1.ui.events.BrowserNavigationCallback) Instead
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
     * @deprecated Call {@link #fireBrowserNavigationCallbacks(java.lang.String) } to determine whether navigation should occur for a particulr URL.
     */
    public BrowserNavigationCallback getBrowserNavigationCallback(){
        return this.browserNavigationCallback;
    }

    /**
     * List of registered browser navigation callbacks.
     */
    private Vector<BrowserNavigationCallback> browserNavigationCallbacks;
    
    private Vector<BrowserNavigationCallback> browserNavigationCallbacks() {
        if (browserNavigationCallbacks == null) {
            browserNavigationCallbacks = new Vector<BrowserNavigationCallback>();
        }
        return browserNavigationCallbacks;
    }
    
    /**
     * Adds a navigation callback.
     * @param callback The callback to call before navigating to a URL.
     */
    public void addBrowserNavigationCallback(BrowserNavigationCallback callback) {
        browserNavigationCallbacks().add(callback);
    }
    
    /**
     * Removes a navigation callback.
     * @param callback The callback to call before navigating to a URL.
     */
    public void removeBrowserNavigationCallback(BrowserNavigationCallback callback) {
        if (browserNavigationCallbacks != null) {
            browserNavigationCallbacks().remove(callback);
        }
    }
    private static final String RETURN_URL_PREFIX = "/!cn1return/";
    private Hashtable<Integer, SuccessCallback<JSRef>> returnValueCallbacks;
    private Hashtable<Integer, SuccessCallback<JSRef>> returnValueCallbacks() {
        if (returnValueCallbacks == null) {
            returnValueCallbacks = new Hashtable<Integer,SuccessCallback<JSRef>>();
        }
        return returnValueCallbacks;
    }
    
    private int nextReturnValueCallbackId = 0;
    private int addReturnValueCallback(SuccessCallback<JSRef> callback) {
        int id = nextReturnValueCallbackId++;
        while (returnValueCallbacks().containsKey(id)) {
            id++;
        }
        returnValueCallbacks().put(id, callback);
        nextReturnValueCallbackId = id+1;
        if (nextReturnValueCallbackId > 10000) {
            nextReturnValueCallbackId=0;
        }
        return id;
    }
    private SuccessCallback<JSRef> popReturnValueCallback(int id) {
        if (returnValueCallbacks != null) {
            return returnValueCallbacks.remove(id);
        }
        return null;
    }
    private JSONParser returnValueParser;
    private JSONParser returnValueParser() {
        if (returnValueParser == null) {
            returnValueParser = new JSONParser();
        }
        return returnValueParser;
    }
    
    /**
     * Decodes a URL
     * @param s The string to decode.
     * @param enc The encoding.  E.g. UTF-8
     * @return The decoded URL.
     */
    private static String decodeURL(String s, String enc){

        boolean needToChange = false;
        int numChars = s.length();
        StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;

        char c;
        byte[] bytes = null;
        while (i < numChars) {
            c = s.charAt(i);
            switch (c) {
            case '+':
                sb.append(' ');
                i++;
                needToChange = true;
                break;
            case '%':
                /*
                 * Starting with this instance of %, process all
                 * consecutive substrings of the form %xy. Each
                 * substring %xy will yield a byte. Convert all
                 * consecutive  bytes obtained this way to whatever
                 * character(s) they represent in the provided
                 * encoding.
                 */

                try {

                    // (numChars-i)/3 is an upper bound for the number
                    // of remaining bytes
                    if (bytes == null)
                        bytes = new byte[(numChars-i)/3];
                    int pos = 0;

                    while ( ((i+2) < numChars) &&
                            (c=='%')) {
                        int v = Integer.parseInt(s.substring(i+1,i+3),16);
                        if (v < 0)
                            throw new IllegalArgumentException("URLDecoder: Illegal hex characters in escape (%) pattern - negative value");
                        bytes[pos++] = (byte) v;
                        i+= 3;
                        if (i < numChars)
                            c = s.charAt(i);
                    }

                    // A trailing, incomplete byte encoding such as
                    // "%x" will cause an exception to be thrown

                    if ((i < numChars) && (c=='%'))
                        throw new IllegalArgumentException(
                         "URLDecoder: Incomplete trailing escape (%) pattern");
                    try {
                        sb.append(new String(bytes, 0, pos, enc));
                    } catch (Throwable t) {
                        throw new RuntimeException(t.getMessage());
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                    "URLDecoder: Illegal hex characters in escape (%) pattern - "
                    + e.getMessage());
                }
                needToChange = true;
                break;
            default:
                sb.append(c);
                i++;
                break;
            }
        }

        return (needToChange? sb.toString() : s);
    }
    
    /**
     * Fires all of the registered browser navigation callbacks against the provided URL.
     * @param url The URL to fire the navigation callbacks against.
     * @return True if all of the callbacks say that they can browse.  False otherwise.
     */
    public boolean fireBrowserNavigationCallbacks(String url) {
        boolean shouldNavigate = true;
        if (browserNavigationCallback != null && !browserNavigationCallback.shouldNavigate(url)) {
            shouldNavigate = false;
        }
        if (browserNavigationCallbacks != null) {
            for (BrowserNavigationCallback cb : browserNavigationCallbacks) {
                if (!cb.shouldNavigate(url)) {
                    shouldNavigate = false;
                }
            }
        }
        if ( !url.startsWith("javascript:") && url.indexOf(RETURN_URL_PREFIX) != -1 ){
            //System.out.println("Received browser navigation callback "+url);
            String result = decodeURL(url.substring(url.indexOf(RETURN_URL_PREFIX) + RETURN_URL_PREFIX.length()), "UTF-8");
            //System.out.println("After decode "+result);
            Result structResult = Result.fromContent(result, Result.JSON);
            int callbackId = structResult.getAsInteger("callbackId");
            final String value = structResult.getAsString("value");
            final String type = structResult.getAsString("type");
            final String errorMessage = structResult.getAsString("errorMessage");
            final SuccessCallback<JSRef> callback = popReturnValueCallback(callbackId);
            if (jsCallbacks != null && jsCallbacks.contains(callback)) {
                // If this is a registered callback, then we treat it more like
                // an event listener, and we retain it for future callbacks.
                returnValueCallbacks.put(callbackId, callback);
            }
            if (callback != null) {
                if (errorMessage != null) {
                    if (fireCallbacksOnEdt) {
                        Display.getInstance().callSerially(new Runnable() {

                            public void run() {
                                if (callback instanceof Callback) {
                                    ((Callback)callback).onError(this, new RuntimeException(errorMessage), 0, errorMessage);

                                }
                            }

                        });
                    } else {
                        if (callback instanceof Callback) {
                            ((Callback)callback).onError(this, new RuntimeException(errorMessage), 0, errorMessage);

                        }
                    }
                    
                } else {
                    if (fireCallbacksOnEdt) {
                        Display.getInstance().callSerially(new Runnable() {

                            public void run() {
                                callback.onSucess(new JSRef(value, type));
                            }

                        });
                    } else {
                         callback.onSucess(new JSRef(value, type));
                    }
                    
                }
            } else {
                Log.e(new RuntimeException("Received return value from javascript, but no callback could be found for that ID"));
            }
            shouldNavigate = false;
        }
        return shouldNavigate;
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
    
    private final Object readyLock = new Object();
    
    /**
     * Uses invokeAndBlock to wait until the BrowserComponent is ready.  The browser component
     * is considered to be ready once the onLoad event has been fired for the first page.
     */
    public void waitForReady() {
        while (!ready) {
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    synchronized(readyLock) {
                        Util.wait(readyLock, 1000);
                    }
                }
            });
        }
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
     * Sets the page URL, jar: URL's must be supported by the implementation. Notice this API isn't supported
     * in all platforms see {@link #isURLWithCustomHeadersSupported() }
     * @param url  the URL
     * @param headers headers to push into the request for the url
     */
    public void setURL(String url, Map<String, String> headers) {
        Display.impl.setBrowserURL(internal, url, headers);
    }

    /**
     * Returns true if the method {@link #setURL(java.lang.String, java.util.Map) } is supported
     * @return false by default 
     */
    public boolean isURLWithCustomHeadersSupported() {
        return Display.impl.isURLWithCustomHeadersSupported();
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
        if (onLoad.equals(type)) {
            synchronized(readyLock) {
                ready = true;
                readyLock.notifyAll();
            }
        }
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
     * Executes given javascript string within current context.
     * @param js The javascript to execute.
     * @param params Parameters to inject into the javascript expression.  The expression should contain placeholders of the form {@literal ${0} }, {@literal ${1} }, etc... to be replaced.  See {@link #injectParameters(java.lang.String, java.lang.Object...) } for more information about injected parameters.
     * by parameters.
     */
    public void execute(String js, Object[] params) {
        execute(injectParameters(js, params));
    }
    

    /**
     * Executes the given JavaScript and returns a result string from the underlying platform
     * where applicable.
     * <p><strong>Note</strong>: Some platforms use {@link Display#invokeAndBlock(java.lang.Runnable) } inside this method which is very costly. Try to avoid this synchronous method, and 
     * prefer to use one of the asynchronous versions.  E.g. {@link #execute(java.lang.String, com.codename1.util.SuccessCallback) }</p>
     * @param javaScript the JavaScript code to execute
     * @return the string returned from the Javascript call
     */
    public String executeAndReturnString(String javaScript){
        return Display.impl.browserExecuteAndReturnString(internal, javaScript);
    }
    
    /**
     * Executes the given javascript and returns the result string from the underlying platform.
     * <p><strong>Note</strong>: Some platforms use {@link Display#invokeAndBlock(java.lang.Runnable) } inside this method which is very costly. Try to avoid this synchronous method, and 
     * prefer to use one of the asynchronous versions.  E.g. {@link #execute(java.lang.String, com.codename1.util.SuccessCallback) }</p>
     * @param javaScript The javascript to execute.
     * @param params Parameters to inject into the javascript expression.  The expression should contain placeholders of the form {@literal ${0} }, {@literal ${1} }, etc... to be replaced.  See {@link #injectParameters(java.lang.String, java.lang.Object...) } for more information about injected parameters.
     * @return The result as a string.
     * @since 5.0
     */
    public String executeAndReturnString(String javaScript, Object[] params) {
        return executeAndReturnString(injectParameters(javaScript, params));
    }
    
    /**
     * Creates a proxy for a Javascript object that makes it easier to call methods, retrieve,
     * and manipulate properties on the object.
     */
    public JSProxy createJSProxy(String javascriptExpression) {
        return new JSProxy(javascriptExpression);
    }
    
    /**
     * A thin wrapper around a Javascript variable that makes it easier to 
     * call methods on that variable.
     */
    public class JSProxy {
        
        /** 
        The javascript variable name.  This can be any javascript expression that resolves
        * to an object.
        */
        private final String self;
        
        /**
         * Creats a new proxy.
         * @param self The javascript expression that should resolve to the object that this
         * will proxy.  E.g. "window", or "document.getElementById('mybutton')".  The expression
         * is just stored as a string and is 'resolved' when calls are made on the proxy.
         */
        private JSProxy(String self) {
            this.self = self;
        }
        
        /**
         * Calls a method on this javascript object.
         * @param timeout The timeout in ms
         * @param method The method name.
         * @param args Arguments to pass to the method.
         * @param callback Callback with the result of the method.
         */
        public void call(int timeout, String method, Object[] args, SuccessCallback<JSRef> callback) {
            StringBuilder js = new StringBuilder();
            js.append("callback.onSuccess("+self+"."+method+"(");
            int len = args.length;
            for (int i=0; i<len; i++) {
                if (i > 0) {
                    js.append(", ");
                }
                js.append("${"+i+"}");
            }
            js.append("))");
            execute(js.toString(), args, callback);
        }
        
        /**
         * Calls a method on this javascript object.
         * @param method The method name.
         * @param args Arguments to pass to the method.
         * @param callback Callback with the result of the method.
         */
        public void call(String method, Object[] args, SuccessCallback<JSRef> callback) {
            call(0, method, args, callback);
        }
        
        /**
         * Calls method on this javascript object and waits for the result using
         * invokeAndBlock.
         * @param timeout The timeout in ms
         * @param method The method name.
         * @param args Arguments for the method.
         * @return JSRef with the result of the method call.
         */
        public JSRef callAndWait(int timeout, String method, Object[] args) {
            StringBuilder js = new StringBuilder();
            js.append("callback.onSuccess("+self+"."+method+"(");
            int len = args.length;
            for (int i=0; i<len; i++) {
                if (i > 0) {
                    js.append(", ");
                }
                js.append("${"+i+"}");
            }
            js.append("))");
            return executeAndWait(timeout, js.toString(), args);
        }
        
        /**
         * Calls method on this javascript object and waits for the result using
         * invokeAndBlock.
         * @param method The method name.
         * @param args Arguments for the method.
         * @return JSRef with the result of the method call.
         */
        public JSRef callAndWait(String method, Object[] args) {
            return callAndWait(0, method, args);
        }
        
        /**
         * Gets a property of this javascript object.
         * @param timeout Timeout in ms
         * @param property The property name.
         * @param callback Callback with the property value.
         */
        public void get(int timeout, String property, SuccessCallback<JSRef> callback) {
            StringBuilder js = new StringBuilder();
            js.append("callback.onSuccess("+self+"."+property+")");
            
            execute(timeout, js.toString(), callback);
        }
        
        /**
         * Gets a property of this javascript object.
         * @param property The property name.
         * @param callback Callback with the property value.
         */
        public void get(String property, SuccessCallback<JSRef> callback) {
            get(0, property, callback);
        }
        
        /**
         * Gets multiple properties as a batch.
         * @param timeout Timeout in ms
         * @param properties List of property names to retrieve.
         * @param callback 
         */
        public void get(int timeout, Collection<String> properties, final SuccessCallback<Map<String,JSRef>> callback) {
            StringBuilder sb = new StringBuilder();
            sb.append("(function(){var outmap={};var prop=null; var propval=null;");
            for (String prop : properties) {
                sb.append("prop='").append(prop).append("';");
                sb.append("propval=").append(self).append("[prop]");
                sb.append("outmap[prop] = {value:propval, type:typeof(propval)};");
            }
            sb.append("callback.onSuccess(JSON.stringify(outmap))})()");
            execute(timeout, sb.toString(), new SuccessCallback<JSRef>() {

                public void onSucess(JSRef value) {
                    JSONParser p = new JSONParser();
                    try {
                        Map m = p.parseJSON(new StringReader(value.getValue()));
                        Map<String,JSRef> out = new HashMap<String,JSRef>();
                        for (String prop : out.keySet()) {
                            Map propVal = (Map)m.get(prop);
                            out.put(prop, new JSRef((String)propVal.get("value"), (String)propVal.get("type")));
                        }
                        callback.onSucess(out);
                    } catch (Exception ex) {
                        Log.e(ex);
                        if (callback instanceof Callback) {
                            ((Callback)callback).onError(BrowserComponent.this, ex, 0, ex.getMessage());
                        } else {
                            callback.onSucess(null);
                        }
                    }
                }
                
            });
            
        }
        
        /**
         * Gets a property of this javascript object and waits for the result
         * using invokeAndBlock.
         * @param property The property to retrieve.
         * @return The property value.
         */
        public void get(Collection<String> properties, final SuccessCallback<Map<String,JSRef>> callback) {
            get(0, properties, callback);
        }
        
        /**
         * Gets a property of this javascript object and waits for the result
         * using invokeAndBlock.
         * @param timeout The timeout in ms
         * @param property The property to retrieve.
         * @return The property value.
         */
        public JSRef getAndWait(int timeout, String property) {
            StringBuilder js = new StringBuilder();
            js.append("callback.onSuccess("+self+"."+property+")");
            
            return executeAndWait(timeout, js.toString());
        }
        
        /**
         * Gets a property of this javascript object and waits for the result
         * using invokeAndBlock.
         * @param property The property to retrieve.
         * @return The property value.
         */
        public JSRef getAndWait(String property) {
            return getAndWait(0, property);
        }
        
        /**
         * Gets multiple properties on this object in a batch.
         * @param timeout The timeout in ms
         * @param properties The property names to get.
         * @return 
         */
        public Map<String,JSRef> getAndWait(int timeout, Collection<String> properties) {
            StringBuilder sb = new StringBuilder();
            sb.append("(function(){var outmap={};var prop=null; var propval=null;");
            for (String prop : properties) {
                sb.append("prop='").append(prop).append("';");
                sb.append("propval=").append(self).append("[prop]");
                sb.append("outmap[prop] = {value:propval, type:typeof(propval)};");
            }
            sb.append("callback.onSuccess(JSON.stringify(outmap))})()");
            JSRef value = executeAndWait(timeout, sb.toString());
            JSONParser p = new JSONParser();
            try {
                Map m = p.parseJSON(new StringReader(value.getValue()));
                Map<String,JSRef> out = new HashMap<String,JSRef>();
                for (String prop : out.keySet()) {
                    Map propVal = (Map)m.get(prop);
                    out.put(prop, new JSRef((String)propVal.get("value"), (String)propVal.get("type")));
                }
                return out;
            } catch (Exception ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException)ex;
                } else {
                    Log.e(ex);
                    throw new RuntimeException(ex.getMessage());
                }
            }
        
        }
        
        /**
         * Gets multiple properties on this object in a batch.
         * @param properties
         * @return 
         */
        public Map<String,JSRef> getAndWait(Collection<String> properties) {
            return getAndWait(0, properties);
        }
        
        
        
        /**
         * Sets a property.
         * @param timeout The timeout in ms
         * @param property The property name.
         * @param value The property value.
         */
        public void set(int timeout, String property, Object value) {
            set(timeout, property, value, (SuccessCallback)null);
        }
        
        /**
         * Sets a property.
         * @param property The property name.
         * @param value The property value.
         */
        public void set(String property, Object value) {
            set(0, property, value);
        }
        
        /**
         * Sets a property on this javascript object.
         * @param timeout The timeout in ms.
         * @param property The property name to set.
         * @param value The property value.
         * @param callback Callback which is called when complete
         */
        public void set(int timeout, String property, Object value, SuccessCallback<JSRef> callback) {
            StringBuilder js = new StringBuilder();
            js.append(self+"['"+property+"']=${0}; callback.onSuccess(undefined);");
            execute(timeout, js.toString(), new Object[]{value}, callback);
        }
        
        /**
         * Sets a property on this javascript object.
         * @param property The property name to set.
         * @param value The property value.
         * @param callback Callback which is called when complete
         */
        public void set(String property, Object value, SuccessCallback<JSRef> callback) {
            set(0, property, value, callback);
        }
        
        /**
         * Sets multiple properties in a single batch.
         * @param timeout The timeout in ms
         * @param properties The properties to set.
         * @param callback Callback called when operation is completed.
         */
        public void set(int timeout, Map<String, Object> properties, SuccessCallback<JSRef> callback) {
            StringBuilder js = new StringBuilder();
            Object[] params = new Object[properties.size()];
            int i=0;
            for (String key : properties.keySet()) {
                js.append("self['").append(key).append("']=${").append(i).append("};");
                params[i] = properties.get(key);
            }
            js.append("callback.onSuccess(undefined)");
            execute(timeout, js.toString(), callback);
        }
        
        /**
         * Sets multiple properties in a single batch.
         * @param properties The properties to set.
         * @param callback Callback called when operation is completed.
         */
        public void set(Map<String,Object> properties, SuccessCallback<JSRef> callback) {
            set(0, properties, callback);
        }
        
        /**
         * Sets multiple properties in a single batch.
         * @param timeout Timeout in ms
         * @param properties The properties to set on this object.
         */
        public void setAndWait(int timeout, Map<String, Object> properties) {
            StringBuilder js = new StringBuilder();
            Object[] params = new Object[properties.size()];
            int i=0;
            for (String key : properties.keySet()) {
                js.append("self['").append(key).append("']=${").append(i).append("};");
                params[i] = properties.get(key);
            }
            js.append("callback.onSuccess(undefined)");
            executeAndWait(timeout, js.toString());
        }
        
        /**
         * Sets multiple properties in a single batch.
         * @param properties The properties to set on this object.
         */
        public void setAndWait(Map<String,Object> properties) {
            setAndWait(0, properties);
        }
        
        /**
         * Sets a property on this javascript object and waits for it to complete using invokeAndBlock.
         * @param timeout The timeout in ms.
         * @param property The property name to set. 
         * @param value The value to set.
         */
        public void setAndWait(int timeout, String property, Object value) {
            StringBuilder js = new StringBuilder();
            js.append(self+"."+property+"=${0}; callback.onSuccess(undefined);");
            
            executeAndWait(timeout, js.toString(), new Object[]{value});
        }
        
        /**
         * Sets a property on this javascript object and waits for it to complete using invokeAndBlock.
         * @param property The property name to set. 
         * @param value The value to set.
         */
        public void setAndWait(String property, Object value) {
            setAndWait(0, property, value);
        }
        
        
        
        
    }
    
    /**
     * A wrapper class for a Javascript value that is returned via the {@link #execute(java.lang.String, com.codename1.util.Callback) }
     * method.  This supports all Javascript primitive types.  See {@link JSType} for a list of the types.
     */
    public static class JSRef {
        
        /**
         * The string value of the javascript variable.
         */
        private final String value;
        
        /**
         * The string type of the javascript variable.  This is the result returned by the javascript typeof operator.
         * See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/typeof
         */
        private final String type;
        
        /**
         * Creates a new JSRef object.
         * @param value The string value of the javascript variable.
         * @param type The string type of the variable as returned by the typeof operator.
         */
        private JSRef(String value, String type) {
            this.value = value;
            this.type = type;
            
        }
        
        /**
         * Gets the javascript value as a string.
         * @return The string value of the reference.
         */
        public String getValue() {
            return value;
        }
        
        /**
         * Returns the type of the value
         * @return 
         */
        private String getType() {
            return type;
        }
        
        /**
         * Returns the type of the value.
         * @return 
         */
        public JSType getJSType() {
            return JSType.get(type);
        }
        
        /**
         * Gets the value as an integer.
         * @return 
         */
        public int getInt() {
            return new Double(Double.parseDouble(value)).intValue();
        }
        
        /**
         * Gets teh value as a double.
         * @return 
         */
        public double getDouble() {
            return Double.parseDouble(value);
        }
        
        /**
         * Gets the value as a boolean.
         * @return 
         */
        public boolean getBoolean() {
            return Boolean.parseBoolean(value);
        }

        @Override
        public String toString() {
            return value;
        }
        
        /**
         * Checks if the variable is null
         * @return 
         */
        public boolean isNull() {
            return value == null;
        }
        
        
    }
    
    /**
     * Enum with the possible types for a {@link JSRef} object.
     */
    public static enum JSType {
        OBJECT("object"),
        FUNCTION("function"),
        NUMBER("number"),
        STRING("string"),
        UNDEFINED("undefined"),
        BOOLEAN("boolean");
        
        private String typeOfValue;
        JSType(String val) {
            typeOfValue = val;
        }
        
        /**
         * Gets the corresponding JSType for the given string type.
         * @param type The string type as returned by the typeof operator.  Possible input values are 'object', 'function', 'number', 'boolean', and 'undefined'
         * @return 
         */
        public static JSType get(String type) {
            if ("object".equals(type)) {
                return OBJECT;
            }
            if ("string".equals(type)) {
                return JSType.STRING;
            }
            if ("number".equals(type)) {
                return JSType.NUMBER;
            }
            if ("function".equals(type)) {
                return JSType.FUNCTION;
            }
            if ("undefined".equals(type)) {
                return JSType.UNDEFINED;
            }
            if ("boolean".equals(type)) {
                return BOOLEAN;
            }
            return UNDEFINED;
        }
    }
    
    
    
    /**
     * Asynchronously executes the provided javascript expression. The expression may provide a callback
     * which you can call inside the expression directly.
     * 
     * <h3>Example</h3>
     * 
     * <strong>Getting the {@literal window } object.</strong>
     * <pre>{@code 
     * bc.execute("callback.onSuccess(window)", value -> {
     *    System.out.println("value="+value+"; type="+value.getJSType());
     *       // value=[object Window]; type=OBJECT
     * });
     * }
     * </pre>
     * <p><strong>Getting an Integer</strong></p>
     * <pre>{@code 
     * bc.execute("callback.onSuccess(1+2)", value -> {
     *      System.out.println("value="+value.getInt()+"; type="+value.getJSType());
     *          // value=3; type=NUMBER
     * });
     * }
     * </pre>
     * <p><strong>Getting a String</strong></p>
     * <pre>{@code 
     * bc.execute("callback.onSuccess('hello world')",value -> {
     *          System.out.println("value="+value+"; type="+value.getJSType());
     *          // value=hello world; type=STRING
     *      }
     * );
     * }
     * </pre>
     * <p><strong>After a Javascript Timeout</strong></p>
     * <p>Since this call is asynchronous, the javascript code can wait to call the 
     * callback to any time in the future - e.g. after a timeout, after an ajax response,
     * in some event handler, etc..  The CN1 UI will not be blocked, the provided callback
     * will be called at the appropriate time on the EDT.</p>
     * <pre>{@code 
     * bc.execute("setTimeout(function(){callback.onSuccess('hello world')}, 1500)", 
     *      value -> {
     *          System.out.println("value="+value+"; type="+value.getJSType());
     *          // value=hello world; type=STRING
     *      }
     * );
     * }
     * </pre>
     * 
     * <strong>NOTE: The callback can only be called once, so you shouldn't use this method to register
     * a callback with an event listener that will be called repeatedly.  If you want to register a Java
     * callback with a Javascript event, you should use the {@link #addJSCallback(java.lang.String, com.codename1.util.Callback) } method
     * instead.</strong>
     * @param js The javascript expression.  If you want to receive any result from this expression, the expression itself must include a call to callback.onSuccess(value).
     * @param callback The callback.  You should call this directly from Javascript.  You can call either <code>callback.onSuccess(value)</code> or <code>callback.onError(message,code)</code>.
     */
    public void execute(String js, SuccessCallback<JSRef> callback) {
        StringBuilder fullJs = new StringBuilder();
        String isSimulator = Display.getInstance().isSimulator() ? "true":"false";
        if (callback == null) {
            callback = new CallbackAdapter<JSRef>();
        }
        int callbackId = addReturnValueCallback(callback);
        fullJs
                .append("(function(){")
                //.append("cn1application.log('we are here');")
                .append("var BASE_URL='https://www.codenameone.com").append(RETURN_URL_PREFIX).append("';")
                .append("function doCallback(val) { ")
                //.append("cn1application.log('in doCallback');")
                .append("  var url = BASE_URL + encodeURIComponent(JSON.stringify(val));")
                .append("  if (window.cn1application && window.cn1application.shouldNavigate) { window.cn1application.shouldNavigate(url) } else if ("+isSimulator+") {window._cn1ready = window._cn1ready || []; window._cn1ready.push(function(){window.cn1application.shouldNavigate(url)});} else {window.location.href=url}")
                .append("} ")
                .append("var result = {value:null, type:null, errorMessage:null, errorCode:0, callbackId:").append(callbackId).append("};")
                .append("var callback = {")
                .append("  onSucess: function(val) { this.onSuccess(val);}, ")
                .append("  onSuccess: function(val) { result.value = val; result.type = typeof(val); if (val !== null && typeof val === 'object') {result.value = val.toString();} doCallback(result);}, ")
                .append("  onError: function(message, code) { result.errorMessage = message; result.errorCode = code; doCallback(result);}")
                .append("};")
                
                .append("try { ").append(js).append("} catch (e) {try {callback.onError(e.message, 0);} catch (e2) {callback.onError('Unknown error', 0);}}")
                
                .append("})();");
        execute(fullJs.toString());
        
    }
    
    /**
     * Execute javascript with a timeout.  If timeout is reached before callback is run,
     * then the callback's onError method is run (if callback is a Callback).  If callback isn't a Callback
     * (i.e. has no onError(), then this will log an error, and call the onSucess method with a null arg.
     * @param js The javascript to execute
     * @param timeout The timeout in milliseconds.
     * @param callback The callback
     */
    public void execute(int timeout, final String js, final SuccessCallback<JSRef> callback) {
        if (callback != null && timeout > 0) {
            UITimer.timer(timeout, false, new Runnable() {

                public void run() {
                    if (returnValueCallbacks().contains(callback)) {
                        Object key = null;
                        for (Map.Entry e : returnValueCallbacks.entrySet()) {
                            if (callback.equals(e.getValue())) {
                                key = e.getKey();
                                break;
                            }
                        }
                        if (key != null) {
                            returnValueCallbacks.remove(key);
                            if (callback instanceof Callback) {
                                ((Callback)callback).onError(BrowserComponent.this, new RuntimeException("Javascript execution timeout"), 1, "Javascript execution timeout");
                            } else {
                                Log.e(new RuntimeException("Javascript execution timeout while running "+js));
                                callback.onSucess(null);
                            }
                        }
                    }
                }

            });
        }
        execute(js, callback);
        
    }
    
    
    
    /**
     * Executes Javascript expression.
     * @param timeout The timeout in ms
     * @param js The javascript expression to execute.
     * @param params Parameters to inject into the javascript expression.  The expression should contain placeholders of the form {@literal ${0} }, {@literal ${1} }, etc... to be replaced.  See {@link #injectParameters(java.lang.String, java.lang.Object...) } for more information about injected parameters.
     * by parameters.
     * @param callback Callback to call when complete.
     */
    public void execute(int timeout, String js, Object[] params, SuccessCallback<JSRef> callback) {
        execute(0, js, params, callback);
    }
    
    /**
     * Executes Javascript expression.
     * @param js The javascript expression to execute.
     * @param params Parameters to inject into the javascript expression.  The expression should contain placeholders of the form {@literal ${0} }, {@literal ${1} }, etc... to be replaced.  See {@link #injectParameters(java.lang.String, java.lang.Object...) } for more information about injected parameters.
     * by parameters.
     * @param callback Callback to call when complete.
     */
    public void execute(String js, Object[] params, SuccessCallback<JSRef> callback) {
        execute(injectParameters(js, params), callback);
    }
    
    /**
     * Sets of callbacks that are registered to persist for multiple calls.
     */
    private Set jsCallbacks;
    private Set jsCallbacks() {
        if (jsCallbacks == null) {
            jsCallbacks = new HashSet();
        }
        return jsCallbacks;
    }
    
    
    /**
     * Registers a Java method as a callback in javascript.  The {@literal callback} argument
     * can be referenced inside the javascript expression so that it can be fired when certain events occur.
     * 
     * <h3>Examples</h3>
     * 
     * <p><strong>Register a Callback to be called whenever a button is clicked</strong></p>
     * 
     * <pre>
     * {@code 
     * bc.addJSCallback("someButton.addEventListener('click', function(){callback.onSuccess('hello world')})", new Callback<JSRef>() {
     *     public void onSucess(JSRef value) {
     *         System.out.println("Received click: "+value);
     *     }
     * });
     * }
     * </pre>
     * @param installJs
     * @param callback 
     */
    public void addJSCallback(String installJs, SuccessCallback<JSRef> callback) {
        jsCallbacks().add(callback);
        execute(installJs, callback);
    }
    
    /**
     * Registers Java method as a callback in Javascript.  The {@literal callback} argument
     * can be referenced inside the javascript expression so that it can be fired when certain events occur.
     * @param installJs The javascript expression. to run.  
     * @param params Parameters to inject into the javascript expression.  The expression should contain placeholders of the form {@literal ${0} }, {@literal ${1} }, etc... to be replaced.  See {@link #injectParameters(java.lang.String, java.lang.Object...) } for more information about injected parameters.
     * by parameters.
     * @param callback The callback to call on completion.
     */
    public void addJSCallback(String installJs, Object[] params, SuccessCallback<JSRef> callback) {
        addJSCallback(injectParameters(installJs, params), callback);
    }
    
    /**
     * Removes a JS callback that was added via the {@link #addJSCallback(java.lang.String, com.codename1.util.SuccessCallback) } method.
     * <p>Note: This won't unregister any callbacks from the Javascript environment.  You'll need to perform your
     * own additional cleanup in Javascript if this callback is registered in any event handlers.
     * @param callback The callback to remove.
     */
    public void removeJSCallback(Callback<JSRef> callback) {
        if (jsCallbacks != null) {
            jsCallbacks.remove(callback);
        }
    }
    
    private class ExecuteResult {
        JSRef value;
        Throwable error;
        boolean complete;
    }
    
    
    private static boolean isNumber(Object o) {
        if (o == null) {
            return false;
        }
        Class c = o.getClass();
        return c == Integer.class || c == Double.class || c == Float.class || c == Long.class || c == Short.class;
    }
    
    /**
     * A wrapper class for a literal javascript expression that can be passed as an 
     * arg in {@link #execute(java.lang.String, java.lang.Object[]) }.
     */
    public static class JSExpression {
        
        private String expression;
        
        /**
         * Creates a literal javascript expression.
         * @param expression The javascript expression.
         */ 
        public JSExpression(String expression) {
            this.expression = expression;
        }
        
        /**
         * Gets the javascript expression as a string.
         * @return The javascript literal expression.
         */
        public String toString() {
            return expression;
        } 
    }
    
    /**
     * Injects parameters into a Javascript string expression.  This will quote strings properly.  The 
     *  expression should include placeholders for each parameter of the form ${0}, ${1}, etc..
     * @param jsExpression The javascript expression with placeholders to inject parameters.
     * @param params
     * @return The expression with placeholders replaced by parameters.
     */
    public static String injectParameters(String jsExpression, Object... params) {
        int i=0;
        for (Object param : params) {
            
            String pattern = "${"+i+"}";
            if (param == null) {
                jsExpression = StringUtil.replaceAll(jsExpression, pattern, "null");
            } else if (param instanceof String) {
                jsExpression = StringUtil.replaceAll(jsExpression, pattern, quote((String)param));
            } else if (param instanceof JSProxy) {
                jsExpression = StringUtil.replaceAll(jsExpression, pattern, ((JSProxy)param).self);
            } else if (param instanceof JSExpression) {
                jsExpression = ((JSExpression)param).expression;
            } else if (param instanceof JSRef) {
                JSRef jsr = (JSRef)param;
                if (jsr.isNull()) {
                    jsExpression = StringUtil.replaceAll(jsExpression, pattern, "null");
                } else if (jsr.getJSType() == JSType.STRING) {
                    jsExpression = StringUtil.replaceAll(jsExpression, pattern, quote((String)jsr.getValue()));
                } else if (jsr.getJSType() == JSType.FUNCTION || jsr.getJSType() == JSType.OBJECT) {
                    throw new IllegalArgumentException("Cannot inject JSRefs of functions or objects as parameters in JS expressions");
                } else {
                    jsExpression = StringUtil.replaceAll(jsExpression, pattern, jsr.getValue());
                }
            } else {
                jsExpression = StringUtil.replaceAll(jsExpression, pattern, String.valueOf(param));
            }
            i++;
        }
        return jsExpression;
    }
    
    /**
      * Produce a string in double quotes with backslash sequences in all the
      * right places. A backslash will be inserted within </, allowing JSON
      * text to be delivered in HTML. In JSON text, a string cannot contain a
      * control character or an unescaped quote or backslash.
      * @param string A String
      * @return  A String correctly formatted for insertion in a JSON text.
      */
     private static String quote(String string) {
         if (string == null || string.length() == 0) {
             return "\"\"";
         }

         char         b;
         char         c = 0;
         int          i;
         int          len = string.length();
         StringBuilder sb = new StringBuilder(len + 4);
         String       t;

         sb.append('"');
         for (i = 0; i < len; i += 1) {
             b = c;
             c = string.charAt(i);
             switch (c) {
             case '\\':
             case '"':
                 sb.append('\\');
                 sb.append(c);
                 break;
             case '/':
                 if (b == '<') {
                     sb.append('\\');
                 }
                 sb.append(c);
                 break;
             case '\b':
                 sb.append("\\b");
                 break;
             case '\t':
                 sb.append("\\t");
                 break;
             case '\n':
                 sb.append("\\n");
                 break;
             case '\f':
                 sb.append("\\f");
                 break;
             case '\r':
                 sb.append("\\r");
                 break;
             default:
                 if (c < ' ') {
                     t = "000" + Integer.toHexString(c);
                     sb.append("\\u" + t.substring(t.length() - 4));
                 } else {
                     sb.append(c);
                 }
             }
         }
         sb.append('"');
         return sb.toString();
     }
     
     /**
      * This uses invokeAndBlock to wait for the result of the given javascript expression.
      * @param timeout Timeout in milliseconds.
      * @param js The javascript expression.
      * @param params Parameters to inject in the expression.  See {@link #injectParameters(java.lang.String, java.lang.Object...) } for details.
      * @return The result.
      */
     public JSRef executeAndWait(int timeout, String js, Object... params) {
         return executeAndWait(timeout, injectParameters(js, params));
     }
     
     /**
      * This uses invokeAndBlock to wait for the result of the given javascript expression.
      * @param js The javascript expression.
      * @param params Parameters to inject in the expression.  See {@link #injectParameters(java.lang.String, java.lang.Object...) } for details.
      * @return The result.
      */
     public JSRef executeAndWait(String js, Object... params) {
         return executeAndWait(0, js, params);
     }
     
     /**
     * This uses invokeAndBlock to wait for the result of the given javascript expression.  It is extremely important
     * that the js expression calls either {@code callback.onSuccess(value) } or {@code literalcallback.onError(message, code) }
     * at some point, or this method will never return.
     * 
     * <h3>{@link   #executeAndWait(java.lang.String)} vs {@link #executeAndReturnString(java.lang.String) }</h3>
     * 
     * <p>{@link #executeAndReturnString(java.lang.String) } is also blocking, but it uses javascript {@literal eval }
     * to return the value of the expression.  Therefore it can't return the result of any asynchronous operations.</p>
     * 
     * <p>{@link #executeAndWait(java.lang.String) } is built directly on top of {@link #execute(java.lang.String, com.codename1.util.SuccessCallback) }
     * which is fully asynchronous, and allows you to specify where and when you call the callback within the
     * javascript code. This means that you <strong>must</strong> explicitly call either {@code callback.onSuccess(value) } or {@code literalcallback.onError(message, code) }
     * at some point in the Javascript expression - or the method will block indefinitely.</p>
     * @param js The javascript expression to execute.  You must call {@code callback.onSuccess(value)} with the result that you want to have returned.
     * @return The result that is returned from javascript when it calls {@code callback.onSuccess(value) }
     */
     public JSRef executeAndWait(String js) {
         return executeAndWait(0, js);
     }
     
     
    /**
     * This uses invokeAndBlock to wait for the result of the given javascript expression.  It is extremely important
     * that the js expression calls either {@code callback.onSuccess(value) } or {@code literalcallback.onError(message, code) }
     * at some point, or this method will never return.
     * 
     * <h3>{@link   #executeAndWait(java.lang.String)} vs {@link #executeAndReturnString(java.lang.String) }</h3>
     * 
     * <p>{@link #executeAndReturnString(java.lang.String) } is also blocking, but it uses javascript {@literal eval }
     * to return the value of the expression.  Therefore it can't return the result of any asynchronous operations.</p>
     * 
     * <p>{@link #executeAndWait(java.lang.String) } is built directly on top of {@link #execute(java.lang.String, com.codename1.util.SuccessCallback) }
     * which is fully asynchronous, and allows you to specify where and when you call the callback within the
     * javascript code. This means that you <strong>must</strong> explicitly call either {@code callback.onSuccess(value) } or {@code literalcallback.onError(message, code) }
     * at some point in the Javascript expression - or the method will block indefinitely.</p>
     * 
     * @param timeout Timeout in ms
     * @param js The javascript expression to execute.  You must call {@code callback.onSuccess(value)} with the result that you want to have returned.
     * @return The result that is returned from javascript when it calls {@code callback.onSuccess(value) }
     */
    public JSRef executeAndWait(int timeout, String js) {
        final ExecuteResult res = new ExecuteResult();
        execute(timeout, js, new Callback<JSRef>() {

            public void onSucess(JSRef value) {
                synchronized(res) {
                    res.complete = true;
                    res.value = value;
                    res.notifyAll();
                }
            }

            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                synchronized(res) {
                    res.complete = true;
                    res.error = err;
                    res.notifyAll();
                }
            }
        });
        
        while (!res.complete) {
            Display.getInstance().invokeAndBlock(new Runnable() {

                public void run() {
                    Util.wait(res, 1000);
                }
                
            });
        }
        if (res.error != null) {
            throw new RuntimeException(res.error.getMessage());
        } else {
            return res.value;
        }
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
