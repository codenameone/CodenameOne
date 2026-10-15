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
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/// Streams, primitive streams, collectors and the stream-era members of the
/// collection interfaces, printed as one line the JVM and ParparVM must agree
/// on. Unordered results (hash maps, Set.of) are sorted before printing.
public class StreamPipelineApp {
    private static final StringBuilder OUT = new StringBuilder();

    private static void out(Object label, Object value) {
        OUT.append(label).append('=').append(value).append(';');
    }

    private static <K, V> String sorted(Map<K, V> map) {
        return new TreeMap<K, V>(map).toString();
    }

    private static <T> String sorted(Collection<T> c) {
        List<String> all = new ArrayList<String>();
        for (T t : c) {
            all.add(String.valueOf(t));
        }
        Collections.sort(all);
        return all.toString();
    }

    private static String ints(int[] a) {
        return Arrays.toString(a);
    }

    interface Check {
        void run();
    }

    private static void fails(String label, Check c) {
        try {
            c.run();
            out(label, "none");
        } catch (NullPointerException e) {
            out(label, "npe");
        } catch (IllegalStateException e) {
            out(label, "ise");
        } catch (IllegalArgumentException e) {
            out(label, "iae");
        } catch (UnsupportedOperationException e) {
            out(label, "uoe");
        } catch (IndexOutOfBoundsException e) {
            out(label, "ioobe");
        } catch (NoSuchElementException e) {
            out(label, "nsee");
        }
    }

    public static void fixtureMain(String[] args) {
        objectStreams();
        laziness();
        primitiveStreams();
        collectors();
        collectionMembers();
        System.out.println("RESULT=" + OUT);
    }

    static void objectStreams() {
        List<String> words = new ArrayList<String>(Arrays.asList("pear", "fig", "apple", "kiwi", "fig", "banana"));
        out("filterMap", words.stream().filter(w -> w.length() > 3).map(String::toUpperCase).collect(Collectors.toList()));
        out("distinctSorted", words.stream().distinct().sorted().collect(Collectors.toList()));
        out("sortedBy", words.stream().sorted(Comparator.comparing(String::length).thenComparing(Comparator.reverseOrder()))
                .collect(Collectors.joining(",", "<", ">")));
        out("skipLimit", words.stream().skip(1).limit(3).collect(Collectors.toList()));
        out("takeDrop", Stream.of(1, 2, 3, 10, 4).takeWhile(i -> i < 5).collect(Collectors.toList()) + "/"
                + Stream.of(1, 2, 3, 10, 4).dropWhile(i -> i < 5).collect(Collectors.toList()));
        out("flatMap", Stream.of("ab", "", "cde").flatMap(s -> IntStream.range(0, s.length()).mapToObj(s::charAt)).collect(Collectors.toList()));
        out("reduce", words.stream().reduce("", (a, b) -> a + b.charAt(0)) + "," + words.stream().map(String::length)
                .reduce(Integer::sum).get() + "," + words.stream().reduce(0, (n, w) -> n + w.length(), Integer::sum));
        out("minMax", words.stream().min(Comparator.naturalOrder()).get() + "," + words.stream().max(Comparator.naturalOrder()).get()
                + "," + Stream.<String>empty().min(Comparator.naturalOrder()).isPresent());
        out("matches", words.stream().anyMatch(w -> w.startsWith("b")) + "," + words.stream().allMatch(w -> w.length() > 2)
                + "," + words.stream().noneMatch(String::isEmpty) + "," + Stream.empty().allMatch(x -> false));
        out("find", words.stream().filter(w -> w.contains("i")).findFirst().get() + ","
                + words.stream().filter(w -> w.contains("z")).findAny().isPresent());
        out("count", words.stream().filter(w -> w.equals("fig")).count());
        out("toArray", Arrays.toString(words.stream().limit(2).toArray()) + Arrays.toString(words.stream().limit(2)
                .toArray(String[]::new)));
        out("iterate", Stream.iterate(1, x -> x * 3).limit(5).collect(Collectors.toList()) + ","
                + Stream.iterate(1, x -> x < 50, x -> x * 4).collect(Collectors.toList()));
        final int[] counter = new int[1];
        out("generate", Stream.generate(() -> counter[0]++).limit(4).collect(Collectors.toList()));
        out("concat", Stream.concat(Stream.of(1, 2), Stream.of(3)).collect(Collectors.toList()));
        out("ofNullable", Stream.ofNullable(null).count() + "," + Stream.ofNullable("x").count());
        Stream.Builder<String> b = Stream.builder();
        b.add("x").add("y");
        b.accept("z");
        out("builder", b.build().collect(Collectors.joining()));
        Iterator<String> it = Stream.of("i1", "i2").iterator();
        StringBuilder seen = new StringBuilder();
        while (it.hasNext()) {
            seen.append(it.next());
        }
        out("iterator", seen);
        out("forEachOrdered", words.stream().limit(2).collect(StringBuilder::new, StringBuilder::append,
                StringBuilder::append));
        out("mapToPrim", words.stream().mapToInt(String::length).sum() + "," + words.stream().mapToLong(String::length)
                .max().getAsLong() + "," + words.stream().mapToDouble(String::length).average().getAsDouble());
        out("flatToPrim", Stream.of("ab", "c").flatMapToInt(s -> IntStream.range(0, s.length()).map(s::charAt)).sum() + ","
                + Stream.of(2L, 3L).flatMapToLong(x -> LongStream.range(0, x)).count() + ","
                + Stream.of(1.5).flatMapToDouble(x -> DoubleStream.of(x, x)).sum());
        out("parallel", words.parallelStream().isParallel() + "," + words.stream().parallel().sequential()
                .unordered().count());
    }

    static void laziness() {
        final StringBuilder trace = new StringBuilder();
        List<Integer> got = Stream.of(1, 2, 3, 4, 5, 6).peek(x -> trace.append('p').append(x)).filter(x -> x % 2 == 0)
                .peek(x -> trace.append('f').append(x)).limit(2).collect(Collectors.toList());
        out("lazyTrace", trace + ":" + got);
        trace.setLength(0);
        Stream<Integer> pending = Stream.of(1, 2, 3).peek(x -> trace.append(x));
        out("nothingYet", "[" + trace + "]");
        pending.findFirst();
        out("findFirstPulls", trace);
        trace.setLength(0);
        out("infiniteFind", Stream.iterate(1, x -> x + 1).peek(x -> trace.append(x)).filter(x -> x > 3).findFirst().get()
                + ":" + trace);
        trace.setLength(0);
        out("anyMatchStops", IntStream.range(0, 100).peek(x -> trace.append(x)).anyMatch(x -> x == 2) + ":" + trace);
        trace.setLength(0);
        out("sortedBarrier", Stream.of(3, 1, 2).peek(x -> trace.append('a').append(x)).sorted()
                .peek(x -> trace.append('b').append(x)).limit(1).collect(Collectors.toList()) + ":" + trace);

        final StringBuilder closed = new StringBuilder();
        Stream<String> s = Stream.of("a").onClose(() -> closed.append('1')).onClose(() -> closed.append('2'));
        s.close();
        s.close();
        out("onClose", closed);
        closed.setLength(0);
        Stream.concat(Stream.of(1).onClose(() -> closed.append('A')), Stream.of(2).onClose(() -> closed.append('B')))
                .close();
        out("concatClose", closed);
        closed.setLength(0);
        out("flatMapCloses", Stream.of(1, 2, 3).flatMap(x -> Stream.of(x, x * 10).onClose(() -> closed.append(x)))
                .limit(3).collect(Collectors.toList()) + ":" + closed);
        closed.setLength(0);
        Stream.of(7, 8).flatMap(x -> Stream.of(x).onClose(() -> closed.append(x))).findFirst();
        out("flatMapFindFirstCloses", closed);
        final Stream<String> once = Stream.of("x");
        once.count();
        fails("reuse", () -> once.count());
        final Stream<String> twice = Stream.of("x");
        twice.map(String::length);
        fails("reuseAfterStage", () -> twice.filter(x -> true));
        fails("negativeLimit", () -> Stream.of(1).limit(-1));
        fails("nullFilter", () -> Stream.of(1).filter(null));
        fails("nullFindFirst", () -> Stream.of((String) null).findFirst());
        fails("generatorTooSmall", () -> Stream.of("a", "b").toArray(n -> new String[1]));

        List<String> live = new ArrayList<String>(Arrays.asList("a"));
        Stream<String> late = live.stream();
        live.add("b");
        out("lateBinding", late.collect(Collectors.toList()));
    }

    static void primitiveStreams() {
        out("range", ints(IntStream.range(2, 6).toArray()) + ints(IntStream.rangeClosed(5, 5).toArray())
                + ints(IntStream.range(5, 2).toArray()));
        out("rangeEdge", IntStream.rangeClosed(Integer.MAX_VALUE - 1, Integer.MAX_VALUE).count() + ","
                + LongStream.rangeClosed(Long.MAX_VALUE - 2, Long.MAX_VALUE).count());
        out("intOps", IntStream.of(5, 3, 8, 3, 1).distinct().sorted().map(x -> x * 2).boxed()
                .collect(Collectors.toList()));
        out("intTerminals", IntStream.of(4, -2, 9).sum() + "," + IntStream.of(4, -2, 9).min().getAsInt() + ","
                + IntStream.of(4, -2, 9).max().getAsInt() + "," + IntStream.of(4, -2, 9).average().getAsDouble() + ","
                + IntStream.empty().average().isPresent() + "," + IntStream.of(1, 2, 3).reduce(10, (a, b) -> a * b) + ","
                + IntStream.of(1, 2, 3).reduce(Math::max).getAsInt());
        out("intOverflow", IntStream.of(Integer.MAX_VALUE, 1).sum());
        IntSummaryStatistics st = IntStream.of(3, 9, -1).summaryStatistics();
        out("intStats", st.getCount() + "," + st.getSum() + "," + st.getMin() + "," + st.getMax() + "," + st.getAverage());
        out("intStatsEmpty", new IntSummaryStatistics().getMin() + "," + new IntSummaryStatistics().getMax());
        out("intConvert", IntStream.of(1, 2).asLongStream().map(x -> x << 40).sum() + ","
                + IntStream.of(1, 2).asDoubleStream().map(x -> x / 4).sum() + ","
                + IntStream.of(65, 66).mapToObj(x -> String.valueOf((char) x)).collect(Collectors.joining()));
        out("intMatch", IntStream.of(2, 4).allMatch(x -> x % 2 == 0) + "," + IntStream.of(2, 4).noneMatch(x -> x > 3)
                + "," + IntStream.of(1, 3).anyMatch(x -> x == 3));
        out("intIterate", ints(IntStream.iterate(1, x -> x * 2).limit(6).toArray()) + ","
                + ints(IntStream.iterate(0, x -> x < 10, x -> x + 3).toArray()));
        final int[] next = {0};
        out("intGenerate", ints(IntStream.generate(() -> next[0]++).skip(2).limit(3).toArray()));
        out("intFlat", ints(IntStream.of(1, 3).flatMap(x -> IntStream.range(0, x)).toArray()));
        out("intTakeDrop", ints(IntStream.of(1, 2, 5, 1).takeWhile(x -> x < 3).toArray())
                + ints(IntStream.of(1, 2, 5, 1).dropWhile(x -> x < 3).toArray()));
        out("intConcat", ints(IntStream.concat(IntStream.of(1), IntStream.of(2, 3)).toArray()));
        out("intCollect", IntStream.of(1, 2, 3).collect(ArrayList<Integer>::new, ArrayList::add, ArrayList::addAll));
        PrimitiveIterator.OfInt pit = IntStream.of(7, 8).iterator();
        StringBuilder pi = new StringBuilder();
        pi.append(pit.nextInt());
        pit.forEachRemaining((IntConsumer) x -> pi.append('+').append(x));
        out("primIterator", pi);
        out("intFind", IntStream.of(9, 8).findFirst().getAsInt() + "," + IntStream.empty().findAny().isPresent());
        fails("emptyGetAsInt", () -> IntStream.empty().max().getAsInt());

        out("longOps", LongStream.of(1L << 40, 3, 3).distinct().map(x -> x + 1).sum() + ","
                + LongStream.range(0, 5).mapToInt(x -> (int) x * 2).sum() + ","
                + LongStream.of(3, 1).sorted().boxed().collect(Collectors.toList()) + ","
                + LongStream.of(5, 7).summaryStatistics().getAverage() + ","
                + LongStream.of(5, 7).asDoubleStream().max().getAsDouble());
        out("doubleOps", DoubleStream.of(1.5, -2.0, 4.25).sum() + "," + DoubleStream.of(1.5, -2.0).min().getAsDouble()
                + "," + DoubleStream.of(0.5, 2.5).average().getAsDouble() + ","
                + DoubleStream.of(2.5, 1.5).mapToLong(x -> (long) Math.floor(x)).sum() + ","
                + DoubleStream.of(2.5, 1.5).mapToInt(x -> (int) x).max().getAsInt() + ","
                + DoubleStream.of(1.0, Double.NaN).max().getAsDouble() + ","
                + DoubleStream.iterate(1.0, x -> x / 2).limit(4).sum());
        DoubleSummaryStatistics ds = DoubleStream.of(1e16, 1.0, -1e16).summaryStatistics();
        out("doubleKahan", ds.getSum() + "," + ds.getMin() + "," + ds.getMax() + "," + ds.getCount());
        out("optionals", OptionalInt.of(3) + "," + OptionalInt.empty() + "," + OptionalLong.of(4).orElse(1) + ","
                + OptionalDouble.empty().orElseGet(() -> 2.5) + "," + OptionalInt.of(5).equals(OptionalInt.of(5)) + ","
                + OptionalInt.of(6).stream().sum() + "," + Optional.of("o").stream().count() + ","
                + Optional.empty().stream().count());
    }

    static class Item {
        final String kind;
        final String name;
        final int qty;

        Item(String kind, String name, int qty) {
            this.kind = kind;
            this.name = name;
            this.qty = qty;
        }

        public String toString() {
            return name;
        }
    }

    static void collectors() {
        List<Item> items = Arrays.asList(new Item("fruit", "apple", 3), new Item("veg", "leek", 1),
                new Item("fruit", "plum", 5), new Item("nut", "pecan", 2), new Item("veg", "kale", 4));
        out("groupingBy", sorted(items.stream().collect(Collectors.groupingBy(i -> i.kind))));
        out("groupingByCounting", sorted(items.stream().collect(Collectors.groupingBy(i -> i.kind, Collectors.counting()))));
        out("groupingByTree", items.stream().collect(Collectors.groupingBy(i -> i.kind, TreeMap::new,
                Collectors.mapping(i -> i.name, Collectors.joining("|")))));
        out("partitioningBy", items.stream().collect(Collectors.partitioningBy(i -> i.qty > 2)));
        out("partitionEmpty", Stream.<Item>empty().collect(Collectors.partitioningBy(i -> true, Collectors.counting())));
        out("toMap", sorted(items.stream().collect(Collectors.toMap(i -> i.name, i -> i.qty))));
        out("toMapMerge", sorted(items.stream().collect(Collectors.toMap(i -> i.kind, i -> i.qty, Integer::sum))));
        out("toMapTree", items.stream().collect(Collectors.toMap(i -> i.kind, i -> i.name, (a, b) -> a + "&" + b,
                TreeMap::new)));
        fails("toMapDuplicate", () -> items.stream().collect(Collectors.toMap(i -> i.kind, i -> i.qty)));
        fails("toMapNullValue", () -> Stream.of("a").collect(Collectors.toMap(x -> x, x -> null)));
        out("numbers", items.stream().collect(Collectors.summingInt(i -> i.qty)) + ","
                + items.stream().collect(Collectors.summingLong(i -> i.qty)) + ","
                + items.stream().collect(Collectors.summingDouble(i -> i.qty / 2.0)) + ","
                + items.stream().collect(Collectors.averagingInt(i -> i.qty)) + ","
                + items.stream().collect(Collectors.averagingLong(i -> i.qty)) + ","
                + items.stream().collect(Collectors.averagingDouble(i -> i.qty)) + ","
                + Stream.<Item>empty().collect(Collectors.averagingInt(i -> i.qty)) + ","
                + items.stream().collect(Collectors.counting()));
        IntSummaryStatistics is = items.stream().collect(Collectors.summarizingInt(i -> i.qty));
        out("summarizing", is.getMin() + "," + is.getMax() + "," + is.getSum() + ","
                + items.stream().collect(Collectors.summarizingLong(i -> i.qty)).getMax() + ","
                + items.stream().collect(Collectors.summarizingDouble(i -> i.qty)).getAverage());
        out("reducing", items.stream().map(i -> i.qty).collect(Collectors.reducing(0, Integer::sum)) + ","
                + items.stream().map(i -> i.qty).collect(Collectors.reducing(Integer::max)).get() + ","
                + items.stream().collect(Collectors.reducing(0, i -> i.qty, Integer::sum)) + ","
                + Stream.<Integer>empty().collect(Collectors.reducing(Integer::max)).isPresent());
        out("minMaxBy", items.stream().collect(Collectors.minBy(Comparator.comparingInt(i -> i.qty))).get() + ","
                + items.stream().collect(Collectors.maxBy(Comparator.comparingInt(i -> i.qty))).get());
        out("filteringFlatMapping", items.stream().collect(Collectors.filtering(i -> i.qty > 1,
                Collectors.mapping(i -> i.name, Collectors.toList()))) + ","
                + items.stream().limit(2).collect(Collectors.flatMapping(i -> Stream.of(i.kind, i.name),
                Collectors.toList())));
        out("collectingAndThen", items.stream().map(i -> i.name).collect(Collectors.collectingAndThen(
                Collectors.toList(), List::size)));
        out("toSet", sorted(items.stream().map(i -> i.kind).collect(Collectors.toSet())) + ","
                + items.stream().map(i -> i.kind).collect(Collectors.toCollection(TreeSet::new)));
        out("joining", Stream.of("a", "b").collect(Collectors.joining()) + "," + Stream.<String>empty()
                .collect(Collectors.joining(",", "[", "]")) + "," + Stream.of("x").collect(Collectors.joining("-", "(", ")")));
        fails("unmodifiableList", () -> Stream.of(1).collect(Collectors.toUnmodifiableList()).add(2));
        fails("unmodifiableListNull", () -> Stream.of((Object) null).collect(Collectors.toUnmodifiableList()));
        fails("unmodifiableSet", () -> Stream.of(1).collect(Collectors.toUnmodifiableSet()).add(2));
        fails("unmodifiableMap", () -> Stream.of(1).collect(Collectors.toUnmodifiableMap(x -> x, x -> x)).clear());
        out("unmodifiableMapMerge", sorted(Stream.of(1, 1, 2).collect(Collectors.toUnmodifiableMap(x -> x, x -> 1,
                Integer::sum))));
        Collector<String, ?, List<String>> custom = Collector.of(ArrayList::new, List::add,
                (a, c) -> { a.addAll(c); return a; });
        out("collectorOf", Stream.of("q").collect(custom) + "," + custom.characteristics().contains(
                Collector.Characteristics.IDENTITY_FINISH) + "," + Stream.of("r", "s").collect(Collector.of(
                StringBuilder::new, StringBuilder::append, StringBuilder::append, StringBuilder::length)));
        fails("nullKey", () -> Stream.of("a").collect(Collectors.groupingBy(x -> null)));
    }

    static void collectionMembers() {
        List<Integer> l = List.of(3, 1, 2);
        out("listOf", l + "," + List.of() + "," + List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).size() + ","
                + l.indexOf(2) + "," + l.contains(1) + "," + l.equals(Arrays.asList(3, 1, 2)) + ","
                + (l.hashCode() == Arrays.asList(3, 1, 2).hashCode()));
        fails("listOfNull", () -> List.of(1, null));
        fails("listOfAdd", () -> l.add(4));
        fails("listOfSet", () -> l.set(0, 4));
        fails("listOfSort", () -> l.sort(null));
        fails("listOfContainsNull", () -> l.contains(null));
        fails("listOfGet", () -> l.get(3));
        Integer[] backing = {1, 2};
        List<Integer> copied = List.of(backing);
        backing[0] = 9;
        out("listOfCopies", copied);
        List<Integer> mutable = new ArrayList<Integer>(Arrays.asList(5, 6));
        List<Integer> copy = List.copyOf(mutable);
        mutable.add(7);
        out("listCopyOf", copy + "," + (List.copyOf(copy) == copy));
        out("setOf", sorted(Set.of("b", "a", "c")) + "," + Set.of().size() + "," + Set.of(1).contains(1) + ","
                + Set.of(1, 2).equals(new HashSet<Integer>(Arrays.asList(2, 1))) + "," + sorted(Set.copyOf(Arrays.asList(1, 1, 2))));
        fails("setOfDuplicate", () -> Set.of(1, 1));
        fails("setOfNull", () -> Set.of((Object) null));
        fails("setOfRemove", () -> Set.of(1).remove(1));
        Map<String, Integer> m = Map.of("one", 1, "two", 2);
        out("mapOf", sorted(m) + "," + Map.of().size() + "," + m.get("two") + "," + m.containsKey("three") + ","
                + m.getOrDefault("x", 0) + "," + sorted(Map.ofEntries(Map.entry("k", "v"), Map.entry("j", "w")))
                + "," + Map.of(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10).size() + ","
                + m.equals(new HashMap<String, Integer>(m)));
        fails("mapOfDuplicate", () -> Map.of("a", 1, "a", 2));
        fails("mapOfNullValue", () -> Map.of("a", null));
        fails("mapOfPut", () -> m.put("x", 1));
        fails("mapOfMerge", () -> m.merge("one", 1, Integer::sum));
        fails("mapOfGetNull", () -> m.get(null));
        Map<String, Integer> hm = new HashMap<String, Integer>(m);
        Map<String, Integer> mc = Map.copyOf(hm);
        hm.put("three", 3);
        out("mapCopyOf", sorted(mc) + "," + (Map.copyOf(mc) == mc));
        Map.Entry<String, Integer> e = Map.entry("k", 5);
        out("entry", e + "," + e.getKey() + e.getValue() + "," + e.equals(new AbstractMap.SimpleEntry<String, Integer>("k", 5)));
        fails("entrySet", () -> e.setValue(6));
        fails("entryNull", () -> Map.entry(null, 1));
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(
                new TreeMap<String, Integer>(Map.of("b", 1, "a", 3, "c", 2)).entrySet());
        Collections.sort(entries, Map.Entry.comparingByValue());
        out("byValue", entries);
        Collections.sort(entries, Map.Entry.comparingByKey(Comparator.reverseOrder()));
        out("byKeyDesc", entries);
        Collections.sort(entries, Map.Entry.<String, Integer>comparingByKey());
        out("byKey", entries);
        Collections.sort(entries, Map.Entry.comparingByValue(Comparator.reverseOrder()));
        out("byValueDesc", entries);

        final StringBuilder rest = new StringBuilder();
        Iterator<String> it = Arrays.asList("a", "b", "c").iterator();
        it.next();
        it.forEachRemaining(rest::append);
        out("forEachRemaining", rest);
        Iterator<String> plain = new Iterator<String>() {
            public boolean hasNext() {
                return false;
            }

            public String next() {
                throw new NoSuchElementException();
            }
        };
        fails("defaultRemove", plain::remove);
        out("toArrayGen", Arrays.toString(new ArrayList<String>(Arrays.asList("x", "y")).toArray(String[]::new)));
        Iterable<Integer> iterable = () -> Arrays.asList(4, 5, 6).iterator();
        out("streamSupport", StreamSupport.stream(iterable.spliterator(), false).map(x -> x * 2)
                .collect(Collectors.toList()));
        Spliterator<String> sp = Arrays.asList("s", "t").spliterator();
        StringBuilder sps = new StringBuilder();
        sp.tryAdvance(sps::append);
        sp.forEachRemaining(sps::append);
        out("spliterator", sps + "," + Arrays.asList(1, 2, 3).spliterator().getExactSizeIfKnown() + ","
                + iterable.spliterator().getExactSizeIfKnown() + ","
                + Arrays.asList(1).spliterator().hasCharacteristics(Spliterator.SIZED));
        Iterator<Integer> fromSp = Spliterators.iterator(Arrays.asList(8, 9).spliterator());
        out("spliteratorsIterator", fromSp.next() + "," + fromSp.hasNext() + "," + fromSp.next() + "," + fromSp.hasNext());
        out("arraysStream", Arrays.stream(new String[] {"a", "b", "c"}, 1, 3).collect(Collectors.toList()) + ","
                + Arrays.stream(new int[] {1, 2, 3}).sum() + "," + Arrays.stream(new long[] {4, 5}, 1, 2).sum() + ","
                + Arrays.stream(new double[] {0.5, 0.25}).sum());
        fails("arraysStreamRange", () -> Arrays.stream(new int[2], 1, 3));
        out("streamOfSet", new TreeSet<Integer>(Arrays.asList(3, 1, 2)).stream().map(String::valueOf)
                .collect(Collectors.joining()));
        Map<String, List<Integer>> grouped = new TreeMap<String, List<Integer>>();
        for (int i = 0; i < 5; i++) {
            grouped.computeIfAbsent(i % 2 == 0 ? "even" : "odd", k -> new ArrayList<Integer>()).add(i);
        }
        out("computeIfAbsent", grouped);
        out("removeIfStream", new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4)).stream().filter(x -> x > 1)
                .mapToInt(Integer::intValue).boxed().collect(Collectors.toCollection(LinkedList::new)));
    }
}
