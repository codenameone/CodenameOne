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
package com.codename1.impl.office;

import java.io.IOException;

/// Reads the protocol buffer wire format, field by field, without a schema.
///
/// The reader walks a byte range: `next()` moves to the next field and reports its number
/// and wire type, then one of the value accessors reads it -- or `next()` skips it. A nested
/// message is read with a new reader over `bytesRange()`. Every length is checked against
/// the range, so damaged input fails with an `IOException`.
///
/// ```java
/// Protobuf p = new Protobuf(data, 0, data.length);
/// while (p.next()) {
///     if (p.field() == 1 && p.wireType() == Protobuf.VARINT) {
///         long id = p.varint();
///     }
/// }
/// ```
public final class Protobuf {
    /// Wire type of a varint.
    public static final int VARINT = 0;

    /// Wire type of a 64-bit value.
    public static final int FIXED64 = 1;

    /// Wire type of a length-delimited value: bytes, a string, a message or a packed list.
    public static final int BYTES = 2;

    /// Wire type of a 32-bit value.
    public static final int FIXED32 = 5;

    private final byte[] data;
    private final int end;
    private int pos;
    private int field;
    private int wireType;
    private int valueStart;
    private int valueEnd;
    private long varintValue;

    /// A reader over a range of bytes.
    ///
    /// #### Parameters
    ///
    /// - `data`: the bytes
    ///
    /// - `off`: where the message starts
    ///
    /// - `len`: its length
    public Protobuf(byte[] data, int off, int len) {
        if (off < 0 || len < 0 || off + len > data.length || off + len < 0) {
            throw new IllegalArgumentException("Range outside the data");
        }
        this.data = data;
        this.pos = off;
        this.end = off + len;
    }

    /// Reads a varint at a position.
    ///
    /// #### Parameters
    ///
    /// - `data`: the bytes
    ///
    /// - `at`: one element: the position, advanced past the varint
    ///
    /// - `end`: where the readable range ends
    ///
    /// #### Returns
    ///
    /// the value
    ///
    /// #### Throws
    ///
    /// - `IOException`: when it runs past the end or past ten bytes
    public static long readVarint(byte[] data, int[] at, int end) throws IOException {
        long v = 0;
        int p = at[0];
        for (int shift = 0; shift < 70; shift += 7) {
            if (p >= end) {
                throw new IOException("Truncated varint");
            }
            int b = data[p++] & 0xff;
            v |= (long) (b & 0x7f) << shift;
            if (b < 0x80) {
                at[0] = p;
                return v;
            }
        }
        throw new IOException("Varint too long");
    }

    /// Moves to the next field, skipping the value of the current one.
    ///
    /// #### Returns
    ///
    /// false at the end of the message
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the message is damaged
    public boolean next() throws IOException {
        if (pos >= end) {
            return false;
        }
        int[] at = {pos};
        long key = readVarint(data, at, end);
        field = (int) (key >>> 3);
        wireType = (int) (key & 7);
        int p = at[0];
        switch (wireType) {
            case VARINT:
                varintValue = readVarint(data, at, end);
                valueStart = p;
                valueEnd = at[0];
                break;
            case FIXED64:
                valueStart = p;
                valueEnd = p + 8;
                break;
            case BYTES:
                long n = readVarint(data, at, end);
                if (n < 0 || n > end - at[0]) {
                    throw new IOException("Field length past the message");
                }
                valueStart = at[0];
                valueEnd = at[0] + (int) n;
                break;
            case FIXED32:
                valueStart = p;
                valueEnd = p + 4;
                break;
            default:
                throw new IOException("Unsupported wire type " + wireType);
        }
        if (valueEnd > end) {
            throw new IOException("Field past the message");
        }
        pos = valueEnd;
        return true;
    }

    /// The current field's number.
    ///
    /// #### Returns
    ///
    /// the number
    public int field() {
        return field;
    }

    /// The current field's wire type.
    ///
    /// #### Returns
    ///
    /// one of the wire type constants
    public int wireType() {
        return wireType;
    }

    /// The current field as a varint.
    ///
    /// #### Returns
    ///
    /// the value, or 0 for a field of another type
    public long varint() {
        return wireType == VARINT ? varintValue : 0;
    }

    /// The current 32-bit field as a float.
    ///
    /// #### Returns
    ///
    /// the value, or 0 for a field of another type
    public float float32() {
        return wireType == FIXED32 ? Float.intBitsToFloat((int) little(4)) : 0;
    }

    /// The current 64-bit field as a double.
    ///
    /// #### Returns
    ///
    /// the value, or 0 for a field of another type
    public double float64() {
        return wireType == FIXED64 ? Double.longBitsToDouble(little(8)) : 0;
    }

    /// The current fixed-width field as an unsigned integer.
    ///
    /// #### Returns
    ///
    /// the value
    public long fixed() {
        return little(valueEnd - valueStart);
    }

    private long little(int n) {
        long v = 0;
        for (int i = n - 1; i >= 0; i--) {
            v = v << 8 | (data[valueStart + i] & 0xff);
        }
        return v;
    }

    /// The current length-delimited field's start and length.
    ///
    /// #### Returns
    ///
    /// two elements: offset and length
    public int[] bytesRange() {
        return new int[] {valueStart, valueEnd - valueStart};
    }

    /// A reader over the current length-delimited field, a nested message.
    ///
    /// #### Returns
    ///
    /// the reader
    public Protobuf message() {
        return new Protobuf(data, valueStart, wireType == BYTES ? valueEnd - valueStart : 0);
    }

    /// The current length-delimited field as UTF-8 text.
    ///
    /// #### Returns
    ///
    /// the text
    public String string() {
        if (wireType != BYTES) {
            return "";
        }
        try {
            return new String(data, valueStart, valueEnd - valueStart, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return "";
        }
    }

    /// The underlying bytes, for ranges from `bytesRange()`.
    ///
    /// #### Returns
    ///
    /// the array this reader reads
    public byte[] data() {
        return data;
    }
}
