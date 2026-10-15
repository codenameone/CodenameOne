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

import com.codename1.impl.JdkHost;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/// java.util.prefs.Preferences over the host's flat store, with the
/// hardening the compat layer's copy earned: escaped keys that never alias
/// another node's entries, a per-node key index kept whole under concurrent
/// writers, and removeNode that removes the subtree and leaves every
/// instance for a removed node throwing.
///
/// Every check here is ParparVM only. The expectations are what
/// `java.util.prefs` answers on the JDK, as literals, because running the
/// JDK's would write to the real preferences of the user running the build.
/// The bulk host keeps preferences in JdkHost's in-memory defaults.
public final class PreferencesChecks {
    private PreferencesChecks() {
    }

    private static Object thrown(Class<?> type, Bulk.Body body) {
        return Bulk.thrown(type, body);
    }

    /// Runs the group.
    public static void run() {
        // Several threads adding keys to one node, each through an instance
        // of its own: every key is kept.
        Bulk.cn1Only("Preferences.keysAddedFromSeveralThreadsAreAllIndexed", () -> {
            final String node = "/cn1/test/concurrent";
            Thread[] writers = new Thread[4];
            for (int t = 0; t < writers.length; t++) {
                final int id = t;
                writers[t] = new Thread(() -> {
                    for (int i = 0; i < 50; i++) {
                        Preferences.userRoot().node(node).put("k" + id + "." + i, "v");
                    }
                });
                writers[t].start();
            }
            for (Thread w : writers) {
                w.join();
            }
            Preferences prefs = Preferences.userRoot().node(node);
            Bulk.expectEquals(Integer.valueOf(200), Integer.valueOf(prefs.keys().length), "keys indexed");
            for (int t = 0; t < writers.length; t++) {
                for (int i = 0; i < 50; i++) {
                    Bulk.expectEquals("v", prefs.get("k" + t + "." + i, null), "k" + t + "." + i);
                }
            }
            prefs.put("a\nb", "1");
            prefs.put("c\\nd", "2");
            List<String> keys = Arrays.asList(prefs.keys());
            Bulk.expect(keys.contains("a\nb"), "a line break in a key");
            Bulk.expect(keys.contains("c\\nd"), "a backslash in a key");
            Bulk.expectEquals(Integer.valueOf(202), Integer.valueOf(keys.size()), "keys");
            prefs.remove("a\nb");
            Bulk.expect(!Arrays.asList(prefs.keys()).contains("a\nb"), "removed");
            prefs.clear();
            Bulk.expectEquals(Integer.valueOf(0), Integer.valueOf(prefs.keys().length), "cleared");
            Bulk.expectEquals("gone", prefs.get("c\\nd", "gone"), "cleared value");
        });
        // A key may hold a slash or a line break, and none of it makes one
        // entry another's.
        Bulk.cn1Only("Preferences.keysNeverAliasAnotherNodesEntries", () -> {
            Preferences a = Preferences.userRoot().node("/cn1/test/alias/a");
            Preferences ab = Preferences.userRoot().node("/cn1/test/alias/a/b");
            a.put("b/c", "x");
            ab.put("c", "y");
            a.put("\nkeys", "not the index");
            a.put("\n", "line break");
            Bulk.expectEquals("x", a.get("b/c", null), "b/c");
            Bulk.expectEquals("y", ab.get("c", null), "c of a/b");
            Bulk.expectEquals("not the index", a.get("\nkeys", null), "a key named like the index");
            Bulk.expectEquals("line break", a.get("\n", null), "a key that is a line break");
            Bulk.expectEquals(Arrays.asList("b/c", "\nkeys", "\n"), Arrays.asList(a.keys()), "keys of a");
            Bulk.expectEquals(Arrays.asList("c"), Arrays.asList(ab.keys()), "keys of a/b");
            a.clear();
            ab.clear();
            Bulk.expectEquals("y", ab.get("c", "y"), "a/b cleared");
            Bulk.expectEquals(Integer.valueOf(0), Integer.valueOf(a.keys().length), "a cleared");
        });
        Bulk.cn1Only("Preferences.removeNodeRemovesTheWholeSubtree", () -> {
            Bulk.expectEquals("UnsupportedOperationException",
                    thrown(UnsupportedOperationException.class, () -> Preferences.userRoot().removeNode()),
                    "the root cannot be removed");
            Preferences root = Preferences.userRoot();
            Preferences a = root.node("/cn1/test/remove/a");
            Preferences b = a.node("b");
            a.put("k", "v");
            b.put("x", "y");
            Preferences.userRoot().node("/cn1/test/remove/keep").put("z", "w");
            Preferences top = root.node("/cn1/test/remove");
            Bulk.expectEquals(Arrays.asList("a", "keep"), Arrays.asList(top.childrenNames()), "children");
            Bulk.expect(root.nodeExists("/cn1/test/remove/a/b"), "a/b exists");

            Preferences doomed = root.node("/cn1/test/remove/a");
            doomed.removeNode();
            for (Preferences gone : new Preferences[] {doomed, a, b}) {
                String n = gone.absolutePath();
                Bulk.expectEquals("IllegalStateException",
                        thrown(IllegalStateException.class, () -> gone.get("k", "d")), "get on removed " + n);
                Bulk.expectEquals("IllegalStateException",
                        thrown(IllegalStateException.class, () -> gone.put("k", "d")), "put on removed " + n);
                Bulk.expectEquals("IllegalStateException",
                        thrown(IllegalStateException.class, () -> gone.sync()), "sync on removed " + n);
                Bulk.expect(!gone.nodeExists(""), "removed " + n + " does not exist");
            }
            // As the JDK: "Node already removed."
            Bulk.expectEquals("IllegalStateException",
                    thrown(IllegalStateException.class, () -> doomed.removeNode()), "removed twice");
            Bulk.expectEquals("a", doomed.name(), "name of a removed node");
            Bulk.expectEquals("/cn1/test/remove/a", doomed.absolutePath(), "path of a removed node");
            Bulk.expect(!root.nodeExists("/cn1/test/remove/a"), "a is gone");
            Bulk.expect(!root.nodeExists("/cn1/test/remove/a/b"), "a/b is gone");
            Bulk.expectEquals(Arrays.asList("keep"), Arrays.asList(top.childrenNames()), "children after");
            Bulk.expectEquals("w", root.node("/cn1/test/remove/keep").get("z", null), "keep is kept");
            Preferences fresh = root.node("/cn1/test/remove/a");
            Bulk.expectEquals("gone", fresh.get("k", "gone"), "a recreated is empty");
            Bulk.expectEquals("gone", fresh.node("b").get("x", "gone"), "a/b recreated is empty");
            Bulk.expectEquals(Integer.valueOf(0), Integer.valueOf(fresh.node("b").keys().length), "a/b keys");
            fresh.put("k", "again");
            Bulk.expectEquals("again", root.node("/cn1/test/remove/a").get("k", null), "a written again");
            top.removeNode();
            Bulk.expect(!root.nodeExists("/cn1/test/remove"), "the top is gone");
        });
        // Typed values are stored as text and read back as the JDK reads them.
        Bulk.cn1Only("Preferences.typedValuesReadAsTheJdkReadsThem", () -> {
            Preferences p = Preferences.userRoot().node("/cn1/test/typed");
            p.putInt("i", 42);
            p.putLong("l", -7L);
            p.putBoolean("b", true);
            p.putFloat("f", 1.5f);
            p.putDouble("d", 2.25);
            p.put("spaced", " 12 ");
            p.put("bool", "TRUE");
            p.put("junk", "x");
            Bulk.expectEquals("42", p.get("i", null), "int as text");
            Bulk.expectEquals(Integer.valueOf(42), Integer.valueOf(p.getInt("i", 0)), "getInt");
            Bulk.expectEquals(Long.valueOf(-7L), Long.valueOf(p.getLong("l", 0L)), "getLong");
            Bulk.expect(p.getBoolean("b", false), "getBoolean");
            Bulk.expectEquals(Float.valueOf(1.5f), Float.valueOf(p.getFloat("f", 0f)), "getFloat");
            Bulk.expectEquals(Double.valueOf(2.25), Double.valueOf(p.getDouble("d", 0.0)), "getDouble");
            Bulk.expectEquals(Integer.valueOf(12), Integer.valueOf(p.getInt("spaced", 0)), "a spaced int");
            Bulk.expect(p.getBoolean("bool", false), "TRUE");
            Bulk.expectEquals(Integer.valueOf(9), Integer.valueOf(p.getInt("junk", 9)), "junk int");
            Bulk.expect(!p.getBoolean("junk", false), "junk boolean");
            Bulk.expectEquals(null, p.get("absent", null), "absent");
            Bulk.expectEquals("NullPointerException",
                    thrown(NullPointerException.class, () -> p.put("k", null)), "a null value");
            Bulk.expectEquals("NullPointerException",
                    thrown(NullPointerException.class, () -> p.get(null, "d")), "a null key");
        });
        Bulk.cn1Only("Preferences.namesAndPathsAsTheJdk", () -> {
            Preferences root = Preferences.userRoot();
            Bulk.expectEquals("", root.name(), "root name");
            Bulk.expectEquals("/", root.absolutePath(), "root path");
            Bulk.expectEquals(null, root.parent(), "root parent");
            Preferences n = root.node("x/y");
            Bulk.expectEquals("y", n.name(), "name");
            Bulk.expectEquals("/x/y", n.absolutePath(), "path");
            Bulk.expectEquals("/x", n.parent().absolutePath(), "parent");
            Bulk.expectEquals("User Preference Node: /x/y", n.toString(), "toString");
            // The original named its own package; this program is in the
            // unnamed one, so a JDK class stands in.
            Bulk.expectEquals("/java/lang", Preferences.userNodeForPackage(String.class).absolutePath(),
                    "userNodeForPackage");
            Bulk.expectEquals("IllegalArgumentException",
                    thrown(IllegalArgumentException.class, () -> Preferences.userNodeForPackage(String[].class)),
                    "an array class");
            // The JDK's constants.
            Bulk.expectEquals(Integer.valueOf(80), Integer.valueOf(Preferences.MAX_KEY_LENGTH), "MAX_KEY_LENGTH");
            Bulk.expectEquals(Integer.valueOf(8192), Integer.valueOf(Preferences.MAX_VALUE_LENGTH),
                    "MAX_VALUE_LENGTH");
            Bulk.expectEquals(Integer.valueOf(80), Integer.valueOf(Preferences.MAX_NAME_LENGTH), "MAX_NAME_LENGTH");
        });
        // The entries go through the host, so a Codename One application keeps
        // them in its own store.
        Bulk.cn1Only("Preferences.writesThroughTheHost", () -> {
            final Map<String, String> store = new HashMap<String, String>();
            JdkHost.install(new JdkHost() {
                @Override
                public String preference(String key) {
                    return store.get(key);
                }

                @Override
                public void setPreference(String key, String value) {
                    store.put(key, value);
                }

                @Override
                public void deletePreference(String key) {
                    store.remove(key);
                }
            });
            Preferences.userRoot().node("/host").put("k", "v");
            Bulk.expectEquals("v", store.get("/host\nk"), "the entry");
            Bulk.expectEquals("k\n", store.get("/host\n\n"), "the node's key index");
            Bulk.expectEquals("host\n", store.get("/\n\n\n"), "the root's child index");
        });
    }
}
