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
package com.codename1.jdkparity;

import com.codename1.impl.JdkHost;
import jdkparity.java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JavaAPI's {@code java.util.prefs.Preferences} over the host's flat store,
 * with the hardening the compat layer's copy earned: escaped keys that never
 * alias another node's entries, a per-node key index kept whole under
 * concurrent writers, and removeNode that removes the subtree and leaves every
 * instance for a removed node throwing. Each expectation is what
 * {@code java.util.prefs} answers on JDK 8 and 17; the JDK's own store is not
 * touched here, since it writes to the user's real preferences.
 *
 * Runs over JdkHost's defaults, which keep preferences in memory.
 */
class PreferencesParityTest {

    @BeforeEach
    void defaults() {
        JdkHost.install(null);
    }

    @AfterEach
    void restore() {
        JdkHost.install(null);
    }

    /** Two threads adding keys to one node, each through an instance of its own: every key is kept. */
    @Test
    void keysAddedFromSeveralThreadsAreAllIndexed() throws Exception {
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
        assertEquals(200, prefs.keys().length);
        for (int t = 0; t < writers.length; t++) {
            for (int i = 0; i < 50; i++) {
                assertEquals("v", prefs.get("k" + t + "." + i, null), "k" + t + "." + i);
            }
        }
        prefs.put("a\nb", "1");
        prefs.put("c\\nd", "2");
        List<String> keys = Arrays.asList(prefs.keys());
        assertTrue(keys.contains("a\nb"), keys.toString());
        assertTrue(keys.contains("c\\nd"), keys.toString());
        assertEquals(202, keys.size());
        prefs.remove("a\nb");
        assertFalse(Arrays.asList(prefs.keys()).contains("a\nb"));
        prefs.clear();
        assertEquals(0, prefs.keys().length);
        assertEquals("gone", prefs.get("c\\nd", "gone"));
    }

    /** A key may hold a slash or a line break, and none of it makes one entry another's. */
    @Test
    void keysNeverAliasAnotherNodesEntries() throws Exception {
        Preferences a = Preferences.userRoot().node("/cn1/test/alias/a");
        Preferences ab = Preferences.userRoot().node("/cn1/test/alias/a/b");
        a.put("b/c", "x");
        ab.put("c", "y");
        a.put("\nkeys", "not the index");
        a.put("\n", "line break");
        assertEquals("x", a.get("b/c", null));
        assertEquals("y", ab.get("c", null));
        assertEquals("not the index", a.get("\nkeys", null));
        assertEquals("line break", a.get("\n", null));
        assertEquals(Arrays.asList("b/c", "\nkeys", "\n"), Arrays.asList(a.keys()));
        assertEquals(Arrays.asList("c"), Arrays.asList(ab.keys()));
        a.clear();
        ab.clear();
        assertEquals("y", ab.get("c", "y"));
        assertEquals(0, a.keys().length);
    }

    @Test
    void removeNodeRemovesTheWholeSubtree() throws Exception {
        assertThrows(UnsupportedOperationException.class, () -> Preferences.userRoot().removeNode());
        Preferences root = Preferences.userRoot();
        Preferences a = root.node("/cn1/test/remove/a");
        Preferences b = a.node("b");
        a.put("k", "v");
        b.put("x", "y");
        Preferences.userRoot().node("/cn1/test/remove/keep").put("z", "w");
        Preferences top = root.node("/cn1/test/remove");
        assertEquals(Arrays.asList("a", "keep"), Arrays.asList(top.childrenNames()));
        assertTrue(root.nodeExists("/cn1/test/remove/a/b"));

        Preferences doomed = root.node("/cn1/test/remove/a");
        doomed.removeNode();
        for (Preferences gone : new Preferences[] {doomed, a, b}) {
            assertThrows(IllegalStateException.class, () -> gone.get("k", "d"));
            assertThrows(IllegalStateException.class, () -> gone.put("k", "d"));
            assertThrows(IllegalStateException.class, () -> gone.sync());
            assertFalse(gone.nodeExists(""));
        }
        try {
            doomed.removeNode();
            fail("a node was removed twice");
        } catch (IllegalStateException expected) {
            // As the JDK: "Node already removed."
        }
        assertEquals("a", doomed.name());
        assertEquals("/cn1/test/remove/a", doomed.absolutePath());
        assertFalse(root.nodeExists("/cn1/test/remove/a"));
        assertFalse(root.nodeExists("/cn1/test/remove/a/b"));
        assertEquals(Arrays.asList("keep"), Arrays.asList(top.childrenNames()));
        assertEquals("w", root.node("/cn1/test/remove/keep").get("z", null));
        Preferences fresh = root.node("/cn1/test/remove/a");
        assertEquals("gone", fresh.get("k", "gone"));
        assertEquals("gone", fresh.node("b").get("x", "gone"));
        assertEquals(0, fresh.node("b").keys().length);
        fresh.put("k", "again");
        assertEquals("again", root.node("/cn1/test/remove/a").get("k", null));
        top.removeNode();
        assertFalse(root.nodeExists("/cn1/test/remove"));
    }

    /** Typed values are stored as text and read back as the JDK reads them. */
    @Test
    void typedValuesReadAsTheJdkReadsThem() throws Exception {
        Preferences p = Preferences.userRoot().node("/cn1/test/typed");
        p.putInt("i", 42);
        p.putLong("l", -7L);
        p.putBoolean("b", true);
        p.putFloat("f", 1.5f);
        p.putDouble("d", 2.25);
        p.put("spaced", " 12 ");
        p.put("bool", "TRUE");
        p.put("junk", "x");
        assertEquals("42", p.get("i", null));
        assertEquals(42, p.getInt("i", 0));
        assertEquals(-7L, p.getLong("l", 0L));
        assertTrue(p.getBoolean("b", false));
        assertEquals(1.5f, p.getFloat("f", 0f));
        assertEquals(2.25, p.getDouble("d", 0.0));
        assertEquals(12, p.getInt("spaced", 0));
        assertTrue(p.getBoolean("bool", false));
        assertEquals(9, p.getInt("junk", 9));
        assertFalse(p.getBoolean("junk", false));
        assertNull(p.get("absent", null));
        assertThrows(NullPointerException.class, () -> p.put("k", null));
        assertThrows(NullPointerException.class, () -> p.get(null, "d"));
    }

    @Test
    void namesAndPathsAsTheJdk() throws Exception {
        Preferences root = Preferences.userRoot();
        assertEquals("", root.name());
        assertEquals("/", root.absolutePath());
        assertNull(root.parent());
        Preferences n = root.node("x/y");
        assertEquals("y", n.name());
        assertEquals("/x/y", n.absolutePath());
        assertEquals("/x", n.parent().absolutePath());
        assertEquals("User Preference Node: /x/y", n.toString());
        assertEquals("/com/codename1/jdkparity",
                Preferences.userNodeForPackage(PreferencesParityTest.class).absolutePath());
        assertThrows(IllegalArgumentException.class, () -> Preferences.userNodeForPackage(String[].class));
        assertEquals(java.util.prefs.Preferences.MAX_KEY_LENGTH, Preferences.MAX_KEY_LENGTH);
        assertEquals(java.util.prefs.Preferences.MAX_VALUE_LENGTH, Preferences.MAX_VALUE_LENGTH);
        assertEquals(java.util.prefs.Preferences.MAX_NAME_LENGTH, Preferences.MAX_NAME_LENGTH);
    }

    /** The entries go through the host, so a Codename One application keeps them in its own store. */
    @Test
    void writesThroughTheHost() throws Exception {
        final java.util.Map<String, String> store = new java.util.HashMap<String, String>();
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
        assertEquals("v", store.get("/host\nk"));
        assertEquals("k\n", store.get("/host\n\n"));
        assertEquals("host\n", store.get("/\n\n\n"));
    }
}
