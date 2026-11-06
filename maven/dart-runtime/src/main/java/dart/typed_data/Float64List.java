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

/// Dart's {@code Float64List}.
public final class Float64List extends TypedFloatList {
    public static final long BYTES_PER_ELEMENT = 8;

    Float64List(byte[] buf, int off, int len) {
        super(buf, off, len);
    }

    public Float64List(long length) {
        this(alloc(length, 8), 0, (int) length);
    }

    public static Float64List fromList(Iterable<?> elements) {
        java.util.ArrayList<Object> copy = new java.util.ArrayList<Object>();
        for (Object o : elements) {
            copy.add(o);
        }
        Float64List l = new Float64List(copy.size());
        for (int i = 0; i < copy.size(); i++) {
            l.store(l.byteIndex(i), ((Number) copy.get(i)).doubleValue());
        }
        return l;
    }

    public static Float64List view(ByteBuffer buffer) {
        return view(buffer, 0, null);
    }

    public static Float64List view(ByteBuffer buffer, long offsetInBytes) {
        return view(buffer, offsetInBytes, null);
    }

    public static Float64List view(ByteBuffer buffer, long offsetInBytes, Long length) {
        int n = Bytes.viewLength(buffer, offsetInBytes, length, 8);
        return new Float64List(buffer.bytes(), (int) offsetInBytes, n);
    }

    @Override
    public Float64List sublist(long start, long end) {
        return (Float64List) super.sublist(start, end);
    }

    @Override
    public Float64List sublist(long start) {
        return sublist(start, len);
    }

    @Override
    int elementSize() {
        return 8;
    }

    @Override
    TypedFloatList make(byte[] buf, int off, int len) {
        return new Float64List(buf, off, len);
    }

    @Override
    double load(int b) {
        return Double.longBitsToDouble(Bytes.read(buf, b, 8, true));
    }

    @Override
    void store(int b, double v) {
        Bytes.write(buf, b, 8, true, Double.doubleToRawLongBits(v));
    }
}
