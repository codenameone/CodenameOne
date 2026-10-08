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
package com.codename1.compat.jdk;

import java.util.ArrayList;
import java.util.List;

/// `java.util.prefs.Preferences` for the Codename One runtime, kept in
/// `com.codename1.io.Preferences`.
///
/// Codename One's preferences are one flat table of the application's own,
/// so a node is a prefix: the value of `key` in the node `/com/acme` is
/// stored as `/com/acme/key`. The user and the system trees are the same
/// tree -- an application on a device has no machine-wide settings to write.
/// Every value is stored as text, as the JDK stores it, so `getInt` of
/// something written with `put` reads it back.
///
/// Each node keeps the list of its keys beside them, which is what `keys`
/// and `clear` answer from; the store cannot be asked for it. Child nodes
/// are not listed: `childrenNames` answers none.
///
/// Writes are saved as they are made. `flush` and `sync` do nothing, and
/// there are no change listeners.
public class Preferences {

    public static final int MAX_KEY_LENGTH = 80;
    public static final int MAX_VALUE_LENGTH = 8192;
    public static final int MAX_NAME_LENGTH = 80;

    /// Under the node's prefix, where no key can be: a key holds no newline.
    private static final String INDEX = "\nkeys";

    private final String path;

    protected Preferences(String path) {
        this.path = path;
    }

    public static Preferences userRoot() {
        return new Preferences("/");
    }

    public static Preferences systemRoot() {
        return new Preferences("/");
    }

    public static Preferences userNodeForPackage(Class<?> c) {
        return new Preferences(packagePath(c));
    }

    public static Preferences systemNodeForPackage(Class<?> c) {
        return new Preferences(packagePath(c));
    }

    private static String packagePath(Class<?> c) {
        if (c.isArray()) {
            throw new IllegalArgumentException("Arrays have no associated preferences node.");
        }
        String name = c.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "/<unnamed>" : "/" + name.substring(0, dot).replace('.', '/');
    }

    /// The node at `pathName`: absolute when it starts with a slash, else
    /// below this one.
    public Preferences node(String pathName) {
        if (pathName.length() == 0) {
            return this;
        }
        if (pathName.charAt(0) == '/') {
            return new Preferences(pathName);
        }
        return new Preferences("/".equals(path) ? "/" + pathName : path + "/" + pathName);
    }

    public String name() {
        return "/".equals(path) ? "" : path.substring(path.lastIndexOf('/') + 1);
    }

    public String absolutePath() {
        return path;
    }

    public Preferences parent() {
        if ("/".equals(path)) {
            return null;
        }
        int slash = path.lastIndexOf('/');
        return new Preferences(slash <= 0 ? "/" : path.substring(0, slash));
    }

    public boolean isUserNode() {
        return true;
    }

    private String storeKey(String key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        return "/".equals(path) ? "/" + key : path + "/" + key;
    }

    private List<String> index() {
        List<String> keys = new ArrayList<String>();
        String all = com.codename1.io.Preferences.get(storeKey(INDEX), "");
        int start = 0;
        while (start < all.length()) {
            int end = all.indexOf('\n', start);
            if (end < 0) {
                end = all.length();
            }
            if (end > start) {
                keys.add(all.substring(start, end));
            }
            start = end + 1;
        }
        return keys;
    }

    private void saveIndex(List<String> keys) {
        if (keys.isEmpty()) {
            com.codename1.io.Preferences.delete(storeKey(INDEX));
            return;
        }
        StringBuilder all = new StringBuilder();
        for (String key : keys) {
            all.append(key).append('\n');
        }
        com.codename1.io.Preferences.set(storeKey(INDEX), all.toString());
    }

    public void put(String key, String value) {
        if (value == null) {
            throw new NullPointerException("Null value");
        }
        com.codename1.io.Preferences.set(storeKey(key), value);
        List<String> keys = index();
        if (!keys.contains(key)) {
            keys.add(key);
            saveIndex(keys);
        }
    }

    public String get(String key, String def) {
        return com.codename1.io.Preferences.get(storeKey(key), def);
    }

    public void remove(String key) {
        com.codename1.io.Preferences.delete(storeKey(key));
        List<String> keys = index();
        if (keys.remove(key)) {
            saveIndex(keys);
        }
    }

    public void clear() throws BackingStoreException {
        for (String key : index()) {
            com.codename1.io.Preferences.delete(storeKey(key));
        }
        saveIndex(new ArrayList<String>());
    }

    public String[] keys() throws BackingStoreException {
        List<String> keys = index();
        return keys.toArray(new String[keys.size()]);
    }

    public String[] childrenNames() throws BackingStoreException {
        return new String[0];
    }

    public boolean nodeExists(String pathName) throws BackingStoreException {
        return !node(pathName).index().isEmpty() || pathName.length() == 0;
    }

    public void removeNode() throws BackingStoreException {
        clear();
    }

    public void putInt(String key, int value) {
        put(key, Integer.toString(value));
    }

    public int getInt(String key, int def) {
        String value = get(key, null);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    public void putLong(String key, long value) {
        put(key, Long.toString(value));
    }

    public long getLong(String key, long def) {
        String value = get(key, null);
        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    public void putBoolean(String key, boolean value) {
        put(key, value ? "true" : "false");
    }

    public boolean getBoolean(String key, boolean def) {
        String value = get(key, null);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return def;
    }

    public void putFloat(String key, float value) {
        put(key, Float.toString(value));
    }

    public float getFloat(String key, float def) {
        String value = get(key, null);
        if (value != null) {
            try {
                return Float.parseFloat(value.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    public void putDouble(String key, double value) {
        put(key, Double.toString(value));
    }

    public double getDouble(String key, double def) {
        String value = get(key, null);
        if (value != null) {
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    public void flush() throws BackingStoreException {
        // Saved as written; see the class description.
    }

    public void sync() throws BackingStoreException {
        // Saved as written; see the class description.
    }

    @Override
    public String toString() {
        return "User Preference Node: " + path;
    }
}
