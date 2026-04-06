/*
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 * Licensed under the PolyForm Noncommercial License 1.0.0
 */
package com.codename1.html5.js.typedarrays;

import com.codename1.html5.js.JSObject;

/**
 * Interface for JavaScript Uint8Array.
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Uint8Array
 */
public interface Uint8Array extends JSObject {
    int getLength();
    int getByteLength();
    byte get(int index);
    int getInt(int index);
    void set(int index, int value);
    void set(int index, byte value);
    void set(int index, short value);
    void set(byte[] array);
    void set(byte[] array, int offset);
    void set(Uint8Array array);
    void set(Uint8Array array, int offset);
    ArrayBuffer getBuffer();
    int getByteOffset();
    static Uint8Array create(int length) {
        return null;
    }
    static Uint8Array create(ArrayBuffer buffer) {
        return null;
    }
    static Uint8Array create(ArrayBuffer buffer, int byteOffset) {
        return null;
    }
    static Uint8Array create(ArrayBuffer buffer, int byteOffset, int length) {
        return null;
    }
}
