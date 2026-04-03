/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.events.EventListener;

public interface HTMLElement extends JSObject {
    String getAttribute(String name);
    void setAttribute(String name, String value);
    void removeAttribute(String name);
    @JSProperty CSSStyleDeclaration getStyle();
    void appendChild(HTMLElement child);
    void removeChild(HTMLElement child);
    HTMLElement getParentElement();
    @JSProperty String getInnerText();
    @JSProperty void setInnerText(String text);
    @JSProperty String getInnerHTML();
    @JSProperty void setInnerHTML(String html);
    @JSProperty String getId();
    @JSProperty void setId(String id);
    @JSProperty String getClassName();
    @JSProperty void setClassName(String className);
    <T extends Event> void addEventListener(String type, EventListener<T> listener);
    <T extends Event> void removeEventListener(String type, EventListener<T> listener);
    void focus();
    void blur();
    void click();
    void scrollIntoView(boolean alignToTop);
    void scrollIntoView();
    void requestFullscreen();
    void requestPointerLock();
    TextRectangle getBoundingClientRect();
}