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
 * 8-bit integers over a Java {@code byte[]}. It is a {@link TypedIntList}, and so a
 * {@code List<int>}, like every other integer typed list: an extension declared on
 * {@code List<int>} or {@code Iterable<int>} applies to it, as it does in Dart.
 */
public final class Uint8List extends TypedIntList {

    public static final long BYTES_PER_ELEMENT = 1;

    /// A window of {@code len} bytes of {@code bytes} at {@code off}, shared, not
    /// copied: writes through it reach the buffer and the buffer's reach it, as Dart's
    /// views do.
    Uint8List(byte[] bytes, int off, int len) {
        super(bytes, off, len);
    }

    public Uint8List(long length) {
        // Checked on the long, before narrowing, as ByteData and the lists are:
        // Uint8List(2^32) cast to an empty list instead of failing.
        this(alloc(length, 1), 0, (int) length);
    }

    private Uint8List(byte[] bytes) {
        this(bytes, 0, bytes.length);
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
        if (!(source instanceof List)) {
            throw new dart.core.TypeError("type '" + (source == null ? "Null" : source.getClass().getName())
                    + "' is not a subtype of type 'List<int>'");
        }
        return fromList((List<?>) source);
    }

    /** {@code Uint8List.fromList(<int>[...])}. */
    public static Uint8List fromList(List<?> elements) {
        if (elements instanceof Uint8List) {
            return new Uint8List(((Uint8List) elements).copyBytes());
        }
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
        System.arraycopy(buf, off, c, 0, len);
        return c;
    }

    @Override
    public Uint8List sublist(long start) {
        return sublist(start, len);
    }

    @Override
    public Uint8List sublist(long start, long end) {
        return (Uint8List) super.sublist(start, end);
    }

    public Uint8List sublist(long start, Long end) {
        return sublist(start, end == null ? len : end.longValue());
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

    /// The bytes as a Java array (used by image decoders and platform channels): the
    /// backing array itself when this list covers all of it, else a copy of the window.
    public byte[] toBytes() {
        return off == 0 && len == buf.length ? buf : copyBytes();
    }

    byte[] storage() {
        return buf;
    }

    int offset() {
        return off;
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

    /// {@code setRange} with a source the transpiler typed only dynamic.
    @SuppressWarnings("unchecked")
    public void setRange(long start, long end, Object from) {
        setRange(start, end, (Iterable<? extends Long>) from, 0);
    }

    @SuppressWarnings("unchecked")
    public void setRange(long start, long end, Object from, long skipCount) {
        setRange(start, end, (Iterable<? extends Long>) from, skipCount);
    }

    @Override
    public void setRange(long start, long end, Iterable<? extends Long> from, long skipCount) {
        if (from instanceof Uint8List) {
            dart.core.RangeError.checkValueInInterval(start, 0, len, "start");
            dart.core.RangeError.checkValueInInterval(end, start, len, "end");
            Uint8List f = (Uint8List) from;
            if (skipCount + (end - start) > f.len) {
                throw new dart.core.StateError("Too few elements");
            }
            // arraycopy handles an overlapping source as memmove, which is what Dart does
            System.arraycopy(f.buf, f.off + (int) skipCount, buf, off + (int) start, (int) (end - start));
            return;
        }
        super.setRange(start, end, from, skipCount);
    }

    @SuppressWarnings("unchecked")
    public void setAll(long index, Object from) {
        setAll(index, (Iterable<? extends Long>) from);
    }

    @Override
    int elementSize() {
        return 1;
    }

    @Override
    TypedIntList make(byte[] buf, int off, int len) {
        return new Uint8List(buf, off, len);
    }

    @Override
    long load(int b) {
        return buf[b] & 0xff;
    }

    @Override
    void store(int b, long v) {
        buf[b] = (byte) v;
    }
}
