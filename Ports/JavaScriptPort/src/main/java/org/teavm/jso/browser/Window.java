/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package org.teavm.jso.browser;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSProperty;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.events.Event;

public interface Window extends JSObject {
    @JSBody(params = {}, script = "return window")
    static Window current() { return null; }
    
    @JSProperty HTMLDocument getDocument();
    @JSProperty double getDevicePixelRatio();
    @JSProperty String getUserAgent();
    @JSProperty String getLanguage();
    @JSProperty int getInnerWidth();
    @JSProperty int getInnerHeight();
    
    @JSBody(script = "return encodeURIComponent")
    static JSObject encodeURIComponent() { return null; }
    
    Object dispatchEvent(Event evt);
    
    @JSBody(params = {"handler", "timeout"}, script = "setTimeout(handler, timeout)")
    void setTimeout(JSObject handler, int timeout);
}

@JSFunctor
public interface TimerHandler extends JSObject {
    void onTimer();
}