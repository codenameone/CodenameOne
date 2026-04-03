/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * HTML input element interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLInputElement
 */
public interface HTMLInputElement extends HTMLElement {
    String getValue();
    void setValue(String value);
    String getType();
    void setType(String type);
    boolean isChecked();
    void setChecked(boolean checked);
    String getPlaceholder();
    void setPlaceholder(String placeholder);
    boolean isDisabled();
    void setDisabled(boolean disabled);
    void focus();
    void blur();
    void select();
    void click();
}