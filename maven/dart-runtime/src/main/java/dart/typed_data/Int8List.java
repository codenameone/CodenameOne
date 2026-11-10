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

/// Dart's {@code Int8List}.
public final class Int8List extends TypedIntList {
    public static final long BYTES_PER_ELEMENT = 1;

    Int8List(byte[] buf, int off, int len) {
        super(buf, off, len);
    }

    public Int8List(long length) {
        this(alloc(length, 1), 0, (int) length);
    }

    public static Int8List fromList(Iterable<?> elements) {
        int n = count(elements);
        return fill(new Int8List(n), elements);
    }

    public static Int8List view(ByteBuffer buffer) {
        return view(buffer, 0, null);
    }

    public static Int8List view(ByteBuffer buffer, long offsetInBytes) {
        return view(buffer, offsetInBytes, null);
    }

    public static Int8List view(ByteBuffer buffer, long offsetInBytes, Long length) {
        int n = Bytes.viewLength(buffer, offsetInBytes, length, 1);
        return new Int8List(buffer.bytes(), (int) offsetInBytes, n);
    }

    @Override
    public Int8List sublist(long start, long end) {
        return (Int8List) super.sublist(start, end);
    }

    @Override
    public Int8List sublist(long start) {
        return sublist(start, len);
    }

    @Override
    int elementSize() {
        return 1;
    }

    @Override
    TypedIntList make(byte[] buf, int off, int len) {
        return new Int8List(buf, off, len);
    }

    @Override
    long load(int b) {
        return Bytes.signExtend(Bytes.read(buf, b, 1, true), 1);
    }

    @Override
    void store(int b, long v) {
        Bytes.write(buf, b, 1, true, v);
    }
}
