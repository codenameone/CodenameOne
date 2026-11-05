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

/// Dart's {@code Float32List}.
public final class Float32List extends TypedFloatList {
    public static final long BYTES_PER_ELEMENT = 4;

    Float32List(byte[] buf, int off, int len) {
        super(buf, off, len);
    }

    public Float32List(long length) {
        this(alloc(length, 4), 0, (int) length);
    }

    public static Float32List fromList(Iterable<?> elements) {
        java.util.ArrayList<Object> copy = new java.util.ArrayList<Object>();
        for (Object o : elements) {
            copy.add(o);
        }
        Float32List l = new Float32List(copy.size());
        for (int i = 0; i < copy.size(); i++) {
            l.store(l.byteIndex(i), ((Number) copy.get(i)).doubleValue());
        }
        return l;
    }

    public static Float32List view(ByteBuffer buffer) {
        return view(buffer, 0, null);
    }

    public static Float32List view(ByteBuffer buffer, long offsetInBytes) {
        return view(buffer, offsetInBytes, null);
    }

    public static Float32List view(ByteBuffer buffer, long offsetInBytes, Long length) {
        int n = Bytes.viewLength(buffer, offsetInBytes, length, 4);
        return new Float32List(buffer.bytes(), (int) offsetInBytes, n);
    }

    @Override
    public Float32List sublist(long start, long end) {
        return (Float32List) super.sublist(start, end);
    }

    @Override
    public Float32List sublist(long start) {
        return sublist(start, len);
    }

    @Override
    int elementSize() {
        return 4;
    }

    @Override
    TypedFloatList make(byte[] buf, int off, int len) {
        return new Float32List(buf, off, len);
    }

    @Override
    double load(int b) {
        return Float.intBitsToFloat((int) Bytes.read(buf, b, 4, true));
    }

    @Override
    void store(int b, double v) {
        Bytes.write(buf, b, 4, true, Float.floatToRawIntBits((float) v));
    }
}
