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
package com.codename1.tools.javac;

/** A growable big-endian byte buffer for class-file output. */
final class ByteBuf {
    byte[] data;
    int length;

    ByteBuf() {
        this(256);
    }

    ByteBuf(int capacity) {
        data = new byte[capacity];
    }

    private void ensure(int extra) {
        if (length + extra > data.length) {
            int n = data.length * 2;
            while (n < length + extra) {
                n *= 2;
            }
            byte[] d = new byte[n];
            System.arraycopy(data, 0, d, 0, length);
            data = d;
        }
    }

    ByteBuf u1(int v) {
        ensure(1);
        data[length++] = (byte) v;
        return this;
    }

    ByteBuf u2(int v) {
        ensure(2);
        data[length++] = (byte) (v >> 8);
        data[length++] = (byte) v;
        return this;
    }

    ByteBuf u4(int v) {
        ensure(4);
        data[length++] = (byte) (v >> 24);
        data[length++] = (byte) (v >> 16);
        data[length++] = (byte) (v >> 8);
        data[length++] = (byte) v;
        return this;
    }

    ByteBuf bytes(byte[] b, int off, int len) {
        ensure(len);
        System.arraycopy(b, off, data, length, len);
        length += len;
        return this;
    }

    ByteBuf bytes(ByteBuf b) {
        return bytes(b.data, 0, b.length);
    }

    void put2(int at, int v) {
        data[at] = (byte) (v >> 8);
        data[at + 1] = (byte) v;
    }

    void put4(int at, int v) {
        data[at] = (byte) (v >> 24);
        data[at + 1] = (byte) (v >> 16);
        data[at + 2] = (byte) (v >> 8);
        data[at + 3] = (byte) v;
    }

    int u1At(int at) {
        return data[at] & 0xFF;
    }

    int u2At(int at) {
        return (data[at] & 0xFF) << 8 | data[at + 1] & 0xFF;
    }

    int s2At(int at) {
        return (short) u2At(at);
    }

    int s4At(int at) {
        return (data[at] & 0xFF) << 24 | (data[at + 1] & 0xFF) << 16 | (data[at + 2] & 0xFF) << 8 | data[at + 3] & 0xFF;
    }

    byte[] toByteArray() {
        byte[] out = new byte[length];
        System.arraycopy(data, 0, out, 0, length);
        return out;
    }
}
