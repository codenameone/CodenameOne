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

import jdkparity.java.util.DoubleSummaryStatistics;
import jdkparity.java.util.IntSummaryStatistics;
import jdkparity.java.util.LongSummaryStatistics;
import jdkparity.java.util.stream.Collector;
import jdkparity.java.util.stream.Collectors;
import jdkparity.java.util.stream.Stream;
import jdkparity.java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Holds vm/JavaAPI's `Collectors`, relocated under `jdkparity.`, against
/// `java.util.stream.Collectors`: each case collects the same elements with
/// both.
///
/// The unmodifiable collectors finish through `List.copyOf`, `Set.copyOf`
/// and `Map.copyOf`, which JDK 8 lacks, so they cannot run here; a ParparVM
/// fixture covers them.
class CollectorsDifferentialTest {

    private static final List<String> WORDS = Collections.unmodifiableList(Arrays.asList(
            "pear", "apple", "fig", "banana", "apple", "kiwi", "cherry", "fig", "date", "avocado"));
    private static final List<String> NONE = Collections.<String>emptyList();

    private static java.util.stream.Stream<String> theirs() {
        return WORDS.stream();
    }

    private static Stream<String> mine() {
        return mine(WORDS);
    }

    private static <T> Stream<T> mine(List<T> list) {
        return StreamSupport.stream(list.spliterator(), false);
    }

    private static String first(String word) {
        return word.substring(0, 1);
    }

    // ---- into collections ----

    @Test
    void toListToSetAndToCollection() {
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
        assertTrue(mine(NONE).collect(Collectors.toList()).isEmpty());
    }

    // ---- joining ----

    @Test
    void joiningInItsThreeForms() {
        assertEquals(theirs().collect(java.util.stream.Collectors.joining()), mine().collect(Collectors.joining()));
        assertEquals(theirs().collect(java.util.stream.Collectors.joining(", ")),
                mine().collect(Collectors.joining(", ")));
        assertEquals(theirs().collect(java.util.stream.Collectors.joining(", ", "[", "]")),
                mine().collect(Collectors.joining(", ", "[", "]")));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.joining()),
                mine(NONE).collect(Collectors.joining()));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.joining("-")),
                mine(NONE).collect(Collectors.joining("-")));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.joining("-", "<", ">")),
                mine(NONE).collect(Collectors.joining("-", "<", ">")));
        assertEquals(theirs().limit(1).collect(java.util.stream.Collectors.joining("-", "<", ">")),
                mine().limit(1).collect(Collectors.joining("-", "<", ">")));
        List<CharSequence> mixed = Arrays.<CharSequence>asList("a", new StringBuilder("b"), null, "");
        assertEquals(mixed.stream().collect(java.util.stream.Collectors.joining("|")),
                mine(mixed).collect(Collectors.joining("|")));
    }

    // ---- reductions ----

    @Test
    void countingSummingAndAveraging() {
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
    void theReductionsOfNothing() {
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.counting()),
                mine(NONE).collect(Collectors.counting()));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.summingInt(String::length)),
                mine(NONE).collect(Collectors.summingInt(String::length)));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.summingDouble(String::length)),
                mine(NONE).collect(Collectors.summingDouble(String::length)));
        // An empty average is zero, not absent and not NaN.
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.averagingInt(String::length)),
                mine(NONE).collect(Collectors.averagingInt(String::length)));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.averagingLong(String::length)),
                mine(NONE).collect(Collectors.averagingLong(String::length)));
        assertEquals(NONE.stream().collect(java.util.stream.Collectors.averagingDouble(String::length)),
                mine(NONE).collect(Collectors.averagingDouble(String::length)));
        assertEquals(Double.valueOf(0.0), mine(NONE).collect(Collectors.averagingInt(String::length)));
        Comparator<String> natural = Comparator.naturalOrder();
        assertFalse(mine(NONE).collect(Collectors.minBy(natural)).isPresent());
        assertFalse(mine(NONE).collect(Collectors.maxBy(natural)).isPresent());
        assertFalse(NONE.stream().collect(java.util.stream.Collectors.maxBy(natural)).isPresent());
    }

    @Test
    void summarizing() {
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
    void reducingMinByAndMaxBy() {
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
    void mappingAndCollectingAndThen() {
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
    void filteringAndFlatMapping() {
        // Later JDKs; held to their documentation.
        assertEquals(Arrays.asList("apple", "banana", "apple", "cherry", "avocado"),
                mine().collect(Collectors.filtering(w -> w.length() > 4, Collectors.toList())));
        assertEquals(Arrays.asList("p", "e", "a", "r", "a", "p", "p", "l", "e"),
                mine().limit(2).collect(Collectors.flatMapping(
                        w -> mine(Arrays.asList(w.split(""))), Collectors.toList())));
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
    void groupingByAClassifier() {
        Map<Integer, List<String>> theirGroups = theirs().collect(
                java.util.stream.Collectors.groupingBy(String::length));
        Map<Integer, List<String>> myGroups = mine().collect(Collectors.groupingBy(String::length));
        assertEquals(theirGroups, myGroups);
        // Within a group, encounter order.
        assertEquals(Arrays.asList("pear", "kiwi", "date"), myGroups.get(4));
        myGroups.get(4).add("mutable");
        myGroups.put(99, new ArrayList<String>());
        assertTrue(mine(NONE).collect(Collectors.groupingBy(String::length)).isEmpty());
    }

    @Test
    void groupingByWithADownstream() {
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
    void groupingByIntoAChosenMap() {
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
    void groupingByRefusesANullKey() {
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
            // And JavaAPI's.
        }
    }

    @Test
    void partitioningAlwaysHasBothHalves() {
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
        Map<Boolean, List<String>> myEmpty = mine(NONE).collect(Collectors.partitioningBy(w -> true));
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
    void toMapOfDistinctKeys() {
        assertEquals(theirs().distinct().collect(java.util.stream.Collectors.toMap(w -> w, String::length)),
                mine().distinct().collect(Collectors.toMap(w -> w, String::length)));
        Map<String, Integer> map = mine().distinct().collect(Collectors.toMap(w -> w, String::length));
        map.put("mutable", 1);
        assertTrue(map instanceof HashMap);
    }

    @Test
    void toMapRefusesADuplicateKey() {
        try {
            theirs().collect(java.util.stream.Collectors.toMap(w -> w, String::length));
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().startsWith("Duplicate key"), expected.getMessage());
        }
        try {
            mine().collect(Collectors.toMap(w -> w, String::length));
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().startsWith("Duplicate key apple"), expected.getMessage());
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
    void toMapMergesWhenToldHow() {
        assertEquals(
                theirs().collect(java.util.stream.Collectors.toMap(w -> first(w), w -> w, (a, b) -> a + "+" + b)),
                mine().collect(Collectors.toMap(w -> first(w), w -> w, (a, b) -> a + "+" + b)));
        assertEquals(theirs().collect(java.util.stream.Collectors.toMap(String::length, w -> 1, Integer::sum)),
                mine().collect(Collectors.toMap(String::length, w -> 1, Integer::sum)));
    }

    @Test
    void toMapIntoAChosenMap() {
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
    void toMapRefusesANullValue() {
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
            // And JavaAPI's.
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
    void collectorOf() {
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
