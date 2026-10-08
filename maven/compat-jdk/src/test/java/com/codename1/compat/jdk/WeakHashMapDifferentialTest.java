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
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds the [WeakHashMap] shim against `java.util.WeakHashMap` while every
/// key is strongly held, which is the only time the two are comparable: what
/// a collector clears, and when, is not something either promises.
public class WeakHashMapDifferentialTest {

    /// Equal by value and colliding on purpose, so that a bucket holds
    /// several keys.
    private static final class Key implements Comparable<Key> {
        final int id;

        Key(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key && ((Key) o).id == id;
        }

        @Override
        public int hashCode() {
            return id % 7;
        }

        @Override
        public int compareTo(Key o) {
            return id < o.id ? -1 : id == o.id ? 0 : 1;
        }

        @Override
        public String toString() {
            return "k" + id;
        }
    }

    private static String sorted(Map<Key, String> map) {
        return new TreeMap<Key, String>(map).toString();
    }

    @Test
    public void aRandomSequenceOfOperationsMatchesTheJdk() {
        Random random = new Random(20260917L);
        // Strong references to every key ever used: nothing may be cleared.
        List<Key> held = new ArrayList<Key>();
        for (int i = 0; i < 40; i++) {
            held.add(new Key(i));
        }
        java.util.WeakHashMap<Key, String> theirs = new java.util.WeakHashMap<Key, String>();
        WeakHashMap<Key, String> mine = new WeakHashMap<Key, String>(4);
        for (int step = 0; step < 4000; step++) {
            Key key = held.get(random.nextInt(held.size()));
            // An equal key that is another object: found by equals, not identity.
            Key equal = new Key(key.id);
            String value = random.nextInt(8) == 0 ? null : "v" + step;
            switch (random.nextInt(6)) {
                case 0:
                case 1:
                    assertEquals(theirs.put(key, value), mine.put(key, value));
                    break;
                case 2:
                    assertEquals(theirs.remove(equal), mine.remove(equal));
                    break;
                case 3:
                    assertEquals(theirs.get(equal), mine.get(equal));
                    assertEquals(theirs.containsKey(equal), mine.containsKey(equal));
                    break;
                case 4:
                    assertEquals(theirs.containsValue(value), mine.containsValue(value));
                    break;
                default:
                    if (random.nextInt(200) == 0) {
                        theirs.clear();
                        mine.clear();
                    }
                    break;
            }
            assertEquals(theirs.size(), mine.size());
            assertEquals(theirs.isEmpty(), mine.isEmpty());
        }
        assertEquals(sorted(theirs), sorted(mine));
        assertEquals(theirs.keySet().size(), mine.keySet().size());
        assertTrue(theirs.keySet().containsAll(mine.keySet()));
        assertEquals(new ArrayList<String>(new TreeMap<Key, String>(theirs).values()),
                new ArrayList<String>(new TreeMap<Key, String>(mine).values()));
    }

    @Test
    public void theNullKeyIsAKeyLikeAnyOther() {
        java.util.WeakHashMap<Object, String> theirs = new java.util.WeakHashMap<Object, String>();
        WeakHashMap<Object, String> mine = new WeakHashMap<Object, String>();
        assertEquals(theirs.put(null, "a"), mine.put(null, "a"));
        assertEquals(theirs.put(null, "b"), mine.put(null, "b"));
        assertEquals(theirs.get(null), mine.get(null));
        assertEquals(theirs.containsKey(null), mine.containsKey(null));
        assertEquals(theirs.size(), mine.size());
        assertEquals(theirs.remove(null), mine.remove(null));
        assertEquals(theirs.containsKey(null), mine.containsKey(null));
        assertTrue(mine.isEmpty());
    }

    @Test
    public void theConstructorsRejectWhatTheJdkRejects() {
        int[][] arguments = {{-1, 1}, {4, 0}, {4, -1}};
        for (int[] a : arguments) {
            boolean theirsThrew = false;
            try {
                new java.util.WeakHashMap<Object, Object>(a[0], a[1]);
            } catch (IllegalArgumentException e) {
                theirsThrew = true;
            }
            try {
                new WeakHashMap<Object, Object>(a[0], a[1]);
                assertFalse("capacity " + a[0] + ", load factor " + a[1], theirsThrew);
            } catch (IllegalArgumentException e) {
                assertTrue("capacity " + a[0] + ", load factor " + a[1], theirsThrew);
            }
        }
        try {
            new WeakHashMap<Object, Object>(4, Float.NaN);
            fail("NaN is no load factor");
        } catch (IllegalArgumentException expected) {
            assertNull(expected.getCause());
        }
        Map<String, String> source = new TreeMap<String, String>();
        source.put("a", "1");
        source.put("b", "2");
        assertEquals(new java.util.WeakHashMap<String, String>(source), new WeakHashMap<String, String>(source));
    }

    /// The keys are held weakly: an entry does not keep its key alive. A
    /// collector is asked, not ordered, so the test gives it time and makes
    /// no claim when the key is still there at the end.
    @Test
    public void anEntryDoesNotKeepItsKeyAlive() throws Exception {
        WeakHashMap<Object, String> map = new WeakHashMap<Object, String>();
        Object kept = new Object();
        map.put(kept, "kept");
        map.put(new Object(), "dropped");
        for (int i = 0; i < 50 && map.size() > 1; i++) {
            System.gc();
            Thread.sleep(20);
        }
        assertEquals("kept", map.get(kept));
        assertTrue(map.size() >= 1 && map.size() <= 2);
        if (map.size() == 1) {
            assertSame(kept, map.keySet().iterator().next());
            assertFalse(map.containsValue("dropped"));
        }
    }

    /// `ClassLoader.loadClass`, which the remap sends to
    /// [Resources#loadClass]: the class when the application has it, and the
    /// desktop's exception when it does not.
    @Test
    public void loadClassAnswersAsAClassLoaderDoes() throws Exception {
        ClassLoader loader = WeakHashMapDifferentialTest.class.getClassLoader();
        assertSame(loader.loadClass("com.codename1.compat.jdk.Resources"),
                Resources.loadClass(loader, "com.codename1.compat.jdk.Resources"));
        try {
            Resources.loadClass(loader, "no.such.Thing");
            fail("There is no such class");
        } catch (ClassNotFoundException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("no.such.Thing"));
        }
        try {
            Resources.loadClass(null, "java.lang.String");
            fail("A null loader is a NullPointerException on a desktop");
        } catch (NullPointerException expected) {
            assertNull(expected.getCause());
        }
    }
}
