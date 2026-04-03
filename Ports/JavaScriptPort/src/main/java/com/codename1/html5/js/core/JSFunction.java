/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.core;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSFunctor;

/**
 * JavaScript Function interface.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Function
 */
public interface JSFunction extends JSObject {
    Object call(Object thisArg);
    Object call(Object thisArg, Object... args);
    Object apply(Object thisArg, JSArray<?> args);
    Object bind(Object thisArg);
    int getLength();
}