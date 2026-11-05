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

/// Int32x4List, Float32x4List and Float64x2List: a fixed-length list of 16-byte
/// values over a {@link ByteBuffer}'s bytes, little endian. An element is decoded
/// on every read and encoded on every write, so views of the same buffer see each
/// other's stores.
public abstract class SimdList<E> extends dart.core.DartList<E> implements TypedDataList<E> {

    public static final long bytesPerElement = 16;

    final byte[] buf;
    final int off;
    final int len;

    SimdList(byte[] buf, int off, int len) {
        super(false, false);
        this.buf = buf;
        this.off = off;
        this.len = len;
    }

    abstract E load(int byteIndex);

    abstract void store(int byteIndex, E value);

    abstract SimdList<E> make(byte[] buf, int off, int len);

    static byte[] alloc(long length) {
        dart.core.RangeError.checkNotNegative(length, "length");
        dart.core.RangeError.checkAllocatable(length * 16);
        return new byte[(int) length * 16];
    }

    final int byteIndex(long index) {
        if (index < 0 || index >= len) {
            dart.core.RangeError.indexError(index, len);
        }
        return off + (int) index * 16;
    }

    static int viewLength(ByteBuffer buffer, long offsetInBytes, Long length) {
        return Bytes.viewLength(buffer, offsetInBytes, length, 16);
    }

    static <E> SimdList<E> fill(SimdList<E> l, Iterable<?> elements) {
        int i = 0;
        for (Object o : elements) {
            @SuppressWarnings("unchecked")
            E e = (E) o;
            l.store(l.byteIndex(i++), e);
        }
        return l;
    }

    static int count(Iterable<?> elements) {
        int n = 0;
        for (Object o : elements) {
            n++;
        }
        return n;
    }

    int readInt(int b) {
        return (int) Bytes.read(buf, b, 4, true);
    }

    void writeInt(int b, int v) {
        Bytes.write(buf, b, 4, true, v);
    }

    @Override
    public ByteBuffer buffer() {
        return new ByteBuffer(buf);
    }

    @Override
    public long length() {
        return len;
    }

    @Override
    public long lengthInBytes() {
        return (long) len * 16;
    }

    @Override
    public long offsetInBytes() {
        return off;
    }

    @Override
    public long elementSizeInBytes() {
        return 16;
    }

    @Override
    public int size() {
        return len;
    }

    @Override
    public E get(int index) {
        return load(byteIndex(index));
    }

    @Override
    public E set(int index, E element) {
        int b = byteIndex(index);
        E old = load(b);
        store(b, element);
        return old;
    }

    public E $index(long index) {
        return load(byteIndex(index));
    }

    public void $indexSet(long index, E value) {
        store(byteIndex(index), value);
    }

    @Override
    public boolean add(E e) {
        throw new dart.core.UnsupportedError("Cannot add to a fixed-length list");
    }

    @Override
    public void add(int index, E element) {
        throw new dart.core.UnsupportedError("Cannot add to a fixed-length list");
    }

    @Override
    public E remove(int index) {
        throw new dart.core.UnsupportedError("Cannot remove from a fixed-length list");
    }

    @Override
    public void setRange(long start, long end, Iterable<? extends E> from) {
        dart.core.CoreExt.setRange(this, start, end, from);
    }

    @Override
    public void setRange(long start, long end, Iterable<? extends E> from, long skipCount) {
        dart.core.CoreExt.setRange(this, start, end, from, skipCount);
    }

    @Override
    public void fillRange(long start, long end) {
        throw new dart.core.ArgumentError("fillRange on a SIMD list needs a value");
    }

    @Override
    public void fillRange(long start, long end, E value) {
        dart.core.CoreExt.fillRange(this, start, end, value);
    }

    @Override
    public SimdList<E> sublist(long start, long end) {
        dart.core.RangeError.checkValueInInterval(start, 0, len, "start");
        dart.core.RangeError.checkValueInInterval(end, start, len, "end");
        byte[] copy = new byte[(int) (end - start) * 16];
        System.arraycopy(buf, off + (int) start * 16, copy, 0, copy.length);
        return make(copy, 0, (int) (end - start));
    }

    @Override
    public SimdList<E> sublist(long start) {
        return sublist(start, len);
    }
}
