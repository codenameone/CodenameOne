/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * Interface for the JavaScript Element object.
 * https://developer.mozilla.org/en-US/docs/Web/API/Element
 */
public interface Element extends JSObject {
    String getTagName();
    String getId();
    void setId(String id);
    String getAttribute(String name);
    void setAttribute(String name, String value);
    void removeAttribute(String name);
    boolean hasAttribute(String name);
    void addEventListener(String type, Object listener);
    void addEventListener(String type, Object listener, boolean capture);
    void removeEventListener(String type, Object listener);
    void removeEventListener(String type, Object listener, boolean capture);
    Element getParentNode();
    Object getFirstChild();
    Object getLastChild();
    Object getNextSibling();
    Object getPreviousSibling();
    void appendChild(Object child);
    Element insertBefore(Object newChild, Object refChild);
    void removeChild(Object child);
    void setInnerHTML(String html);
    String getInnerHTML();
    void setTextContent(String text);
    String getTextContent();
    CSSStyleDeclaration getStyle();
    TextRectangle getBoundingClientRect();
    boolean dispatchEvent(Event evt);
    HTMLDocument getOwnerDocument();
}