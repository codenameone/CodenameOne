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
package androidx.work;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// The immutable key/value input and output of a worker. Values are the
/// primitive types, `String` and their arrays, as on Android; the limit of
/// [#MAX_DATA_BYTES] is checked against the serialized form this runtime
/// persists.
public final class Data {

    public static final int MAX_DATA_BYTES = 10 * 1024;
    public static final Data EMPTY = new Data(new HashMap<String, Object>());

    final Map<String, Object> values;

    Data(Map<String, Object> values) {
        this.values = values;
    }

    public Data(Data other) {
        this(new HashMap<String, Object>(other.values));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object v = values.get(key);
        return v instanceof Boolean ? ((Boolean) v).booleanValue() : defaultValue;
    }

    public boolean[] getBooleanArray(String key) {
        Object v = values.get(key);
        return v instanceof boolean[] ? (boolean[]) v : null;
    }

    public byte getByte(String key, byte defaultValue) {
        Object v = values.get(key);
        return v instanceof Byte ? ((Byte) v).byteValue() : defaultValue;
    }

    public byte[] getByteArray(String key) {
        Object v = values.get(key);
        return v instanceof byte[] ? (byte[]) v : null;
    }

    public int getInt(String key, int defaultValue) {
        Object v = values.get(key);
        return v instanceof Integer ? ((Integer) v).intValue() : defaultValue;
    }

    public int[] getIntArray(String key) {
        Object v = values.get(key);
        return v instanceof int[] ? (int[]) v : null;
    }

    public long getLong(String key, long defaultValue) {
        Object v = values.get(key);
        return v instanceof Long ? ((Long) v).longValue() : defaultValue;
    }

    public long[] getLongArray(String key) {
        Object v = values.get(key);
        return v instanceof long[] ? (long[]) v : null;
    }

    public float getFloat(String key, float defaultValue) {
        Object v = values.get(key);
        return v instanceof Float ? ((Float) v).floatValue() : defaultValue;
    }

    public float[] getFloatArray(String key) {
        Object v = values.get(key);
        return v instanceof float[] ? (float[]) v : null;
    }

    public double getDouble(String key, double defaultValue) {
        Object v = values.get(key);
        return v instanceof Double ? ((Double) v).doubleValue() : defaultValue;
    }

    public double[] getDoubleArray(String key) {
        Object v = values.get(key);
        return v instanceof double[] ? (double[]) v : null;
    }

    public String getString(String key) {
        Object v = values.get(key);
        return v instanceof String ? (String) v : null;
    }

    public String[] getStringArray(String key) {
        Object v = values.get(key);
        return v instanceof String[] ? (String[]) v : null;
    }

    public Map<String, Object> getKeyValueMap() {
        return Collections.unmodifiableMap(values);
    }

    public boolean hasKeyWithValueOfType(String key, Class<?> klass) {
        Object v = values.get(key);
        return v != null && klass.isInstance(v);
    }

    public int size() {
        return values.size();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Data)) {
            return false;
        }
        Data other = (Data) o;
        if (other.values.size() != values.size()) {
            return false;
        }
        for (Map.Entry<String, Object> e : values.entrySet()) {
            Object a = e.getValue();
            Object b = other.values.get(e.getKey());
            if (!WorkStore.valueEquals(a, b)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return values.keySet().hashCode();
    }

    @Override
    public String toString() {
        return "Data " + values.keySet();
    }

    public static final class Builder {
        private final Map<String, Object> values = new HashMap<String, Object>();

        public Builder putBoolean(String key, boolean value) {
            values.put(key, Boolean.valueOf(value));
            return this;
        }

        public Builder putBooleanArray(String key, boolean[] value) {
            values.put(key, value == null ? null : value.clone());
            return this;
        }

        public Builder putByte(String key, byte value) {
            values.put(key, Byte.valueOf(value));
            return this;
        }

        public Builder putByteArray(String key, byte[] value) {
            values.put(key, value == null ? null : value.clone());
            return this;
        }

        public Builder putInt(String key, int value) {
            values.put(key, Integer.valueOf(value));
            return this;
        }

        public Builder putIntArray(String key, int[] value) {
            values.put(key, value == null ? null : value.clone());
            return this;
        }

        public Builder putLong(String key, long value) {
            values.put(key, Long.valueOf(value));
            return this;
        }

        public Builder putLongArray(String key, long[] value) {
            values.put(key, value == null ? null : value.clone());
            return this;
        }

        public Builder putFloat(String key, float value) {
            values.put(key, Float.valueOf(value));
            return this;
        }

        public Builder putFloatArray(String key, float[] value) {
            values.put(key, value == null ? null : value.clone());
            return this;
        }

        public Builder putDouble(String key, double value) {
            values.put(key, Double.valueOf(value));
            return this;
        }

        public Builder putDoubleArray(String key, double[] value) {
            values.put(key, value == null ? null : value.clone());
            return this;
        }

        public Builder putString(String key, String value) {
            values.put(key, value);
            return this;
        }

        public Builder putStringArray(String key, String[] value) {
            values.put(key, value == null ? null : value.clone());
            return this;
        }

        public Builder putAll(Data data) {
            values.putAll(data.values);
            return this;
        }

        /// Values of an unsupported type are refused, as on Android.
        public Builder putAll(Map<String, Object> map) {
            for (Map.Entry<String, Object> e : map.entrySet()) {
                put(e.getKey(), e.getValue());
            }
            return this;
        }

        public Builder put(String key, Object value) {
            if (value != null && !WorkStore.isSupportedValue(value)) {
                throw new IllegalArgumentException("Key " + key + " has invalid type " + value.getClass());
            }
            values.put(key, value);
            return this;
        }

        public Data build() {
            Data d = new Data(new HashMap<String, Object>(values));
            if (WorkStore.encodedSize(d) > MAX_DATA_BYTES) {
                throw new IllegalStateException("Data cannot occupy more than " + MAX_DATA_BYTES + " bytes when serialized");
            }
            return d;
        }
    }
}
