/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

/**
 * Interface for the JavaScript HTMLElement object.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement
 */
public interface HTMLElement extends Element {
    int getClientWidth();
    int getClientHeight();
    int getOffsetWidth();
    int getOffsetHeight();
    int getOffsetTop();
    int getOffsetLeft();
    void click();
    void focus();
    void blur();
    int getTabIndex();
    void setTabIndex(int index);
    boolean isHidden();
    void setHidden(boolean hidden);
    Object cloneNode(boolean deep);
    NodeList querySelectorAll(String selector);
}