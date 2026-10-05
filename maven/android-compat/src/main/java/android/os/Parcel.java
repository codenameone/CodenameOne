/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package android.os;

import java.util.ArrayList;
import java.util.List;

/// An in-memory parcel: one list slot per written value. Positions count slots
/// rather than bytes, so `dataPosition()` / `setDataPosition()` are consistent
/// with each other but not with Android's byte offsets. As on Android, reads
/// and writes share one position: rewind with `setDataPosition(0)` before
/// reading back what was written.
public final class Parcel {

    private final ArrayList<Object> values = new ArrayList<Object>();
    private int pos;

    private Parcel() {
    }

    public static Parcel obtain() {
        return new Parcel();
    }

    public void recycle() {
        values.clear();
        pos = 0;
    }

    public int dataSize() {
        return values.size();
    }

    public int dataPosition() {
        return pos;
    }

    public void setDataPosition(int p) {
        pos = p;
    }

    /// Writes at the current position and advances it, overwriting a value
    /// already there: a Parcelable that reserves a size slot and seeks back to
    /// fill it in must replace the slot, not append after the payload. Seeking
    /// past the end pads with nulls, as Android pads with zeros.
    private void put(Object v) {
        while (values.size() < pos) {
            values.add(null);
        }
        if (pos < values.size()) {
            values.set(pos, v);
        } else {
            values.add(v);
        }
        pos++;
    }

    private Object next() {
        return pos < values.size() ? values.get(pos++) : null;
    }

    public void writeInt(int v) {
        put(Integer.valueOf(v));
    }

    public int readInt() {
        Object o = next();
        return o instanceof Integer ? ((Integer) o).intValue() : 0;
    }

    public void writeLong(long v) {
        put(Long.valueOf(v));
    }

    public long readLong() {
        Object o = next();
        return o instanceof Long ? ((Long) o).longValue() : 0;
    }

    public void writeFloat(float v) {
        put(Float.valueOf(v));
    }

    public float readFloat() {
        Object o = next();
        return o instanceof Float ? ((Float) o).floatValue() : 0;
    }

    public void writeDouble(double v) {
        put(Double.valueOf(v));
    }

    public double readDouble() {
        Object o = next();
        return o instanceof Double ? ((Double) o).doubleValue() : 0;
    }

    public void writeByte(byte v) {
        put(Byte.valueOf(v));
    }

    public byte readByte() {
        Object o = next();
        return o instanceof Byte ? ((Byte) o).byteValue() : 0;
    }

    public void writeString(String v) {
        put(v);
    }

    public String readString() {
        Object o = next();
        return o instanceof String ? (String) o : null;
    }

    public void writeBoolean(boolean v) {
        put(Boolean.valueOf(v));
    }

    public boolean readBoolean() {
        Object o = next();
        return o instanceof Boolean && ((Boolean) o).booleanValue();
    }

    public void writeValue(Object v) {
        put(v);
    }

    public Object readValue(ClassLoader loader) {
        return next();
    }

    public void writeBundle(Bundle b) {
        put(b);
    }

    public Bundle readBundle() {
        Object o = next();
        return o instanceof Bundle ? (Bundle) o : null;
    }

    public Bundle readBundle(ClassLoader loader) {
        return readBundle();
    }

    public void writeParcelable(Parcelable p, int flags) {
        put(p);
    }

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> T readParcelable(ClassLoader loader) {
        Object o = next();
        return o instanceof Parcelable ? (T) o : null;
    }

    public void writeStringList(List<String> list) {
        put(list == null ? null : new ArrayList<String>(list));
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> createStringArrayList() {
        Object o = next();
        return o instanceof ArrayList ? (ArrayList<String>) o : null;
    }

    public void readStringList(List<String> out) {
        ArrayList<String> l = createStringArrayList();
        if (l != null) {
            out.addAll(l);
        }
    }

    public void writeList(List<?> list) {
        put(list == null ? null : new ArrayList<Object>(list));
    }

    @SuppressWarnings("unchecked")
    public void readList(List out, ClassLoader loader) {
        Object o = next();
        if (o instanceof List) {
            out.addAll((List) o);
        }
    }

    public <T extends Parcelable> void writeTypedList(List<T> list) {
        writeList(list);
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> createTypedArrayList(Parcelable.Creator<T> c) {
        Object o = next();
        return o instanceof ArrayList ? (ArrayList<T>) o : null;
    }

    public void writeIntArray(int[] v) {
        put(v == null ? null : v.clone());
    }

    public int[] createIntArray() {
        Object o = next();
        return o instanceof int[] ? (int[]) o : null;
    }

    public void writeStringArray(String[] v) {
        put(v == null ? null : v.clone());
    }

    public String[] createStringArray() {
        Object o = next();
        return o instanceof String[] ? (String[]) o : null;
    }

    public void writeByteArray(byte[] v) {
        put(v == null ? null : v.clone());
    }

    public byte[] createByteArray() {
        Object o = next();
        return o instanceof byte[] ? (byte[]) o : null;
    }

    public void writeSerializable(Object s) {
        put(s);
    }

    public Object readSerializable() {
        return next();
    }
}
