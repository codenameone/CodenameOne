/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.browser;

import com.codename1.html5.js.JSObject;

/**
 * Interface for the JavaScript History object.
 * https://developer.mozilla.org/en-US/docs/Web/API/History
 */
public interface History extends JSObject {
    int getLength();
    void back();
    void forward();
    void go(int delta);
    void pushState(Object state, String title);
    void pushState(Object state, String title, String url);
    void replaceState(Object state, String title);
    void replaceState(Object state, String title, String url);
}