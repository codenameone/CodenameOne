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
        this.buffer = new byte[(int) length];
    }

    public long lengthInBytes() {
        return buffer.length;
    }

    public long getUint8(long byteOffset) {
        return buffer[(int) byteOffset] & 0xFF;
    }

    public void setUint8(long byteOffset, long value) {
        buffer[(int) byteOffset] = (byte) value;
    }

    public long getInt32(long byteOffset) {
        int o = (int) byteOffset;
        return ((buffer[o] & 0xFF) << 24)
                | ((buffer[o + 1] & 0xFF) << 16)
                | ((buffer[o + 2] & 0xFF) << 8)
                | (buffer[o + 3] & 0xFF);
    }

    public void setInt32(long byteOffset, long value) {
        int o = (int) byteOffset;
        buffer[o] = (byte) (value >> 24);
        buffer[o + 1] = (byte) (value >> 16);
        buffer[o + 2] = (byte) (value >> 8);
        buffer[o + 3] = (byte) value;
    }
}
