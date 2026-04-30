/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.typedarrays;

import com.codename1.html5.js.JSObject;

/**
 * Int32Array typed array interface.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Int32Array
 */
public interface Int32Array extends JSObject {
    static Int32Array create(int length) {
        return null; // Native implementation
    }
    
    static Int32Array create(ArrayBuffer buffer) {
        return null; // Native implementation
    }
    
    int getLength();
    int get(int index);
    void set(int index, int value);
    Int32Array subarray(int begin);
    Int32Array subarray(int begin, int end);
    ArrayBuffer getBuffer();
    int getByteOffset();
    int getByteLength();
}