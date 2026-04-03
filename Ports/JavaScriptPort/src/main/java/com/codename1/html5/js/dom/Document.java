/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;

/**
 * Base Document interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/Document
 */
public interface Document extends JSObject {
    Element getElementById(String id);
    Element querySelector(String selector);
    Object querySelectorAll(String selector);
    HTMLElement createElement(String tagName);
    Element getDocumentElement();
    HTMLElement getBody();
    HTMLElement getHead();
    String getTitle();
    void setTitle(String title);
    void addEventListener(String type, EventListener listener);
    void addEventListener(String type, EventListener listener, boolean capture);
    void removeEventListener(String type, EventListener listener);
    void removeEventListener(String type, EventListener listener, boolean capture);
    void dispatchEvent(Event evt);
}