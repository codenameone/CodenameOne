/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * HTML script element interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLScriptElement
 */
public interface HTMLScriptElement extends HTMLElement {
    String getSrc();
    void setSrc(String src);
    String getType();
    void setType(String type);
    String getText();
    void setText(String text);
    boolean isAsync();
    void setAsync(boolean async);
    boolean isDefer();
    void setDefer(boolean defer);
}