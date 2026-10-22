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
package com.codename1.unitycompat.unityengine;

import java.util.ArrayList;

/// `UnityEngine.PlayerPrefs`: small values a game keeps between runs, by
/// name -- a high score, a volume setting.
///
/// Each key holds an int, a float or a string, and a getter of another
/// kind than the key was set as answers its default, as Unity's does.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class PlayerPrefs {
    private static PlayerPrefsStore store;
    /// Keys and values in turn, for when no store is installed.
    private static final ArrayList memory = new ArrayList();

    private PlayerPrefs() {
    }

    /// Installs where preferences are kept, or with null returns them to
    /// memory. What is in memory is not carried across.
    public static void $store(PlayerPrefsStore target) {
        store = target;
    }

    public static PlayerPrefsStore $store() {
        return store;
    }

    /// Empties the preferences held in memory. A headless run starts from
    /// this, so that two runs of one session print one trace.
    public static void $resetMemory() {
        memory.clear();
    }

    private static int find(String key) {
        for (int i = 0; i + 1 < memory.size(); i += 2) {
            if (memory.get(i).equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private static java.lang.Object read(String key) {
        if (key == null) {
            return null;
        }
        if (store != null) {
            return store.get(key);
        }
        int at = find(key);
        return at < 0 ? null : memory.get(at + 1);
    }

    private static void write(String key, java.lang.Object value) {
        if (key == null) {
            throw new IllegalArgumentException("PlayerPrefs: the key is null");
        }
        if (store != null) {
            store.set(key, value);
            return;
        }
        int at = find(key);
        if (at < 0) {
            memory.add(key);
            memory.add(value);
        } else {
            memory.set(at + 1, value);
        }
    }

    public static void SetInt(String key, int value) {
        write(key, Integer.valueOf(value));
    }

    public static int GetInt(String key) {
        return GetInt(key, 0);
    }

    public static int GetInt(String key, int defaultValue) {
        java.lang.Object v = read(key);
        return v instanceof Integer ? ((Integer) v).intValue() : defaultValue;
    }

    public static void SetFloat(String key, float value) {
        write(key, Float.valueOf(value));
    }

    public static float GetFloat(String key) {
        return GetFloat(key, 0f);
    }

    public static float GetFloat(String key, float defaultValue) {
        java.lang.Object v = read(key);
        return v instanceof Float ? ((Float) v).floatValue() : defaultValue;
    }

    public static void SetString(String key, String value) {
        write(key, value == null ? "" : value);
    }

    public static String GetString(String key) {
        return GetString(key, "");
    }

    public static String GetString(String key, String defaultValue) {
        java.lang.Object v = read(key);
        return v instanceof String ? (String) v : defaultValue;
    }

    public static boolean HasKey(String key) {
        return read(key) != null;
    }

    public static void DeleteKey(String key) {
        if (key == null) {
            return;
        }
        if (store != null) {
            store.delete(key);
            return;
        }
        int at = find(key);
        if (at >= 0) {
            memory.remove(at + 1);
            memory.remove(at);
        }
    }

    public static void DeleteAll() {
        if (store != null) {
            store.deleteAll();
        } else {
            memory.clear();
        }
    }

    public static void Save() {
        if (store != null) {
            store.save();
        }
    }
}
