/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * HTML link element interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLLinkElement
 */
public interface HTMLLinkElement extends HTMLElement {
    String getHref();
    void setHref(String href);
    String getRel();
    void setRel(String rel);
    String getType();
    void setType(String type);
    String getMedia();
    void setMedia(String media);
}