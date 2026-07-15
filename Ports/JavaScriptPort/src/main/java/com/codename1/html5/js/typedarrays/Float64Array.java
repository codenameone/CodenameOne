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