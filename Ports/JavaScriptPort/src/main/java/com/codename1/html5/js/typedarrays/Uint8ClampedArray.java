/*
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 * Licensed under the PolyForm Noncommercial License 1.0.0
 */
package com.codename1.html5.js.typedarrays;

import com.codename1.html5.js.JSObject;

/**
 * Interface for JavaScript Uint8ClampedArray.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Uint8ClampedArray
 */
public interface Uint8ClampedArray extends JSObject {
    int getLength();
    byte get(int index);
    void set(int index, byte value);
    void set(int index, int value);
    void set(byte[] array);
    void set(byte[] array, int offset);
    void set(Uint8ClampedArray array);
    void set(Uint8ClampedArray array, int offset);
    ArrayBuffer getBuffer();
    int getByteOffset();
    int getByteLength();
    static Uint8ClampedArray create(int length) {
        return null;
    }
    static Uint8ClampedArray create(ArrayBuffer buffer) {
        return null;
    }
    static Uint8ClampedArray create(ArrayBuffer buffer, int byteOffset) {
        return null;
    }
    static Uint8ClampedArray create(ArrayBuffer buffer, int byteOffset, int length) {
        return null;
    }
}
