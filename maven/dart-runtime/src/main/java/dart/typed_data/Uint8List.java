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

    public Uint8List(long length) {
        // Checked on the long, before narrowing, as ByteData and the lists are:
        // Uint8List(2^32) cast to an empty list instead of failing.
        dart.core.RangeError.checkNotNegative(length, "length");
        dart.core.RangeError.checkAllocatable(length);
        this.bytes = new byte[(int) length];
    }

    private Uint8List(byte[] bytes) {
        this.bytes = bytes;
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
            return fromBytes(((Uint8List) source).bytes);
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

    public long length() {
        return bytes.length;
    }

    /** {@code bytes[i]}: the unsigned byte, as Dart reads a Uint8List. */
    public long $index(long index) {
        dart.core.RangeError.checkValidIndex(index, bytes.length);
        return bytes[(int) index] & 0xff;
    }

    /// The index operator as the transpiler spells it on a receiver it types as a list.
    public long idx(long index) {
        return $index(index);
    }

    public void $indexSet(long index, long value) {
        dart.core.RangeError.checkValidIndex(index, bytes.length);
        bytes[(int) index] = (byte) value;
    }

    public dart.core.DartList<Long> toList() {
        dart.core.DartList<Long> out = new dart.core.DartList<Long>();
        for (byte b : bytes) {
            out.add(Long.valueOf(b & 0xff));
        }
        return out;
    }

    public Uint8List sublist(long start) {
        return sublist(start, null);
    }

    public Uint8List sublist(long start, Long end) {
        int e = end == null ? bytes.length : (int) end.longValue();
        if (start < 0 || start > e || e > bytes.length) {
            throw new dart.core.RangeError("Invalid range " + start + ".." + e);
        }
        byte[] out = new byte[e - (int) start];
        System.arraycopy(bytes, (int) start, out, 0, out.length);
        return new Uint8List(out);
    }

    public boolean isEmpty() {
        return bytes.length == 0;
    }

    public boolean isNotEmpty() {
        return bytes.length != 0;
    }

    /** The raw backing array (used by image decoders). */
    public byte[] toBytes() {
        return bytes;
    }

    /// `[1, 2, 3]`, as Dart prints any list; it printed the Java object id.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(bytes[i] & 0xff);
        }
        return sb.append(']').toString();
    }

    public ByteBuffer buffer() {
        return new ByteBuffer(bytes);
    }

    public long lengthInBytes() {
        return bytes.length;
    }

    public long offsetInBytes() {
        return 0;
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

    /// A view over the whole buffer shares its bytes. This class keeps no offset, so a
    /// view of part of the buffer is a copy of that part: writes through it do not reach
    /// the buffer. Use ByteData.view or another typed list for a shared partial view.
    public static Uint8List view(ByteBuffer buffer, long offsetInBytes, Long length) {
        int n = Bytes.viewLength(buffer, offsetInBytes, length, 1);
        byte[] b = buffer.bytes();
        if (offsetInBytes == 0 && n == b.length) {
            return new Uint8List(b);
        }
        byte[] c = new byte[n];
        System.arraycopy(b, (int) offsetInBytes, c, 0, n);
        return new Uint8List(c);
    }

    public void setRange(long start, long end, Object from) {
        setRange(start, end, from, 0);
    }

    public void setRange(long start, long end, Object from, long skipCount) {
        dart.core.RangeError.checkValueInInterval(start, 0, bytes.length, "start");
        dart.core.RangeError.checkValueInInterval(end, start, bytes.length, "end");
        if (from instanceof Uint8List) {
            byte[] src = ((Uint8List) from).bytes;
            if (skipCount + (end - start) > src.length) {
                throw new dart.core.StateError("Too few elements");
            }
            System.arraycopy(src, (int) skipCount, bytes, (int) start, (int) (end - start));
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
            bytes[(int) i] = (byte) ((Number) it.next()).longValue();
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
        dart.core.RangeError.checkValueInInterval(start, 0, bytes.length, "start");
        dart.core.RangeError.checkValueInInterval(end, start, bytes.length, "end");
        java.util.Arrays.fill(bytes, (int) start, (int) end, (byte) (value == null ? 0 : value.longValue()));
    }
}
