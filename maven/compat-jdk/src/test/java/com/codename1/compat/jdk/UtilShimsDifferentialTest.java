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
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds the `java.util` and `java.util.concurrent` shims against the JDK
/// classes they are named after.
public class UtilShimsDifferentialTest {

    // ------------------------------------------------------------------
    // Optional
    // ------------------------------------------------------------------

    @Test
    public void optionalMatchesTheJdk() {
        String[] values = {null, "", "a", "hello"};
        for (String value : values) {
            java.util.Optional<String> theirs = java.util.Optional.ofNullable(value);
            Optional<String> mine = Optional.ofNullable(value);
            assertEquals(theirs.isPresent(), mine.isPresent());
            assertEquals(theirs.toString(), mine.toString());
            assertEquals(theirs.hashCode(), mine.hashCode());
            assertEquals(theirs.orElse("other"), mine.orElse("other"));
            assertEquals(theirs.orElseGet(() -> "made"), mine.orElseGet(() -> "made"));
            assertEquals(theirs.map(String::length).toString(), mine.map(String::length).toString());
            assertEquals(theirs.map(s -> (String) null).isPresent(), mine.map(s -> (String) null).isPresent());
            assertEquals(theirs.filter(s -> s.length() > 1).toString(), mine.filter(s -> s.length() > 1).toString());
            assertEquals(theirs.flatMap(s -> java.util.Optional.of(s + "!")).toString(),
                    mine.flatMap(s -> Optional.of(s + "!")).toString());
            assertEquals(theirs.flatMap(s -> java.util.Optional.<String>empty()).toString(),
                    mine.flatMap(s -> Optional.<String>empty()).toString());
            List<String> theirSeen = new ArrayList<String>();
            List<String> mySeen = new ArrayList<String>();
            theirs.ifPresent(theirSeen::add);
            mine.ifPresent(mySeen::add);
            assertEquals(theirSeen, mySeen);
            String theirGet;
            String myGet;
            try {
                theirGet = theirs.get();
            } catch (NoSuchElementException e) {
                theirGet = "NoSuchElementException:" + e.getMessage();
            }
            try {
                myGet = mine.get();
            } catch (NoSuchElementException e) {
                myGet = "NoSuchElementException:" + e.getMessage();
            }
            assertEquals(theirGet, myGet);
            try {
                assertEquals(value, mine.orElseThrow(() -> new IllegalStateException("absent")));
                assertTrue(theirs.isPresent());
            } catch (IllegalStateException e) {
                assertFalse(theirs.isPresent());
                assertEquals("absent", e.getMessage());
            }
        }
        assertEquals(Optional.of("a"), Optional.of("a"));
        assertNotEquals(Optional.of("a"), Optional.of("b"));
        assertNotEquals(Optional.of("a"), Optional.empty());
        assertSame(Optional.empty(), Optional.empty());
        assertSame(Optional.empty(), Optional.ofNullable(null));
    }

    @Test
    public void optionalRejectsNullLikeTheJdk() {
        try {
            Optional.of(null);
            fail("of(null)");
        } catch (NullPointerException expected) {
            // As the JDK.
        }
        try {
            Optional.of("a").map(null);
            fail("map(null)");
        } catch (NullPointerException expected) {
            // As the JDK.
        }
        try {
            Optional.of("a").flatMap(s -> null);
            fail("flatMap answering null");
        } catch (NullPointerException expected) {
            // As the JDK.
        }
        try {
            Optional.empty().filter(null);
            fail("filter(null)");
        } catch (NullPointerException expected) {
            // As the JDK, which checks the predicate even when empty.
        }
    }

    // ------------------------------------------------------------------
    // StringJoiner, EventObject, TimeUnit
    // ------------------------------------------------------------------

    @Test
    public void stringJoinerMatchesTheJdk() {
        String[][] parts = {{}, {"a"}, {"a", "b"}, {"", ""}, {"x", "", "z"}, {"null", "1"}};
        for (String[] elements : parts) {
            for (int variant = 0; variant < 4; variant++) {
                java.util.StringJoiner theirs = variant < 2
                        ? new java.util.StringJoiner(", ") : new java.util.StringJoiner("-", "[", "]");
                StringJoiner mine = variant < 2 ? new StringJoiner(", ") : new StringJoiner("-", "[", "]");
                if (variant % 2 == 1) {
                    theirs.setEmptyValue("EMPTY");
                    mine.setEmptyValue("EMPTY");
                }
                assertEquals(theirs.toString(), mine.toString());
                assertEquals(theirs.length(), mine.length());
                for (String element : elements) {
                    theirs.add(element);
                    mine.add(element);
                    assertEquals(theirs.toString(), mine.toString());
                    assertEquals(theirs.length(), mine.length());
                }
                java.util.StringJoiner theirOther = new java.util.StringJoiner("+", "<", ">");
                StringJoiner myOther = new StringJoiner("+", "<", ">");
                theirs.merge(theirOther);
                mine.merge(myOther);
                assertEquals(theirs.toString(), mine.toString());
                theirOther.add("p").add("q");
                myOther.add("p").add("q");
                theirs.merge(theirOther);
                mine.merge(myOther);
                assertEquals(theirs.toString(), mine.toString());
                theirs.merge(theirs);
                mine.merge(mine);
                assertEquals(theirs.toString(), mine.toString());
                theirs.add(null);
                mine.add(null);
                assertEquals(theirs.toString(), mine.toString());
            }
        }
        try {
            new StringJoiner(null);
            fail("null delimiter");
        } catch (NullPointerException expected) {
            // As the JDK.
        }
    }

    @Test
    public void eventObjectMatchesTheJdk() {
        Object source = "the source";
        EventObject mine = new EventObject(source);
        java.util.EventObject theirs = new java.util.EventObject(source);
        assertSame(source, mine.getSource());
        assertEquals(theirs.toString().replace("java.util.EventObject", EventObject.class.getName()),
                mine.toString());
        try {
            new EventObject(null);
            fail("null source");
        } catch (IllegalArgumentException e) {
            assertEquals("null source", e.getMessage());
        }
    }

    @Test
    public void timeUnitMatchesTheJdk() {
        long[] durations = {
            0, 1, -1, 59, 60, 999, 1000, 1001, 86399, 86400, 1234567890123L, -1234567890123L,
            Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE / 1000, Long.MAX_VALUE / 1000 + 1,
            Long.MIN_VALUE / 1000 - 1, 9223372036854L, 9223372036855L, 106751991167L,
        };
        java.util.concurrent.TimeUnit[] theirs = java.util.concurrent.TimeUnit.values();
        TimeUnit[] mine = TimeUnit.values();
        assertEquals(theirs.length, mine.length);
        for (int i = 0; i < theirs.length; i++) {
            assertEquals(theirs[i].name(), mine[i].name());
            assertEquals(theirs[i].ordinal(), mine[i].ordinal());
            for (long duration : durations) {
                String what = theirs[i] + " " + duration;
                assertEquals(what, theirs[i].toNanos(duration), mine[i].toNanos(duration));
                assertEquals(what, theirs[i].toMicros(duration), mine[i].toMicros(duration));
                assertEquals(what, theirs[i].toMillis(duration), mine[i].toMillis(duration));
                assertEquals(what, theirs[i].toSeconds(duration), mine[i].toSeconds(duration));
                assertEquals(what, theirs[i].toMinutes(duration), mine[i].toMinutes(duration));
                assertEquals(what, theirs[i].toHours(duration), mine[i].toHours(duration));
                assertEquals(what, theirs[i].toDays(duration), mine[i].toDays(duration));
                for (int j = 0; j < theirs.length; j++) {
                    assertEquals(what + " from " + theirs[j], theirs[i].convert(duration, theirs[j]),
                            mine[i].convert(duration, mine[j]));
                }
            }
        }
        assertSame(TimeUnit.SECONDS, TimeUnit.valueOf("SECONDS"));
    }

    @Test
    public void exceptionsCarryWhatTheJdkOnesCarry() {
        Throwable cause = new IllegalStateException("why");
        assertEquals(new java.util.concurrent.ExecutionException(cause).getMessage(),
                new ExecutionException(cause).getMessage());
        assertSame(cause, new ExecutionException("m", cause).getCause());
        assertEquals("m", new CancellationException("m").getMessage());
        assertNull(new CancellationException().getMessage());
        assertEquals("m", new TimeoutException("m").getMessage());
        MissingResourceException missing = new MissingResourceException("text", "Class", "key");
        assertEquals("text", missing.getMessage());
        assertEquals("Class", missing.getClassName());
        assertEquals("key", missing.getKey());
        assertEquals("bad", new MalformedURLException("bad").getMessage());
        assertTrue(new MalformedURLException() instanceof java.io.IOException);
    }

    // ------------------------------------------------------------------
    // ConcurrentHashMap
    // ------------------------------------------------------------------

    private static String sorted(Map<String, Integer> map) {
        return new TreeMap<String, Integer>(map).toString();
    }

    @Test
    public void concurrentHashMapFollowsTheJdkThroughRandomOperations() {
        Random random = new Random(20260101L);
        for (int round = 0; round < 200; round++) {
            java.util.concurrent.ConcurrentHashMap<String, Integer> theirs =
                    new java.util.concurrent.ConcurrentHashMap<String, Integer>();
            ConcurrentHashMap<String, Integer> mine = new ConcurrentHashMap<String, Integer>();
            StringBuilder trace = new StringBuilder();
            for (int step = 0; step < 60; step++) {
                String key = "k" + random.nextInt(8);
                Integer value = Integer.valueOf(random.nextInt(5));
                int op = random.nextInt(16);
                trace.append(op).append(':').append(key).append('=').append(value).append(' ');
                Object expected;
                Object actual;
                switch (op) {
                    case 0:
                        expected = theirs.put(key, value);
                        actual = mine.put(key, value);
                        break;
                    case 1:
                        expected = theirs.remove(key);
                        actual = mine.remove(key);
                        break;
                    case 2:
                        expected = theirs.putIfAbsent(key, value);
                        actual = mine.putIfAbsent(key, value);
                        break;
                    case 3:
                        expected = Boolean.valueOf(theirs.remove(key, value));
                        actual = Boolean.valueOf(mine.remove(key, value));
                        break;
                    case 4:
                        expected = theirs.replace(key, value);
                        actual = mine.replace(key, value);
                        break;
                    case 5:
                        expected = Boolean.valueOf(theirs.replace(key, value, Integer.valueOf(9)));
                        actual = Boolean.valueOf(mine.replace(key, value, Integer.valueOf(9)));
                        break;
                    case 6:
                        expected = theirs.getOrDefault(key, Integer.valueOf(-1));
                        actual = mine.getOrDefault(key, Integer.valueOf(-1));
                        break;
                    case 7:
                        expected = theirs.computeIfAbsent(key, k -> value.intValue() == 0 ? null : value);
                        actual = mine.computeIfAbsent(key, k -> value.intValue() == 0 ? null : value);
                        break;
                    case 8:
                        expected = theirs.computeIfPresent(key, (k, v) -> v.intValue() == 0 ? null : v + 1);
                        actual = mine.computeIfPresent(key, (k, v) -> v.intValue() == 0 ? null : v + 1);
                        break;
                    case 9:
                        expected = theirs.compute(key,
                                (k, v) -> v == null ? value : (v.intValue() == 1 ? null : Integer.valueOf(v + 10)));
                        actual = mine.compute(key,
                                (k, v) -> v == null ? value : (v.intValue() == 1 ? null : Integer.valueOf(v + 10)));
                        break;
                    case 10:
                        expected = theirs.merge(key, value, (a, b) -> a + b == 4 ? null : Integer.valueOf(a + b));
                        actual = mine.merge(key, value, (a, b) -> a + b == 4 ? null : Integer.valueOf(a + b));
                        break;
                    case 11:
                        expected = theirs.containsKey(key) + "/" + theirs.containsValue(value) + "/"
                                + theirs.contains(value) + "/" + theirs.get(key);
                        actual = mine.containsKey(key) + "/" + mine.containsValue(value) + "/"
                                + mine.contains(value) + "/" + mine.get(key);
                        break;
                    case 12:
                        theirs.replaceAll((k, v) -> Integer.valueOf(v + 1));
                        mine.replaceAll((k, v) -> Integer.valueOf(v + 1));
                        expected = null;
                        actual = null;
                        break;
                    case 13:
                        expected = Boolean.valueOf(theirs.keySet().remove(key));
                        actual = Boolean.valueOf(mine.keySet().remove(key));
                        break;
                    case 14:
                        expected = Boolean.valueOf(theirs.values().remove(value));
                        actual = Boolean.valueOf(mine.values().remove(value));
                        break;
                    default:
                        expected = Boolean.valueOf(theirs.entrySet().removeIf(e -> e.getValue().equals(value)));
                        actual = Boolean.valueOf(mine.entrySet().removeIf(e -> e.getValue().equals(value)));
                        break;
                }
                assertEquals(trace.toString(), expected, actual);
                assertEquals(trace.toString(), sorted(theirs), sorted(mine));
                assertEquals(trace.toString(), theirs.size(), mine.size());
                assertEquals(trace.toString(), theirs.mappingCount(), mine.mappingCount());
                assertEquals(trace.toString(), theirs.isEmpty(), mine.isEmpty());
                assertEquals(trace.toString(), theirs.hashCode(), mine.hashCode());
                assertTrue(trace.toString(), mine.equals(theirs));
                assertTrue(trace.toString(), theirs.equals(mine));
            }
        }
    }

    @Test
    public void concurrentHashMapRejectsNullLikeTheJdk() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        map.put("a", "1");
        Runnable[] attempts = {
            () -> map.put(null, "x"),
            () -> map.put("x", null),
            () -> map.get(null),
            () -> map.containsKey(null),
            () -> map.containsValue(null),
            () -> map.remove(null),
            () -> map.putIfAbsent("x", null),
            () -> map.putIfAbsent(null, "x"),
            () -> map.replace("a", null),
            () -> map.replace("a", "1", null),
            () -> map.merge("a", null, (x, y) -> x),
            () -> map.computeIfAbsent(null, k -> "x"),
            () -> map.getOrDefault(null, "x"),
            () -> map.putAll(Collections.<String, String>singletonMap("x", null)),
        };
        for (int i = 0; i < attempts.length; i++) {
            try {
                attempts[i].run();
                fail("attempt " + i + " accepted a null");
            } catch (NullPointerException expected) {
                // As the JDK.
            }
        }
        assertEquals("{a=1}", map.toString());
        // A value that is not there is simply not removed, null or not.
        assertFalse(map.remove("a", null));
    }

    @Test
    public void concurrentHashMapIteratesWhileItChanges() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<String, Integer>();
        for (int i = 0; i < 10; i++) {
            map.put("k" + i, Integer.valueOf(i));
        }
        // The JDK's iterators never throw ConcurrentModificationException;
        // this one walks the keys present when it was created.
        int seen = 0;
        for (String key : map.keySet()) {
            map.remove(key);
            map.put("new" + key, Integer.valueOf(0));
            seen++;
        }
        assertEquals(10, seen);
        assertEquals(10, map.size());
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            entry.setValue(Integer.valueOf(7));
            map.put("again" + entry.getKey(), Integer.valueOf(1));
        }
        assertEquals(20, map.size());
        assertEquals(Integer.valueOf(7), map.get("newk3"));
        Iterator<Integer> values = map.values().iterator();
        while (values.hasNext()) {
            if (values.next().intValue() == 1) {
                values.remove();
            }
        }
        assertEquals(10, map.size());
        map.forEach((k, v) -> map.put(k + "!", v));
        assertEquals(20, map.size());

        int count = 0;
        for (Enumeration<String> keys = map.keys(); keys.hasMoreElements();) {
            assertTrue(map.containsKey(keys.nextElement()));
            count++;
        }
        assertEquals(20, count);
        count = 0;
        for (Enumeration<Integer> elements = map.elements(); elements.hasMoreElements();) {
            assertEquals(Integer.valueOf(7), elements.nextElement());
            count++;
        }
        assertEquals(20, count);
    }

    @Test
    public void keySetViewsBehaveLikeTheJdk() {
        ConcurrentHashMap.KeySetView<String, Boolean> mine = ConcurrentHashMap.newKeySet();
        java.util.concurrent.ConcurrentHashMap.KeySetView<String, Boolean> theirs =
                java.util.concurrent.ConcurrentHashMap.newKeySet();
        for (String s : new String[] {"a", "b", "a", "c"}) {
            assertEquals(Boolean.valueOf(theirs.add(s)), Boolean.valueOf(mine.add(s)));
        }
        assertEquals(theirs.size(), mine.size());
        assertEquals(Boolean.valueOf(theirs.remove("b")), Boolean.valueOf(mine.remove("b")));
        assertEquals(Boolean.valueOf(theirs.contains("b")), Boolean.valueOf(mine.contains("b")));
        assertTrue(mine.containsAll(Arrays.asList("a", "c")));
        assertEquals(theirs, mine);
        assertEquals(theirs.hashCode(), mine.hashCode());

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<String, Integer>();
        ConcurrentHashMap.KeySetView<String, Integer> view = map.keySet(Integer.valueOf(5));
        assertTrue(view.add("x"));
        assertFalse(view.add("x"));
        assertEquals(Integer.valueOf(5), map.get("x"));
        assertEquals(Integer.valueOf(5), view.getMappedValue());
        assertSame(map, view.getMap());
        try {
            map.keySet().add("y");
            fail("a plain key set has no value to map new keys to");
        } catch (UnsupportedOperationException expected) {
            // As the JDK.
        }
    }

    // ------------------------------------------------------------------
    // CopyOnWriteArrayList
    // ------------------------------------------------------------------

    @Test
    public void copyOnWriteArrayListFollowsTheJdkThroughRandomOperations() {
        Random random = new Random(42L);
        for (int round = 0; round < 200; round++) {
            java.util.concurrent.CopyOnWriteArrayList<Integer> theirs =
                    new java.util.concurrent.CopyOnWriteArrayList<Integer>();
            CopyOnWriteArrayList<Integer> mine = new CopyOnWriteArrayList<Integer>();
            StringBuilder trace = new StringBuilder();
            for (int step = 0; step < 50; step++) {
                Integer value = Integer.valueOf(random.nextInt(6));
                int index = random.nextInt(theirs.size() + 2) - 1;
                int op = random.nextInt(18);
                trace.append(op).append(':').append(index).append('=').append(value).append(' ');
                Object expected;
                Object actual;
                try {
                    expected = apply(theirs, theirs, op, index, value);
                } catch (RuntimeException e) {
                    expected = e.getClass().getName();
                }
                try {
                    actual = apply(mine, mine, op, index, value);
                } catch (RuntimeException e) {
                    actual = e.getClass().getName();
                }
                assertEquals(trace.toString(), expected, actual);
                assertEquals(trace.toString(), theirs.toString(), mine.toString());
                assertEquals(trace.toString(), theirs.hashCode(), mine.hashCode());
                assertTrue(trace.toString(), mine.equals(theirs));
                assertTrue(trace.toString(), theirs.equals(mine));
                assertEquals(trace.toString(), Arrays.toString(theirs.toArray()), Arrays.toString(mine.toArray()));
                assertEquals(trace.toString(), Arrays.toString(theirs.toArray(new Integer[0])),
                        Arrays.toString(mine.toArray(new Integer[0])));
            }
        }
    }

    /// One operation, applied the same way to either list. `list` and
    /// `typed` are the same object: once as the interface both classes share,
    /// once for the methods only the two concrete classes have.
    private static Object apply(List<Integer> list, Object typed, int op, int index, Integer value) {
        switch (op) {
            case 0:
                return Boolean.valueOf(list.add(value));
            case 1:
                list.add(index, value);
                return null;
            case 2:
                return list.remove(index);
            case 3:
                return Boolean.valueOf(list.remove(value));
            case 4:
                return list.set(index, value);
            case 5:
                return list.get(index);
            case 6:
                return list.indexOf(value) + "/" + list.lastIndexOf(value) + "/" + list.contains(value);
            case 7:
                return Boolean.valueOf(list.addAll(Arrays.asList(value, Integer.valueOf(index))));
            case 8:
                return Boolean.valueOf(list.addAll(index, Arrays.asList(value, value)));
            case 9:
                return Boolean.valueOf(list.removeAll(Arrays.asList(value, Integer.valueOf(0))));
            case 10:
                return Boolean.valueOf(list.retainAll(Arrays.asList(value, Integer.valueOf(1), Integer.valueOf(2))));
            case 11:
                return Boolean.valueOf(list.removeIf(v -> v.equals(value)));
            case 12:
                list.replaceAll(v -> Integer.valueOf((v + 1) % 6));
                return null;
            case 13:
                list.sort(index < 0 ? null : Collections.<Integer>reverseOrder());
                return null;
            case 14:
                if (typed instanceof CopyOnWriteArrayList) {
                    @SuppressWarnings("unchecked")
                    CopyOnWriteArrayList<Integer> mine = (CopyOnWriteArrayList<Integer>) typed;
                    return mine.addIfAbsent(value) + "/" + mine.addAllAbsent(Arrays.asList(value, Integer.valueOf(3)))
                            + "/" + mine.indexOf(value, Math.max(0, index)) + "/"
                            + mine.lastIndexOf(value, Math.min(mine.size() - 1, Math.max(0, index)));
                }
                @SuppressWarnings("unchecked")
                java.util.concurrent.CopyOnWriteArrayList<Integer> theirs =
                        (java.util.concurrent.CopyOnWriteArrayList<Integer>) typed;
                return theirs.addIfAbsent(value) + "/" + theirs.addAllAbsent(Arrays.asList(value, Integer.valueOf(3)))
                        + "/" + theirs.indexOf(value, Math.max(0, index)) + "/"
                        + theirs.lastIndexOf(value, Math.min(theirs.size() - 1, Math.max(0, index)));
            case 15:
                return list.subList(Math.max(0, index), list.size()).toString() + list.containsAll(Arrays.asList(value));
            case 16: {
                StringBuilder sb = new StringBuilder();
                ListIterator<Integer> it = list.listIterator(index);
                while (it.hasPrevious()) {
                    sb.append(it.previousIndex()).append('=').append(it.previous()).append(' ');
                }
                while (it.hasNext()) {
                    sb.append(it.nextIndex()).append('=').append(it.next()).append(' ');
                }
                return sb.toString();
            }
            default:
                if (index < 0) {
                    list.clear();
                }
                return list.size() + "/" + list.isEmpty();
        }
    }

    @Test
    public void copyOnWriteIteratorsSeeTheListAsItWas() {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>(Arrays.asList("a", "b", "c"));
        StringBuilder seen = new StringBuilder();
        // The reason desktop listener lists use this class: a listener that
        // removes itself, or adds another, during the walk.
        for (String s : list) {
            seen.append(s);
            list.remove(s);
            list.add(s + s);
        }
        assertEquals("abc", seen.toString());
        assertEquals("[aa, bb, cc]", list.toString());

        Iterator<String> it = list.iterator();
        it.next();
        try {
            it.remove();
            fail("a snapshot iterator cannot remove");
        } catch (UnsupportedOperationException expected) {
            // As the JDK.
        }
        ListIterator<String> listIterator = list.listIterator();
        listIterator.next();
        try {
            listIterator.set("x");
            fail("a snapshot iterator cannot set");
        } catch (UnsupportedOperationException expected) {
            // As the JDK.
        }
        try {
            listIterator.add("x");
            fail("a snapshot iterator cannot add");
        } catch (UnsupportedOperationException expected) {
            // As the JDK.
        }
        list.clear();
        try {
            list.iterator().next();
            fail("nothing left");
        } catch (NoSuchElementException expected) {
            // As the JDK.
        }
    }

    @Test
    public void copyOnWriteCloneIsIndependent() {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>(new String[] {"a", "b"});
        Object copy = list.clone();
        assertTrue(copy instanceof CopyOnWriteArrayList);
        assertEquals(list, copy);
        list.add("c");
        assertEquals("[a, b]", copy.toString());
        assertEquals("[a, b, c]", list.toString());
    }
}
