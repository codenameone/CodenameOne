/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
