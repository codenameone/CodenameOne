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

import java.util.List;

/**
 * Dart's {@code dart:typed_data} Uint8List: a fixed-length list of unsigned
 * 8-bit integers backed by a Java {@code byte[]}. Only the surface used by
 * the Flutter gallery (construction + length) is implemented.
 */
public final class Uint8List {

    private final byte[] bytes;
    private final int off;
    private final int len;

    public Uint8List(long length) {
        // Checked on the long, before narrowing, as ByteData and the lists are:
        // Uint8List(2^32) cast to an empty list instead of failing.
        dart.core.RangeError.checkNotNegative(length, "length");
        dart.core.RangeError.checkAllocatable(length);
        this.bytes = new byte[(int) length];
        this.off = 0;
        this.len = bytes.length;
    }

    private Uint8List(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /// A window of {@code len} bytes of {@code bytes} at {@code off}, shared, not
    /// copied: writes through it reach the buffer and the buffer's reach it, as Dart's
    /// views do.
    private Uint8List(byte[] bytes, int off, int len) {
        this.bytes = bytes;
        this.off = off;
        this.len = len;
    }

    /// A Uint8List over {@code bytes} itself, not a copy: a ByteBuffer's view.
    static Uint8List view(byte[] bytes) {
        return new Uint8List(bytes);
    }

    /// A Uint8List holding a copy of {@code bytes}: how a Java byte buffer crosses into
    /// Dart (a platform channel's binary result, as Flutter's codec delivers it).
    public static Uint8List fromBytes(byte[] bytes) {
        return new Uint8List(bytes == null ? new byte[0] : bytes.clone());
    }

    /** {@code Uint8List.fromList(source)} with a source typed only dynamic. */
    public static Uint8List fromList(Object source) {
        if (source instanceof Uint8List) {
            // a Uint8List IS a List<int> in Dart (utf8.encode returns one), but not a java.util.List here
            return new Uint8List(((Uint8List) source).copyBytes());
        }
        if (!(source instanceof List)) {
            throw new dart.core.TypeError("type '" + (source == null ? "Null" : source.getClass().getName())
                    + "' is not a subtype of type 'List<int>'");
        }
        return fromList((List<?>) source);
    }

    /** {@code Uint8List.fromList(<int>[...])}. */
    public static Uint8List fromList(List<?> elements) {
        byte[] b = new byte[elements.size()];
        for (int i = 0; i < b.length; i++) {
            Object o = elements.get(i);
            // The List<int> contract is checked per element, as Dart does: a string
            // became byte zero and a double was narrowed, where Dart throws TypeError.
            if (!(o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte)) {
                throw new dart.core.TypeError("type '" + (o == null ? "Null" : o instanceof Number ? "double"
                        : o.getClass().getSimpleName()) + "' is not a subtype of type 'int'");
            }
            b[i] = ((Number) o).byteValue();
        }
        return new Uint8List(b);
    }

    private byte[] copyBytes() {
        byte[] c = new byte[len];
        System.arraycopy(bytes, off, c, 0, len);
        return c;
    }

    public long length() {
        return len;
    }

    /** {@code bytes[i]}: the unsigned byte, as Dart reads a Uint8List. */
    public long $index(long index) {
        dart.core.RangeError.checkValidIndex(index, len);
        return bytes[off + (int) index] & 0xff;
    }

    /// The index operator as the transpiler spells it on a receiver it types as a list.
    public long idx(long index) {
        return $index(index);
    }

    /// The index assignment as the transpiler spells it on a receiver it types as a list.
    public void idxSet(long index, long value) {
        $indexSet(index, value);
    }

    public void $indexSet(long index, long value) {
        dart.core.RangeError.checkValidIndex(index, len);
        bytes[off + (int) index] = (byte) value;
    }

    public dart.core.DartList<Long> toList() {
        dart.core.DartList<Long> out = new dart.core.DartList<Long>();
        for (int i = 0; i < len; i++) {
            out.add(Long.valueOf(bytes[off + i] & 0xff));
        }
        return out;
    }

    public Uint8List sublist(long start) {
        return sublist(start, null);
    }

    public Uint8List sublist(long start, Long end) {
        int e = end == null ? len : (int) end.longValue();
        if (start < 0 || start > e || e > len) {
            throw new dart.core.RangeError("Invalid range " + start + ".." + e);
        }
        byte[] out = new byte[e - (int) start];
        System.arraycopy(bytes, off + (int) start, out, 0, out.length);
        return new Uint8List(out);
    }

    /// {@code Uint8List.sublistView(data, start, end)}: a view sharing the storage of
    /// any typed list or ByteData, element offsets counted in bytes of {@code data}.
    public static Uint8List sublistView(Object data) {
        return sublistView(data, 0, null);
    }

    public static Uint8List sublistView(Object data, long start) {
        return sublistView(data, start, null);
    }

    public static Uint8List sublistView(Object data, long start, Long end) {
        long[] w = Bytes.sublistWindow(data, start, end);
        return new Uint8List(Bytes.storage(data), (int) w[0], (int) w[1]);
    }

    public boolean isEmpty() {
        return len == 0;
    }

    public boolean isNotEmpty() {
        return len != 0;
    }

    /// The bytes as a Java array (used by image decoders and platform channels): the
    /// backing array itself when this list covers all of it, else a copy of the window.
    public byte[] toBytes() {
        return off == 0 && len == bytes.length ? bytes : copyBytes();
    }

    byte[] storage() {
        return bytes;
    }

    /// `[1, 2, 3]`, as Dart prints any list; it printed the Java object id.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(bytes[off + i] & 0xff);
        }
        return sb.append(']').toString();
    }

    public ByteBuffer buffer() {
        return new ByteBuffer(bytes);
    }

    public long lengthInBytes() {
        return len;
    }

    public long offsetInBytes() {
        return off;
    }

    public long elementSizeInBytes() {
        return 1;
    }

    public static Uint8List view(ByteBuffer buffer) {
        return view(buffer, 0, null);
    }

    public static Uint8List view(ByteBuffer buffer, long offsetInBytes) {
        return view(buffer, offsetInBytes, null);
    }

    /// A view of part of the buffer shares its bytes, as Dart's does.
    public static Uint8List view(ByteBuffer buffer, long offsetInBytes, Long length) {
        int n = Bytes.viewLength(buffer, offsetInBytes, length, 1);
        return new Uint8List(buffer.bytes(), (int) offsetInBytes, n);
    }

    public void setRange(long start, long end, Object from) {
        setRange(start, end, from, 0);
    }

    public void setRange(long start, long end, Object from, long skipCount) {
        dart.core.RangeError.checkValueInInterval(start, 0, len, "start");
        dart.core.RangeError.checkValueInInterval(end, start, len, "end");
        if (from instanceof Uint8List) {
            Uint8List f = (Uint8List) from;
            if (skipCount + (end - start) > f.len) {
                throw new dart.core.StateError("Too few elements");
            }
            // arraycopy handles an overlapping source as memmove, which is what Dart does
            System.arraycopy(f.bytes, f.off + (int) skipCount, bytes, off + (int) start, (int) (end - start));
            return;
        }
        java.util.Iterator<?> it = ((Iterable<?>) from).iterator();
        for (long i = 0; i < skipCount && it.hasNext(); i++) {
            it.next();
        }
        for (long i = start; i < end; i++) {
            if (!it.hasNext()) {
                throw new dart.core.StateError("Too few elements");
            }
            bytes[off + (int) i] = (byte) ((Number) it.next()).longValue();
        }
    }

    public void setAll(long index, Object from) {
        int i = (int) index;
        for (Object o : (from instanceof Uint8List ? ((Uint8List) from).toList() : (Iterable<?>) from)) {
            $indexSet(i++, ((Number) o).longValue());
        }
    }

    public void fillRange(long start, long end) {
        fillRange(start, end, null);
    }

    public void fillRange(long start, long end, Long value) {
        dart.core.RangeError.checkValueInInterval(start, 0, len, "start");
        dart.core.RangeError.checkValueInInterval(end, start, len, "end");
        java.util.Arrays.fill(bytes, off + (int) start, off + (int) end, (byte) (value == null ? 0 : value.longValue()));
    }
}
