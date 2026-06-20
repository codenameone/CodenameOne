/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.vector;

/// A minimal growable `int` buffer used while decoding vector-tile geometry.
///
/// Tile geometry is a long run of packed integers; collecting it into a
/// `java.util.List<Integer>` would box every value. This primitive buffer
/// avoids that overhead, which matters when a single tile holds tens of
/// thousands of coordinates.
final class IntArray {

    private int[] data;
    private int size;

    IntArray() {
        this(16);
    }

    IntArray(int initialCapacity) {
        data = new int[initialCapacity < 4 ? 4 : initialCapacity];
    }

    void add(int value) {
        if (size == data.length) {
            int[] grown = new int[data.length * 2];
            System.arraycopy(data, 0, grown, 0, size);
            data = grown;
        }
        data[size++] = value;
    }

    int get(int index) {
        return data[index];
    }

    int size() {
        return size;
    }

    void clear() {
        size = 0;
    }

    /// A trimmed copy holding exactly [#size] elements.
    int[] toArray() {
        int[] out = new int[size];
        System.arraycopy(data, 0, out, 0, size);
        return out;
    }
}
