/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.browser;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.dom.HTMLDocument;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;

/**
 * Interface for the JavaScript Window object.
 * https://developer.mozilla.org/en-US/docs/Web/API/Window
 */
public interface Window extends JSObject {
    static Window current() {
        return null; // Native implementation
    }
    
    static int setTimeout(Object handler, int delay) {
        return 0; // Native implementation
    }
    
    static void clearTimeout(int id) {
        // Native implementation
    }
    
    static int setInterval(Object handler, int delay) {
        return 0; // Native implementation
    }
    
    static void clearInterval(int id) {
        // Native implementation
    }
    
    static String encodeURIComponent(String value) {
        return null; // Native implementation
    }
    
    HTMLDocument getDocument();
    void alert(String message);
    int requestAnimationFrame(AnimationFrameCallback callback);
    void cancelAnimationFrame(int id);
    int getInnerWidth();
    int getInnerHeight();
    Object getConsole();
    Location getLocation();
    void setLocation(String url);
    void addEventListener(String type, EventListener listener);
    void addEventListener(String type, EventListener listener, boolean capture);
    void removeEventListener(String type, EventListener listener);
    void removeEventListener(String type, EventListener listener, boolean capture);
    void dispatchEvent(Event evt);
    void scrollTo(int x, int y);
    Window open(String url, String target);
    String eval(String script);
    Object getNavigator();
    History getHistory();
    void setName(String name);
    void close();
    void resizeTo(int width, int height);
}