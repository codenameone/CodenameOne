/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventListener;

public interface HTMLDocument extends JSObject {
    @JSBody(script = "return document")
    static HTMLDocument current() { return null; }
    
    HTMLElement getElementById(String id);
    @JSProperty HTMLHeadElement getHead();
    @JSProperty HTMLBodyElement getBody();
    @JSProperty void setBody(HTMLBodyElement body);
    <T extends HTMLElement> T createElement(String tagName);
    <T extends HTMLElement> T createElementNS(String ns, String tagName);
    <T extends HTMLElement> T querySelector(String selectors);
    NodeList<HTMLElement> querySelectorAll(String selectors);
    @JSProperty String getTitle();
    @JSProperty void setTitle(String title);
    @JSProperty String getCookie();
    @JSProperty void setCookie(String cookie);
    <T extends Event> void addEventListener(String type, EventListener<T> listener);
    <T extends Event> void removeEventListener(String type, EventListener<T> listener);
    Object createEvent(String type);
}