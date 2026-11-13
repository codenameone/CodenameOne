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

/// Dart's {@code Float64x2List}: a fixed-length list of {@link Float64x2} values, 16 bytes each.
public final class Float64x2List extends SimdList<Float64x2> {

    Float64x2List(byte[] buf, int off, int len) {
        super(buf, off, len);
    }

    public Float64x2List(long length) {
        this(alloc(length), 0, (int) length);
    }

    public static Float64x2List fromList(Iterable<?> elements) {
        return (Float64x2List) fill(new Float64x2List(count(elements)), elements);
    }

    public static Float64x2List view(ByteBuffer buffer) {
        return view(buffer, 0, null);
    }

    public static Float64x2List view(ByteBuffer buffer, long offsetInBytes) {
        return view(buffer, offsetInBytes, null);
    }

    public static Float64x2List view(ByteBuffer buffer, long offsetInBytes, Long length) {
        return new Float64x2List(buffer.bytes(), (int) offsetInBytes, viewLength(buffer, offsetInBytes, length));
    }

    @Override
    Float64x2 load(int b) {
        return new Float64x2(Double.longBitsToDouble(Bytes.read(buf, b, 8, true)),
                Double.longBitsToDouble(Bytes.read(buf, b + 8, 8, true)));
    }

    @Override
    void store(int b, Float64x2 v) {
        Bytes.write(buf, b, 8, true, Double.doubleToRawLongBits(v.x));
        Bytes.write(buf, b + 8, 8, true, Double.doubleToRawLongBits(v.y));
    }

    @Override
    SimdList<Float64x2> make(byte[] buf, int off, int len) {
        return new Float64x2List(buf, off, len);
    }

    @Override
    public Float64x2List sublist(long start, long end) {
        return (Float64x2List) super.sublist(start, end);
    }

    /// {@code sublist(start, [end])} with the end left out passed as null.
    public Float64x2List sublist(long start, Long end) {
        return sublist(start, end == null ? len : end.longValue());
    }

    @Override
    public Float64x2List sublist(long start) {
        return sublist(start, len);
    }
}
