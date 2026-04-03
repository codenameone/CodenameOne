/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.typedarrays;

import com.codename1.html5.js.JSObject;

/**
 * ArrayBufferView base interface for typed arrays.
 */
public interface ArrayBufferView extends JSObject {
    ArrayBuffer getBuffer();
    int getByteOffset();
    int getByteLength();
}