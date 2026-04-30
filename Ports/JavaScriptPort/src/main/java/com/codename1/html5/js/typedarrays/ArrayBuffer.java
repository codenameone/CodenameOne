/*
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 * Licensed under the PolyForm Noncommercial License 1.0.0
 */
package com.codename1.html5.js.typedarrays;

import com.codename1.html5.js.JSObject;

/**
 * Interface for JavaScript ArrayBuffer.
 * https://developer.mozilla.org/en-US/docs/Web/API/ArrayBuffer
 */
public interface ArrayBuffer extends JSObject {
    int getByteLength();
    byte[] getBytes();
    static ArrayBuffer create(int length) {
        return null;
    }
}
