/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
