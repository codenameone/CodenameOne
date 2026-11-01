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
    private final int off;
    private final int len;

    public ByteData(long length) {
        dart.core.RangeError.checkNotNegative(length, "length");
        // Checked on the long, before narrowing: ByteData(2^32) cast to a zero-byte
        // buffer instead of failing, as the list constructors already refuse to.
        dart.core.RangeError.checkAllocatable(length);
        this.buffer = new byte[(int) length];
        this.off = 0;
        this.len = (int) length;
    }

    private ByteData(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    private ByteData(byte[] buffer, int off, int len) {
        this.buffer = buffer;
        this.off = off;
        this.len = len;
    }

    public static ByteData view(ByteBuffer buffer) {
        return view(buffer, 0, null);
    }

    public static ByteData view(ByteBuffer buffer, long offsetInBytes) {
        return view(buffer, offsetInBytes, null);
    }

    public static ByteData view(ByteBuffer buffer, long offsetInBytes, Long length) {
        int n = Bytes.viewLength(buffer, offsetInBytes, length, 1);
        return new ByteData(buffer.bytes(), (int) offsetInBytes, n);
    }

    public long offsetInBytes() {
        return off;
    }

    public long elementSizeInBytes() {
        return 1;
    }

    /// A ByteData holding {@code bytes} itself: how a Java encoder's output crosses into
    /// Dart without a copy (`ui.Image.toByteData`).
    public static ByteData fromBytes(byte[] bytes) {
        return new ByteData(bytes == null ? new byte[0] : bytes);
    }

    /// {@code byteData.buffer}: the bytes this view is over, shared, so
    /// {@code buffer.asUint8List()} reads what the ByteData holds.
    public ByteBuffer buffer() {
        return new ByteBuffer(buffer);
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
        if (byteOffset < 0 || byteOffset > len - width) {
            throw new dart.core.RangeError("Invalid value: Not in inclusive range 0.."
                    + (len - width) + ": " + byteOffset + " (byteOffset)");
        }
        return off + (int) byteOffset;
    }

    public long lengthInBytes() {
        return len;
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

    public long getInt8(long byteOffset) {
        return (byte) buffer[at(byteOffset, 1)];
    }

    public void setInt8(long byteOffset, long value) {
        buffer[at(byteOffset, 1)] = (byte) value;
    }

    public long getUint16(long byteOffset) {
        return getUint16(byteOffset, null);
    }

    public long getUint16(long byteOffset, Endian endian) {
        return Bytes.read(buffer, at(byteOffset, 2), 2, Endian.little(endian));
    }

    public void setUint16(long byteOffset, long value) {
        setUint16(byteOffset, value, null);
    }

    public void setUint16(long byteOffset, long value, Endian endian) {
        Bytes.write(buffer, at(byteOffset, 2), 2, Endian.little(endian), value);
    }

    public long getInt16(long byteOffset) {
        return getInt16(byteOffset, null);
    }

    public long getInt16(long byteOffset, Endian endian) {
        return Bytes.signExtend(Bytes.read(buffer, at(byteOffset, 2), 2, Endian.little(endian)), 2);
    }

    public void setInt16(long byteOffset, long value) {
        setInt16(byteOffset, value, null);
    }

    public void setInt16(long byteOffset, long value, Endian endian) {
        Bytes.write(buffer, at(byteOffset, 2), 2, Endian.little(endian), value);
    }

    public long getUint32(long byteOffset) {
        return getUint32(byteOffset, null);
    }

    public long getUint32(long byteOffset, Endian endian) {
        return Bytes.read(buffer, at(byteOffset, 4), 4, Endian.little(endian));
    }

    public void setUint32(long byteOffset, long value) {
        setUint32(byteOffset, value, null);
    }

    public void setUint32(long byteOffset, long value, Endian endian) {
        Bytes.write(buffer, at(byteOffset, 4), 4, Endian.little(endian), value);
    }

    public long getInt64(long byteOffset) {
        return getInt64(byteOffset, null);
    }

    public long getInt64(long byteOffset, Endian endian) {
        return Bytes.read(buffer, at(byteOffset, 8), 8, Endian.little(endian));
    }

    public void setInt64(long byteOffset, long value) {
        setInt64(byteOffset, value, null);
    }

    public void setInt64(long byteOffset, long value, Endian endian) {
        Bytes.write(buffer, at(byteOffset, 8), 8, Endian.little(endian), value);
    }

    public long getUint64(long byteOffset) {
        return getUint64(byteOffset, null);
    }

    public long getUint64(long byteOffset, Endian endian) {
        return Bytes.read(buffer, at(byteOffset, 8), 8, Endian.little(endian));
    }

    public void setUint64(long byteOffset, long value) {
        setUint64(byteOffset, value, null);
    }

    public void setUint64(long byteOffset, long value, Endian endian) {
        Bytes.write(buffer, at(byteOffset, 8), 8, Endian.little(endian), value);
    }

    public long getInt32(long byteOffset, Endian endian) {
        return Bytes.signExtend(Bytes.read(buffer, at(byteOffset, 4), 4, Endian.little(endian)), 4);
    }

    public void setInt32(long byteOffset, long value, Endian endian) {
        Bytes.write(buffer, at(byteOffset, 4), 4, Endian.little(endian), value);
    }

    public double getFloat32(long byteOffset) {
        return getFloat32(byteOffset, null);
    }

    public double getFloat32(long byteOffset, Endian endian) {
        return Float.intBitsToFloat((int) Bytes.read(buffer, at(byteOffset, 4), 4, Endian.little(endian)));
    }

    public void setFloat32(long byteOffset, double value) {
        setFloat32(byteOffset, value, null);
    }

    public void setFloat32(long byteOffset, double value, Endian endian) {
        Bytes.write(buffer, at(byteOffset, 4), 4, Endian.little(endian), Float.floatToRawIntBits((float) value));
    }

    public double getFloat64(long byteOffset) {
        return getFloat64(byteOffset, null);
    }

    public double getFloat64(long byteOffset, Endian endian) {
        return Double.longBitsToDouble(Bytes.read(buffer, at(byteOffset, 8), 8, Endian.little(endian)));
    }

    public void setFloat64(long byteOffset, double value) {
        setFloat64(byteOffset, value, null);
    }

    public void setFloat64(long byteOffset, double value, Endian endian) {
        Bytes.write(buffer, at(byteOffset, 8), 8, Endian.little(endian), Double.doubleToRawLongBits(value));
    }
}
