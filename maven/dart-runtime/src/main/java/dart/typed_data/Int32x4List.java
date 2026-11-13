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

/// Dart's {@code Int32x4List}: a fixed-length list of {@link Int32x4} values, 16 bytes each.
public final class Int32x4List extends SimdList<Int32x4> {

    Int32x4List(byte[] buf, int off, int len) {
        super(buf, off, len);
    }

    public Int32x4List(long length) {
        this(alloc(length), 0, (int) length);
    }

    public static Int32x4List fromList(Iterable<?> elements) {
        return (Int32x4List) fill(new Int32x4List(count(elements)), elements);
    }

    public static Int32x4List view(ByteBuffer buffer) {
        return view(buffer, 0, null);
    }

    public static Int32x4List view(ByteBuffer buffer, long offsetInBytes) {
        return view(buffer, offsetInBytes, null);
    }

    public static Int32x4List view(ByteBuffer buffer, long offsetInBytes, Long length) {
        return new Int32x4List(buffer.bytes(), (int) offsetInBytes, viewLength(buffer, offsetInBytes, length));
    }

    @Override
    Int32x4 load(int b) {
        return new Int32x4(readInt(b), readInt(b + 4), readInt(b + 8), readInt(b + 12));
    }

    @Override
    void store(int b, Int32x4 v) {
        writeInt(b, v.x);
        writeInt(b + 4, v.y);
        writeInt(b + 8, v.z);
        writeInt(b + 12, v.w);
    }

    @Override
    SimdList<Int32x4> make(byte[] buf, int off, int len) {
        return new Int32x4List(buf, off, len);
    }

    @Override
    public Int32x4List sublist(long start, long end) {
        return (Int32x4List) super.sublist(start, end);
    }

    /// {@code sublist(start, [end])} with the end left out passed as null.
    public Int32x4List sublist(long start, Long end) {
        return sublist(start, end == null ? len : end.longValue());
    }

    @Override
    public Int32x4List sublist(long start) {
        return sublist(start, len);
    }
}
