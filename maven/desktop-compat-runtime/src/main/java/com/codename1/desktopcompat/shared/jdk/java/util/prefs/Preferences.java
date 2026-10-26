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
package com.codename1.desktopcompat.shared.jdk.java.util.prefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// `java.util.prefs.Preferences` for desktop sources, kept in Codename One's
/// `com.codename1.io.Preferences`.
///
/// The store is one flat table of the application's own, so a node is a
/// prefix: the value of `key` in the node `/com/acme` is stored under the
/// node's path and the key. The user and the system trees are the same
/// tree -- an application on a device has no machine-wide settings to write.
/// Every value is stored as text, as the JDK stores it, so `getInt` of
/// something written with `put` reads it back.
///
/// Each node keeps the list of its keys beside them, which is what `keys`
/// and `clear` answer from; the store cannot be asked for it. A node also
/// keeps the names of its children that hold a key, or have a descendant
/// that does: that is what `childrenNames` answers and what `removeNode`
/// walks to remove a whole subtree. A node that only ever was asked for,
/// and never written to, is not listed.
///
/// Writes are saved as they are made. `flush` and `sync` do nothing, and
/// there are no change listeners.
public abstract class Preferences {

    public static final int MAX_KEY_LENGTH = 80;
    public static final int MAX_VALUE_LENGTH = 8192;
    public static final int MAX_NAME_LENGTH = 80;

    private final String path;

    /// [#removalCount] as it stood when this instance was made: a removal
    /// of this node, or of one above it, numbered higher happened to it.
    private final long born;

    /// For the JDK's shape; the nodes this class hands out are its own.
    protected Preferences() {
        this("/");
    }

    private Preferences(String path) {
        this.path = path;
        synchronized (STORE_LOCK) {
            born = removalCount;
        }
    }

    public static Preferences userRoot() {
        return new Node("/");
    }

    public static Preferences systemRoot() {
        return new Node("/");
    }

    public static Preferences userNodeForPackage(Class<?> c) {
        return new Node(packagePath(c));
    }

    public static Preferences systemNodeForPackage(Class<?> c) {
        return new Node(packagePath(c));
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
        checkLive();
        if (pathName.length() == 0) {
            return this;
        }
        if (pathName.charAt(0) == '/') {
            return new Node(pathName);
        }
        return new Node("/".equals(path) ? "/" + pathName : path + "/" + pathName);
    }

    public String name() {
        return "/".equals(path) ? "" : path.substring(path.lastIndexOf('/') + 1);
    }

    public String absolutePath() {
        return path;
    }

    public Preferences parent() {
        checkLive();
        if ("/".equals(path)) {
            return null;
        }
        int slash = path.lastIndexOf('/');
        return new Node(slash <= 0 ? "/" : path.substring(0, slash));
    }

    public boolean isUserNode() {
        return true;
    }

    /// Where `key` of this node is kept in Codename One's flat store: the
    /// node's path and the key, each escaped, around one line break.
    ///
    /// A key may hold a `/`, so joining the two with one aliased entries:
    /// `node("a").put("b/c", ...)` and `node("a/b").put("c", ...)` both
    /// wrote `/a/b/c`. A node name may hold anything but `/`, a line break
    /// too, so both halves are escaped and the one raw line break left is
    /// where they meet. The index ([#indexKey()]) ends in a second raw line
    /// break, which no escaped key can be.
    private String storeKey(String key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        return escape(path) + '\n' + escape(key);
    }

    private String indexKey() {
        return escape(path) + "\n\n";
    }

    /// Where the names of the node's children are kept: three raw line
    /// breaks, which neither a key ([#storeKey]) nor the index can end in.
    private String childrenKey() {
        return childrenKey(path);
    }

    private static String childrenKey(String nodePath) {
        return escape(nodePath) + "\n\n\n";
    }

    private static String parentPath(String nodePath) {
        int slash = nodePath.lastIndexOf('/');
        return slash <= 0 ? "/" : nodePath.substring(0, slash);
    }

    private static String escape(String text) {
        if (text.indexOf('\\') < 0 && text.indexOf('\n') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else if (c == '\n') {
                out.append("\\n");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /// Guards every use of the store, from every node. Codename One's
    /// Preferences, and the Storage under it, are written for its one
    /// thread: two workers saving at once threw inside Storage, which logs
    /// the failure and goes on, so a value could be lost without a word.
    /// It also makes each read-modify-write of an index whole: two threads
    /// adding different keys to one node each read the same index, and the
    /// one that saved last dropped the other's key for good. Static, since
    /// two instances can stand for one node.
    private static final Object STORE_LOCK = new Object();

    /// The nodes removed in this run, each with the number of its removal.
    /// Nodes are not cached -- two calls to `node` hand out two instances
    /// for one path -- so an instance cannot be told it was removed; it
    /// asks here instead, for its path and every one above it.
    private static final Map<String, Long> REMOVALS = new HashMap<String, Long>();

    private static long removalCount;

    /// Throws as the JDK does for any use of a removed node.
    private void checkLive() {
        synchronized (STORE_LOCK) {
            if (isRemoved()) {
                throw new IllegalStateException("Node has been removed.");
            }
        }
    }

    /// Called under [#STORE_LOCK].
    private boolean isRemoved() {
        if (REMOVALS.isEmpty()) {
            return false;
        }
        String at = path;
        while (true) {
            Long when = REMOVALS.get(at);
            if (when != null && when.longValue() > born) {
                return true;
            }
            if ("/".equals(at)) {
                return false;
            }
            at = parentPath(at);
        }
    }

    /// The node's keys, one per line. A key may hold any character, a line
    /// break too, so a backslash and a line break inside one are written as
    /// `\\` and `\n`; see [#saveIndex].
    private List<String> index() {
        return readList(indexKey());
    }

    private void saveIndex(List<String> keys) {
        saveList(indexKey(), keys);
    }

    private static List<String> readList(String storeKey) {
        List<String> keys = new ArrayList<String>();
        String all = stored(storeKey, "");
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < all.length(); i++) {
            char c = all.charAt(i);
            if (c == '\\' && i + 1 < all.length()) {
                i++;
                key.append(all.charAt(i) == 'n' ? '\n' : all.charAt(i));
            } else if (c == '\n') {
                keys.add(key.toString());
                key.setLength(0);
            } else {
                key.append(c);
            }
        }
        if (key.length() > 0) {
            keys.add(key.toString());
        }
        return keys;
    }

    private static void saveList(String storeKey, List<String> keys) {
        if (keys.isEmpty()) {
            com.codename1.io.Preferences.delete(storeKey);
            return;
        }
        StringBuilder all = new StringBuilder();
        for (String key : keys) {
            for (int i = 0; i < key.length(); i++) {
                char c = key.charAt(i);
                if (c == '\\') {
                    all.append("\\\\");
                } else if (c == '\n') {
                    all.append("\\n");
                } else {
                    all.append(c);
                }
            }
            all.append('\n');
        }
        com.codename1.io.Preferences.set(storeKey, all.toString());
    }

    /// Lists this node with its parent, and so on up, so that removing an
    /// ancestor finds it. Stops at the first one already listed: a node is
    /// only ever listed after everything above it.
    private void register() {
        String at = path;
        while (!"/".equals(at)) {
            String parent = parentPath(at);
            String name = at.substring(at.lastIndexOf('/') + 1);
            String key = childrenKey(parent);
            List<String> names = readList(key);
            if (names.contains(name)) {
                return;
            }
            names.add(name);
            saveList(key, names);
            at = parent;
        }
    }

    /// Deletes every entry of `nodePath` and of every node listed below it.
    private static void removeTree(String nodePath) {
        for (String child : readList(childrenKey(nodePath))) {
            removeTree("/".equals(nodePath) ? "/" + child : nodePath + "/" + child);
        }
        String prefix = escape(nodePath) + '\n';
        for (String key : readList(prefix + '\n')) {
            com.codename1.io.Preferences.delete(prefix + escape(key));
        }
        com.codename1.io.Preferences.delete(prefix + '\n');
        com.codename1.io.Preferences.delete(childrenKey(nodePath));
    }

    public void put(String key, String value) {
        if (value == null) {
            throw new NullPointerException("Null value");
        }
        synchronized (STORE_LOCK) {
            checkLive();
            com.codename1.io.Preferences.set(storeKey(key), value);
            List<String> keys = index();
            if (!keys.contains(key)) {
                keys.add(key);
                saveIndex(keys);
            }
            register();
        }
    }

    public String get(String key, String def) {
        synchronized (STORE_LOCK) {
            checkLive();
            return stored(storeKey(key), def);
        }
    }

    public void remove(String key) {
        synchronized (STORE_LOCK) {
            checkLive();
            com.codename1.io.Preferences.delete(storeKey(key));
            List<String> keys = index();
            if (keys.remove(key)) {
                saveIndex(keys);
            }
        }
    }

    public void clear() throws BackingStoreException {
        synchronized (STORE_LOCK) {
            checkLive();
            for (String key : index()) {
                com.codename1.io.Preferences.delete(storeKey(key));
            }
            saveIndex(new ArrayList<String>());
        }
    }

    public String[] keys() throws BackingStoreException {
        synchronized (STORE_LOCK) {
            checkLive();
            List<String> keys = index();
            return keys.toArray(new String[keys.size()]);
        }
    }

    /// The children that hold a key, or have a descendant that does; see
    /// the class description.
    public String[] childrenNames() throws BackingStoreException {
        synchronized (STORE_LOCK) {
            checkLive();
            List<String> names = readList(childrenKey());
            return names.toArray(new String[names.size()]);
        }
    }

    /// As the JDK: `nodeExists("")` answers whether this node is still
    /// there, and is the one question a removed node may still be asked.
    public boolean nodeExists(String pathName) throws BackingStoreException {
        synchronized (STORE_LOCK) {
            if (pathName.length() == 0) {
                return !isRemoved();
            }
            Preferences node = node(pathName);
            return !node.index().isEmpty() || !readList(node.childrenKey()).isEmpty();
        }
    }

    /// Removes this node and everything below it, as the JDK does: their
    /// entries are deleted, this instance and every other one for the
    /// removed nodes throw `IllegalStateException` from then on, and a node
    /// asked for again by its path afterwards is a new, empty one. The root
    /// cannot be removed.
    public void removeNode() throws BackingStoreException {
        if ("/".equals(path)) {
            throw new UnsupportedOperationException("Can't remove the root!");
        }
        synchronized (STORE_LOCK) {
            if (isRemoved()) {
                throw new IllegalStateException("Node already removed.");
            }
            removeTree(path);
            String parentKey = childrenKey(parentPath(path));
            List<String> siblings = readList(parentKey);
            if (siblings.remove(path.substring(path.lastIndexOf('/') + 1))) {
                saveList(parentKey, siblings);
            }
            recordRemoval(path);
        }
    }

    /// Called under [#STORE_LOCK].
    private static void recordRemoval(String nodePath) {
        removalCount++;
        REMOVALS.put(nodePath, Long.valueOf(removalCount));
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
        // Saved as written; see the class description. A removed node
        // cannot be synced, as with the JDK (flushing one is allowed).
        checkLive();
    }

    @Override
    public String toString() {
        return "User Preference Node: " + path;
    }

    /// The stored value of `storeKey`, or `def`.
    private static String stored(String storeKey, String def) {
        String value = com.codename1.io.Preferences.get(storeKey, (String) null);
        return value == null ? def : value;
    }

    /// The one concrete node.
    private static final class Node extends Preferences {
        Node(String path) {
            super(path);
        }
    }
}
