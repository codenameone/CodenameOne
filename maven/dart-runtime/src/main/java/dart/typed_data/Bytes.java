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

/// Raw multi-byte loads and stores shared by ByteData and the typed lists.
final class Bytes {
    private Bytes() {
    }

    static long read(byte[] b, int o, int width, boolean little) {
        long v = 0;
        for (int i = 0; i < width; i++) {
            int idx = little ? o + width - 1 - i : o + i;
            v = (v << 8) | (b[idx] & 0xFFL);
        }
        return v;
    }

    static void write(byte[] b, int o, int width, boolean little, long v) {
        for (int i = 0; i < width; i++) {
            int idx = little ? o + i : o + width - 1 - i;
            b[idx] = (byte) v;
            v >>= 8;
        }
    }

    static long signExtend(long v, int width) {
        int shift = 64 - width * 8;
        return shift == 0 ? v : (v << shift) >> shift;
    }

    /// The view's byte range, checked as Dart checks a view constructor.
    static int viewLength(ByteBuffer buffer, long offsetInBytes, Long length, int elementSize) {
        long total = buffer.lengthInBytes();
        if (offsetInBytes < 0 || offsetInBytes > total) {
            throw new dart.core.RangeError("Invalid value: Not in inclusive range 0.." + total
                    + ": " + offsetInBytes + " (offsetInBytes)");
        }
        if (offsetInBytes % elementSize != 0) {
            throw new dart.core.ArgumentError("The offset in bytes must be a multiple of " + elementSize);
        }
        long n = length != null ? length.longValue() : (total - offsetInBytes) / elementSize;
        if (n < 0 || offsetInBytes + n * elementSize > total) {
            throw new dart.core.RangeError("Invalid value: Not in inclusive range 0.."
                    + ((total - offsetInBytes) / elementSize) + ": " + n + " (length)");
        }
        if (length == null && (total - offsetInBytes) % elementSize != 0) {
            throw new dart.core.ArgumentError("The length in bytes must be a multiple of " + elementSize);
        }
        return (int) n;
    }

    /// The backing array of any typed list or ByteData.
    static byte[] storage(Object data) {
        if (data instanceof Uint8List) {
            return ((Uint8List) data).storage();
        }
        if (data instanceof TypedIntList) {
            return ((TypedIntList) data).buf;
        }
        if (data instanceof TypedFloatList) {
            return ((TypedFloatList) data).buf;
        }
        if (data instanceof ByteData) {
            return ((ByteData) data).buffer().bytes();
        }
        throw new dart.core.ArgumentError("Not a TypedData: " + data);
    }

    /// {@code X.sublistView(data, start, end)} for a 1-byte target: the byte offset
    /// into the storage and the byte length, {@code start}/{@code end} counted in
    /// elements of {@code data} and checked as Dart checks them.
    static long[] sublistWindow(Object data, long start, Long end) {
        long es;
        long count;
        long base;
        if (data instanceof Uint8List) {
            Uint8List u = (Uint8List) data;
            es = 1;
            count = u.length();
            base = u.offsetInBytes();
        } else if (data instanceof TypedIntList) {
            TypedIntList t = (TypedIntList) data;
            es = t.elementSize();
            count = t.len;
            base = t.off;
        } else if (data instanceof TypedFloatList) {
            TypedFloatList t = (TypedFloatList) data;
            es = t.elementSizeInBytes();
            count = t.len;
            base = t.off;
        } else if (data instanceof ByteData) {
            ByteData d = (ByteData) data;
            es = 1;
            count = d.lengthInBytes();
            base = d.offsetInBytes();
        } else {
            throw new dart.core.ArgumentError("Not a TypedData: " + data);
        }
        long e = end == null ? count : end.longValue();
        dart.core.RangeError.checkValueInInterval(start, 0, count, "start");
        dart.core.RangeError.checkValueInInterval(e, start, count, "end");
        return new long[] {base + start * es, (e - start) * es};
    }
}
