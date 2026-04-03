/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * HTML text area element interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLTextAreaElement
 */
public interface HTMLTextAreaElement extends HTMLElement {
    String getValue();
    void setValue(String value);
    String getPlaceholder();
    void setPlaceholder(String placeholder);
    boolean isDisabled();
    void setDisabled(boolean disabled);
    void focus();
    void blur();
    void select();
    int getSelectionStart();
    void setSelectionStart(int start);
    int getSelectionEnd();
    void setSelectionEnd(int end);
}