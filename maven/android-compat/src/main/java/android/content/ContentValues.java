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
package android.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/// A set of column values for an insert or update. Iteration follows
/// insertion order, so the columns of a generated INSERT come out in the
/// order the application put them.
public final class ContentValues {

    public static final String TAG = "ContentValues";

    private final LinkedHashMap<String, Object> mMap;

    public ContentValues() {
        mMap = new LinkedHashMap<String, Object>(8);
    }

    public ContentValues(int size) {
        mMap = new LinkedHashMap<String, Object>(Math.max(1, size));
    }

    public ContentValues(ContentValues from) {
        mMap = new LinkedHashMap<String, Object>(from.mMap);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ContentValues)) {
            return false;
        }
        return mMap.equals(((ContentValues) object).mMap);
    }

    @Override
    public int hashCode() {
        return mMap.hashCode();
    }

    public void put(String key, String value) {
        mMap.put(key, value);
    }

    public void putAll(ContentValues other) {
        mMap.putAll(other.mMap);
    }

    public void put(String key, Byte value) {
        mMap.put(key, value);
    }

    public void put(String key, Short value) {
        mMap.put(key, value);
    }

    public void put(String key, Integer value) {
        mMap.put(key, value);
    }

    public void put(String key, Long value) {
        mMap.put(key, value);
    }

    public void put(String key, Float value) {
        mMap.put(key, value);
    }

    public void put(String key, Double value) {
        mMap.put(key, value);
    }

    public void put(String key, Boolean value) {
        mMap.put(key, value);
    }

    public void put(String key, byte[] value) {
        mMap.put(key, value);
    }

    public void putNull(String key) {
        mMap.put(key, null);
    }

    public int size() {
        return mMap.size();
    }

    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    public void remove(String key) {
        mMap.remove(key);
    }

    public void clear() {
        mMap.clear();
    }

    public boolean containsKey(String key) {
        return mMap.containsKey(key);
    }

    public Object get(String key) {
        return mMap.get(key);
    }

    public String getAsString(String key) {
        Object value = mMap.get(key);
        return value != null ? value.toString() : null;
    }

    public Long getAsLong(String key) {
        Object value = mMap.get(key);
        if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(Long.parseLong(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getAsInteger(String key) {
        Object value = mMap.get(key);
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Short getAsShort(String key) {
        Object value = mMap.get(key);
        if (value instanceof Number) {
            return Short.valueOf(((Number) value).shortValue());
        }
        if (value == null) {
            return null;
        }
        try {
            return Short.valueOf(Short.parseShort(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Byte getAsByte(String key) {
        Object value = mMap.get(key);
        if (value instanceof Number) {
            return Byte.valueOf(((Number) value).byteValue());
        }
        if (value == null) {
            return null;
        }
        try {
            return Byte.valueOf(Byte.parseByte(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getAsDouble(String key) {
        Object value = mMap.get(key);
        if (value instanceof Number) {
            return Double.valueOf(((Number) value).doubleValue());
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Float getAsFloat(String key) {
        Object value = mMap.get(key);
        if (value instanceof Number) {
            return Float.valueOf(((Number) value).floatValue());
        }
        if (value == null) {
            return null;
        }
        try {
            return Float.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean getAsBoolean(String key) {
        Object value = mMap.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof CharSequence) {
            String s = value.toString();
            return Boolean.valueOf("true".equalsIgnoreCase(s) || "1".equals(s));
        }
        if (value instanceof Number) {
            return Boolean.valueOf(((Number) value).intValue() != 0);
        }
        return null;
    }

    public byte[] getAsByteArray(String key) {
        Object value = mMap.get(key);
        return value instanceof byte[] ? (byte[]) value : null;
    }

    public Set<Map.Entry<String, Object>> valueSet() {
        return mMap.entrySet();
    }

    public Set<String> keySet() {
        return mMap.keySet();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : mMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }
}
