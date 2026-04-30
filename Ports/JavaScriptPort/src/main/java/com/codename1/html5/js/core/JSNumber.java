/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.core;

import com.codename1.html5.js.JSObject;

/**
 * JavaScript Number interface.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number
 */
public interface JSNumber extends JSObject {
    static JSNumber valueOf(int value) {
        return null; // Native implementation
    }
    
    static JSNumber valueOf(double value) {
        return null; // Native implementation
    }
    
    double doubleValue();
    int intValue();
    long longValue();
    String toLocaleString();
    String toLocaleString(String locale);
    String toFixed(int digits);
    String toExponential();
    String toExponential(int digits);
    String toPrecision(int digits);
}