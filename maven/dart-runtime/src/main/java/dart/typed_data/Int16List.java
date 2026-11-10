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

/// Dart's {@code Int16List}.
public final class Int16List extends TypedIntList {
    public static final long BYTES_PER_ELEMENT = 2;

    Int16List(byte[] buf, int off, int len) {
        super(buf, off, len);
    }

    public Int16List(long length) {
        this(alloc(length, 2), 0, (int) length);
    }

    public static Int16List fromList(Iterable<?> elements) {
        int n = count(elements);
        return fill(new Int16List(n), elements);
    }

    public static Int16List view(ByteBuffer buffer) {
        return view(buffer, 0, null);
    }

    public static Int16List view(ByteBuffer buffer, long offsetInBytes) {
        return view(buffer, offsetInBytes, null);
    }

    public static Int16List view(ByteBuffer buffer, long offsetInBytes, Long length) {
        int n = Bytes.viewLength(buffer, offsetInBytes, length, 2);
        return new Int16List(buffer.bytes(), (int) offsetInBytes, n);
    }

    @Override
    public Int16List sublist(long start, long end) {
        return (Int16List) super.sublist(start, end);
    }

    @Override
    public Int16List sublist(long start) {
        return sublist(start, len);
    }

    @Override
    int elementSize() {
        return 2;
    }

    @Override
    TypedIntList make(byte[] buf, int off, int len) {
        return new Int16List(buf, off, len);
    }

    @Override
    long load(int b) {
        return Bytes.signExtend(Bytes.read(buf, b, 2, true), 2);
    }

    @Override
    void store(int b, long v) {
        Bytes.write(buf, b, 2, true, v);
    }
}
