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
package com.codename1.flutter.plugins.sharedpreferences;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.codename1.io.Storage;
import com.codename1.ui.Display;

import dart.async.Future;
import dart.core.DartList;
import dart.core.DartMap;
import dart.core.DartSet;

/// shared_preferences' `SharedPreferences`: typed key/value pairs that outlive
/// the app. They are kept as one object in Codename One's [Storage] -- not in
/// `Preferences`, which can neither list its keys nor keep a value's type -- and
/// in memory alone when there is no Display (a headless run or a test).
///
/// Writes are synchronous underneath; the futures the API returns are completed.
public final class SharedPreferences {

    static final String STORAGE_NAME = "cn1_flutter_shared_preferences";

    private static SharedPreferences instance;

    private final Hashtable<String, Object> values;

    private SharedPreferences(Hashtable<String, Object> values) {
        this.values = values;
    }

    public static Future<SharedPreferences> getInstance() {
        if (instance == null) {
            instance = new SharedPreferences(load());
        }
        return Future.value(instance);
    }

    /// `SharedPreferences.setMockInitialValues`: what a test starts from.
    public static void setMockInitialValues(Map<String, Object> initial) {
        Hashtable<String, Object> v = new Hashtable<String, Object>();
        if (initial != null) {
            for (Map.Entry<String, Object> e : initial.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    v.put(e.getKey(), stored(e.getValue()));
                }
            }
        }
        instance = new SharedPreferences(v);
        // As in pub, a mocked store lives in memory only: a test's clear() or set*
        // must not overwrite the app's real persisted preferences.
        mocked = true;
    }

    private static boolean mocked;

    @SuppressWarnings("unchecked")
    private static Hashtable<String, Object> load() {
        if (Display.isInitialized()) {
            Object o = Storage.getInstance().readObject(STORAGE_NAME);
            if (o instanceof Hashtable) {
                return (Hashtable<String, Object>) o;
            }
        }
        return new Hashtable<String, Object>();
    }

    private Future<Boolean> save() {
        boolean ok = true;
        if (!mocked && Display.isInitialized()) {
            ok = Storage.getInstance().writeObject(STORAGE_NAME, values);
        }
        return Future.value(Boolean.valueOf(ok));
    }

    /// Storage serializes Vector, not DartList; a list is stored as a Vector copy.
    private static Object stored(Object value) {
        if (value instanceof List) {
            return new Vector<Object>((List<?>) value);
        }
        if (value instanceof Integer) {
            return Long.valueOf(((Integer) value).longValue());
        }
        return value;
    }

    public DartSet<String> getKeys() {
        return DartSet.from(values.keySet());
    }

    public Object get(String key) {
        Object v = values.get(key);
        return v instanceof Vector ? getStringList(key) : v;
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    public String getString(String key) {
        Object v = values.get(key);
        return v instanceof String ? (String) v : null;
    }

    public Long getInt(String key) {
        Object v = values.get(key);
        return v instanceof Long ? (Long) v : null;
    }

    public Double getDouble(String key) {
        Object v = values.get(key);
        return v instanceof Double ? (Double) v : null;
    }

    public Boolean getBool(String key) {
        Object v = values.get(key);
        return v instanceof Boolean ? (Boolean) v : null;
    }

    public DartList<String> getStringList(String key) {
        Object v = values.get(key);
        if (!(v instanceof Vector)) {
            return null;
        }
        DartList<String> out = new DartList<String>();
        for (Object o : (Vector<?>) v) {
            out.add(o instanceof String ? (String) o : String.valueOf(o));
        }
        return out;
    }

    private Future<Boolean> put(String key, Object value) {
        if (value == null) {
            return remove(key);
        }
        values.put(key, stored(value));
        return save();
    }

    public Future<Boolean> setString(String key, String value) {
        return put(key, value);
    }

    public Future<Boolean> setInt(String key, long value) {
        return put(key, Long.valueOf(value));
    }

    public Future<Boolean> setDouble(String key, double value) {
        return put(key, Double.valueOf(value));
    }

    public Future<Boolean> setBool(String key, boolean value) {
        return put(key, Boolean.valueOf(value));
    }

    public Future<Boolean> setStringList(String key, List<String> value) {
        return put(key, value);
    }

    public Future<Boolean> remove(String key) {
        values.remove(key);
        return save();
    }

    public Future<Boolean> clear() {
        values.clear();
        return save();
    }

    public Future<Object> reload() {
        if (!mocked && Display.isInitialized()) {
            Hashtable<String, Object> fresh = load();
            values.clear();
            values.putAll(fresh);
        }
        return Future.value(null);
    }

    public Future<Boolean> commit() {
        return save();
    }

    /// Everything, as `{key: value}` -- for apps that dump their settings.
    public DartMap<String, Object> toMap() {
        DartMap<String, Object> m = new DartMap<String, Object>();
        for (String k : values.keySet()) {
            m.put(k, get(k));
        }
        return m;
    }
}
