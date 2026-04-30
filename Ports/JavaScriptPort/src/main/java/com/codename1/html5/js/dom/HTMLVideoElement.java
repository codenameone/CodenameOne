/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * HTML video element interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLVideoElement
 */
public interface HTMLVideoElement extends HTMLMediaElement {
    int getVideoWidth();
    int getVideoHeight();
    String getPoster();
    void setPoster(String poster);
    void setAutoplay(boolean autoplay);
}