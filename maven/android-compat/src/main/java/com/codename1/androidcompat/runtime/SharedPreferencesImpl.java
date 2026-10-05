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

    private boolean persist() {
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
        return Storage.getInstance().writeObject(file, out);
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

    /// The editor's mark for a removed key.
    private static final Object REMOVED = new Object();

    private final class EditorImpl implements Editor {
        /// One modification per key, the last call's, as on Android: a put
        /// then a remove removes, a remove then a put puts. A removal is
        /// recorded as [#REMOVED].
        private final HashMap<String, Object> pending = new HashMap<String, Object>();
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
            pending.put(key, REMOVED);
            return this;
        }

        @Override
        public Editor clear() {
            clear = true;
            return this;
        }

        // Deliberately unsynchronized. Codename One code is single threaded:
        // preferences are edited on the EDT, the runtime adds no locks, and the
        // platform Storage layer serializes its own writes. Two worker threads
        // committing the same file at once is outside that model and is not
        // guarded here.
        @Override
        public boolean commit() {
            // As on Android, a commit consumes the editor's batch: an editor
            // reused afterwards starts empty, so a later commit neither
            // re-applies these values nor repeats this clear() over what
            // another editor wrote in between.
            HashMap<String, Object> batch = new HashMap<String, Object>(pending);
            boolean clearAll = clear;
            pending.clear();
            clear = false;
            ArrayList<String> changed = new ArrayList<String>();
            ArrayList<String> cleared = null;
            if (clearAll) {
                cleared = new ArrayList<String>(values.keySet());
                values.clear();
            }
            for (Map.Entry<String, Object> e : batch.entrySet()) {
                Object v = e.getValue();
                if (v == REMOVED || v == null) {
                    if (values.remove(e.getKey()) != null) {
                        changed.add(e.getKey());
                    }
                } else {
                    // As on Android, writing the value a key already holds is
                    // not a change and notifies no listener.
                    Object old = values.put(e.getKey(), v);
                    if (!v.equals(old)) {
                        changed.add(e.getKey());
                    }
                }
            }
            if (cleared != null) {
                // Every key clear() removed is reported, unless this edit put
                // it back (the put above already reported it). Android 11+
                // reports a clear as one call with a null key instead; one
                // call per key tells a listener the same thing without
                // handing a null to listeners written for the older contract.
                for (String k : cleared) {
                    if (!values.containsKey(k)) {
                        changed.add(k);
                    }
                }
            }
            // As on Android, the in-memory values and the listeners reflect
            // the edit even when writing it out fails; only the result says
            // whether it will survive a restart.
            boolean persisted = persist();
            if (!listeners.isEmpty()) {
                for (String k : changed) {
                    for (OnSharedPreferenceChangeListener l
                            : new ArrayList<OnSharedPreferenceChangeListener>(listeners)) {
                        l.onSharedPreferenceChanged(SharedPreferencesImpl.this, k);
                    }
                }
            }
            return persisted;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
