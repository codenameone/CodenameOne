/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * HTML option element interface for select lists.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLOptionElement
 */
public interface HTMLOptionElement extends HTMLElement {
    boolean isSelected();
    void setSelected(boolean selected);
    String getValue();
    void setValue(String value);
    String getText();
    void setText(String text);
}