/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.jso.util;


import com.codename1.html5.js.JSBody;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.core.JSFunction;
import com.codename1.html5.js.dom.EventListener;

/**
 *
 * @author shannah
 */
public class EventUtil {
    @JSBody(params={"functor"}, script="return functor")
    private native static JSFunction _getFunctorFunc(JSObject o);
    
    @JSBody(params={"target", "eventType", "handler"}, script="target.addEventListener(eventType, handler, false)")
    private native static void _addEventListener(JSObject target, String eventType, JSFunction handler);
    
    @JSBody(params={"target", "eventType", "handler", "useCapture"}, script="target.addEventListener(eventType, handler, useCapture)")
    private native static void _addEventListener(JSObject target, String eventType, JSFunction handler, boolean useCapture);
    
    
    @JSBody(params={"target", "eventType", "handler"}, script="target.removeEventListener(eventType, handler, false)")
    private native static void _removeEventListener(JSObject target, String eventType, JSFunction handler);
    
    @JSBody(params={"target", "eventType", "handler", "useCapture"}, script="target.removeEventListener(eventType, handler, useCapture)")
    private native static void _removeEventListener(JSObject target, String eventType, JSFunction handler, boolean useCapture);
    
    
    public static JSFunction addEventListener(JSObject target, String eventType, EventListener l) {
        JSFunction f = _getFunctorFunc(l);
        _addEventListener(target, eventType, f);
        return f;
    }
    public static JSFunction addEventListener(JSObject target, String eventType, EventListener l, boolean useCapture) {
        JSFunction f = _getFunctorFunc(l);
        _addEventListener(target, eventType, f, useCapture);
        return f;
    }
    
    public static void removeEventListener(JSObject target, String eventType, JSFunction handle) {
        _removeEventListener(target, eventType, handle);
    }
    public static void removeEventListener(JSObject target, String eventType, JSFunction handle, boolean useCapture) {
        _removeEventListener(target, eventType, handle, useCapture);
    }
}
