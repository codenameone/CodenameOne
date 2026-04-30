/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.typedarrays;

import com.codename1.html5.js.JSObject;

/**
 * Float32Array typed array interface.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Float32Array
 */
public interface Float32Array extends JSObject {
    int getLength();
    float get(int index);
    void set(int index, float value);
    void set(Float32Array source);
    void set(Float32Array source, int offset);
    Float32Array subarray(int begin);
    Float32Array subarray(int begin, int end);
    ArrayBuffer getBuffer();
    int getByteOffset();
    int getByteLength();
}