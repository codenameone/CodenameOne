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

/// Float32List and Float64List: a fixed-length {@code List<double>} over a
/// {@link ByteBuffer}'s bytes, little endian.
public abstract class TypedFloatList extends dart.core.DartDoubleList implements TypedDataList<Double> {

    final byte[] buf;
    final int off;
    final int len;

    TypedFloatList(byte[] buf, int off, int len) {
        super(true);
        this.buf = buf;
        this.off = off;
        this.len = len;
    }

    abstract int elementSize();

    abstract TypedFloatList make(byte[] buf, int off, int len);

    final int byteIndex(long index) {
        if (index < 0 || index >= len) {
            dart.core.RangeError.indexError(index, len);
        }
        return off + (int) index * elementSize();
    }

    static byte[] alloc(long length, int elementSize) {
        dart.core.RangeError.checkNotNegative(length, "length");
        dart.core.RangeError.checkAllocatable(length * elementSize);
        return new byte[(int) length * elementSize];
    }

    public ByteBuffer buffer() {
        return new ByteBuffer(buf);
    }

    public long lengthInBytes() {
        return (long) len * elementSize();
    }

    public long offsetInBytes() {
        return off;
    }

    public long elementSizeInBytes() {
        return elementSize();
    }

    public void setRange(long start, long end, Iterable<? extends Double> from) {
        dart.core.CoreExt.setRange(this, start, end, from, 0);
    }

    public void setRange(long start, long end, Iterable<? extends Double> from, long skipCount) {
        dart.core.CoreExt.setRange(this, start, end, from, skipCount);
    }

    public void setAll(long index, Iterable<? extends Double> from) {
        dart.core.CoreExt.setAll(this, index, from);
    }

    public void fillRange(long start, long end) {
        dart.core.CoreExt.fillRange(this, start, end, (Double) (0.0));
    }

    public void fillRange(long start, long end, Double value) {
        dart.core.CoreExt.fillRange(this, start, end, value == null ? (0.0) : value);
    }

    @Override
    public int size() {
        return len;
    }

    @Override
    public void add(int index, Double element) {
        throw new dart.core.UnsupportedError("Cannot add to a fixed-length list");
    }

    @Override
    public boolean add(Double e) {
        throw new dart.core.UnsupportedError("Cannot add to a fixed-length list");
    }

    @Override
    public Double remove(int index) {
        throw new dart.core.UnsupportedError("Cannot remove from a fixed-length list");
    }

    @Override
    public TypedFloatList sublist(long start, long end) {
        dart.core.RangeError.checkValueInInterval(start, 0, len, "start");
        dart.core.RangeError.checkValueInInterval(end, start, len, "end");
        int es = elementSize();
        byte[] copy = new byte[(int) (end - start) * es];
        System.arraycopy(buf, off + (int) start * es, copy, 0, copy.length);
        return make(copy, 0, (int) (end - start));
    }

    @Override
    public TypedFloatList sublist(long start) {
        return sublist(start, len);
    }

    public Double $index(long index) {
        return getDouble(index);
    }

    public void $indexSet(long index, double value) {
        setDouble(index, value);
    }

    abstract double load(int byteIndex);

    abstract void store(int byteIndex, double value);

    @Override
    public double getDouble(long index) {
        return load(byteIndex(index));
    }

    @Override
    public double setDouble(long index, double value) {
        store(byteIndex(index), value);
        return value;
    }

    @Override
    public boolean addDouble(double value) {
        throw new dart.core.UnsupportedError("Cannot add to a fixed-length list");
    }

    @Override
    public Double get(int index) {
        return getDouble(index);
    }

    @Override
    public Double set(int index, Double element) {
        int b = byteIndex(index);
        double old = load(b);
        store(b, element);
        return old;
    }
}
