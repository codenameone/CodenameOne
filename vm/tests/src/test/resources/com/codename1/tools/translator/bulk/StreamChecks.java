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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/// java.util.stream.Stream: the intermediate and terminal operations, the
/// order they keep, laziness and short-circuiting, closing, and the
/// factories.
public final class StreamChecks {
    private static final List<String> WORDS = Collections.unmodifiableList(Arrays.asList(
            "pear", "apple", "fig", "banana", "apple", "kiwi", "cherry", "fig", "date"));
    private static final List<Integer> NUMBERS = Collections.unmodifiableList(Arrays.asList(
            5, 3, 8, 3, 1, 9, 2, 8, 7, 4, 6, 0));

    private StreamChecks() {
    }

    /// A stream over `list` through its spliterator, which is what
    /// `Collection.stream()` does.
    private static <T> Stream<T> src(List<T> list) {
        return StreamSupport.stream(list.spliterator(), false);
    }

    private static <T> List<T> listed(Stream<T> stream) {
        return stream.collect(Collectors.<T>toList());
    }

    /// The characters of `s`, as `String.chars()` answers them.
    private static IntStream chars(String s) {
        return IntStream.range(0, s.length()).map(s::charAt);
    }

    /// Runs the group.
    public static void run() {
        intermediate();
        laziness();
        closing();
        terminal();
        life();
        factories();
        later();
        primitives();
    }

    private static void intermediate() {
        Bulk.check("Stream.sortedRefusesANullComparator", () -> {
            Bulk.pThrown("sortedNull", NullPointerException.class, () -> Stream.of("b", "a").sorted(null));
        });
        Bulk.check("Stream.filterAndMapKeepEncounterOrder", () -> {
            Bulk.p("words", listed(src(WORDS).filter(w -> w.length() > 3).map(w -> w.toUpperCase() + w.length())));
            Bulk.p("numbers", listed(src(NUMBERS).map(n -> n * n).filter(n -> n % 2 == 0)));
        });
        Bulk.check("Stream.distinctKeepsTheFirstOfEachInOrder", () -> {
            Bulk.p("words", listed(src(WORDS).distinct()));
            Bulk.p("numbers", listed(src(NUMBERS).distinct()));
            Bulk.p("withNull", listed(src(Arrays.asList("a", null, "b", null, "a")).distinct()));
        });
        Bulk.check("Stream.sortedIsNaturalOrByComparatorAndStable", () -> {
            Bulk.p("natural", listed(src(WORDS).sorted()));
            Bulk.p("reverse", listed(src(NUMBERS).sorted(Collections.reverseOrder())));
            // By length alone: words of one length must keep their order.
            Comparator<String> byLength = (a, b) -> a.length() - b.length();
            Bulk.p("byLength", listed(src(WORDS).sorted(byLength)));
        });
        Bulk.check("Stream.limitAndSkipAtEveryBoundary", () -> {
            long[] counts = {0, 1, 3, NUMBERS.size() - 1, NUMBERS.size(), NUMBERS.size() + 5, Long.MAX_VALUE};
            for (long n : counts) {
                Bulk.p("limit " + n, listed(src(NUMBERS).limit(n)));
                Bulk.p("skip " + n, listed(src(NUMBERS).skip(n)));
                Bulk.p("skip 2 limit " + n, listed(src(NUMBERS).skip(2).limit(n)));
                Bulk.p("limit skip 2 " + n, listed(src(NUMBERS).limit(n).skip(2)));
            }
        });
        Bulk.check("Stream.aNegativeLimitOrSkipIsRefused", () -> {
            Bulk.pThrown("limit", IllegalArgumentException.class, () -> src(NUMBERS).limit(-1));
            Bulk.pThrown("skip", IllegalArgumentException.class, () -> src(NUMBERS).skip(-1));
        });
        Bulk.check("Stream.flatMapConcatenatesInOrderAndSkipsNull", () -> {
            Bulk.p("skipsNull", listed(src(WORDS).flatMap(w -> w.length() == 3 ? null
                    : src(Arrays.asList(w, w.substring(0, 1))))));
            Bulk.p("limited", listed(src(NUMBERS).flatMap(n -> Stream.of(n, -n)).limit(7)));
            Bulk.p("empties", listed(src(NUMBERS).flatMap(n -> Stream.<Integer>empty())));
        });
        Bulk.check("Stream.flatMapToThePrimitiveStreams", () -> {
            Bulk.p("toInt", src(WORDS).flatMapToInt(w -> chars(w)).limit(20).toArray());
            Bulk.p("toLong", src(NUMBERS).flatMapToLong(n -> LongStream.rangeClosed(1, n)).toArray());
            Bulk.p("toDouble", src(NUMBERS).flatMapToDouble(n -> DoubleStream.of(n, n / 2.0)).toArray());
        });
    }

    private static void laziness() {
        Bulk.check("Stream.nothingRunsUntilATerminalOperation", () -> {
            List<Integer> seen = new ArrayList<Integer>();
            Stream<Integer> pending = src(NUMBERS).peek(seen::add).filter(n -> n > 2).map(n -> n + 1);
            Bulk.p("seenBefore", seen);
            Bulk.p("first", pending.findFirst().get());
            Bulk.p("seenAfter", seen);
        });
        Bulk.check("Stream.findFirstPullsOnlyWhatItNeeds", () -> {
            List<String> seen = new ArrayList<String>();
            Bulk.p("first", src(WORDS).peek(seen::add).filter(w -> w.startsWith("b")).findFirst().get());
            Bulk.p("seen", seen);
        });
        Bulk.check("Stream.limitStopsThePipelineAboveIt", () -> {
            List<Integer> seen = new ArrayList<Integer>();
            Bulk.p("out", listed(src(NUMBERS).peek(seen::add).map(n -> n * 2).limit(3)));
            Bulk.p("seen", seen);
        });
        Bulk.check("Stream.theMatchesStopAtTheirAnswer", () -> {
            List<Integer> seen = new ArrayList<Integer>();
            Bulk.p("any", src(NUMBERS).peek(seen::add).anyMatch(n -> n == 8));
            Bulk.p("anySeen", seen);
            seen.clear();
            Bulk.p("all", src(NUMBERS).peek(seen::add).allMatch(n -> n != 1));
            Bulk.p("allSeen", seen);
            seen.clear();
            Bulk.p("none", src(NUMBERS).peek(seen::add).noneMatch(n -> n == 9));
            Bulk.p("noneSeen", seen);
        });
        Bulk.check("Stream.theMatchesOfAnEmptyStream", () -> {
            List<Integer> none = Collections.<Integer>emptyList();
            Bulk.p("any", src(none).anyMatch(n -> true));
            Bulk.p("all", src(none).allMatch(n -> false));
            Bulk.p("none", src(none).noneMatch(n -> true));
        });
        Bulk.check("Stream.anInfiniteStreamIsCutByLimit", () -> {
            Bulk.p("iterate", listed(Stream.iterate(1, n -> n * 2).limit(12)));
            final int[] next = {0};
            Bulk.p("generate", listed(Stream.generate(() -> next[0]++).filter(n -> n % 3 == 0).limit(5)));
            Bulk.p("generated", next[0]);
            Bulk.p("anyOfInfinite", Stream.iterate(1, n -> n + 1).anyMatch(n -> n == 1000));
        });
        Bulk.check("Stream.flatMapIsLazyInsideItsInnerStream", () -> {
            // JDK 8 pushes a whole inner stream before it looks at the
            // limit; the later ones stop, and so does JavaAPI's.
            List<Integer> seen = new ArrayList<Integer>();
            Bulk.p("first", src(NUMBERS).flatMap(n -> Stream.iterate(1, i -> i + 1).peek(seen::add)).findFirst().get());
            Bulk.p("seen", seen);
        });
    }

    private static void closing() {
        // A mapped stream is closed whether or not all of it was wanted.
        Bulk.check("Stream.aMappedStreamLeftPartReadIsClosed", () -> {
            List<Integer> source = Arrays.asList(1, 2, 3);
            List<String> closed = new ArrayList<String>();
            Bulk.p("first", src(source).flatMap(n -> Stream.of(n, -n).onClose(() -> closed.add("first " + n)))
                    .findFirst().get());
            Bulk.p("limit", listed(src(source).flatMap(n -> Stream.of(n, -n).onClose(() -> closed.add("limit " + n)))
                    .limit(3)));
            Bulk.p("any", src(source).flatMap(n -> Stream.of(n, -n).onClose(() -> closed.add("any " + n)))
                    .anyMatch(v -> v == 2));
            Bulk.p("all", src(source).flatMap(n -> Stream.of(n, -n).onClose(() -> closed.add("all " + n)))
                    .allMatch(v -> v > 0));
            Bulk.p("none", src(source).flatMap(n -> Stream.of(n, -n).onClose(() -> closed.add("none " + n)))
                    .noneMatch(v -> v == 1));
            Bulk.p("int", src(source).flatMapToInt(n -> IntStream.of(n, -n).onClose(() -> closed.add("int " + n)))
                    .findFirst().getAsInt());
            Bulk.p("closed", closed);

            closed.clear();
            Bulk.p("takeWhile", listed(src(source)
                    .flatMap(n -> Stream.of(n, -n).onClose(() -> closed.add("while " + n))).takeWhile(v -> v != -2)));
            Bulk.p("whileClosed", closed);

            // A limit says so about the stages before it only: the mapped
            // stream after it is still being read when the limit runs out.
            List<String> order = new ArrayList<String>();
            src(source).limit(1).flatMap(n -> Stream.of(n, -n).onClose(() -> order.add("closed")))
                    .forEach(v -> order.add(String.valueOf(v)));
            Bulk.p("order", order);
        });
        // An iterator nobody reads to the end is the caller's to close.
        // JavaAPI's iterator pulls the mapped stream lazily and leaves it
        // open; the JDK's buffers the whole mapped stream on the first
        // next() and closes it there, so the two differ by design.
        Bulk.cn1Only("Stream.aMappedStreamLeftPartReadIsClosedByTheCaller", () -> {
            List<String> closed = new ArrayList<String>();
            Stream<Integer> open = src(Arrays.asList(1, 2, 3))
                    .flatMap(n -> Stream.of(n, -n).onClose(() -> closed.add("it " + n)));
            Iterator<Integer> it = open.iterator();
            Bulk.expectEquals(Integer.valueOf(1), it.next(), "first element");
            Bulk.expect(closed.isEmpty(), "nothing closed before the caller closes: " + closed);
            open.close();
            open.close();
            Bulk.expectEquals(Collections.singletonList("it 1"), closed, "closed once by the caller");
        });
        // Every close handler runs, past an Error too, and the failures
        // after the first are suppressed in it.
        Bulk.check("Stream.closeRunsEveryHandlerAndKeepsEveryFailure", () -> {
            final IllegalStateException a = new IllegalStateException("a");
            final AssertionError b = new AssertionError("b");
            final IllegalArgumentException c = new IllegalArgumentException("c");
            final List<String> ran = new ArrayList<String>();
            Runnable first = () -> {
                ran.add("1");
                throw a;
            };
            Runnable second = () -> {
                ran.add("2");
                throw b;
            };
            Runnable third = () -> {
                ran.add("3");
                throw c;
            };
            Runnable fourth = () -> ran.add("4");
            Runnable again = () -> {
                ran.add("5");
                throw a;
            };
            Stream<Integer> s = Stream.of(1).onClose(first).onClose(second).onClose(third).onClose(fourth)
                    .onClose(again);
            String outcome = "none";
            try {
                s.close();
            } catch (IllegalStateException e) {
                outcome = e == a ? "first" : "other ISE";
            }
            Bulk.p("threw", outcome);
            Bulk.p("ran", ran);
            Throwable[] suppressed = a.getSuppressed();
            Bulk.p("suppressedCount", suppressed.length);
            for (int i = 0; i < suppressed.length; i++) {
                String which = suppressed[i] == b ? "b" : suppressed[i] == c ? "c" : suppressed[i] == a ? "a" : "?";
                Bulk.p("suppressed" + i, which);
            }
        });
        // Closing any stage closes the pipeline: a stage made from the
        // closed one, before or after, runs nothing.
        Bulk.check("Stream.aClosedPipelineRefusesEveryStage", () -> {
            Stream<Integer> closedItself = Stream.of(1).map(x -> x);
            closedItself.close();
            Stream<Integer> source = Stream.of(1);
            Stream<Integer> madeBefore = source.map(x -> x);
            source.close();
            Stream<Integer> early = Stream.of(1);
            Stream<Integer> later = early.filter(x -> true).map(x -> x);
            later.close();
            Bulk.p("closedItselfCount", Bulk.thrown(IllegalStateException.class, () -> closedItself.count()));
            Bulk.p("madeBeforeCount", Bulk.thrown(IllegalStateException.class, () -> madeBefore.count()));
            Bulk.p("madeBeforeFilter", Bulk.thrown(IllegalStateException.class, () -> madeBefore.filter(x -> true)));
            Bulk.p("madeBeforeOnClose", Bulk.thrown(IllegalStateException.class, () -> madeBefore.onClose(() -> { })));
            Bulk.p("laterIterator", Bulk.thrown(IllegalStateException.class, () -> later.iterator()));
            String message = "none";
            try {
                Stream<Integer> s = Stream.of(1);
                Stream<Integer> m = s.map(x -> x);
                s.close();
                m.count();
            } catch (IllegalStateException e) {
                message = e.getMessage() != null ? "has message" : "null message";
            }
            Bulk.p("message", message);
            // Closing again is allowed, and runs nothing twice.
            List<String> ran = new ArrayList<String>();
            Stream<Integer> once = Stream.of(1).onClose(() -> ran.add("closed"));
            Stream<Integer> mapped = once.map(x -> x);
            mapped.close();
            mapped.close();
            Bulk.p("ran", ran);
        });
    }

    private static void terminal() {
        Bulk.check("Stream.reduceInItsThreeForms", () -> {
            Bulk.p("identity", src(NUMBERS).reduce(100, (a, b) -> a - b));
            Bulk.p("optional", src(WORDS).reduce((a, b) -> a + "," + b).get());
            Bulk.p("combiner", src(WORDS).reduce(0, (n, w) -> n + w.length(), (a, b) -> a + b));
            List<String> none = Collections.<String>emptyList();
            Bulk.p("emptyOptional", src(none).reduce((a, b) -> a + b).isPresent());
            Bulk.p("emptyIdentity", src(none).reduce("seed", (a, b) -> a + b));
        });
        Bulk.check("Stream.minMaxAndCount", () -> {
            Comparator<String> natural = Comparator.naturalOrder();
            Comparator<String> byLength = (a, b) -> a.length() - b.length();
            // Words of one length are told apart by where they stand.
            List<String> tagged = new ArrayList<String>();
            for (int i = 0; i < WORDS.size(); i++) {
                tagged.add(WORDS.get(i) + "#" + i);
            }
            Comparator<String> byWordLength = (a, b) -> a.indexOf('#') - b.indexOf('#');
            Bulk.p("min", src(WORDS).min(natural).get());
            Bulk.p("max", src(WORDS).max(natural).get());
            // Ties: which of the equal ones is answered.
            Bulk.p("minByLength", src(WORDS).min(byLength).get());
            Bulk.p("maxByLength", src(WORDS).max(byLength).get());
            Bulk.p("minTie", src(tagged).min(byWordLength).get());
            Bulk.p("maxTie", src(tagged).max(byWordLength).get());
            Bulk.p("count", src(WORDS).count());
            Bulk.p("countFiltered", src(WORDS).filter(w -> w.length() == 3).count());
            List<String> none = Collections.<String>emptyList();
            Bulk.p("emptyMin", src(none).min(natural).isPresent());
            Bulk.p("emptyMax", src(none).max(natural).isPresent());
            Bulk.p("emptyCount", src(none).count());
        });
        Bulk.check("Stream.findFirstAndFindAny", () -> {
            Bulk.p("first", src(WORDS).findFirst().get());
            Bulk.p("any", src(WORDS).skip(3).findAny().get());
            Bulk.p("emptyFirst", src(WORDS).filter(w -> w.isEmpty()).findFirst().isPresent());
        });
        Bulk.check("Stream.findFirstRefusesANullElement", () -> {
            Bulk.pThrown("findFirst", NullPointerException.class, () -> src(Arrays.asList(null, "a")).findFirst());
        });
        Bulk.check("Stream.toArrayPlainAndTyped", () -> {
            Bulk.p("plain", src(WORDS).sorted().toArray());
            String[] typed = src(WORDS).filter(w -> w.length() > 4).toArray(String[]::new);
            Bulk.p("typed", typed);
            Bulk.p("typedIsStringArray", typed.getClass() == String[].class);
            Bulk.p("emptyLength", src(Collections.<String>emptyList()).toArray(String[]::new).length);
        });
        Bulk.check("Stream.forEachAndForEachOrderedVisitInOrder", () -> {
            List<String> seen = new ArrayList<String>();
            src(WORDS).filter(w -> w.length() != 4).forEach(seen::add);
            Bulk.p("forEach", seen);
            List<String> ordered = new ArrayList<String>();
            src(WORDS).filter(w -> w.length() != 4).forEachOrdered(ordered::add);
            Bulk.p("forEachOrdered", ordered);
        });
        Bulk.check("Stream.collectWithASupplierAndAccumulator", () -> {
            Bulk.p("initials", src(WORDS).collect(StringBuilder::new, (sb, w) -> sb.append(w.charAt(0)),
                    (a, b) -> a.append(b)).toString());
        });
        Bulk.check("Stream.theIteratorWalksThePipeline", () -> {
            Iterator<String> it = src(WORDS).filter(w -> w.length() > 4).map(String::toUpperCase).iterator();
            List<String> walked = new ArrayList<String>();
            while (it.hasNext()) {
                walked.add(it.next());
            }
            Bulk.p("walked", walked);
            Bulk.p("hasNext", it.hasNext());
            Bulk.pThrown("nextAtEnd", NoSuchElementException.class, it::next);
        });
    }

    private static void life() {
        Bulk.check("Stream.aStreamRunsOnce", () -> {
            Stream<String> stream = src(WORDS);
            Bulk.p("count", stream.count());
            Bulk.pThrown("again", IllegalStateException.class, stream::count);
        });
        Bulk.check("Stream.aStageTakesOneOperation", () -> {
            Stream<String> stream = src(WORDS);
            stream.filter(w -> true);
            Bulk.pThrown("second", IllegalStateException.class, () -> stream.map(w -> w));
        });
        Bulk.check("Stream.theSourceIsReadWhenTheTerminalOperationStarts", () -> {
            List<String> source = new ArrayList<String>(Arrays.asList("a", "b"));
            Stream<String> stream = src(source).map(String::toUpperCase);
            source.add("c");
            Bulk.p("out", listed(stream));
        });
        Bulk.check("Stream.closeRunsTheHandlersOnceInOrder", () -> {
            List<String> ran = new ArrayList<String>();
            Stream<String> stream = src(WORDS).onClose(() -> ran.add("one"))
                    .filter(w -> true).onClose(() -> ran.add("two"));
            stream.close();
            stream.close();
            Bulk.p("ran", ran);
        });
        Bulk.check("Stream.sequentialParallelAndUnordered", () -> {
            Bulk.p("sequential", src(WORDS).sequential().isParallel());
            // JavaAPI runs a parallel stream sequentially, which the JDK
            // allows, but reports what was asked for as the JDK does.
            Bulk.p("parallel", src(WORDS).parallel().isParallel());
            Bulk.p("parallelMap", listed(src(WORDS).parallel().map(String::length)));
            Bulk.p("unorderedSorted", listed(src(WORDS).unordered().sorted()));
        });
        Bulk.check("Stream.aNullFunctionIsRefusedAtOnce", () -> {
            Bulk.pThrown("map", NullPointerException.class, () -> src(WORDS).map(null));
            Bulk.pThrown("filter", NullPointerException.class, () -> src(WORDS).filter(null));
            Bulk.pThrown("forEach", NullPointerException.class, () -> src(WORDS).sorted().forEach(null));
        });
    }

    private static void factories() {
        Bulk.check("Stream.ofEmptyConcatAndBuilder", () -> {
            Bulk.p("of", listed(Stream.of("a", "b", "c")));
            Bulk.p("ofOne", listed(Stream.of("solo")));
            Bulk.p("empty", listed(Stream.<String>empty()));
            Bulk.p("concat", listed(Stream.concat(src(WORDS).limit(2), src(WORDS).skip(7))));
            Bulk.p("builder", listed(Stream.<String>builder().add("x").add("y").build()));
            Stream.Builder<String> built = Stream.builder();
            built.accept("z");
            built.build();
            Bulk.pThrown("addAfterBuild", IllegalStateException.class, () -> built.add("late"));
        });
        Bulk.check("Stream.concatIsLazyAndClosesBoth", () -> {
            List<String> seen = new ArrayList<String>();
            List<String> closed = new ArrayList<String>();
            Stream<String> both = Stream.concat(src(WORDS).peek(seen::add).onClose(() -> closed.add("a")),
                    src(WORDS).peek(seen::add).onClose(() -> closed.add("b")));
            Bulk.p("seenBefore", seen);
            Bulk.p("first", both.findFirst().get());
            Bulk.p("seenAfter", seen);
            both.close();
            Bulk.p("closed", closed);
        });
    }

    private static void later() {
        Bulk.check("Stream.takeWhileAndDropWhile", () -> {
            Bulk.p("take", listed(src(NUMBERS).takeWhile(n -> n < 8)));
            Bulk.p("drop", listed(src(NUMBERS).dropWhile(n -> n < 8)));
            Bulk.p("takeAll", listed(src(NUMBERS).takeWhile(n -> true)));
            Bulk.p("dropNone", listed(src(NUMBERS).dropWhile(n -> false)));
            Bulk.p("takeNone", listed(src(NUMBERS).takeWhile(n -> false)));
            Bulk.p("dropAll", listed(src(NUMBERS).dropWhile(n -> true)));
            // takeWhile stops asking: an infinite stream ends.
            Bulk.p("infinite", listed(Stream.iterate(1, n -> n + 1).takeWhile(n -> n < 5)));
            // dropWhile stops testing after the first that fails.
            List<Integer> tested = new ArrayList<Integer>();
            listed(src(NUMBERS).dropWhile(n -> {
                tested.add(n);
                return n != 8;
            }));
            Bulk.p("tested", tested);
        });
        Bulk.check("Stream.ofNullableAndTheBoundedIterate", () -> {
            Bulk.p("ofNullable", listed(Stream.ofNullable("a")));
            Bulk.p("ofNullableNull", listed(Stream.<String>ofNullable(null)));
            Bulk.p("iterate", listed(Stream.iterate(1, n -> n < 50, n -> n * 3)));
            Bulk.p("iterateNone", listed(Stream.iterate(1, n -> n < 0, n -> n * 3)));
        });
        Bulk.check("Stream.toListIsUnmodifiableAndTakesNull", () -> {
            List<String> out = src(Arrays.asList("a", null, "b")).toList();
            Bulk.p("out", out);
            Bulk.pThrown("add", UnsupportedOperationException.class, () -> out.add("c"));
            Bulk.pThrown("set", UnsupportedOperationException.class, () -> out.set(0, "c"));
        });
    }

    private static void primitives() {
        Bulk.check("Stream.mapToIntLongAndDouble", () -> {
            Bulk.p("int", src(WORDS).mapToInt(String::length).sum());
            Bulk.p("long", src(WORDS).mapToLong(w -> w.length() * 10000000000L).sum());
            Bulk.p("double", src(WORDS).mapToDouble(w -> w.length() / 4.0).sum());
            Bulk.p("boxed", listed(src(WORDS).mapToInt(String::length).boxed()));
            Bulk.p("mapToObj", listed(src(WORDS).mapToInt(String::length).mapToObj(n -> "#" + n)));
        });
        Bulk.check("Stream.primitiveStreamsAreLazyToo", () -> {
            List<Integer> seen = new ArrayList<Integer>();
            Bulk.p("first", src(NUMBERS).peek(seen::add).mapToInt(n -> n).filter(n -> n > 5).findFirst().getAsInt());
            Bulk.p("seen", seen);
            Bulk.p("iterate", IntStream.iterate(1, n -> n * 2).limit(5).toArray());
        });
    }
}
