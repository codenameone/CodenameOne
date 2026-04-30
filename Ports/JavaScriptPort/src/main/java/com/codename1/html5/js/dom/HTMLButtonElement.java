/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * HTML button element interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLButtonElement
 */
public interface HTMLButtonElement extends HTMLElement {
    String getValue();
    void setValue(String value);
    boolean isDisabled();
    void setDisabled(boolean disabled);
    void click();
}