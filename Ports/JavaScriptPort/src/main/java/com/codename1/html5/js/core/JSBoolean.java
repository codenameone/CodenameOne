/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.core;

import com.codename1.html5.js.JSObject;

/**
 * JavaScript Boolean interface.
 */
public interface JSBoolean extends JSObject {
    static JSBoolean valueOf(boolean value) {
        return null; // Native implementation
    }
    
    boolean booleanValue();
}