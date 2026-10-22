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

/// `List.of`, `Set.of`, `Map.of`, their `copyOf` forms, `Map.ofEntries` and
/// `Map.entry`, through the public factories only. (The parity test this
/// replaces called the package-private `ImmutableCollections` the factories
/// delegate to; every one of those calls is made here through the factory
/// that reaches it.) A set and a map iterate in an unspecified order, so
/// they are printed sorted, never in iteration order.
public final class ImmutableChecks {
    private ImmutableChecks() {
    }

    private static void unsupported(String what, Bulk.Body body) {
        Bulk.pThrown(what, UnsupportedOperationException.class, body);
    }

    private static void refusesNull(String what, Bulk.Body body) {
        Bulk.pThrown(what, NullPointerException.class, body);
    }

    private static void duplicate(String what, Bulk.Body body) {
        Bulk.pThrown(what, IllegalArgumentException.class, body);
    }

    /// Runs the group.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void run() {
        Bulk.check("Immutable.listOfHoldsItsElementsInOrder", () -> {
            Bulk.p("empty", List.of().equals(Collections.emptyList()));
            Bulk.p("one", List.of("a"));
            Bulk.p("two", List.of("a", "b"));
            Bulk.p("ten", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
            Bulk.p("array11", List.of(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}));
            // Duplicates are a list's business.
            List<String> list = List.of("x", "y", "x");
            List<String> reference = Collections.unmodifiableList(Arrays.asList("x", "y", "x"));
            Bulk.p("equalsReference", reference.equals(list));
            Bulk.p("referenceEquals", list.equals(reference));
            Bulk.p("hashCode", list.hashCode());
            Bulk.p("hashCodeAsReference", list.hashCode() == reference.hashCode());
            Bulk.p("toString", list.toString());
            Bulk.p("size", list.size());
            Bulk.p("indexOf", list.indexOf("x"));
            Bulk.p("lastIndexOf", list.lastIndexOf("x"));
            Bulk.p("subList", list.subList(1, 3));
            Bulk.p("toArray", list.toArray());
            Bulk.p("toArrayTyped", list.toArray(new String[0]));
            Bulk.p("containsY", list.contains("y"));
            Bulk.p("containsZ", list.contains("z"));
            Bulk.p("containsAll", list.containsAll(Arrays.asList("x", "y")));
            Bulk.pThrown("get3", IndexOutOfBoundsException.class, () -> list.get(3));
        });
        Bulk.check("Immutable.listOfCannotBeChanged", () -> {
            final List<String> list = List.of("a", "b", "c");
            unsupported("add", () -> list.add("d"));
            unsupported("addAt", () -> list.add(0, "d"));
            unsupported("addAll", () -> list.addAll(Arrays.asList("d")));
            unsupported("addAllAt", () -> list.addAll(1, Arrays.asList("d")));
            unsupported("remove", () -> list.remove("a"));
            unsupported("removeAt", () -> list.remove(0));
            unsupported("removeAll", () -> list.removeAll(Arrays.asList("a")));
            unsupported("retainAll", () -> list.retainAll(Arrays.asList("a")));
            unsupported("set", () -> list.set(0, "z"));
            unsupported("clear", () -> list.clear());
            unsupported("sort", () -> list.sort(null));
            unsupported("replaceAll", () -> list.replaceAll(s -> s));
            unsupported("removeIf", () -> list.removeIf(s -> true));
            unsupported("CollectionsSort", () -> Collections.sort(list));
            unsupported("iteratorRemove", () -> {
                Iterator<String> it = list.iterator();
                it.next();
                it.remove();
            });
            unsupported("listIteratorSet", () -> {
                ListIterator<String> it = list.listIterator();
                it.next();
                it.set("z");
            });
            unsupported("listIteratorAdd", () -> list.listIterator().add("z"));
            unsupported("subListClear", () -> list.subList(0, 2).clear());
            unsupported("subListSet", () -> list.subList(0, 2).set(0, "z"));
            unsupported("emptyAdd", () -> List.<String>of().add("d"));
            unsupported("emptyArrayAdd", () -> List.of(new String[0]).add("d"));
            unsupported("singleAdd", () -> List.of("a").add("d"));
            Bulk.p("list", list);
        });
        Bulk.check("Immutable.listOfRefusesNull", () -> {
            refusesNull("of1", () -> List.of((String) null));
            refusesNull("of2", () -> List.of("a", null));
            refusesNull("of5", () -> List.of("a", "b", null, "d", "e"));
            refusesNull("ofArray", () -> List.of(new String[] {"a", null}));
            refusesNull("ofNullArray", () -> List.of((String[]) null));
            refusesNull("copyOf", () -> List.copyOf(Arrays.asList("a", null)));
            refusesNull("copyOfNull", () -> List.copyOf(null));
            refusesNull("contains", () -> List.of("a").contains(null));
            refusesNull("indexOf", () -> List.of("a").indexOf(null));
            refusesNull("lastIndexOf", () -> List.of("a").lastIndexOf(null));
            refusesNull("containsMany", () -> List.of("a", "b", "c").contains(null));
        });
        Bulk.check("Immutable.listOfAnArrayDoesNotKeepTheArray", () -> {
            String[] array = {"a", "b"};
            List<String> list = List.of(array);
            array[0] = "changed";
            Bulk.p("two", list);
            String[] longer = {"a", "b", "c", "d"};
            List<String> four = List.of(longer);
            longer[0] = "changed";
            Bulk.p("four", four);
        });
        Bulk.check("Immutable.listCopyOfIsASnapshot", () -> {
            List<String> source = new ArrayList<String>(Arrays.asList("a", "b"));
            final List<String> copy = List.copyOf(source);
            source.add("c");
            Bulk.p("copy", copy);
            unsupported("add", () -> copy.add("d"));
            // An immutable list is its own copy.
            Bulk.p("selfCopySame", copy == List.copyOf(copy));
            Bulk.p("fromSet", List.copyOf(new LinkedHashSet<String>(Arrays.asList("b", "a", "b"))));
        });
        Bulk.check("Immutable.setOfHoldsItsElements", () -> {
            Bulk.p("empty", Set.of().equals(Collections.emptySet()));
            Set<String> set = Set.of("a", "b", "c");
            Set<String> reference = new HashSet<String>(Arrays.asList("c", "b", "a"));
            Bulk.p("equalsReference", reference.equals(set));
            Bulk.p("referenceEquals", set.equals(reference));
            Bulk.p("hashCode", set.hashCode());
            Bulk.p("size", set.size());
            Bulk.p("containsB", set.contains("b"));
            Bulk.p("containsZ", set.contains("z"));
            Bulk.p("containsAll", set.containsAll(reference));
            Bulk.p("toArray", Bulk.sorted(Arrays.asList(set.toArray(new String[0]))));
            Bulk.p("ten", Bulk.sorted(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
            Bulk.p("array11", Bulk.sorted(Set.of(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})));
            List<String> walked = new ArrayList<String>();
            for (String s : set) {
                walked.add(s);
            }
            Bulk.p("walked", Bulk.sorted(walked));
        });
        Bulk.check("Immutable.setOfCannotBeChanged", () -> {
            final Set<String> set = Set.of("a", "b", "c");
            unsupported("add", () -> set.add("d"));
            unsupported("addPresent", () -> set.add("a"));
            unsupported("addAll", () -> set.addAll(Arrays.asList("d")));
            unsupported("remove", () -> set.remove("a"));
            unsupported("removeAll", () -> set.removeAll(Arrays.asList("a")));
            unsupported("retainAll", () -> set.retainAll(Arrays.asList("a")));
            unsupported("removeIf", () -> set.removeIf(s -> true));
            unsupported("clear", () -> set.clear());
            unsupported("iteratorRemove", () -> {
                Iterator<String> it = set.iterator();
                it.next();
                it.remove();
            });
            unsupported("emptyAdd", () -> Set.<String>of().add("d"));
            unsupported("emptyArrayAdd", () -> Set.of(new String[0]).add("d"));
            Bulk.p("size", set.size());
        });
        Bulk.check("Immutable.setOfRefusesNullAndDuplicates", () -> {
            refusesNull("of1", () -> Set.of((String) null));
            refusesNull("of3", () -> Set.of("a", null, "c"));
            refusesNull("ofArray", () -> Set.of(new String[] {"a", null}));
            refusesNull("copyOf", () -> Set.copyOf(Arrays.asList("a", null)));
            refusesNull("contains", () -> Set.of("a").contains(null));
            refusesNull("containsMany", () -> Set.of("a", "b", "c").contains(null));
            duplicate("of2", () -> Set.of("a", "a"));
            duplicate("of4", () -> Set.of("a", "b", "c", "a"));
            duplicate("ofArray", () -> Set.of(new String[] {"a", "b", "b"}));
        });
        Bulk.check("Immutable.setCopyOfDropsDuplicatesInsteadOfRefusingThem", () -> {
            final Set<String> copy = Set.copyOf(Arrays.asList("a", "b", "a"));
            Bulk.p("copy", Bulk.sorted(copy));
            Bulk.p("selfCopySame", copy == Set.copyOf(copy));
            unsupported("add", () -> copy.add("z"));
        });
        Bulk.check("Immutable.mapOfHoldsItsEntries", () -> {
            Bulk.p("empty", Map.of().equals(Collections.emptyMap()));
            Map<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);
            Map<String, Integer> reference = new HashMap<String, Integer>();
            reference.put("a", 1);
            reference.put("b", 2);
            reference.put("c", 3);
            Bulk.p("equalsReference", reference.equals(map));
            Bulk.p("referenceEquals", map.equals(reference));
            Bulk.p("hashCode", map.hashCode());
            Bulk.p("size", map.size());
            Bulk.p("getB", map.get("b"));
            Bulk.p("getZ", map.get("z"));
            Bulk.p("containsKeyC", map.containsKey("c"));
            Bulk.p("containsKeyZ", map.containsKey("z"));
            Bulk.p("containsValue3", map.containsValue(3));
            Bulk.p("containsValue4", map.containsValue(4));
            Bulk.p("keySet", Bulk.sorted(map.keySet()));
            Bulk.p("keySetEqualsReference", reference.keySet().equals(map.keySet()));
            Bulk.p("values", Bulk.sorted(map.values()));
            Bulk.p("entrySetEqualsReference", reference.entrySet().equals(map.entrySet()));
            Bulk.p("getOrDefault", map.getOrDefault("z", 9));
            Bulk.p("sorted", Bulk.sortedMap(map));
            Map<Integer, Integer> ten = Map.of(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10);
            Bulk.p("tenSize", ten.size());
            Bulk.p("tenGet10", ten.get(10));
            Bulk.p("ten", Bulk.sortedMap(ten));
        });
        Bulk.check("Immutable.mapOfCannotBeChanged", () -> {
            final Map<String, Integer> map = Map.of("a", 1, "b", 2);
            unsupported("put", () -> map.put("c", 3));
            unsupported("putPresent", () -> map.put("a", 1));
            unsupported("putAll", () -> map.putAll(Collections.singletonMap("c", 3)));
            unsupported("remove", () -> map.remove("a"));
            unsupported("clear", () -> map.clear());
            unsupported("putIfAbsent", () -> map.putIfAbsent("c", 3));
            unsupported("removePair", () -> map.remove("a", 1));
            unsupported("replace", () -> map.replace("a", 5));
            unsupported("replacePair", () -> map.replace("a", 1, 5));
            unsupported("replaceAll", () -> map.replaceAll((k, v) -> v));
            unsupported("computeIfAbsent", () -> map.computeIfAbsent("c", k -> 3));
            unsupported("computeIfPresent", () -> map.computeIfPresent("a", (k, v) -> v));
            unsupported("compute", () -> map.compute("a", (k, v) -> v));
            unsupported("merge", () -> map.merge("a", 1, Integer::sum));
            unsupported("keysRemove", () -> map.keySet().remove("a"));
            unsupported("keysClear", () -> map.keySet().clear());
            unsupported("valuesRemove", () -> map.values().remove(1));
            unsupported("valuesClear", () -> map.values().clear());
            unsupported("entriesClear", () -> map.entrySet().clear());
            unsupported("entrySetValue", () -> map.entrySet().iterator().next().setValue(7));
            unsupported("entriesIteratorRemove", () -> {
                Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
                it.next();
                it.remove();
            });
            unsupported("keysIteratorRemove", () -> {
                Iterator<String> it = map.keySet().iterator();
                it.next();
                it.remove();
            });
            unsupported("emptyPut", () -> Map.<String, Integer>of().put("c", 3));
            unsupported("emptyEntriesPut", () -> Map.<String, Integer>ofEntries().put("c", 3));
            Bulk.p("size", map.size());
            Bulk.p("getA", map.get("a"));
        });
        Bulk.check("Immutable.mapOfRefusesNullAndDuplicateKeys", () -> {
            refusesNull("nullKey", () -> Map.of(null, 1));
            refusesNull("nullValue", () -> Map.of("a", null));
            refusesNull("nullLater", () -> Map.of("a", 1, "b", null));
            refusesNull("get", () -> Map.of("a", 1).get(null));
            refusesNull("containsKey", () -> Map.of("a", 1).containsKey(null));
            refusesNull("containsValue", () -> Map.of("a", 1).containsValue(null));
            refusesNull("getMany", () -> Map.of("a", 1, "b", 2, "c", 3).get(null));
            refusesNull("containsKeyMany", () -> Map.of("a", 1, "b", 2, "c", 3).containsKey(null));
            refusesNull("containsValueMany", () -> Map.of("a", 1, "b", 2, "c", 3).containsValue(null));
            refusesNull("copyOfKey", () -> Map.copyOf(Collections.singletonMap(null, 1)));
            refusesNull("copyOfValue", () -> Map.copyOf(Collections.singletonMap("a", null)));
            refusesNull("copyOfNull", () -> Map.copyOf(null));
            duplicate("of2", () -> Map.of("a", 1, "a", 2));
            duplicate("of3", () -> Map.of("a", 1, "b", 2, "a", 1));
        });
        Bulk.check("Immutable.entryOfEntriesAndCopyOf", () -> {
            final Map.Entry<String, Integer> entry = Map.entry("k", 5);
            Map.Entry<String, Integer> reference = new AbstractMap.SimpleImmutableEntry<String, Integer>("k", 5);
            Bulk.p("key", entry.getKey());
            Bulk.p("value", entry.getValue());
            Bulk.p("equalsReference", reference.equals(entry));
            Bulk.p("referenceEquals", entry.equals(reference));
            Bulk.p("hashCode", entry.hashCode());
            Bulk.p("toString", entry.toString());
            unsupported("setValue", () -> entry.setValue(6));
            refusesNull("entryNullKey", () -> Map.entry(null, 1));
            refusesNull("entryNullValue", () -> Map.entry("a", null));

            final Map<String, Integer> map = Map.ofEntries(Map.entry("a", 1), Map.entry("b", 2));
            Bulk.p("ofEntriesSize", map.size());
            Bulk.p("ofEntriesGetB", map.get("b"));
            unsupported("ofEntriesPut", () -> map.put("c", 3));
            Bulk.p("ofEntriesEmpty", Map.ofEntries(new Map.Entry[0]).isEmpty());
            duplicate("ofEntriesDuplicate", () -> Map.ofEntries(Map.entry("a", 1), Map.entry("a", 2)));
            refusesNull("ofEntriesNull", () -> Map.ofEntries(Map.entry("a", 1), null));

            Map<String, Integer> source = new TreeMap<String, Integer>();
            source.put("x", 1);
            Map<String, Integer> copy = Map.copyOf(source);
            source.put("y", 2);
            Bulk.p("copy", Bulk.sortedMap(copy));
            Bulk.p("copyEqualsSingleton", Collections.singletonMap("x", 1).equals(copy));
            Bulk.p("selfCopySame", copy == Map.copyOf(copy));
            Bulk.p("notTheSource", copy != source);
        });
    }
}
