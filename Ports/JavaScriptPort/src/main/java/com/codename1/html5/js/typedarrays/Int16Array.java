/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.typedarrays;

import com.codename1.html5.js.JSObject;

/**
 * Int16Array typed array interface.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Int16Array
 */
public interface Int16Array extends JSObject {
    static Int16Array create(int length) {
        return null; // Native implementation
    }
    
    static Int16Array create(ArrayBuffer buffer) {
        return null; // Native implementation
    }
    
    int getLength();
    short get(int index);
    void set(int index, short value);
    Int16Array subarray(int begin);
    Int16Array subarray(int begin, int end);
    ArrayBuffer getBuffer();
    int getByteOffset();
    int getByteLength();
}