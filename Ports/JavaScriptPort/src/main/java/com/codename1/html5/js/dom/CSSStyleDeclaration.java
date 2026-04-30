/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * CSS Style Declaration interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/CSSStyleDeclaration
 */
public interface CSSStyleDeclaration extends JSObject {
    String getProperty(String property);
    String getPropertyValue(String property);
    void setProperty(String property, String value);
    String removeProperty(String property);
    String getCssText();
    void setCssText(String text);
    int getLength();
    String item(int index);
}