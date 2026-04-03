/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.typedarrays;

import com.codename1.html5.js.JSObject;

/**
 * Float64Array typed array interface.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Float64Array
 */
public interface Float64Array extends JSObject {
    static Float64Array create(int length) {
        return null; // Native implementation
    }
    
    static Float64Array create(ArrayBuffer buffer) {
        return null; // Native implementation
    }
    
    int getLength();
    double get(int index);
    void set(int index, double value);
    void set(Float64Array source);
    void set(Float64Array source, int offset);
    Float64Array subarray(int begin);
    Float64Array subarray(int begin, int end);
    ArrayBuffer getBuffer();
    int getByteOffset();
    int getByteLength();
}