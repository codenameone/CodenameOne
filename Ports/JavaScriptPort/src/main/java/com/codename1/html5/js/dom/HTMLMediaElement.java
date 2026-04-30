/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.dom;

import com.codename1.html5.js.JSObject;

/**
 * HTML media element interface.
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement
 */
public interface HTMLMediaElement extends HTMLElement {
    void play();
    void pause();
    double getCurrentTime();
    void setCurrentTime(double time);
    double getDuration();
    boolean isPaused();
    boolean isEnded();
    double getVolume();
    void setVolume(double volume);
    void setMuted(boolean muted);
    boolean isMuted();
    void setSrc(String src);
    String getSrc();
    JSObject getError();
}