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
package jdkparity.java.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Holds vm/JavaAPI's `ImmutableCollections`, relocated under `jdkparity.`,
/// to the contract of `List.of`, `Set.of`, `Map.of`, their `copyOf` forms,
/// `Map.ofEntries` and `Map.entry`. Those factories delegate to it, and it
/// is package private, so this test lives in its relocated package and calls
/// it the way each factory does.
///
/// The factories arrived after JDK 8, which this suite runs on, so they are
/// held to their documented contract -- and, where one exists, to the JDK's
/// own unmodifiable wrapper of the same content. A set and a map iterate in
/// hash order (the JDK's order is unspecified), so they are compared as sets
/// and maps, never by iteration order.
class ImmutableCollectionsDifferentialTest {

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

    /// What a fixed-arity `List.of(e1, ...)` answers: a fresh array handed over.
    @SafeVarargs
    private static <E> List<E> listOf(E... elements) {
        return ImmutableCollections.list(elements);
    }

    /// What `List.of(E...)` answers when given an array: a copy of it.
    private static <E> List<E> listOfArray(E[] elements) {
        return ImmutableCollections.listCopy(elements);
    }

    @SafeVarargs
    private static <E> Set<E> setOf(E... elements) {
        return ImmutableCollections.set(elements, true);
    }

    private static <K, V> Map<K, V> mapOf(Object... keysAndValues) {
        return ImmutableCollections.map(keysAndValues);
    }

    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new ImmutableCollections.ImmutableEntry<K, V>(key, value);
    }

    @SafeVarargs
    private static <K, V> Map<K, V> mapOfEntries(Map.Entry<? extends K, ? extends V>... entries) {
        return ImmutableCollections.mapOfEntries(entries);
    }

    // ---- List.of ----

    @Test
    void listOfHoldsItsElementsInOrder() {
        assertEquals(Collections.emptyList(), listOf());
        assertEquals(Arrays.asList("a"), listOf("a"));
        assertEquals(Arrays.asList("a", "b"), listOf("a", "b"));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                listOfArray(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}));
        // Duplicates are a list's business.
        List<String> list = listOf("x", "y", "x");
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
    void listOfCannotBeChanged() {
        final List<String> list = listOf("a", "b", "c");
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
        unsupported("empty add", () -> ImmutableCollections.<String>list(new Object[] {}).add("d"));
        unsupported("single add", () -> listOf("a").add("d"));
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    void listOfRefusesNull() {
        refusesNull("of 1", () -> listOf((String) null));
        refusesNull("of 2", () -> listOf("a", null));
        refusesNull("of 5", () -> listOf("a", "b", null, "d", "e"));
        refusesNull("of array", () -> listOfArray(new String[] {"a", null}));
        refusesNull("of null array", () -> listOfArray((String[]) null));
        refusesNull("copyOf", () -> ImmutableCollections.listCopyOf(Arrays.asList("a", null)));
        refusesNull("copyOf null", () -> ImmutableCollections.listCopyOf(null));
        refusesNull("contains", () -> listOf("a").contains(null));
        refusesNull("indexOf", () -> listOf("a").indexOf(null));
        refusesNull("lastIndexOf", () -> listOf("a").lastIndexOf(null));
    }

    @Test
    void listOfAnArrayDoesNotKeepTheArray() {
        String[] array = {"a", "b"};
        List<String> list = listOfArray(array);
        array[0] = "changed";
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    void listCopyOfIsASnapshot() {
        List<String> source = new ArrayList<String>(Arrays.asList("a", "b"));
        final List<String> copy = ImmutableCollections.listCopyOf(source);
        source.add("c");
        assertEquals(Arrays.asList("a", "b"), copy);
        unsupported("add", () -> copy.add("d"));
        // An immutable list is its own copy.
        assertSame(copy, ImmutableCollections.listCopyOf(copy));
        assertEquals(Arrays.asList("b", "a"), ImmutableCollections.listCopyOf(new LinkedHashSet<String>(
                Arrays.asList("b", "a", "b"))));
    }

    // ---- Set.of ----

    @Test
    void setOfHoldsItsElements() {
        assertEquals(Collections.emptySet(), setOf());
        Set<String> set = setOf("a", "b", "c");
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
                setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertEquals(new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)),
                setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        int walked = 0;
        for (String s : set) {
            assertTrue(reference.contains(s));
            walked++;
        }
        assertEquals(3, walked);
    }

    @Test
    void setOfCannotBeChanged() {
        final Set<String> set = setOf("a", "b", "c");
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
        unsupported("empty add", () -> ImmutableCollections.<String>set(new Object[] {}, true).add("d"));
        assertEquals(3, set.size());
    }

    @Test
    void setOfRefusesNullAndDuplicates() {
        refusesNull("of 1", () -> setOf((String) null));
        refusesNull("of 3", () -> setOf("a", null, "c"));
        refusesNull("of array", () -> setOf(new String[] {"a", null}));
        refusesNull("copyOf", () -> ImmutableCollections.setCopyOf(Arrays.asList("a", null)));
        refusesNull("contains", () -> setOf("a").contains(null));
        duplicate("of 2", () -> setOf("a", "a"));
        duplicate("of 4", () -> setOf("a", "b", "c", "a"));
        duplicate("of array", () -> setOf(new String[] {"a", "b", "b"}));
    }

    @Test
    void setCopyOfDropsDuplicatesInsteadOfRefusingThem() {
        Set<String> copy = ImmutableCollections.setCopyOf(Arrays.asList("a", "b", "a"));
        assertEquals(new HashSet<String>(Arrays.asList("a", "b")), copy);
        assertSame(copy, ImmutableCollections.setCopyOf(copy));
        unsupported("add", () -> copy.add("z"));
    }

    // ---- Map.of ----

    @Test
    void mapOfHoldsItsEntries() {
        assertEquals(Collections.emptyMap(), mapOf());
        Map<String, Integer> map = mapOf("a", 1, "b", 2, "c", 3);
        Map<String, Integer> reference = new HashMap<String, Integer>();
        reference.put("a", 1);
        reference.put("b", 2);
        reference.put("c", 3);
        assertEquals(reference, map);
        assertEquals(map, reference);
        assertEquals(reference.hashCode(), map.hashCode());
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertNull(map.get("z"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("z"));
        assertTrue(map.containsValue(3));
        assertFalse(map.containsValue(4));
        assertEquals(reference.keySet(), map.keySet());
        assertEquals(new HashSet<Integer>(reference.values()), new HashSet<Integer>(map.values()));
        assertEquals(reference.entrySet(), map.entrySet());
        assertEquals(Integer.valueOf(9), map.getOrDefault("z", 9));
        Map<Integer, Integer> ten = mapOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10);
        assertEquals(10, ten.size());
        assertEquals(Integer.valueOf(10), ten.get(10));
    }

    @Test
    void mapOfCannotBeChanged() {
        final Map<String, Integer> map = mapOf("a", 1, "b", 2);
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
        unsupported("empty put", () -> ImmutableCollections.<String, Integer>map(new Object[0]).put("c", 3));
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
    }

    @Test
    void mapOfRefusesNullAndDuplicateKeys() {
        refusesNull("null key", () -> mapOf(null, 1));
        refusesNull("null value", () -> mapOf("a", null));
        refusesNull("null later", () -> mapOf("a", 1, "b", null));
        refusesNull("get", () -> mapOf("a", 1).get(null));
        refusesNull("containsKey", () -> mapOf("a", 1).containsKey(null));
        refusesNull("containsValue", () -> mapOf("a", 1).containsValue(null));
        refusesNull("copyOf key", () -> ImmutableCollections.mapCopyOf(Collections.singletonMap(null, 1)));
        refusesNull("copyOf value", () -> ImmutableCollections.mapCopyOf(Collections.singletonMap("a", null)));
        refusesNull("copyOf null", () -> ImmutableCollections.mapCopyOf(null));
        duplicate("of 2", () -> mapOf("a", 1, "a", 2));
        duplicate("of 3", () -> mapOf("a", 1, "b", 2, "a", 1));
    }

    @Test
    void entryOfEntriesAndCopyOf() {
        final Map.Entry<String, Integer> entry = entry("k", 5);
        Map.Entry<String, Integer> reference = new AbstractMap.SimpleImmutableEntry<String, Integer>("k", 5);
        assertEquals("k", entry.getKey());
        assertEquals(Integer.valueOf(5), entry.getValue());
        assertEquals(reference, entry);
        assertEquals(entry, reference);
        assertEquals(reference.hashCode(), entry.hashCode());
        assertEquals(reference.toString(), entry.toString());
        unsupported("setValue", () -> entry.setValue(6));
        refusesNull("null key", () -> entry(null, 1));
        refusesNull("null value", () -> entry("a", null));

        final Map<String, Integer> map = mapOfEntries(entry("a", 1), entry("b", 2));
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(2), map.get("b"));
        unsupported("put", () -> map.put("c", 3));
        assertTrue(ImmutableCollections.<String, Integer>mapOfEntries(new Map.Entry[0]).isEmpty());
        duplicate("ofEntries", () -> mapOfEntries(entry("a", 1), entry("a", 2)));
        refusesNull("ofEntries", () -> mapOfEntries(entry("a", 1), null));

        Map<String, Integer> source = new TreeMap<String, Integer>();
        source.put("x", 1);
        Map<String, Integer> copy = ImmutableCollections.mapCopyOf(source);
        source.put("y", 2);
        assertEquals(Collections.singletonMap("x", 1), copy);
        assertSame(copy, ImmutableCollections.mapCopyOf(copy));
        assertNotSame(source, copy);
    }
}
