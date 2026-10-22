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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/// java.util.stream.Collectors and Collector.of.
public final class CollectorsChecks {
    private static final List<String> WORDS = Collections.unmodifiableList(Arrays.asList(
            "pear", "apple", "fig", "banana", "apple", "kiwi", "cherry", "fig", "date", "avocado"));
    private static final List<String> NONE = Collections.<String>emptyList();

    private CollectorsChecks() {
    }

    private static Stream<String> words() {
        return src(WORDS);
    }

    private static <T> Stream<T> src(List<T> list) {
        return StreamSupport.stream(list.spliterator(), false);
    }

    private static String first(String word) {
        return word.substring(0, 1);
    }

    /// The letters of `word`, one string each, as `word.split("")` answers
    /// them on the JDK (JavaAPI has no regex `split`).
    private static List<String> letters(String word) {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < word.length(); i++) {
            out.add(word.substring(i, i + 1));
        }
        return out;
    }

    /// `v` printed the same on both runs: a hash map or set in sorted order,
    /// at every depth; a list or a sorted collection as it iterates.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String canon(Object v) {
        if (v instanceof Map) {
            Map m = (Map) v;
            List keys = new ArrayList(m.keySet());
            if (!(v instanceof TreeMap) && !(v instanceof LinkedHashMap)) {
                Collections.sort(keys);
            }
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(canon(keys.get(i))).append('=').append(canon(m.get(keys.get(i))));
            }
            return sb.append('}').toString();
        }
        if (v instanceof Collection) {
            List items = new ArrayList((Collection) v);
            if (v instanceof Set && !(v instanceof TreeSet)) {
                Collections.sort(items);
            }
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(canon(items.get(i)));
            }
            return sb.append(']').toString();
        }
        return Bulk.fmt(v);
    }

    /// Runs the group.
    public static void run() {
        collections();
        reductions();
        grouping();
        toMap();
        own();
    }

    private static void collections() {
        Bulk.check("Collectors.toListToSetAndToCollection", () -> {
            List<String> list = words().collect(Collectors.toList());
            Bulk.p("list", list);
            Bulk.p("listMutable", list.add("mutable"));
            Set<String> set = words().collect(Collectors.toSet());
            Bulk.p("set", canon(set));
            Bulk.p("setMutable", set.add("mutable"));
            TreeSet<String> sorted = words().collect(Collectors.toCollection(TreeSet::new));
            Bulk.p("treeSet", sorted);
            Bulk.p("emptyList", src(NONE).collect(Collectors.toList()));
        });
        Bulk.check("Collectors.joiningInItsThreeForms", () -> {
            Bulk.p("plain", words().collect(Collectors.joining()));
            Bulk.p("delimited", words().collect(Collectors.joining(", ")));
            Bulk.p("wrapped", words().collect(Collectors.joining(", ", "[", "]")));
            Bulk.p("emptyPlain", src(NONE).collect(Collectors.joining()));
            Bulk.p("emptyDelimited", src(NONE).collect(Collectors.joining("-")));
            Bulk.p("emptyWrapped", src(NONE).collect(Collectors.joining("-", "<", ">")));
            Bulk.p("oneWrapped", words().limit(1).collect(Collectors.joining("-", "<", ">")));
            List<CharSequence> mixed = Arrays.<CharSequence>asList("a", new StringBuilder("b"), null, "");
            Bulk.p("mixed", src(mixed).collect(Collectors.joining("|")));
        });
    }

    private static void reductions() {
        Bulk.check("Collectors.countingSummingAndAveraging", () -> {
            Bulk.p("counting", words().collect(Collectors.counting()));
            Bulk.p("summingInt", words().collect(Collectors.summingInt(String::length)));
            Bulk.p("summingLong", words().collect(Collectors.summingLong(w -> w.length() * 5000000000L)));
            Bulk.p("summingDouble", words().collect(Collectors.summingDouble(w -> w.length() / 10.0)));
            Bulk.p("averagingInt", words().collect(Collectors.averagingInt(String::length)));
            Bulk.p("averagingLong", words().collect(Collectors.averagingLong(w -> w.length() * 5000000000L)));
            Bulk.p("averagingDouble", words().collect(Collectors.averagingDouble(w -> w.length() / 10.0)));
        });
        Bulk.check("Collectors.theReductionsOfNothing", () -> {
            Bulk.p("counting", src(NONE).collect(Collectors.counting()));
            Bulk.p("summingInt", src(NONE).collect(Collectors.summingInt(String::length)));
            Bulk.p("summingDouble", src(NONE).collect(Collectors.summingDouble(String::length)));
            // An empty average is zero, not absent and not NaN.
            Bulk.p("averagingInt", src(NONE).collect(Collectors.averagingInt(String::length)));
            Bulk.p("averagingLong", src(NONE).collect(Collectors.averagingLong(String::length)));
            Bulk.p("averagingDouble", src(NONE).collect(Collectors.averagingDouble(String::length)));
            Comparator<String> natural = Comparator.naturalOrder();
            Bulk.p("minBy", src(NONE).collect(Collectors.minBy(natural)).isPresent());
            Bulk.p("maxBy", src(NONE).collect(Collectors.maxBy(natural)).isPresent());
        });
        Bulk.check("Collectors.summarizing", () -> {
            IntSummaryStatistics i = words().collect(Collectors.summarizingInt(String::length));
            Bulk.p("int", i.getCount() + " " + i.getSum() + " " + i.getMin() + " " + i.getMax() + " " + i.getAverage());
            LongSummaryStatistics l = words().collect(Collectors.summarizingLong(w -> w.length() * 5000000000L));
            Bulk.p("long", l.getSum() + " " + l.getMax());
            DoubleSummaryStatistics d = words().collect(Collectors.summarizingDouble(w -> w.length() / 10.0));
            Bulk.p("double", d.getSum() + " " + d.getMin() + " " + d.getAverage());
        });
        Bulk.check("Collectors.reducingMinByAndMaxBy", () -> {
            Bulk.p("reducingIdentity", words().collect(Collectors.reducing("", (a, b) -> a + first(b))));
            Bulk.p("reducing", words().collect(Collectors.reducing((a, b) -> a + first(b))).get());
            Bulk.p("reducingMapped", words().collect(Collectors.reducing(0, String::length, Integer::sum)));
            // Among equals, which one: told apart by where each stands.
            List<String> tagged = new ArrayList<String>();
            for (int i = 0; i < WORDS.size(); i++) {
                tagged.add(WORDS.get(i) + "#" + i);
            }
            Comparator<String> byLength = (a, b) -> a.indexOf('#') - b.indexOf('#');
            Bulk.p("minBy", src(tagged).collect(Collectors.minBy(byLength)).get());
            Bulk.p("maxBy", src(tagged).collect(Collectors.maxBy(byLength)).get());
        });
        Bulk.check("Collectors.mappingAndCollectingAndThen", () -> {
            Bulk.p("mapping", words().collect(Collectors.mapping(String::length, Collectors.toList())));
            Bulk.p("size", words().collect(Collectors.collectingAndThen(Collectors.<String>toList(), l -> l.size())));
            List<String> wrapped = words().collect(Collectors.collectingAndThen(Collectors.<String>toList(),
                    l -> Collections.<String>unmodifiableList(l)));
            Bulk.p("wrapped", wrapped);
        });
        Bulk.check("Collectors.filteringAndFlatMapping", () -> {
            Bulk.p("filtering", words().collect(Collectors.filtering(w -> w.length() > 4, Collectors.toList())));
            Bulk.p("flatMapping", words().limit(2).collect(Collectors.flatMapping(
                    w -> src(letters(w)), Collectors.toList())));
            // A group nothing survives the filter in is still there, and empty.
            Map<Integer, List<String>> groups = words().collect(Collectors.groupingBy(String::length,
                    Collectors.filtering(w -> w.startsWith("a"), Collectors.toList())));
            Bulk.p("groups", canon(groups));
            // A null inner stream is no elements.
            Bulk.p("nullInner", words().collect(Collectors.flatMapping(w -> (Stream<String>) null,
                    Collectors.counting())));
        });
    }

    private static void grouping() {
        Bulk.check("Collectors.groupingByAClassifier", () -> {
            Map<Integer, List<String>> groups = words().collect(Collectors.groupingBy(String::length));
            Bulk.p("groups", canon(groups));
            // Within a group, encounter order; and the result is mutable.
            Bulk.p("addToGroup", groups.get(4).add("mutable"));
            groups.put(99, new ArrayList<String>());
            Bulk.p("afterPut", groups.size());
            Bulk.p("empty", src(NONE).collect(Collectors.groupingBy(String::length)).isEmpty());
        });
        Bulk.check("Collectors.groupingByWithADownstream", () -> {
            Bulk.p("counting", canon(words().collect(Collectors.groupingBy(w -> first(w), Collectors.counting()))));
            Bulk.p("joining", canon(words().collect(Collectors.groupingBy(String::length, Collectors.joining("+")))));
            Bulk.p("toSet", canon(words().collect(Collectors.groupingBy(String::length, Collectors.toSet()))));
            Bulk.p("averaging", canon(words().collect(Collectors.groupingBy(String::length,
                    Collectors.averagingInt(w -> w.charAt(0))))));
            // A group of groups.
            Bulk.p("nested", canon(words().collect(Collectors.groupingBy(String::length,
                    Collectors.groupingBy(w -> first(w))))));
        });
        Bulk.check("Collectors.groupingByIntoAChosenMap", () -> {
            TreeMap<String, Long> sorted = words().collect(
                    Collectors.groupingBy(w -> first(w), TreeMap::new, Collectors.counting()));
            Bulk.p("treeMap", sorted.toString());
            LinkedHashMap<Integer, List<String>> ordered = words().collect(
                    Collectors.groupingBy(String::length, LinkedHashMap::new, Collectors.toList()));
            Bulk.p("linkedHashMap", ordered.toString());
        });
        Bulk.check("Collectors.groupingByRefusesANullKey", () -> {
            Bulk.pThrown("nullKey", NullPointerException.class,
                    () -> words().collect(Collectors.groupingBy(w -> w.length() == 3 ? null : w)));
        });
        Bulk.check("Collectors.partitioningAlwaysHasBothHalves", () -> {
            Map<Boolean, List<String>> halves = words().collect(Collectors.partitioningBy(w -> w.length() > 4));
            Bulk.p("toString", halves.toString());
            Bulk.p("true", halves.get(true));
            Bulk.p("false", halves.get(false));
            Bulk.p("size", halves.size());
            Bulk.p("keySet", halves.keySet().toString());
            Map<Boolean, List<String>> empty = src(NONE).collect(Collectors.partitioningBy(w -> true));
            Bulk.p("emptyToString", empty.toString());
            Bulk.p("emptyHasTrue", empty.containsKey(Boolean.TRUE));
            Bulk.p("emptyHasFalse", empty.containsKey(Boolean.FALSE));
            Bulk.p("emptyHasString", empty.containsKey("true"));
            Bulk.p("counting", words().collect(Collectors.partitioningBy(w -> w.contains("a"),
                    Collectors.counting())).toString());
            Bulk.p("joining", words().collect(Collectors.partitioningBy(w -> w.contains("a"),
                    Collectors.joining("/"))).toString());
        });
    }

    private static void toMap() {
        Bulk.check("Collectors.toMapOfDistinctKeys", () -> {
            Map<String, Integer> map = words().distinct().collect(Collectors.toMap(w -> w, String::length));
            Bulk.p("map", canon(map));
            Bulk.p("put", map.put("mutable", 1));
            Bulk.p("isHashMap", map instanceof HashMap);
        });
        Bulk.check("Collectors.toMapRefusesADuplicateKey", () -> {
            String message = "none";
            try {
                words().collect(Collectors.toMap(w -> w, String::length));
            } catch (IllegalStateException expected) {
                message = String.valueOf(expected.getMessage().startsWith("Duplicate key apple"));
            }
            Bulk.p("namesTheKey", message);
            // The same key from different elements counts as well.
            Bulk.pThrown("sameKey", IllegalStateException.class,
                    () -> words().collect(Collectors.toMap(w -> first(w), w -> w)));
        });
        Bulk.check("Collectors.toMapMergesWhenToldHow", () -> {
            Bulk.p("concat", canon(words().collect(Collectors.toMap(w -> first(w), w -> w, (a, b) -> a + "+" + b))));
            Bulk.p("sum", canon(words().collect(Collectors.toMap(String::length, w -> 1, Integer::sum))));
        });
        Bulk.check("Collectors.toMapIntoAChosenMap", () -> {
            LinkedHashMap<String, String> linked = words().collect(
                    Collectors.toMap(w -> first(w), w -> w, (a, b) -> b, LinkedHashMap::new));
            Bulk.p("linked", linked.toString());
            TreeMap<Integer, String> sorted = words().collect(
                    Collectors.toMap(String::length, w -> w, (a, b) -> a, TreeMap::new));
            Bulk.p("sorted", sorted.toString());
        });
        Bulk.check("Collectors.toMapRefusesANullValue", () -> {
            Bulk.pThrown("merging", NullPointerException.class,
                    () -> words().collect(Collectors.toMap(w -> w + w.hashCode(), w -> (String) null, (a, b) -> a)));
            Bulk.pThrown("plain", NullPointerException.class,
                    () -> words().distinct().collect(Collectors.toMap(w -> w, w -> (String) null)));
        });
    }

    private static void own() {
        Bulk.check("Collectors.collectorOf", () -> {
            Collector<String, StringBuilder, String> initials = Collector.of(StringBuilder::new,
                    (sb, w) -> sb.append(w.charAt(0)), (a, b) -> a.append(b), StringBuilder::toString);
            Bulk.p("initials", words().collect(initials));
            Bulk.p("initialsIdentityFinish",
                    initials.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH));
            Collector<String, List<String>, List<String>> identity = Collector.of(ArrayList::new, List::add,
                    (a, b) -> {
                        a.addAll(b);
                        return a;
                    });
            Bulk.p("identity", words().collect(identity));
            Bulk.p("identityIdentityFinish",
                    identity.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH));
            Collector.Characteristics[] all = Collector.Characteristics.values();
            StringBuilder names = new StringBuilder();
            for (Collector.Characteristics c : all) {
                names.append(c.name()).append('=').append(c.ordinal()).append(' ')
                        .append(Collector.Characteristics.valueOf(c.name()).ordinal()).append(';');
            }
            Bulk.p("characteristics", names.toString());
        });
    }
}
