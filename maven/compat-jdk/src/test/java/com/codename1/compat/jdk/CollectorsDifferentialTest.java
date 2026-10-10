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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds `Collectors` against `java.util.stream.Collectors`. `J` is the JDK's
/// class under a short name; each case collects the same elements with both.
public class CollectorsDifferentialTest {

    private static final List<String> WORDS = Collections.unmodifiableList(Arrays.asList(
            "pear", "apple", "fig", "banana", "apple", "kiwi", "cherry", "fig", "date", "avocado"));
    private static final List<String> NONE = Collections.<String>emptyList();

    private static java.util.stream.Stream<String> theirs() {
        return WORDS.stream();
    }

    private static Stream<String> mine() {
        return JdkCollections.stream(WORDS);
    }

    private static String first(String word) {
        return word.substring(0, 1);
    }

    // ---- into collections ----

    @Test
    public void toListToSetAndToCollection() {
        List<String> list = mine().collect(Collectors.toList());
        assertEquals(theirs().collect(java.util.stream.Collectors.toList()), list);
        list.add("mutable");
        Set<String> set = mine().collect(Collectors.toSet());
        assertEquals(theirs().collect(java.util.stream.Collectors.toSet()), set);
        set.add("mutable");
        TreeSet<String> sorted = mine().collect(Collectors.toCollection(TreeSet::new));
        assertEquals(theirs().collect(java.util.stream.Collectors.toCollection(TreeSet::new)), sorted);
        assertEquals(new ArrayList<String>(theirs().collect(java.util.stream.Collectors.toCollection(TreeSet::new))),
                new ArrayList<String>(sorted));
        assertTrue(JdkCollections.stream(NONE).collect(Collectors.toList()).isEmpty());
    }

    @Test
    public void theUnmodifiableCollections() {
        List<String> list = mine().collect(Collectors.toUnmodifiableList());
        assertEquals(WORDS, list);
        try {
            list.add("x");
            fail();
        } catch (UnsupportedOperationException expected) {
            // As documented.
        }
        Set<String> set = mine().collect(Collectors.toUnmodifiableSet());
        assertEquals(new TreeSet<String>(WORDS), new TreeSet<String>(set));
        try {
            set.remove("fig");
            fail();
        } catch (UnsupportedOperationException expected) {
            // As documented.
        }
        try {
            JdkCollections.stream(Arrays.asList("a", null)).collect(Collectors.toUnmodifiableList());
            fail();
        } catch (NullPointerException expected) {
            // As documented: no null elements.
        }
        Map<String, Integer> map = mine().distinct().collect(Collectors.toUnmodifiableMap(w -> w, String::length));
        assertEquals(theirs().distinct().collect(java.util.stream.Collectors.toMap(w -> w, String::length)), map);
        try {
            map.put("x", 1);
            fail();
        } catch (UnsupportedOperationException expected) {
            // As documented.
        }
        try {
            mine().collect(Collectors.toUnmodifiableMap(w -> w, String::length));
            fail();
        } catch (IllegalStateException expected) {
            // Two "apple".
        }
        assertEquals(theirs().collect(java.util.stream.Collectors.toMap(w -> first(w), String::length, Integer::sum)),
                mine().collect(Collectors.toUnmodifiableMap(w -> first(w), String::length, Integer::sum)));
    }

    // ---- joining ----

    @Test
    public void joiningInItsThreeForms() {
        assertEquals(theirs().collect(java.util.stream.Collectors.joining()), mine().collect(Collectors.joining()));
        assertEquals(theirs().collect(java.util.stream.Collectors.joining(", ")),
                mine().collect(Collectors.joining(", ")));
        assertEquals(theirs().collect(java.util.stream.Collectors.joining(", ", "[", "]")),
                mine().collect(Collectors.joining(", ", "[", "]")));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.joining()),
                JdkCollections.stream(NONE).collect(Collectors.joining()));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.joining("-")),
                JdkCollections.stream(NONE).collect(Collectors.joining("-")));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.joining("-", "<", ">")),
                JdkCollections.stream(NONE).collect(Collectors.joining("-", "<", ">")));
        assertEquals(theirs().limit(1).collect(java.util.stream.Collectors.joining("-", "<", ">")),
                mine().limit(1).collect(Collectors.joining("-", "<", ">")));
        List<CharSequence> mixed = Arrays.<CharSequence>asList("a", new StringBuilder("b"), null, "");
        assertEquals(mixed.stream().collect(java.util.stream.Collectors.joining("|")),
                JdkCollections.stream(mixed).collect(Collectors.joining("|")));
    }

    // ---- reductions ----

    @Test
    public void countingSummingAndAveraging() {
        assertEquals(theirs().collect(java.util.stream.Collectors.counting()), mine().collect(Collectors.counting()));
        assertEquals(theirs().collect(java.util.stream.Collectors.summingInt(String::length)),
                mine().collect(Collectors.summingInt(String::length)));
        assertEquals(theirs().collect(java.util.stream.Collectors.summingLong(w -> w.length() * 5000000000L)),
                mine().collect(Collectors.summingLong(w -> w.length() * 5000000000L)));
        assertEquals(theirs().collect(java.util.stream.Collectors.summingDouble(w -> w.length() / 10.0)),
                mine().collect(Collectors.summingDouble(w -> w.length() / 10.0)));
        assertEquals(theirs().collect(java.util.stream.Collectors.averagingInt(String::length)),
                mine().collect(Collectors.averagingInt(String::length)));
        assertEquals(theirs().collect(java.util.stream.Collectors.averagingLong(w -> w.length() * 5000000000L)),
                mine().collect(Collectors.averagingLong(w -> w.length() * 5000000000L)));
        assertEquals(theirs().collect(java.util.stream.Collectors.averagingDouble(w -> w.length() / 10.0)),
                mine().collect(Collectors.averagingDouble(w -> w.length() / 10.0)));
    }

    @Test
    public void theReductionsOfNothing() {
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.counting()),
                JdkCollections.stream(NONE).collect(Collectors.counting()));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.summingInt(String::length)),
                JdkCollections.stream(NONE).collect(Collectors.summingInt(String::length)));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.summingDouble(String::length)),
                JdkCollections.stream(NONE).collect(Collectors.summingDouble(String::length)));
        // An empty average is zero, not absent and not NaN.
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.averagingInt(String::length)),
                JdkCollections.stream(NONE).collect(Collectors.averagingInt(String::length)));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.averagingLong(String::length)),
                JdkCollections.stream(NONE).collect(Collectors.averagingLong(String::length)));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.averagingDouble(String::length)),
                JdkCollections.stream(NONE).collect(Collectors.averagingDouble(String::length)));
        assertEquals(Double.valueOf(0.0), JdkCollections.stream(NONE).collect(Collectors.averagingInt(String::length)));
        Comparator<String> natural = Comparator.naturalOrder();
        assertFalse(JdkCollections.stream(NONE).collect(Collectors.minBy(natural)).isPresent());
        assertFalse(JdkCollections.stream(NONE).collect(Collectors.maxBy(natural)).isPresent());
        assertFalse(NONE.stream().collect(java.util.stream.Collectors.maxBy(natural)).isPresent());
    }

    @Test
    public void summarizing() {
        java.util.IntSummaryStatistics theirInts = theirs().collect(
                java.util.stream.Collectors.summarizingInt(String::length));
        IntSummaryStatistics myInts = mine().collect(Collectors.summarizingInt(String::length));
        assertEquals(theirInts.getCount(), myInts.getCount());
        assertEquals(theirInts.getSum(), myInts.getSum());
        assertEquals(theirInts.getMin(), myInts.getMin());
        assertEquals(theirInts.getMax(), myInts.getMax());
        assertEquals(theirInts.getAverage(), myInts.getAverage(), 0.0);
        java.util.LongSummaryStatistics theirLongs = theirs().collect(
                java.util.stream.Collectors.summarizingLong(w -> w.length() * 5000000000L));
        LongSummaryStatistics myLongs = mine().collect(Collectors.summarizingLong(w -> w.length() * 5000000000L));
        assertEquals(theirLongs.getSum(), myLongs.getSum());
        assertEquals(theirLongs.getMax(), myLongs.getMax());
        java.util.DoubleSummaryStatistics theirDoubles = theirs().collect(
                java.util.stream.Collectors.summarizingDouble(w -> w.length() / 10.0));
        DoubleSummaryStatistics myDoubles = mine().collect(Collectors.summarizingDouble(w -> w.length() / 10.0));
        assertEquals(theirDoubles.getSum(), myDoubles.getSum(), 0.0);
        assertEquals(theirDoubles.getMin(), myDoubles.getMin(), 0.0);
        assertEquals(theirDoubles.getAverage(), myDoubles.getAverage(), 0.0);
    }

    @Test
    public void reducingMinByAndMaxBy() {
        assertEquals(theirs().collect(java.util.stream.Collectors.reducing("", (a, b) -> a + first(b))),
                mine().collect(Collectors.reducing("", (a, b) -> a + first(b))));
        assertEquals(theirs().collect(java.util.stream.Collectors.reducing((a, b) -> a + first(b))).get(),
                mine().collect(Collectors.reducing((a, b) -> a + first(b))).get());
        assertEquals(theirs().collect(java.util.stream.Collectors.reducing(0, String::length, Integer::sum)),
                mine().collect(Collectors.reducing(0, String::length, Integer::sum)));
        Comparator<String> byLength = (a, b) -> a.length() - b.length();
        // Among equals, the same one.
        assertSame(theirs().collect(java.util.stream.Collectors.minBy(byLength)).get(),
                mine().collect(Collectors.minBy(byLength)).get());
        assertSame(theirs().collect(java.util.stream.Collectors.maxBy(byLength)).get(),
                mine().collect(Collectors.maxBy(byLength)).get());
    }

    // ---- adapters ----

    @Test
    public void mappingAndCollectingAndThen() {
        assertEquals(
                theirs().collect(java.util.stream.Collectors.mapping(String::length,
                        java.util.stream.Collectors.toList())),
                mine().collect(Collectors.mapping(String::length, Collectors.toList())));
        Integer theirSize = theirs().collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.<String>toList(), l -> l.size()));
        Integer mySize = mine().collect(Collectors.collectingAndThen(Collectors.<String>toList(), l -> l.size()));
        assertEquals(theirSize, mySize);
        List<String> wrapped = mine().collect(
                Collectors.collectingAndThen(Collectors.<String>toList(), l -> Collections.<String>unmodifiableList(l)));
        assertEquals(WORDS, wrapped);
    }

    @Test
    public void filteringAndFlatMapping() {
        // Later JDKs; held to their documentation.
        assertEquals(Arrays.asList("apple", "banana", "apple", "cherry", "avocado"),
                mine().collect(Collectors.filtering(w -> w.length() > 4, Collectors.toList())));
        assertEquals(Arrays.asList("p", "e", "a", "r", "a", "p", "p", "l", "e"),
                mine().limit(2).collect(Collectors.flatMapping(
                        w -> JdkCollections.stream(Arrays.asList(w.split(""))), Collectors.toList())));
        // A group nothing survives the filter in is still there, and empty.
        Map<Integer, List<String>> groups = mine().collect(Collectors.groupingBy(String::length,
                Collectors.filtering(w -> w.startsWith("a"), Collectors.toList())));
        assertEquals(Collections.<String>emptyList(), groups.get(3));
        assertEquals(Arrays.asList("apple", "apple"), groups.get(5));
        // A null inner stream is no elements.
        assertEquals(Long.valueOf(0),
                mine().collect(Collectors.flatMapping(w -> (Stream<String>) null, Collectors.counting())));
    }

    // ---- grouping ----

    @Test
    public void groupingByAClassifier() {
        Map<Integer, List<String>> theirGroups = theirs().collect(
                java.util.stream.Collectors.groupingBy(String::length));
        Map<Integer, List<String>> myGroups = mine().collect(Collectors.groupingBy(String::length));
        assertEquals(theirGroups, myGroups);
        // Within a group, encounter order.
        assertEquals(Arrays.asList("pear", "kiwi", "date"), myGroups.get(4));
        myGroups.get(4).add("mutable");
        myGroups.put(99, new ArrayList<String>());
        assertTrue(JdkCollections.stream(NONE).collect(Collectors.groupingBy(String::length)).isEmpty());
    }

    @Test
    public void groupingByWithADownstream() {
        assertEquals(
                theirs().collect(java.util.stream.Collectors.groupingBy(w -> first(w),
                        java.util.stream.Collectors.counting())),
                mine().collect(Collectors.groupingBy(w -> first(w), Collectors.counting())));
        assertEquals(
                theirs().collect(java.util.stream.Collectors.groupingBy(String::length,
                        java.util.stream.Collectors.joining("+"))),
                mine().collect(Collectors.groupingBy(String::length, Collectors.joining("+"))));
        assertEquals(
                theirs().collect(java.util.stream.Collectors.groupingBy(String::length,
                        java.util.stream.Collectors.toSet())),
                mine().collect(Collectors.groupingBy(String::length, Collectors.toSet())));
        assertEquals(
                theirs().collect(java.util.stream.Collectors.groupingBy(String::length,
                        java.util.stream.Collectors.averagingInt(w -> w.charAt(0)))),
                mine().collect(Collectors.groupingBy(String::length, Collectors.averagingInt(w -> w.charAt(0)))));
        // A group of groups.
        assertEquals(
                theirs().collect(java.util.stream.Collectors.groupingBy(String::length,
                        java.util.stream.Collectors.groupingBy(w -> first(w)))),
                mine().collect(Collectors.groupingBy(String::length, Collectors.groupingBy(w -> first(w)))));
    }

    @Test
    public void groupingByIntoAChosenMap() {
        TreeMap<String, Long> theirGroups = theirs().collect(java.util.stream.Collectors.groupingBy(w -> first(w),
                TreeMap::new, java.util.stream.Collectors.counting()));
        TreeMap<String, Long> myGroups = mine().collect(
                Collectors.groupingBy(w -> first(w), TreeMap::new, Collectors.counting()));
        assertEquals(theirGroups, myGroups);
        assertEquals(theirGroups.toString(), myGroups.toString());
        LinkedHashMap<Integer, List<String>> ordered = mine().collect(
                Collectors.groupingBy(String::length, LinkedHashMap::new, Collectors.toList()));
        assertEquals(
                theirs().collect(java.util.stream.Collectors.groupingBy(String::length, LinkedHashMap::new,
                        java.util.stream.Collectors.toList())).toString(),
                ordered.toString());
    }

    @Test
    public void groupingByRefusesANullKey() {
        try {
            theirs().collect(java.util.stream.Collectors.groupingBy(w -> w.length() == 3 ? null : w));
            fail();
        } catch (NullPointerException expected) {
            // The JDK's answer.
        }
        try {
            mine().collect(Collectors.groupingBy(w -> w.length() == 3 ? null : w));
            fail();
        } catch (NullPointerException expected) {
            // And the shim's.
        }
    }

    @Test
    public void partitioningAlwaysHasBothHalves() {
        Map<Boolean, List<String>> theirHalves = theirs().collect(
                java.util.stream.Collectors.partitioningBy(w -> w.length() > 4));
        Map<Boolean, List<String>> myHalves = mine().collect(Collectors.partitioningBy(w -> w.length() > 4));
        assertEquals(theirHalves, myHalves);
        assertEquals(theirHalves.toString(), myHalves.toString());
        assertEquals(theirHalves.get(true), myHalves.get(true));
        assertEquals(theirHalves.get(false), myHalves.get(false));
        assertEquals(2, myHalves.size());
        assertEquals(theirHalves.keySet().toString(), myHalves.keySet().toString());

        Map<Boolean, List<String>> theirEmpty = NONE.stream().collect(
                java.util.stream.Collectors.partitioningBy(w -> true));
        Map<Boolean, List<String>> myEmpty = JdkCollections.stream(NONE).collect(Collectors.partitioningBy(w -> true));
        assertEquals(theirEmpty, myEmpty);
        assertEquals(theirEmpty.toString(), myEmpty.toString());
        assertTrue(myEmpty.containsKey(Boolean.TRUE));
        assertTrue(myEmpty.containsKey(Boolean.FALSE));
        assertFalse(myEmpty.containsKey("true"));

        assertEquals(
                theirs().collect(java.util.stream.Collectors.partitioningBy(w -> w.contains("a"),
                        java.util.stream.Collectors.counting())),
                mine().collect(Collectors.partitioningBy(w -> w.contains("a"), Collectors.counting())));
        assertEquals(
                theirs().collect(java.util.stream.Collectors.partitioningBy(w -> w.contains("a"),
                        java.util.stream.Collectors.joining("/"))).toString(),
                mine().collect(Collectors.partitioningBy(w -> w.contains("a"), Collectors.joining("/"))).toString());
    }

    // ---- toMap ----

    @Test
    public void toMapOfDistinctKeys() {
        assertEquals(theirs().distinct().collect(java.util.stream.Collectors.toMap(w -> w, String::length)),
                mine().distinct().collect(Collectors.toMap(w -> w, String::length)));
        Map<String, Integer> map = mine().distinct().collect(Collectors.toMap(w -> w, String::length));
        map.put("mutable", 1);
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void toMapRefusesADuplicateKey() {
        try {
            theirs().collect(java.util.stream.Collectors.toMap(w -> w, String::length));
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("Duplicate key"));
        }
        try {
            mine().collect(Collectors.toMap(w -> w, String::length));
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("Duplicate key apple"));
        }
        // The same key from different elements counts as well.
        try {
            mine().collect(Collectors.toMap(w -> first(w), w -> w));
            fail();
        } catch (IllegalStateException expected) {
            // "pear" and "apple" do not collide; "apple" and "apple" do.
        }
    }

    @Test
    public void toMapMergesWhenToldHow() {
        assertEquals(
                theirs().collect(java.util.stream.Collectors.toMap(w -> first(w), w -> w, (a, b) -> a + "+" + b)),
                mine().collect(Collectors.toMap(w -> first(w), w -> w, (a, b) -> a + "+" + b)));
        assertEquals(theirs().collect(java.util.stream.Collectors.toMap(String::length, w -> 1, Integer::sum)),
                mine().collect(Collectors.toMap(String::length, w -> 1, Integer::sum)));
    }

    @Test
    public void toMapIntoAChosenMap() {
        LinkedHashMap<String, String> theirMap = theirs().collect(java.util.stream.Collectors.toMap(w -> first(w),
                w -> w, (a, b) -> b, LinkedHashMap::new));
        LinkedHashMap<String, String> myMap = mine().collect(
                Collectors.toMap(w -> first(w), w -> w, (a, b) -> b, LinkedHashMap::new));
        assertEquals(theirMap.toString(), myMap.toString());
        TreeMap<Integer, String> sorted = mine().collect(
                Collectors.toMap(String::length, w -> w, (a, b) -> a, TreeMap::new));
        assertEquals(
                theirs().collect(java.util.stream.Collectors.toMap(String::length, w -> w, (a, b) -> a,
                        TreeMap::new)).toString(),
                sorted.toString());
    }

    @Test
    public void toMapRefusesANullValue() {
        try {
            theirs().collect(java.util.stream.Collectors.toMap(w -> w + w.hashCode(), w -> (String) null,
                    (a, b) -> a));
            fail();
        } catch (NullPointerException expected) {
            // The JDK's answer.
        }
        try {
            mine().collect(Collectors.toMap(w -> w + w.hashCode(), w -> (String) null, (a, b) -> a));
            fail();
        } catch (NullPointerException expected) {
            // And the shim's.
        }
        try {
            mine().distinct().collect(Collectors.toMap(w -> w, w -> (String) null));
            fail();
        } catch (NullPointerException expected) {
            // And without a merge function.
        }
    }

    // ---- a collector of one's own ----

    @Test
    public void collectorOf() {
        Collector<String, StringBuilder, String> initials = Collector.of(StringBuilder::new,
                (sb, w) -> sb.append(w.charAt(0)), (a, b) -> a.append(b), StringBuilder::toString);
        java.util.stream.Collector<String, StringBuilder, String> theirInitials = java.util.stream.Collector.of(
                StringBuilder::new, (sb, w) -> sb.append(w.charAt(0)), (a, b) -> a.append(b),
                StringBuilder::toString);
        assertEquals(theirs().collect(theirInitials), mine().collect(initials));
        assertFalse(initials.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH));

        Collector<String, List<String>, List<String>> identity = Collector.of(ArrayList::new, List::add,
                (a, b) -> {
                    a.addAll(b);
                    return a;
                });
        assertEquals(WORDS, mine().collect(identity));
        assertTrue(identity.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH));
        assertEquals(java.util.stream.Collector.Characteristics.values().length,
                Collector.Characteristics.values().length);
        for (java.util.stream.Collector.Characteristics c : java.util.stream.Collector.Characteristics.values()) {
            assertEquals(c.ordinal(), Collector.Characteristics.valueOf(c.name()).ordinal());
        }
    }
}
