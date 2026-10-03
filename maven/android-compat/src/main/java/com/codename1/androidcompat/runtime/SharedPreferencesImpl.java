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
package com.codename1.androidcompat.runtime;

import android.content.SharedPreferences;
import com.codename1.io.Storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/// SharedPreferences stored as one Codename One storage entry per file.
/// Values keep their types (Integer, Long, Float, Boolean, String, and string
/// sets as vectors), so `getAll` answers as on Android.
final class SharedPreferencesImpl implements SharedPreferences {

    private final String file;
    private final HashMap<String, Object> values = new HashMap<String, Object>();
    private final ArrayList<OnSharedPreferenceChangeListener> listeners =
            new ArrayList<OnSharedPreferenceChangeListener>();

    SharedPreferencesImpl(String name) {
        this.file = "shared_prefs_" + name;
        Object stored = Storage.getInstance().readObject(file);
        if (stored instanceof Map) {
            for (Object e : ((Map<?, ?>) stored).entrySet()) {
                Map.Entry<?, ?> en = (Map.Entry<?, ?>) e;
                Object v = en.getValue();
                if (v instanceof Vector) {
                    HashSet<String> set = new HashSet<String>();
                    for (Object o : (Vector<?>) v) {
                        set.add(String.valueOf(o));
                    }
                    v = set;
                }
                values.put(String.valueOf(en.getKey()), v);
            }
        }
    }

    private void persist() {
        Hashtable<String, Object> out = new Hashtable<String, Object>();
        for (Map.Entry<String, Object> e : values.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Set) {
                Vector<String> vec = new Vector<String>();
                for (Object o : (Set<?>) v) {
                    vec.add(String.valueOf(o));
                }
                v = vec;
            }
            if (v != null) {
                out.put(e.getKey(), v);
            }
        }
        Storage.getInstance().writeObject(file, out);
    }

    @Override
    public Map<String, ?> getAll() {
        return new HashMap<String, Object>(values);
    }

    @Override
    public String getString(String key, String defValue) {
        Object o = values.get(key);
        return o instanceof String ? (String) o : defValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object o = values.get(key);
        return o instanceof Set ? new HashSet<String>((Set<String>) o) : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object o = values.get(key);
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o != null) {
            throw new ClassCastException("Key " + key + " is not an int");
        }
        return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object o = values.get(key);
        if (o instanceof Long) {
            return ((Long) o).longValue();
        }
        if (o != null) {
            throw new ClassCastException("Key " + key + " is not a long");
        }
        return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object o = values.get(key);
        if (o instanceof Float) {
            return ((Float) o).floatValue();
        }
        if (o != null) {
            throw new ClassCastException("Key " + key + " is not a float");
        }
        return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object o = values.get(key);
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        if (o != null) {
            throw new ClassCastException("Key " + key + " is not a boolean");
        }
        return defValue;
    }

    @Override
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new EditorImpl();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        listeners.remove(listener);
    }

    private final class EditorImpl implements Editor {
        private final HashMap<String, Object> pending = new HashMap<String, Object>();
        private final HashSet<String> removed = new HashSet<String>();
        private boolean clear;

        @Override
        public Editor putString(String key, String value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> value) {
            pending.put(key, value == null ? null : new HashSet<String>(value));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            pending.put(key, Integer.valueOf(value));
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            pending.put(key, Long.valueOf(value));
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            pending.put(key, Float.valueOf(value));
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            pending.put(key, Boolean.valueOf(value));
            return this;
        }

        @Override
        public Editor remove(String key) {
            removed.add(key);
            return this;
        }

        @Override
        public Editor clear() {
            clear = true;
            return this;
        }

        @Override
        public boolean commit() {
            ArrayList<String> changed = new ArrayList<String>();
            if (clear) {
                values.clear();
            }
            for (String k : removed) {
                if (values.remove(k) != null) {
                    changed.add(k);
                }
            }
            for (Map.Entry<String, Object> e : pending.entrySet()) {
                if (e.getValue() == null) {
                    values.remove(e.getKey());
                } else {
                    values.put(e.getKey(), e.getValue());
                }
                changed.add(e.getKey());
            }
            persist();
            if (!listeners.isEmpty()) {
                for (String k : changed) {
                    for (OnSharedPreferenceChangeListener l
                            : new ArrayList<OnSharedPreferenceChangeListener>(listeners)) {
                        l.onSharedPreferenceChanged(SharedPreferencesImpl.this, k);
                    }
                }
            }
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
