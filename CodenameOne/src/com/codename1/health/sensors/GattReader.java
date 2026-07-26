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
package com.codename1.health.sensors;

/// A bounds-checked, little-endian cursor over a GATT characteristic
/// value.
///
/// Every Bluetooth SIG health characteristic is little-endian with
/// variable-length optional fields selected by a flags byte, so a parser
/// that reads a field the device did not send walks off the end of the
/// array. Rather than let that surface as an
/// `ArrayIndexOutOfBoundsException` from inside a notification callback,
/// this reader records an underflow and every subsequent read returns
/// zero; the parser checks [#isValid()] once at the end and returns null.
///
/// The other trap this exists to close is sign. Java bytes are signed, so
/// a heart rate of 200 read as a raw `byte` is -56. Every accessor here
/// masks explicitly.
final class GattReader {

    private final byte[] data;
    private int offset;
    private boolean underflow;

    GattReader(byte[] data) {
        this.data = data == null ? new byte[0] : data;
    }

    /// `true` while every read so far has stayed inside the payload.
    boolean isValid() {
        return !underflow;
    }

    /// Bytes not yet consumed.
    int remaining() {
        return underflow ? 0 : data.length - offset;
    }

    /// Advances past `count` bytes.
    void skip(int count) {
        if (!require(count)) {
            return;
        }
        offset += count;
    }

    /// An unsigned 8-bit value, 0..255.
    int uint8() {
        if (!require(1)) {
            return 0;
        }
        return data[offset++] & 0xFF;
    }

    /// A signed 8-bit value, -128..127.
    int sint8() {
        if (!require(1)) {
            return 0;
        }
        return data[offset++];
    }

    /// An unsigned 16-bit little-endian value, 0..65535.
    int uint16() {
        if (!require(2)) {
            return 0;
        }
        int lo = data[offset++] & 0xFF;
        int hi = data[offset++] & 0xFF;
        return lo | (hi << 8);
    }

    /// A signed 16-bit little-endian value.
    int sint16() {
        int v = uint16();
        return (v & 0x8000) != 0 ? v - 0x10000 : v;
    }

    /// An unsigned 24-bit little-endian value.
    int uint24() {
        if (!require(3)) {
            return 0;
        }
        int b0 = data[offset++] & 0xFF;
        int b1 = data[offset++] & 0xFF;
        int b2 = data[offset++] & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16);
    }

    /// An unsigned 32-bit little-endian value, widened to a `long` so that
    /// values above 2^31 -- a wheel-revolution counter, for instance --
    /// stay positive.
    long uint32() {
        if (!require(4)) {
            return 0;
        }
        long b0 = data[offset++] & 0xFF;
        long b1 = data[offset++] & 0xFF;
        long b2 = data[offset++] & 0xFF;
        long b3 = data[offset++] & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private boolean require(int count) {
        if (underflow) {
            return false;
        }
        if (offset + count > data.length) {
            underflow = true;
            return false;
        }
        return true;
    }
}
