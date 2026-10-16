/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.typed_data;

/**
 * Dart's {@code dart:typed_data} ByteData: a fixed-length, random-access view
 * over a byte buffer with typed accessors. Minimal big-endian implementation
 * covering the 8- and 32-bit integer accessors.
 */
public final class ByteData {

    private final byte[] buffer;

    public ByteData(long length) {
        dart.core.RangeError.checkNotNegative(length, "length");
        // Checked on the long, before narrowing: ByteData(2^32) cast to a zero-byte
        // buffer instead of failing, as the list constructors already refuse to.
        dart.core.RangeError.checkAllocatable(length);
        this.buffer = new byte[(int) length];
    }

    /**
     * The offset as an array index, after checking the whole access fits -- on the
     * long, before narrowing. The cast came first before, so an offset of 2^32 wrapped
     * to 0: getUint8 read byte zero and setUint8 overwrote it instead of throwing.
     */
    private int at(long byteOffset, int width) {
        // Against length - width, never offset + width: near Long.MAX_VALUE the sum
        // overflowed negative, passed, and the narrowed index threw Java's
        // ArrayIndexOutOfBoundsException instead of Dart's RangeError.
        if (byteOffset < 0 || byteOffset > buffer.length - width) {
            throw new dart.core.RangeError("Invalid value: Not in inclusive range 0.."
                    + (buffer.length - width) + ": " + byteOffset + " (byteOffset)");
        }
        return (int) byteOffset;
    }

    public long lengthInBytes() {
        return buffer.length;
    }

    public long getUint8(long byteOffset) {
        return buffer[at(byteOffset, 1)] & 0xFF;
    }

    public void setUint8(long byteOffset, long value) {
        buffer[at(byteOffset, 1)] = (byte) value;
    }

    public long getInt32(long byteOffset) {
        int o = at(byteOffset, 4);
        return ((buffer[o] & 0xFF) << 24)
                | ((buffer[o + 1] & 0xFF) << 16)
                | ((buffer[o + 2] & 0xFF) << 8)
                | (buffer[o + 3] & 0xFF);
    }

    public void setInt32(long byteOffset, long value) {
        int o = at(byteOffset, 4);
        buffer[o] = (byte) (value >> 24);
        buffer[o + 1] = (byte) (value >> 16);
        buffer[o + 2] = (byte) (value >> 8);
        buffer[o + 3] = (byte) value;
    }
}
