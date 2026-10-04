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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/// A string-keyed map of values passed between activities and saved as
/// state. In-process only: values are stored by reference.
public final class Bundle implements Parcelable, Cloneable {

    public static final Bundle EMPTY = new Bundle();

    private final HashMap<String, Object> map = new HashMap<String, Object>();

    public Bundle() {
    }

    public Bundle(int capacity) {
    }

    public Bundle(Bundle b) {
        if (b != null) {
            map.putAll(b.map);
        }
    }

    public Bundle(ClassLoader loader) {
    }

    public void setClassLoader(ClassLoader loader) {
    }

    @Override
    public Object clone() {
        return new Bundle(this);
    }

    /// A copy that shares no mutable container with this bundle: nested
    /// bundles are deep copied, lists and arrays are copied (a list's bundle
    /// elements deep copied too). Other values -- strings, boxed numbers,
    /// parcelables -- are shared, as on Android.
    public Bundle deepCopy() {
        Bundle out = new Bundle();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            out.map.put(e.getKey(), deepCopyValue(e.getValue()));
        }
        return out;
    }

    private static Object deepCopyValue(Object v) {
        if (v instanceof Bundle) {
            return ((Bundle) v).deepCopy();
        }
        if (v instanceof ArrayList) {
            ArrayList<?> src = (ArrayList<?>) v;
            ArrayList<Object> copy = new ArrayList<Object>(src.size());
            for (int i = 0; i < src.size(); i++) {
                copy.add(deepCopyValue(src.get(i)));
            }
            return copy;
        }
        // Array clone() keeps the runtime type (a String[] stays a String[])
        // on every target; only Object.clone() of a non-array is unavailable.
        if (v instanceof Object[]) {
            return ((Object[]) v).clone();
        }
        if (v instanceof int[]) {
            return ((int[]) v).clone();
        }
        if (v instanceof long[]) {
            return ((long[]) v).clone();
        }
        if (v instanceof float[]) {
            return ((float[]) v).clone();
        }
        if (v instanceof double[]) {
            return ((double[]) v).clone();
        }
        if (v instanceof boolean[]) {
            return ((boolean[]) v).clone();
        }
        if (v instanceof byte[]) {
            return ((byte[]) v).clone();
        }
        if (v instanceof short[]) {
            return ((short[]) v).clone();
        }
        if (v instanceof char[]) {
            return ((char[]) v).clone();
        }
        return v;
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public Object get(String key) {
        return map.get(key);
    }

    public void remove(String key) {
        map.remove(key);
    }

    public void putAll(Bundle b) {
        map.putAll(b.map);
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    // ------------------------------------------------------------ put

    public void putBoolean(String key, boolean v) {
        map.put(key, Boolean.valueOf(v));
    }

    public void putByte(String key, byte v) {
        map.put(key, Byte.valueOf(v));
    }

    public void putChar(String key, char v) {
        map.put(key, Character.valueOf(v));
    }

    public void putShort(String key, short v) {
        map.put(key, Short.valueOf(v));
    }

    public void putInt(String key, int v) {
        map.put(key, Integer.valueOf(v));
    }

    public void putLong(String key, long v) {
        map.put(key, Long.valueOf(v));
    }

    public void putFloat(String key, float v) {
        map.put(key, Float.valueOf(v));
    }

    public void putDouble(String key, double v) {
        map.put(key, Double.valueOf(v));
    }

    public void putString(String key, String v) {
        map.put(key, v);
    }

    public void putCharSequence(String key, CharSequence v) {
        map.put(key, v);
    }

    public void putParcelable(String key, Parcelable v) {
        map.put(key, v);
    }

    public void putSerializable(String key, java.io.Serializable v) {
        map.put(key, v);
    }

    public void putBundle(String key, Bundle v) {
        map.put(key, v);
    }

    public void putIntArray(String key, int[] v) {
        map.put(key, v);
    }

    public void putLongArray(String key, long[] v) {
        map.put(key, v);
    }

    public void putFloatArray(String key, float[] v) {
        map.put(key, v);
    }

    public void putDoubleArray(String key, double[] v) {
        map.put(key, v);
    }

    public void putBooleanArray(String key, boolean[] v) {
        map.put(key, v);
    }

    public void putByteArray(String key, byte[] v) {
        map.put(key, v);
    }

    public void putCharArray(String key, char[] v) {
        map.put(key, v);
    }

    public void putStringArray(String key, String[] v) {
        map.put(key, v);
    }

    public void putCharSequenceArray(String key, CharSequence[] v) {
        map.put(key, v);
    }

    public void putParcelableArray(String key, Parcelable[] v) {
        map.put(key, v);
    }

    public void putStringArrayList(String key, ArrayList<String> v) {
        map.put(key, v);
    }

    public void putIntegerArrayList(String key, ArrayList<Integer> v) {
        map.put(key, v);
    }

    public void putCharSequenceArrayList(String key, ArrayList<CharSequence> v) {
        map.put(key, v);
    }

    public void putParcelableArrayList(String key, ArrayList<? extends Parcelable> v) {
        map.put(key, v);
    }

    public void putSparseParcelableArray(String key, android.util.SparseArray<? extends Parcelable> v) {
        map.put(key, v);
    }

    // ------------------------------------------------------------ get

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean def) {
        Object o = map.get(key);
        return o instanceof Boolean ? ((Boolean) o).booleanValue() : def;
    }

    public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }

    public Byte getByte(String key, byte def) {
        Object o = map.get(key);
        return o instanceof Byte ? (Byte) o : Byte.valueOf(def);
    }

    public char getChar(String key) {
        return getChar(key, (char) 0);
    }

    public char getChar(String key, char def) {
        Object o = map.get(key);
        return o instanceof Character ? ((Character) o).charValue() : def;
    }

    public short getShort(String key) {
        return getShort(key, (short) 0);
    }

    public short getShort(String key, short def) {
        Object o = map.get(key);
        return o instanceof Short ? ((Short) o).shortValue() : def;
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int def) {
        Object o = map.get(key);
        return o instanceof Integer ? ((Integer) o).intValue() : def;
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long def) {
        Object o = map.get(key);
        return o instanceof Long ? ((Long) o).longValue() : def;
    }

    public float getFloat(String key) {
        return getFloat(key, 0f);
    }

    public float getFloat(String key, float def) {
        Object o = map.get(key);
        return o instanceof Float ? ((Float) o).floatValue() : def;
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double def) {
        Object o = map.get(key);
        return o instanceof Double ? ((Double) o).doubleValue() : def;
    }

    public String getString(String key) {
        Object o = map.get(key);
        return o instanceof String ? (String) o : null;
    }

    public String getString(String key, String def) {
        String s = getString(key);
        return s == null ? def : s;
    }

    public CharSequence getCharSequence(String key) {
        Object o = map.get(key);
        return o instanceof CharSequence ? (CharSequence) o : null;
    }

    public CharSequence getCharSequence(String key, CharSequence def) {
        CharSequence s = getCharSequence(key);
        return s == null ? def : s;
    }

    public Bundle getBundle(String key) {
        Object o = map.get(key);
        return o instanceof Bundle ? (Bundle) o : null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> T getParcelable(String key) {
        Object o = map.get(key);
        return o instanceof Parcelable ? (T) o : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParcelable(String key, Class<T> clazz) {
        Object o = map.get(key);
        return o != null && clazz.isInstance(o) ? (T) o : null;
    }

    public java.io.Serializable getSerializable(String key) {
        Object o = map.get(key);
        return o instanceof java.io.Serializable ? (java.io.Serializable) o : null;
    }

    @SuppressWarnings("unchecked")
    public <T extends java.io.Serializable> T getSerializable(String key, Class<T> clazz) {
        Object o = map.get(key);
        return o != null && clazz.isInstance(o) ? (T) o : null;
    }

    public int[] getIntArray(String key) {
        Object o = map.get(key);
        return o instanceof int[] ? (int[]) o : null;
    }

    public long[] getLongArray(String key) {
        Object o = map.get(key);
        return o instanceof long[] ? (long[]) o : null;
    }

    public float[] getFloatArray(String key) {
        Object o = map.get(key);
        return o instanceof float[] ? (float[]) o : null;
    }

    public double[] getDoubleArray(String key) {
        Object o = map.get(key);
        return o instanceof double[] ? (double[]) o : null;
    }

    public boolean[] getBooleanArray(String key) {
        Object o = map.get(key);
        return o instanceof boolean[] ? (boolean[]) o : null;
    }

    public byte[] getByteArray(String key) {
        Object o = map.get(key);
        return o instanceof byte[] ? (byte[]) o : null;
    }

    public char[] getCharArray(String key) {
        Object o = map.get(key);
        return o instanceof char[] ? (char[]) o : null;
    }

    public String[] getStringArray(String key) {
        Object o = map.get(key);
        return o instanceof String[] ? (String[]) o : null;
    }

    public CharSequence[] getCharSequenceArray(String key) {
        Object o = map.get(key);
        return o instanceof CharSequence[] ? (CharSequence[]) o : null;
    }

    public Parcelable[] getParcelableArray(String key) {
        Object o = map.get(key);
        return o instanceof Parcelable[] ? (Parcelable[]) o : null;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getStringArrayList(String key) {
        Object o = map.get(key);
        return o instanceof ArrayList ? (ArrayList<String>) o : null;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Integer> getIntegerArrayList(String key) {
        Object o = map.get(key);
        return o instanceof ArrayList ? (ArrayList<Integer>) o : null;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<CharSequence> getCharSequenceArrayList(String key) {
        Object o = map.get(key);
        return o instanceof ArrayList ? (ArrayList<CharSequence>) o : null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> ArrayList<T> getParcelableArrayList(String key) {
        Object o = map.get(key);
        return o instanceof ArrayList ? (ArrayList<T>) o : null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> android.util.SparseArray<T> getSparseParcelableArray(String key) {
        Object o = map.get(key);
        return o instanceof android.util.SparseArray ? (android.util.SparseArray<T>) o : null;
    }

    /// Runtime use: the underlying map, for copying between bundles.
    public Map<String, Object> asMap() {
        return map;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this);
    }

    public static final Parcelable.Creator<Bundle> CREATOR = new Parcelable.Creator<Bundle>() {
        @Override
        public Bundle createFromParcel(Parcel source) {
            Object o = source.readValue(null);
            return o instanceof Bundle ? (Bundle) o : new Bundle();
        }

        @Override
        public Bundle[] newArray(int size) {
            return new Bundle[size];
        }
    };

    @Override
    public String toString() {
        return "Bundle[" + map + "]";
    }
}
