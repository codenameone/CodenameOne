/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.impl.html5.HTML5Implementation.TouchEvent;
import com.codename1.impl.html5.JSOImplementations.DocumentExt;
import com.codename1.impl.html5.JSOImplementations.HTMLIFrameElement;
import com.codename1.impl.html5.JSOImplementations.WindowExt;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Log;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.teavm.jso.util.JS;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.sun.org.apache.xml.internal.serializer.ToHTMLStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.UIManager;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.CSSStyleDeclaration;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.MessageEvent;
import com.codename1.html5.js.dom.MouseEvent;
import com.codename1.html5.js.dom.HTMLDocument;
import com.codename1.html5.js.dom.HTMLElement;
import com.codename1.html5.js.dom.TextRectangle;

/**
 *
 * @author shannah
 */
public class HTML5BrowserComponent extends HTML5Peer {

    BrowserComponent parent;
    HTMLIFrameElement iframe;
    Map<String,Object> properties = new HashMap<String,Object>();
    private boolean isAdded = false;
    private boolean isCORSRestricted = false;
    private static int nextId = 1;
    private int id;
    private boolean useProxy;
    private boolean proxyInitialized;
    private HTML5Proxy proxy;
    private String proxyUrl;
    private ArrayList<String> proxyHistory;
    private int proxyHistoryPos = -1;
    private boolean doNotAddToHistory = false;
    private EventListener<MessageEvent> messageListener = new EventListener<MessageEvent>() {


        @Override
        public void handleEvent(final MessageEvent e) {
            Window win = iframe == null ? Window.current() : iframe.getContentWindow();
            //HTML5Implementation._log("Received event "+e.getDataAsString());
            if (getEventSource(e) == win) {
                //HTML5Implementation._log("From our iframe");
                HTML5Implementation.callSerially(new Runnable() {
                    public void run() {
                        parent.fireWebEvent(BrowserComponent.onMessage, new ActionEvent(e.getDataAsString()));

                    }
                });
            } else {
                //HTML5Implementation._log("Not from our iframe");
            }

        }

    };
    
    @JSBody(params="evt", script="return evt.source")
    private native static JSObject getEventSource(Event evt);
    
    @JSBody(params={"type", "bubbles", "cancelable"}, script="return new CustomEvent(type, {bubbles:bubbles, cancelable:cancelable})")
    private native static CustomEvent newCustomEvent(String type, boolean bubbles, boolean cancelable);
    
    @JSBody(params={"event", "iframe", "x", "y"}, script="return window.copyTouchEvent(event, iframe, x, y)")
    public native static TouchEvent copyTouchEvent(Event event, HTMLIFrameElement iframe, int x, int y);
    
    @JSBody(params={"event", "iframe", "x", "y"}, script="return window.copyWheelEvent(event, iframe, x, y)")
    public native static Event copyWheelEvent(Event event, HTMLIFrameElement iframe, int x, int y);

    
    @JSBody(params={"iframe", "message", "targetOrigin"}, script="iframe.contentWindow.postMessage(message, targetOrigin);")
    private native static void _postMessage(HTMLIFrameElement iframe, String message, String targetOrigin);
    
    
    
    public void postMessage(String message, String targetOrigin) {
        if (iframe != null) {
            _postMessage(iframe, message, targetOrigin);
        }
    }

    private boolean messageListenerInstalled;
    public void installMessageListener() {
        if (iframe == null) {
            return;
        }
        if (!messageListenerInstalled) {
            messageListenerInstalled = true;
            //HTML5Implementation._log("Installing message listener");
            Window.current().addEventListener("message", messageListener, false);
        }
            
    }
    
    public void uninstallMessageListener() {
        if (messageListenerInstalled) {
            messageListenerInstalled = false;
            Window.current().removeEventListener("message", messageListener, false);
        }
        
    }

    
    
    private static interface CustomEvent extends Event {
        @JSProperty
        public void setClientX(int x);
        
        @JSProperty
        public void setClientY(int y);
    }
    
    private boolean cancelScroll;
    
    
    

    private EventListener eventRouter = new EventListener() {
        @Override
        public void handleEvent(Event event) {
            String eventType = event.getType();
            //HTML5Implementation._loSg("Routing event");
            //HTML5Implementation._log(event.getType());
            if (iframe == null) {
                return;
            }
            TextRectangle clRect = iframe.getBoundingClientRect();
            Event evt;
            if ("MozMousePixelScroll".equals(eventType) || eventType.equals(HTML5Implementation.getWheelEventType())) {
                evt = copyWheelEvent(event, iframe, clRect.getLeft(), clRect.getTop()); 
            } else if (eventType.startsWith("mouse") || eventType.startsWith("pointer") 
                    // On parts of the canvas painted over top of the iframe, we don't seem to 
                    // get mouse events *but* we *do* get pointer events, so we'll use
                    // these events to pass along to the peers container so that 
                    // CN1 can process these events.  FFS
                    || (eventType.equals("pointerdown") || eventType.equals("pointerup"))) {
                
                MouseEvent mevt = (MouseEvent)event;
                CustomEvent cevt = newCustomEvent(eventType, true, true);
                cevt.setClientX(mevt.getClientX() + clRect.getLeft());
                cevt.setClientY(mevt.getClientY() + clRect.getTop());
                evt = cevt;
            
            } else if (eventType.startsWith("touch")) {
                TouchEvent tevt = copyTouchEvent(event, iframe, clRect.getLeft(), clRect.getTop());
                evt = tevt;
            } else if (eventType.startsWith("pointer")){
                MouseEvent mevt = (MouseEvent)event;
                CustomEvent cevt = newCustomEvent("hittest", true, true);
                cevt.setClientX(mevt.getClientX() + clRect.getLeft());
                cevt.setClientY(mevt.getClientY() + clRect.getTop());
                evt = cevt;
                
            } else {
                return;
            }
            
            HTMLElement targetEl = isFirefox() ? HTML5Implementation.getInstance().peersContainer : iframe;
            if (!targetEl.dispatchEvent(evt)) {
                
                if ("touchmove".equals(eventType) || "pointermove".equals(eventType)) {
                    // On iOS we need to cancel scrolling of the iframe
                    // for touch events that are blocked by the CN1 UI
                    cancelScroll();
                }
                event.preventDefault();
                event.stopPropagation();
            } else {
                // Allow scrolling in case it wasn't allowed before
                uncancelScroll();
            }
            if ("touchend".equals(eventType) || "pointerup".equals(eventType)) {
                // Touchend should always re-allow scrolling.
                uncancelScroll();
            }
        }
        
    };
    

    private void cancelScroll() {
        if (iframe == null) {
            return;
        }
        if (!cancelScroll) {
            cancelScroll = true;
            if (isIOSMobile()) {
                //  On iOS we cancel scrolling by preventing the parent div
                // from scrolling.
                HTMLElement el = el();
                el.getStyle().setProperty("overflow", "hidden");
                el.getStyle().removeProperty("-webkit-overflow-scrolling");
            } else {
                iframe.getStyle().setProperty("pointer-events", "none");
            }
        }
    }
    
    private void uncancelScroll() {
        if (iframe == null) {
            return;
        }
        if (cancelScroll) {
            cancelScroll = false;
            if (isIOSMobile()) {
                HTMLElement el = el();
                el.getStyle().setProperty("overflow", "auto");
                el.getStyle().setProperty("-webkit-overflow-scrolling", "touch");
            } else {
                iframe.getStyle().setProperty("pointer-events", "auto");
            }
        }
    }
    
    @JSBody(params={"doc", "str"}, script="doc.write(str)")
    private static native void documentWrite(HTMLDocument doc, String str);
    
    @JSBody(
            params={},
            script="var d=(typeof document!=='undefined'&&document)?document:((typeof window!=='undefined'&&window.document)?window.document:null);"
                    + "return !!(d&&('srcdoc' in d.createElement('iframe')));")
    private static native boolean supportsSrcdocAttribute();
    private boolean supportsSrcdocAttribute;
    
    @JSBody(params={"iframe"}, script="try{if(iframe.contentWindow.document){return false} else {return true}}catch(e){return true}")
    private native static boolean isCORSRestricted(HTMLIFrameElement iframe);
    
    private boolean listenersInstalled;
    private List<EventListener> frameListeners;
    private void installFrameListeners() {
        if (listenersInstalled) {
            return;
        }
        listenersInstalled = true;
        if (frameListeners == null) {
            frameListeners = new ArrayList<>();
            frameListeners.add(new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    //HTML5Implementation._log("In onLoad");
                    // This hook is for javascript callbacks in the BrowserComponents.
                    // Most platforms use browser navigation callbacks to pass information 
                    // from javascript to java.  This involves attempting to change the page URL
                    // to a URL with an encoded message, the platform would parse the URL
                    // and then tell the browser to not navigate to it.
                    // We don't have that kind of control in iframes so instead, we provide 
                    // this alternate hook that the BrowserComponent's callback will look for
                    // before it tries to pass the value via URL.
                    isCORSRestricted = isCORSRestricted(iframe);

                    if (!isCORSRestricted) {
                        HTML5Implementation._log("Is NOT cors restricted");
                        try {
                            for (String type : new String[]{"pointerdown", "pointerup", "pointermove", "mousedown", "mouseup", "mousemove", "touchstart", "touchend", "touchmove", isFirefox() ? "MozMousePixelScroll" : HTML5Implementation.getWheelEventType()}) {
                                // Comment on using MozMousePixelScroll in firefox (Nov. 2018)
                                // Through the rest of the app we use DOMMouseScroll for wheel events when in Firefox (see normalizeWheel.getEventType() defined in fontmetrics.js)
                                // However, consuming this event (i.e. evt.preventDefault() doesn't prevent scrolling 
                                // in firefox.  The MozMousePixelScroll event can be consumed, so, since the event router works by passing
                                // the events up to Codename one to be consumed (if there is a component over top of the iframe),
                                // we need to use this event in this case.
                                // The copyWheelEvent function (in fontmetrics) propagates this as a DOMMouseScroll event so that
                                // CN1 will deal with it as a regular wheel event.
                                // See https://stackoverflow.com/a/46612031/2935174

                                iframe.getContentWindow().addEventListener(type, eventRouter, true);
                            }

                        } catch (Throwable t) {
                            HTML5Implementation._log("Failed to add event handlers to iframe, probably due to a CORS error");
                        }

                        installShouldLoadURLCallback(iframe, new ShouldLoadURLCallback() {

                            @Override
                            public boolean shouldLoadURL(final JSObject url) {
                                new Thread() {
                                    public void run() {

                                        parent.fireBrowserNavigationCallbacks(JS.unwrapString(url));
                                    }
                                }.start();

                                // We always return false since this will only be used for 
                                // the javascript bridge and we don't want it to try to 
                                // do a window.location load.
                                return false;
                            }
                        });
                    } else {
                        HTML5Implementation._log("Is cors restricted");
                    }

                    new Thread() {
                        public void run() {

                            //String url = useProxy ? proxyUrl : (iframe.getContentWindow()).getLocation().getFullURL();
                            String url = useProxy ? proxyUrl : getIframeUrl(iframe);
                            parent.fireWebEvent("onStart", new ActionEvent(url));
                            parent.fireWebEvent("onLoad", new ActionEvent(url));
                        }
                    }.start();

                }

            });
            /*
            frameListeners.add(new EventListener() {

                @Override
                public void handleEvent(Event evt) {
                    new Thread() {
                        public void run() {

                            //String url = useProxy ? proxyUrl : (iframe.getContentWindow()).getLocation().getFullURL();
                            String url = useProxy ? proxyUrl : getIframeUrl(iframe);
                            parent.fireWebEvent("onStart", new ActionEvent(url));
                            parent.fireWebEvent("onLoad", new ActionEvent(url));
                        }
                    }.start();

                }

            });
            */
        }
        if (iframe != null) {
            for (EventListener e : frameListeners) {
                iframe.addEventListener("load", e, false);
            }
        }
        
        
    }
    
    private void uninstallFrameListeners() {
        if (!listenersInstalled) {
            return;
        }
        listenersInstalled = false;
        if (frameListeners != null && iframe != null) {
            for (EventListener e : frameListeners) {
                iframe.removeEventListener("load", e, false);
            }
        }
    }
    
    public HTML5BrowserComponent(HTMLElement el, Object p) {
        super(wrapEl(el));
        supportsSrcdocAttribute = supportsSrcdocAttribute();
        id = ++nextId;
        if (el == null) {
            if (p instanceof BrowserComponent) {
                parent = (BrowserComponent)p;
                parent.fireWebEvent("onStart", new ActionEvent(CN.getProperty("browser.window.location.href", "")));
                parent.fireWebEvent("onLoad", new ActionEvent(CN.getProperty("browser.window.location.href", "")));
                installShouldLoadURLCallbackShared(new ShouldLoadURLCallback() {

                    @Override
                    public boolean shouldLoadURL(final JSObject url) {
                        new Thread() {
                                public void run() {

                                    parent.fireBrowserNavigationCallbacks(JS.unwrapString(url));
                                }
                        }.start();

                        // We always return false since this will only be used for 
                        // the javascript bridge and we don't want it to try to 
                        // do a window.location load.
                        return false;
                    }
                });
            }
            
            return;
        }
        iframe=(HTMLIFrameElement)el;
        el = (HTMLElement)(isIOSMobile() ? el.getParentNode() : el);
        this.parent=(BrowserComponent)p;
        if (isFirefox()) {
            // For some unknown reason firefox won't deliver events to the iframe properly
            // if it is a child of the container.  We need to add it to the body itself.
            el.getOwnerDocument().getBody().appendChild(el);
            
        } else {
            HTML5Implementation.getInstance().peersContainer.appendChild(el);
        }
        isAdded = true;
        el.getStyle().setProperty("display", "none");
        if (isIOSMobile()) {
            // On iOS, iFrames won't scroll on their own.  (Yes even in iOS 12).
            // so we need to wrap it in a div, and make the div scrollable.
            
            
            iframe.getStyle().setProperty("height", "100%");
            iframe.getStyle().setProperty("width", "100%");
            iframe.getStyle().setProperty("overflow", "auto");
            iframe.getStyle().setProperty("-webkit-overflow-scrolling", "touch");
            el.getStyle().setProperty("overflow", "auto");
            el.getStyle().setProperty("-webkit-overflow-scrolling", "touch");
            
        }
        Log.p("In HTML5BrowserComponent constructor.... installing frame listeners");
        installFrameListeners();
        
        
        
    }
    @JSBody(script="return /iPhone|iPod|iPad/.test(navigator.userAgent)")
    private static native boolean isIOSMobile_();
    
    private static boolean isIOSMobile() {
        return false;
        //return isIOSMobile_() && "true".equals(CN.getProperty("javascript.ios.iframe.scroll", "false"));
    }
    
    @JSBody(script="return (typeof InstallTrigger !== 'undefined')")
    private static native boolean isFirefox();
    
    private static HTMLElement wrapEl(HTMLElement el) {
        if (el == null) {
            return HTML5Implementation.getInstance().window.getDocument().createElement("div");
        }
        if (isIOSMobile()) {
            HTMLElement wrapper = HTML5Implementation.getInstance().window.getDocument().createElement("div");
            if (el.getParentNode() != null) {
                el.getParentNode().removeChild(el);
            }
            
            wrapper.appendChild(el);
            return wrapper;
        } else {
            return el;
        }
    }
    
    public void setURL(String url){
        if (iframe == null) {
            return;
        }
        if (Boolean.TRUE.equals(parent.getClientProperty("javascript.useProxy")) ) {
            useProxy = true;
        } else {
            useProxy = false;
        }
        if (useProxy) {
            if (!proxyInitialized) {
                proxyInitialized = true;
                proxyHistory = new ArrayList<String>();
                proxy = new HTML5Proxy();
                installLoadPageCallback(iframe, new LoadPageCallback() {
                    @Override
                    public void load(final String url) {
                        //System.out.println("Handling callback for url "+url);
                        new Thread() {
                            public void run() {
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        setURL(url);
                                    }
                                });
                            }
                        }.start();
                        
                        

                    }

                });
            }
            if (!doNotAddToHistory) {
                if (proxyHistory.size() > proxyHistoryPos+1) {
                    List<String> oldHistory = proxyHistory.subList(0, proxyHistoryPos+1);
                    proxyHistory = new ArrayList<String>();
                    proxyHistory.addAll(oldHistory);
                }
                proxyHistory.add(url);
                proxyHistoryPos++;
            }
            proxy.load(url, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent t) {
                    // NOt sure if we need to do anything here yet.
                }
            });
        } else {
            iframe.setAttribute("src", url);
        }
    }
    
    @JSBody(params={"iframe"}, script="var src; try { src = iframe.contentWindow.location.href} catch (e) {src = iframe.src} return src")
    private static native String getIframeUrl(HTMLIFrameElement iframe);
    
    public String getURL(){
        if (iframe == null) {
            return Window.current().getLocation().getFullURL();
        }
        return useProxy ? proxyUrl : getIframeUrl(iframe);
    }
    
    public void reload(){
        setURL(getURL());
    }

    @JSBody(
            params={"el"},
            script="var doc = el ? el.ownerDocument : null;"
                    + "if (!doc) { return false; }"
                    + "if (doc.documentElement && typeof doc.documentElement.contains === 'function') {"
                    + "  return doc.documentElement.contains(el);"
                    + "}"
                    + "if (doc.body && typeof doc.body.contains === 'function') {"
                    + "  return doc.body.contains(el);"
                    + "}"
                    + "return !!el.isConnected;")
    private native static boolean documentContains(HTMLElement el);
    
    
    @Override
    protected void initComponent() {
        HTMLElement el = el();
        Log.p("In HTML5BrowserComponent::initComponent()");
        if (!documentContains(el)) {
            Log.p("Not added to document yet.  Appending...");
            uninstallFrameListeners();
            if (isFirefox()) {
                // For some unknown reason firefox won't deliver events to the iframe properly
                // if it is a child of the container.  We need to add it to the body itself.
                el.getOwnerDocument().getBody().appendChild(el);

            } else {
                HTML5Implementation.getInstance().peersContainer.appendChild(el);
            }
            isAdded = true;
            installFrameListeners();
        } else {
            Log.p("Iframe was alreday added to document. not appending");
        }
        el.getStyle().setProperty("display", "block");
        Log.p("Calling super initComponent");
        super.initComponent(); 
        
        
    }

    @Override
    protected void deinitialize() {
        super.deinitialize();
        isAdded = false;
    }
    
    
    
    public void setPage(String content, String baseUrl){
        if (iframe == null) {
            return;
        }
        if (!supportsSrcdocAttribute) {
            DocumentExt doc = (DocumentExt)iframe.getContentWindow().getDocument();
            doc.open("text/htmlreplace");
            doc.write(content);
            doc.close();
                
        } else {
            iframe.setAttribute("srcdoc", content);
        }
    }
    
    public void setProperty(String key, Object value){
        properties.put(key, value);
    }
    
    public Object getProperty(String key){
        return properties.get(key);
    }
    
    public void execute(String javascript){
        if (isCORSRestricted) {
            throw new RuntimeException("Cannot execute javascript in this browser component because it is CORS-restricted. Javascript was "+javascript);
        }
        WindowExt win =  iframe == null ? ((WindowExt)Window.current()) : (WindowExt)iframe.getContentWindow();
        
        win.eval(javascript);
        //Window win = iframe.getContentWindow();
        //win.getLocation().assign("javascript:"+javascript);
        
    }
    
    @JSBody(params={"win", "js"}, script="return ''+win.eval(js);")
    private static native String evalStr(Window win, String js);
    
    public String executeAndReturnString(String javascript){
        if (isCORSRestricted) {
            throw new RuntimeException("Cannot execute javascript in this browser component because it is CORS-restricted.");
        }
        //WindowExt win = (WindowExt)iframe.getContentWindow();
        Window win = iframe == null ? Window.current() : iframe.getContentWindow();
        return evalStr(win, javascript);
        //return win.eval(javascript);
    }

    public boolean hasBack() {
        if (iframe == null) {
            return false;
        }
        if (useProxy) {
            return proxyHistoryPos > 0;
        } else {
            return true;
        }
    }
    
    public void back() {
        if (iframe == null) {
            return;
        }
        if (useProxy) {
            if (proxyHistoryPos > 0) {
                proxyHistoryPos--;
                doNotAddToHistory = true;
                setURL(proxyHistory.get(proxyHistoryPos));
                doNotAddToHistory = false;
            }
        } else {
            if (isCORSRestricted) {
                throw new RuntimeException("Cannot go back() in this browser component because it is CORS-restricted.");
            }
            iframe.getContentWindow().getHistory().back();
        }
    }

    public boolean hasForward() {
        if (iframe == null) {
            return false;
        }
        // Well there's no easy way to know ... so let's just say yes.
        if (useProxy) {
            return proxyHistoryPos < proxyHistory.size()-1;
        }
            
        return true;
    }

    public void forward() {
        if (iframe == null) {
            return;
        }
        if (useProxy) {
            if (proxyHistoryPos < proxyHistory.size()-1) {
                proxyHistoryPos++;
                doNotAddToHistory = true;
                setURL(proxyHistory.get(proxyHistoryPos));
                doNotAddToHistory = false;
            }
        } else {
            if (isCORSRestricted) {
                throw new RuntimeException("Cannot go forward in this browser component because it is CORS-restricted.");
            }
            iframe.getContentWindow().getHistory().forward();
        }
    }
    
    @JSBody(params={"url", "content", "iframe"}, script="return window.cn1.proxifyContent(url, content, iframe);")
    private native static void proxifyContent(String url, String content, JSObject iframe);
    
    private class HTML5Proxy {
        /*
        private String getHost(String absUrl) {
            int slashSlashPos = absUrl.indexOf("//");
            if (slashSlashPos < 0) {
                return "";
            }
            absUrl = absUrl.substring(slashSlashPos+2);
            int slashPos = absUrl.indexOf("/");
            if (slashPos >= 0) {
                absUrl = absUrl.substring(0, slashPos);
            }
            return absUrl;
        }
        
        private String getHostName(String absUrl) {
            absUrl = getHost(absUrl);
            int colonPos = absUrl.indexOf(":");
            if (colonPos >= 0) {
                absUrl = absUrl.substring(0, colonPos);
            }
            return absUrl;
        }
        
        private String getHash(String absUrl) {
            int hashPos = absUrl.indexOf("#");
            if (hashPos >= 0) {
                absUrl = absUrl.substring(hashPos);
            }
            return absUrl;
        }
        
        private String getProtocol(String absUrl) {
            int colonPos = absUrl.indexOf(":");
            if (colonPos > 0) {
                return absUrl.substring(0, colonPos+1);
            }
            return "";
        }
        
        private String getPort(String absUrl) {
            absUrl = getHost(absUrl);
            int colonPos = absUrl.indexOf(":");
            if (colonPos >= 0) {
                return absUrl.substring(colonPos+1);
            } else {
                return "";
            }
        }
        
        private String getPathname(String absUrl) {
            absUrl = absUrl.substring(getHost(absUrl).length());
            int hashPos = absUrl.indexOf("#");
            if (hashPos>=0) {
                absUrl = absUrl.substring(0, hashPos);
            }
            int qpos = absUrl.indexOf("?");
            if (qpos >= 0) {
                absUrl = absUrl.substring(0, qpos);
            }
            return absUrl;
            
        }
        
        private String getSearch(String absUrl) {
            int qpos = absUrl.indexOf("?");
            absUrl = absUrl.substring(qpos);
            int hashPos = absUrl.indexOf("#");
            if (hashPos >= 0) {
                absUrl = absUrl.substring(0, hashPos);
            }
            return absUrl;
        }
        
        
        private String getOrigin(String absUrl) {
            int slashSlashPos = absUrl.indexOf("//");
            if (slashSlashPos >= 0) {
                slashSlashPos = absUrl.indexOf("/", slashSlashPos+2);
            } else {
                slashSlashPos = absUrl.indexOf("/");
            }
            
            if (slashSlashPos >= 0) {
                return absUrl.substring(0, slashSlashPos);
            } else {
                int qpos = absUrl.indexOf("?");
                if (qpos >= 0) {
                    return absUrl.substring(0, qpos);
                }
                int hashPos = absUrl.indexOf("#");
                if (hashPos >= 0) {
                    return absUrl.substring(0, hashPos);
                }
                return absUrl;
            }
        }
        
        private String getBaseUrl(String absUrl) {
            int qpos = absUrl.indexOf("?");
            if (qpos >= 0) {
                absUrl = absUrl.substring(0, qpos);
            }
            int hashPos = absUrl.indexOf("#");
            if (hashPos >= 0) {
                absUrl = absUrl.substring(0, hashPos);
            }
            JSString jstr = JSString.valueOf(absUrl);
            if (jstr.search(JSString.valueOf("\\.(html|htm|php|jsp|asp|xml|pl|py|cgi)$")) > 0 || jstr.search(JSString.valueOf("/$")) > 0) {
                return absUrl.substring(absUrl.lastIndexOf("/"));
            } else {
                return absUrl;
            }
        }
        
        private String makeUrlAbsolute(String baseUrl, String url) {
            if (baseUrl.charAt(baseUrl.length()-1) != '/') {
                baseUrl += "/";
            }
            if (url.length() > 1 && url.charAt(0) == '/' && url.charAt(1) == '/') {
                return getProtocol(baseUrl) + url;
            }
            if (url.length() > 0 && url.charAt(0) == '/') {
                return getOrigin(baseUrl) + url;
            }
            if ("".equals(getHost(url))) {
                return baseUrl + url;
            }
            
            return url;
        }
        */
        void load(final String url, ActionListener onComplete) {
            final ConnectionRequest req = new ConnectionRequest();
            //System.out.println("Sending connection request to "+url);
            req.setUrl(url);
            req.addResponseListener(new ActionListener<NetworkEvent>() {

                @Override
                public void actionPerformed(NetworkEvent t) {
                    byte data[] = req.getResponseData();
                    try {
                        proxyUrl = url;
                        //System.out.println("Setting proxyURL to "+url);
                        proxifyContent(url, new String(data, "UTF-8"), iframe);
                        //HTML5BrowserComponent.this.setPage(str, url);
                        
                    } catch (UnsupportedEncodingException ex) {
                        System.err.println("Failed to get content "+ex.getMessage());
                    }
                }
            
            });
            
            NetworkManager.getInstance().addToQueue(req);
        }
    }
    
    
    @JSFunctor
    static interface LoadPageCallback extends JSObject {
        void load(String url);
    }
    
    @JSBody(params={"iframe","callback"}, script="jQuery(iframe).on('cn1load', function(evt, url){ callback(url);});")
    native static void installLoadPageCallback(HTMLIFrameElement el, LoadPageCallback callback);
    
    
    @JSFunctor 
    interface ShouldLoadURLCallback extends JSObject {
        boolean shouldLoadURL(JSObject url);
    }
    
    @JSBody(params={"iframe","callback"}, script="try {var win=iframe.contentWindow||iframe; win.cn1application = win.cn1application || {}; win.cn1application.shouldNavigate=callback;} catch (e) { console.log('Failed to install shouldNavigate in iframe for browser component.');}")
    native static void installShouldLoadURLCallback(HTMLIFrameElement el, ShouldLoadURLCallback callback);
    
    @JSBody(params={"callback"}, script="try {var win=window; win.cn1application = win.cn1application || {}; win.cn1application.shouldNavigate=callback;} catch (e) { console.log('Failed to install shouldNavigate in iframe for browser component.');}")
    native static void installShouldLoadURLCallbackShared(ShouldLoadURLCallback callback);
    
}
