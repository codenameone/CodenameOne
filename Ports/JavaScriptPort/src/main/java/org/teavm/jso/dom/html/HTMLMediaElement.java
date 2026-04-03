/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSProperty;

public interface HTMLVideoElement extends HTMLMediaElement {
    @JSProperty int getVideoWidth();
    @JSProperty int getVideoHeight();
}

public interface HTMLMediaElement extends HTMLElement {
    @JSProperty String getSrc();
    @JSProperty void setSrc(String src);
    @JSProperty String getCurrentSrc();
    @JSProperty double getCurrentTime();
    @JSProperty void setCurrentTime(double time);
    @JSProperty double getDuration();
    @JSProperty boolean isPaused();
    @JSProperty boolean isEnded();
    @JSProperty double getVolume();
    @JSProperty void setVolume(double volume);
    @JSProperty boolean isMuted();
    @JSProperty void setMuted(boolean muted);
    @JSProperty Object getError();
    void play();
    void pause();
    void load();
}