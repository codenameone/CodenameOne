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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds `JdkCollections` against the JDK. The immutable factories arrived
/// after the JDK this suite runs on, so they are held to their documented
/// contract -- and, where one exists, to the JDK's own unmodifiable wrapper
/// of the same content.
public class JdkCollectionsDifferentialTest {

    private interface Action {
        void run();
    }

    private static void unsupported(String what, Action action) {
        try {
            action.run();
            fail(what + " was allowed");
        } catch (UnsupportedOperationException expected) {
            // The contract.
        }
    }

    private static void refusesNull(String what, Action action) {
        try {
            action.run();
            fail(what + " took a null");
        } catch (NullPointerException expected) {
            // The contract.
        }
    }

    private static void duplicate(String what, Action action) {
        try {
            action.run();
            fail(what + " took a duplicate");
        } catch (IllegalArgumentException expected) {
            // The contract.
        }
    }

    // ---- List.of ----

    @Test
    public void listOfHoldsItsElementsInOrder() {
        assertEquals(Collections.emptyList(), JdkCollections.listOf());
        assertEquals(Arrays.asList("a"), JdkCollections.listOf("a"));
        assertEquals(Arrays.asList("a", "b"), JdkCollections.listOf("a", "b"));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), JdkCollections.listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                JdkCollections.listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        // Duplicates are a list's business.
        List<String> list = JdkCollections.listOf("x", "y", "x");
        List<String> reference = Collections.unmodifiableList(Arrays.asList("x", "y", "x"));
        assertEquals(reference, list);
        assertEquals(list, reference);
        assertEquals(reference.hashCode(), list.hashCode());
        assertEquals(reference.toString(), list.toString());
        assertEquals(reference.size(), list.size());
        assertEquals(reference.indexOf("x"), list.indexOf("x"));
        assertEquals(reference.lastIndexOf("x"), list.lastIndexOf("x"));
        assertEquals(reference.subList(1, 3), list.subList(1, 3));
        assertArrayEquals(reference.toArray(), list.toArray());
        assertArrayEquals(reference.toArray(new String[0]), list.toArray(new String[0]));
        assertTrue(list.contains("y"));
        assertFalse(list.contains("z"));
        assertTrue(list.containsAll(Arrays.asList("x", "y")));
        try {
            list.get(3);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            // As any list.
        }
    }

    @Test
    public void listOfCannotBeChanged() {
        final List<String> list = JdkCollections.listOf("a", "b", "c");
        unsupported("add", () -> list.add("d"));
        unsupported("add at", () -> list.add(0, "d"));
        unsupported("addAll", () -> list.addAll(Arrays.asList("d")));
        unsupported("addAll at", () -> list.addAll(1, Arrays.asList("d")));
        unsupported("remove", () -> list.remove("a"));
        unsupported("remove at", () -> list.remove(0));
        unsupported("removeAll", () -> list.removeAll(Arrays.asList("a")));
        unsupported("retainAll", () -> list.retainAll(Arrays.asList("a")));
        unsupported("set", () -> list.set(0, "z"));
        unsupported("clear", () -> list.clear());
        unsupported("sort", () -> list.sort(null));
        unsupported("replaceAll", () -> list.replaceAll(s -> s));
        unsupported("removeIf", () -> list.removeIf(s -> true));
        unsupported("Collections.sort", () -> Collections.sort(list));
        unsupported("iterator remove", () -> {
            Iterator<String> it = list.iterator();
            it.next();
            it.remove();
        });
        unsupported("list iterator set", () -> {
            ListIterator<String> it = list.listIterator();
            it.next();
            it.set("z");
        });
        unsupported("list iterator add", () -> list.listIterator().add("z"));
        unsupported("sub list clear", () -> list.subList(0, 2).clear());
        unsupported("sub list set", () -> list.subList(0, 2).set(0, "z"));
        unsupported("empty add", () -> JdkCollections.<String>listOf().add("d"));
        unsupported("single add", () -> JdkCollections.listOf("a").add("d"));
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void listOfRefusesNull() {
        refusesNull("of 1", () -> JdkCollections.listOf((String) null));
        refusesNull("of 2", () -> JdkCollections.listOf("a", null));
        refusesNull("of 5", () -> JdkCollections.listOf("a", "b", null, "d", "e"));
        refusesNull("of array", () -> JdkCollections.listOf(new String[] {"a", null}));
        refusesNull("of null array", () -> JdkCollections.listOf((String[]) null));
        refusesNull("copyOf", () -> JdkCollections.listCopyOf(Arrays.asList("a", null)));
        refusesNull("copyOf null", () -> JdkCollections.listCopyOf(null));
        refusesNull("contains", () -> JdkCollections.listOf("a").contains(null));
        refusesNull("indexOf", () -> JdkCollections.listOf("a").indexOf(null));
        refusesNull("lastIndexOf", () -> JdkCollections.listOf("a").lastIndexOf(null));
    }

    @Test
    public void listOfAnArrayDoesNotKeepTheArray() {
        String[] array = {"a", "b"};
        List<String> list = JdkCollections.listOf(array);
        array[0] = "changed";
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void listCopyOfIsASnapshot() {
        List<String> source = new ArrayList<String>(Arrays.asList("a", "b"));
        final List<String> copy = JdkCollections.listCopyOf(source);
        source.add("c");
        assertEquals(Arrays.asList("a", "b"), copy);
        unsupported("add", () -> copy.add("d"));
        // An immutable list is its own copy.
        assertSame(copy, JdkCollections.listCopyOf(copy));
        assertEquals(Arrays.asList("b", "a"), JdkCollections.listCopyOf(new java.util.LinkedHashSet<String>(
                Arrays.asList("b", "a", "b"))));
    }

    // ---- Set.of ----

    @Test
    public void setOfHoldsItsElements() {
        assertEquals(Collections.emptySet(), JdkCollections.setOf());
        Set<String> set = JdkCollections.setOf("a", "b", "c");
        Set<String> reference = new HashSet<String>(Arrays.asList("c", "b", "a"));
        assertEquals(reference, set);
        assertEquals(set, reference);
        assertEquals(reference.hashCode(), set.hashCode());
        assertEquals(3, set.size());
        assertTrue(set.contains("b"));
        assertFalse(set.contains("z"));
        assertTrue(set.containsAll(reference));
        assertEquals(reference, new HashSet<String>(Arrays.asList(set.toArray(new String[0]))));
        assertEquals(new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),
                JdkCollections.setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertEquals(new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)),
                JdkCollections.setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        int walked = 0;
        for (String s : set) {
            assertTrue(reference.contains(s));
            walked++;
        }
        assertEquals(3, walked);
    }

    @Test
    public void setOfCannotBeChanged() {
        final Set<String> set = JdkCollections.setOf("a", "b", "c");
        unsupported("add", () -> set.add("d"));
        unsupported("add present", () -> set.add("a"));
        unsupported("addAll", () -> set.addAll(Arrays.asList("d")));
        unsupported("remove", () -> set.remove("a"));
        unsupported("removeAll", () -> set.removeAll(Arrays.asList("a")));
        unsupported("retainAll", () -> set.retainAll(Arrays.asList("a")));
        unsupported("removeIf", () -> set.removeIf(s -> true));
        unsupported("clear", () -> set.clear());
        unsupported("iterator remove", () -> {
            Iterator<String> it = set.iterator();
            it.next();
            it.remove();
        });
        unsupported("empty add", () -> JdkCollections.<String>setOf().add("d"));
        assertEquals(3, set.size());
    }

    @Test
    public void setOfRefusesNullAndDuplicates() {
        refusesNull("of 1", () -> JdkCollections.setOf((String) null));
        refusesNull("of 3", () -> JdkCollections.setOf("a", null, "c"));
        refusesNull("of array", () -> JdkCollections.setOf(new String[] {"a", null}));
        refusesNull("copyOf", () -> JdkCollections.setCopyOf(Arrays.asList("a", null)));
        refusesNull("contains", () -> JdkCollections.setOf("a").contains(null));
        duplicate("of 2", () -> JdkCollections.setOf("a", "a"));
        duplicate("of 4", () -> JdkCollections.setOf("a", "b", "c", "a"));
        duplicate("of array", () -> JdkCollections.setOf(new String[] {"a", "b", "b"}));
    }

    @Test
    public void setCopyOfDropsDuplicatesInsteadOfRefusingThem() {
        Set<String> copy = JdkCollections.setCopyOf(Arrays.asList("a", "b", "a"));
        assertEquals(new HashSet<String>(Arrays.asList("a", "b")), copy);
        assertSame(copy, JdkCollections.setCopyOf(copy));
        unsupported("add", () -> copy.add("z"));
    }

    // ---- Map.of ----

    @Test
    public void mapOfHoldsItsEntries() {
        assertEquals(Collections.emptyMap(), JdkCollections.mapOf());
        Map<String, Integer> map = JdkCollections.mapOf("a", 1, "b", 2, "c", 3);
        Map<String, Integer> reference = new HashMap<String, Integer>();
        reference.put("a", 1);
        reference.put("b", 2);
        reference.put("c", 3);
        assertEquals(reference, map);
        assertEquals(map, reference);
        assertEquals(reference.hashCode(), map.hashCode());
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(null, map.get("z"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("z"));
        assertTrue(map.containsValue(3));
        assertFalse(map.containsValue(4));
        assertEquals(reference.keySet(), map.keySet());
        assertEquals(new HashSet<Integer>(reference.values()), new HashSet<Integer>(map.values()));
        assertEquals(reference.entrySet(), map.entrySet());
        assertEquals(Integer.valueOf(9), map.getOrDefault("z", 9));
        Map<Integer, Integer> ten = JdkCollections.mapOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10);
        assertEquals(10, ten.size());
        assertEquals(Integer.valueOf(10), ten.get(10));
    }

    @Test
    public void mapOfCannotBeChanged() {
        final Map<String, Integer> map = JdkCollections.mapOf("a", 1, "b", 2);
        unsupported("put", () -> map.put("c", 3));
        unsupported("put present", () -> map.put("a", 1));
        unsupported("putAll", () -> map.putAll(Collections.singletonMap("c", 3)));
        unsupported("remove", () -> map.remove("a"));
        unsupported("clear", () -> map.clear());
        unsupported("putIfAbsent", () -> map.putIfAbsent("c", 3));
        unsupported("remove pair", () -> map.remove("a", 1));
        unsupported("replace", () -> map.replace("a", 5));
        unsupported("replace pair", () -> map.replace("a", 1, 5));
        unsupported("replaceAll", () -> map.replaceAll((k, v) -> v));
        unsupported("computeIfAbsent", () -> map.computeIfAbsent("c", k -> 3));
        unsupported("computeIfPresent", () -> map.computeIfPresent("a", (k, v) -> v));
        unsupported("compute", () -> map.compute("a", (k, v) -> v));
        unsupported("merge", () -> map.merge("a", 1, Integer::sum));
        unsupported("keys remove", () -> map.keySet().remove("a"));
        unsupported("keys clear", () -> map.keySet().clear());
        unsupported("values remove", () -> map.values().remove(1));
        unsupported("values clear", () -> map.values().clear());
        unsupported("entries clear", () -> map.entrySet().clear());
        unsupported("entry setValue", () -> map.entrySet().iterator().next().setValue(7));
        unsupported("entries iterator remove", () -> {
            Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
            it.next();
            it.remove();
        });
        unsupported("keys iterator remove", () -> {
            Iterator<String> it = map.keySet().iterator();
            it.next();
            it.remove();
        });
        unsupported("empty put", () -> JdkCollections.<String, Integer>mapOf().put("c", 3));
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
    }

    @Test
    public void mapOfRefusesNullAndDuplicateKeys() {
        refusesNull("null key", () -> JdkCollections.mapOf(null, 1));
        refusesNull("null value", () -> JdkCollections.mapOf("a", null));
        refusesNull("null later", () -> JdkCollections.mapOf("a", 1, "b", null));
        refusesNull("get", () -> JdkCollections.mapOf("a", 1).get(null));
        refusesNull("containsKey", () -> JdkCollections.mapOf("a", 1).containsKey(null));
        refusesNull("containsValue", () -> JdkCollections.mapOf("a", 1).containsValue(null));
        refusesNull("copyOf key", () -> JdkCollections.mapCopyOf(Collections.singletonMap(null, 1)));
        refusesNull("copyOf value", () -> JdkCollections.mapCopyOf(Collections.singletonMap("a", null)));
        refusesNull("copyOf null", () -> JdkCollections.mapCopyOf(null));
        duplicate("of 2", () -> JdkCollections.mapOf("a", 1, "a", 2));
        duplicate("of 3", () -> JdkCollections.mapOf("a", 1, "b", 2, "a", 1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void entryOfEntriesAndCopyOf() {
        final Map.Entry<String, Integer> entry = JdkCollections.entry("k", 5);
        Map.Entry<String, Integer> reference = new java.util.AbstractMap.SimpleImmutableEntry<String, Integer>("k", 5);
        assertEquals("k", entry.getKey());
        assertEquals(Integer.valueOf(5), entry.getValue());
        assertEquals(reference, entry);
        assertEquals(entry, reference);
        assertEquals(reference.hashCode(), entry.hashCode());
        assertEquals(reference.toString(), entry.toString());
        unsupported("setValue", () -> entry.setValue(6));
        refusesNull("null key", () -> JdkCollections.entry(null, 1));
        refusesNull("null value", () -> JdkCollections.entry("a", null));

        final Map<String, Integer> map = JdkCollections.mapOfEntries(JdkCollections.entry("a", 1),
                JdkCollections.entry("b", 2));
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(2), map.get("b"));
        unsupported("put", () -> map.put("c", 3));
        assertTrue(JdkCollections.<String, Integer>mapOfEntries().isEmpty());
        duplicate("ofEntries", () -> JdkCollections.mapOfEntries(JdkCollections.entry("a", 1),
                JdkCollections.entry("a", 2)));
        refusesNull("ofEntries", () -> JdkCollections.mapOfEntries(JdkCollections.entry("a", 1), null));

        Map<String, Integer> source = new TreeMap<String, Integer>();
        source.put("x", 1);
        Map<String, Integer> copy = JdkCollections.mapCopyOf(source);
        source.put("y", 2);
        assertEquals(Collections.singletonMap("x", 1), copy);
        assertSame(copy, JdkCollections.mapCopyOf(copy));
        assertNotSame(source, copy);
    }

    // ---- what the JDK this suite runs on has too ----

    @Test
    public void entryComparators() {
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("pear", 3);
        map.put("apple", 9);
        map.put("fig", 1);
        map.put("kiwi", 3);
        List<Map.Entry<String, Integer>> theirs = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        List<Map.Entry<String, Integer>> mine = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        theirs.sort(Map.Entry.<String, Integer>comparingByKey());
        mine.sort(JdkCollections.<String, Integer>comparingByKey());
        assertEquals(theirs, mine);
        theirs.sort(Map.Entry.<String, Integer>comparingByValue());
        mine.sort(JdkCollections.<String, Integer>comparingByValue());
        assertEquals(theirs, mine);
        theirs.sort(Map.Entry.<String, Integer>comparingByKey(Collections.reverseOrder()));
        mine.sort(JdkCollections.<String, Integer>comparingByKey(Collections.reverseOrder()));
        assertEquals(theirs, mine);
        theirs.sort(Map.Entry.<String, Integer>comparingByValue(Collections.reverseOrder()));
        mine.sort(JdkCollections.<String, Integer>comparingByValue(Collections.reverseOrder()));
        assertEquals(theirs, mine);
        refusesNull("byKey", () -> JdkCollections.<String, Integer>comparingByKey(null));
        refusesNull("byValue", () -> JdkCollections.<String, Integer>comparingByValue(null));
    }

    @Test
    public void streamOfACollection() {
        List<String> list = Arrays.asList("b", "a", "c");
        assertEquals(list.stream().sorted().collect(java.util.stream.Collectors.toList()),
                JdkCollections.stream(list).sorted().collect(Collectors.toList()));
        // The receiver of a call whose type the remap could not name.
        Object opaque = new java.util.ArrayDeque<String>(list);
        assertEquals(list, JdkCollections.<String>streamOf(opaque).collect(Collectors.toList()));
        refusesNull("stream", () -> JdkCollections.stream((List<String>) null));
        refusesNull("streamOf", () -> JdkCollections.streamOf(null));
        unsupported("streamOf a non-collection", () -> JdkCollections.streamOf("text"));
    }

    @Test
    public void toArrayWithAGenerator() {
        List<String> list = Arrays.asList("b", "a", "c");
        String[] out = JdkCollections.toArray(list, String[]::new);
        assertArrayEquals(list.toArray(new String[0]), out);
        assertSame(String[].class, out.getClass());
        assertEquals(0, JdkCollections.toArray(Collections.<String>emptyList(), String[]::new).length);
        refusesNull("generator", () -> JdkCollections.toArray(list, null));
    }

    @Test
    public void forEachRemainingAndTheEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        Iterator<String> theirs = list.iterator();
        Iterator<String> mine = list.iterator();
        theirs.next();
        mine.next();
        List<String> theirSeen = new ArrayList<String>();
        List<String> mySeen = new ArrayList<String>();
        theirs.forEachRemaining(theirSeen::add);
        JdkCollections.forEachRemaining(mine, mySeen::add);
        assertEquals(theirSeen, mySeen);
        assertFalse(mine.hasNext());
        refusesNull("action", () -> JdkCollections.forEachRemaining(list.iterator(), null));

        Iterator<String> empty = JdkCollections.emptyIterator();
        assertEquals(Collections.<String>emptyIterator().hasNext(), empty.hasNext());
        try {
            empty.next();
            fail();
        } catch (NoSuchElementException expected) {
            // As the JDK's.
        }
        try {
            empty.remove();
            fail();
        } catch (IllegalStateException expected) {
            // As the JDK's.
        }
    }
}
