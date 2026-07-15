/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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