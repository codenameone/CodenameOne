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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds the stream shims against `java.util.stream`: every case runs the
/// same pipeline on the JDK's stream (`theirs`) and on the shim (`mine`).
///
/// The suite runs on the oldest JDK the module compiles on, so what the JDK
/// gained later -- `takeWhile`, `ofNullable`, `Stream.toList` -- has no JDK
/// side here and is held to what its documentation states.
public class StreamDifferentialTest {

    private static final List<String> WORDS = Collections.unmodifiableList(Arrays.asList(
            "pear", "apple", "fig", "banana", "apple", "kiwi", "cherry", "fig", "date"));
    private static final List<Integer> NUMBERS = Collections.unmodifiableList(Arrays.asList(
            5, 3, 8, 3, 1, 9, 2, 8, 7, 4, 6, 0));

    private static <T> java.util.stream.Stream<T> theirs(List<T> list) {
        return list.stream();
    }

    private static <T> Stream<T> mine(List<T> list) {
        return JdkCollections.stream(list);
    }

    private static <T> List<T> listed(java.util.stream.Stream<T> stream) {
        return stream.collect(java.util.stream.Collectors.<T>toList());
    }

    private static <T> List<T> listed(Stream<T> stream) {
        return stream.collect(Collectors.<T>toList());
    }

    private static List<Integer> boxed(int[] values) {
        List<Integer> out = new ArrayList<Integer>();
        for (int v : values) {
            out.add(v);
        }
        return out;
    }

    // ---- intermediate operations, and the order they keep ----

    @Test
    public void filterAndMapKeepEncounterOrder() {
        assertEquals(listed(theirs(WORDS).filter(w -> w.length() > 3).map(w -> w.toUpperCase() + w.length())),
                listed(mine(WORDS).filter(w -> w.length() > 3).map(w -> w.toUpperCase() + w.length())));
        assertEquals(listed(theirs(NUMBERS).map(n -> n * n).filter(n -> n % 2 == 0)),
                listed(mine(NUMBERS).map(n -> n * n).filter(n -> n % 2 == 0)));
    }

    @Test
    public void distinctKeepsTheFirstOfEachInOrder() {
        assertEquals(listed(theirs(WORDS).distinct()), listed(mine(WORDS).distinct()));
        assertEquals(listed(theirs(NUMBERS).distinct()), listed(mine(NUMBERS).distinct()));
        List<String> withNull = Arrays.asList("a", null, "b", null, "a");
        assertEquals(listed(theirs(withNull).distinct()), listed(mine(withNull).distinct()));
    }

    @Test
    public void sortedIsNaturalOrByComparatorAndStable() {
        assertEquals(listed(theirs(WORDS).sorted()), listed(mine(WORDS).sorted()));
        assertEquals(listed(theirs(NUMBERS).sorted(Collections.reverseOrder())),
                listed(mine(NUMBERS).sorted(Collections.reverseOrder())));
        // By length alone: words of one length must keep their order.
        Comparator<String> byLength = (a, b) -> a.length() - b.length();
        List<String> expected = listed(theirs(WORDS).sorted(byLength));
        assertEquals(expected, listed(mine(WORDS).sorted(byLength)));
        assertEquals(Arrays.asList("fig", "fig", "pear", "kiwi", "date", "apple", "apple", "banana", "cherry"),
                expected);
    }

    @Test
    public void limitAndSkipAtEveryBoundary() {
        long[] counts = {0, 1, 3, NUMBERS.size() - 1, NUMBERS.size(), NUMBERS.size() + 5, Long.MAX_VALUE};
        for (long n : counts) {
            assertEquals("limit " + n, listed(theirs(NUMBERS).limit(n)), listed(mine(NUMBERS).limit(n)));
            assertEquals("skip " + n, listed(theirs(NUMBERS).skip(n)), listed(mine(NUMBERS).skip(n)));
            assertEquals("skip 2 limit " + n, listed(theirs(NUMBERS).skip(2).limit(n)),
                    listed(mine(NUMBERS).skip(2).limit(n)));
            assertEquals("limit skip 2 " + n, listed(theirs(NUMBERS).limit(n).skip(2)),
                    listed(mine(NUMBERS).limit(n).skip(2)));
        }
    }

    @Test
    public void aNegativeLimitOrSkipIsRefused() {
        try {
            theirs(NUMBERS).limit(-1);
            fail();
        } catch (IllegalArgumentException expected) {
            // The JDK's answer.
        }
        try {
            mine(NUMBERS).limit(-1);
            fail();
        } catch (IllegalArgumentException expected) {
            // And the shim's.
        }
        try {
            theirs(NUMBERS).skip(-1);
            fail();
        } catch (IllegalArgumentException expected) {
            // The JDK's answer.
        }
        try {
            mine(NUMBERS).skip(-1);
            fail();
        } catch (IllegalArgumentException expected) {
            // And the shim's.
        }
    }

    @Test
    public void flatMapConcatenatesInOrderAndSkipsNull() {
        assertEquals(
                listed(theirs(WORDS).flatMap(w -> w.length() == 3 ? null
                        : Arrays.asList(w, w.substring(0, 1)).stream())),
                listed(mine(WORDS).flatMap(w -> w.length() == 3 ? null
                        : JdkCollections.stream(Arrays.asList(w, w.substring(0, 1))))));
        assertEquals(
                listed(theirs(NUMBERS).flatMap(n -> java.util.stream.Stream.of(n, -n)).limit(7)),
                listed(mine(NUMBERS).flatMap(n -> Stream.of(n, -n)).limit(7)));
        assertEquals(
                listed(theirs(NUMBERS).flatMap(n -> java.util.stream.Stream.<Integer>empty())),
                listed(mine(NUMBERS).flatMap(n -> Stream.<Integer>empty())));
    }

    @Test
    public void flatMapToThePrimitiveStreams() {
        assertArrayEquals(theirs(WORDS).flatMapToInt(w -> w.chars()).limit(20).toArray(),
                mine(WORDS).flatMapToInt(w -> JdkStrings.chars(w)).limit(20).toArray());
        assertArrayEquals(theirs(NUMBERS).flatMapToLong(n -> java.util.stream.LongStream.rangeClosed(1, n)).toArray(),
                mine(NUMBERS).flatMapToLong(n -> LongStream.rangeClosed(1, n)).toArray());
        assertArrayEquals(
                theirs(NUMBERS).flatMapToDouble(n -> java.util.stream.DoubleStream.of(n, n / 2.0)).toArray(),
                mine(NUMBERS).flatMapToDouble(n -> DoubleStream.of(n, n / 2.0)).toArray(), 0.0);
    }

    // ---- laziness and short-circuiting ----

    @Test
    public void nothingRunsUntilATerminalOperation() {
        List<Integer> seen = new ArrayList<Integer>();
        Stream<Integer> pending = mine(NUMBERS).peek(seen::add).filter(n -> n > 2).map(n -> n + 1);
        assertTrue(seen.isEmpty());
        assertEquals(Integer.valueOf(6), pending.findFirst().get());
        assertEquals(Collections.singletonList(5), seen);
    }

    @Test
    public void findFirstPullsOnlyWhatItNeeds() {
        List<String> theirSeen = new ArrayList<String>();
        List<String> mySeen = new ArrayList<String>();
        assertEquals(theirs(WORDS).peek(theirSeen::add).filter(w -> w.startsWith("b")).findFirst().get(),
                mine(WORDS).peek(mySeen::add).filter(w -> w.startsWith("b")).findFirst().get());
        assertEquals(theirSeen, mySeen);
        assertEquals(4, mySeen.size());
    }

    @Test
    public void limitStopsThePipelineAboveIt() {
        List<Integer> theirSeen = new ArrayList<Integer>();
        List<Integer> mySeen = new ArrayList<Integer>();
        assertEquals(listed(theirs(NUMBERS).peek(theirSeen::add).map(n -> n * 2).limit(3)),
                listed(mine(NUMBERS).peek(mySeen::add).map(n -> n * 2).limit(3)));
        assertEquals(theirSeen, mySeen);
        assertEquals(Arrays.asList(5, 3, 8), mySeen);
    }

    @Test
    public void theMatchesStopAtTheirAnswer() {
        List<Integer> theirSeen = new ArrayList<Integer>();
        List<Integer> mySeen = new ArrayList<Integer>();
        assertEquals(theirs(NUMBERS).peek(theirSeen::add).anyMatch(n -> n == 8),
                mine(NUMBERS).peek(mySeen::add).anyMatch(n -> n == 8));
        assertEquals(theirSeen, mySeen);
        theirSeen.clear();
        mySeen.clear();
        assertEquals(theirs(NUMBERS).peek(theirSeen::add).allMatch(n -> n != 1),
                mine(NUMBERS).peek(mySeen::add).allMatch(n -> n != 1));
        assertEquals(theirSeen, mySeen);
        theirSeen.clear();
        mySeen.clear();
        assertEquals(theirs(NUMBERS).peek(theirSeen::add).noneMatch(n -> n == 9),
                mine(NUMBERS).peek(mySeen::add).noneMatch(n -> n == 9));
        assertEquals(theirSeen, mySeen);
        assertEquals(6, mySeen.size());
    }

    @Test
    public void theMatchesOfAnEmptyStream() {
        List<Integer> none = Collections.<Integer>emptyList();
        assertEquals(theirs(none).anyMatch(n -> true), mine(none).anyMatch(n -> true));
        assertEquals(theirs(none).allMatch(n -> false), mine(none).allMatch(n -> false));
        assertEquals(theirs(none).noneMatch(n -> true), mine(none).noneMatch(n -> true));
        assertTrue(mine(none).allMatch(n -> false));
    }

    @Test
    public void anInfiniteStreamIsCutByLimit() {
        assertEquals(listed(java.util.stream.Stream.iterate(1, n -> n * 2).limit(12)),
                listed(Stream.iterate(1, n -> n * 2).limit(12)));
        final int[] theirNext = {0};
        final int[] myNext = {0};
        assertEquals(listed(java.util.stream.Stream.generate(() -> theirNext[0]++).filter(n -> n % 3 == 0).limit(5)),
                listed(Stream.generate(() -> myNext[0]++).filter(n -> n % 3 == 0).limit(5)));
        assertEquals(theirNext[0], myNext[0]);
        assertTrue(Stream.iterate(1, n -> n + 1).anyMatch(n -> n == 1000));
    }

    @Test
    public void flatMapIsLazyInsideItsInnerStream() {
        // The JDK this suite runs on pushed a whole inner stream before it
        // looked at the limit; the later ones stop, and so does the shim.
        List<Integer> seen = new ArrayList<Integer>();
        assertEquals(Integer.valueOf(1),
                mine(NUMBERS).flatMap(n -> Stream.iterate(1, i -> i + 1).peek(seen::add)).findFirst().get());
        assertEquals(Collections.singletonList(1), seen);
    }

    /// A mapped stream is closed whether or not all of it was wanted. The
    /// JDK this suite runs on read each one to its end first and the later
    /// ones stop part way, and both close it; so does the shim, which stops
    /// part way.
    @Test
    public void aMappedStreamLeftPartReadIsClosed() {
        List<Integer> source = Arrays.asList(1, 2, 3);
        List<String> theirClosed = new ArrayList<String>();
        List<String> myClosed = new ArrayList<String>();
        assertEquals(theirs(source).flatMap(n -> java.util.stream.Stream.of(n, -n)
                        .onClose(() -> theirClosed.add("first " + n))).findFirst().get(),
                mine(source).flatMap(n -> Stream.of(n, -n).onClose(() -> myClosed.add("first " + n)))
                        .findFirst().get());
        assertEquals(listed(theirs(source).flatMap(n -> java.util.stream.Stream.of(n, -n)
                        .onClose(() -> theirClosed.add("limit " + n))).limit(3)),
                listed(mine(source).flatMap(n -> Stream.of(n, -n).onClose(() -> myClosed.add("limit " + n)))
                        .limit(3)));
        assertEquals(theirs(source).flatMap(n -> java.util.stream.Stream.of(n, -n)
                        .onClose(() -> theirClosed.add("any " + n))).anyMatch(v -> v == 2),
                mine(source).flatMap(n -> Stream.of(n, -n).onClose(() -> myClosed.add("any " + n)))
                        .anyMatch(v -> v == 2));
        assertEquals(theirs(source).flatMap(n -> java.util.stream.Stream.of(n, -n)
                        .onClose(() -> theirClosed.add("all " + n))).allMatch(v -> v > 0),
                mine(source).flatMap(n -> Stream.of(n, -n).onClose(() -> myClosed.add("all " + n)))
                        .allMatch(v -> v > 0));
        assertEquals(theirs(source).flatMap(n -> java.util.stream.Stream.of(n, -n)
                        .onClose(() -> theirClosed.add("none " + n))).noneMatch(v -> v == 1),
                mine(source).flatMap(n -> Stream.of(n, -n).onClose(() -> myClosed.add("none " + n)))
                        .noneMatch(v -> v == 1));
        assertEquals(theirs(source).flatMapToInt(n -> java.util.stream.IntStream.of(n, -n)
                        .onClose(() -> theirClosed.add("int " + n))).findFirst().getAsInt(),
                mine(source).flatMapToInt(n -> IntStream.of(n, -n).onClose(() -> myClosed.add("int " + n)))
                        .findFirst().getAsInt());
        assertEquals(theirClosed, myClosed);
        assertEquals(Arrays.asList("first 1", "limit 1", "limit 2", "any 1", "any 2", "all 1", "none 1", "int 1"),
                myClosed);

        // What the JDK of this suite does not have, or reads differently.
        myClosed.clear();
        assertEquals(Arrays.asList(1, -1, 2), listed(mine(source)
                .flatMap(n -> Stream.of(n, -n).onClose(() -> myClosed.add("while " + n))).takeWhile(v -> v != -2)));
        assertEquals(Arrays.asList("while 1", "while 2"), myClosed);

        // A limit says so about the stages before it only: the mapped
        // stream after it is still being read when the limit runs out.
        myClosed.clear();
        List<String> order = new ArrayList<String>();
        mine(source).limit(1).flatMap(n -> Stream.of(n, -n).onClose(() -> order.add("closed")))
                .forEach(v -> order.add(String.valueOf(v)));
        assertEquals(Arrays.asList("1", "-1", "closed"), order);

        // An iterator nobody reads to the end is the caller's to close.
        Stream<Integer> open = mine(source).flatMap(n -> Stream.of(n, -n).onClose(() -> myClosed.add("it " + n)));
        Iterator<Integer> it = open.iterator();
        assertEquals(Integer.valueOf(1), it.next());
        assertTrue(myClosed.isEmpty());
        open.close();
        open.close();
        assertEquals(Collections.singletonList("it 1"), myClosed);
    }

    // ---- terminal operations ----

    @Test
    public void reduceInItsThreeForms() {
        assertEquals(theirs(NUMBERS).reduce(100, (a, b) -> a - b), mine(NUMBERS).reduce(100, (a, b) -> a - b));
        assertEquals(theirs(WORDS).reduce((a, b) -> a + "," + b).get(), mine(WORDS).reduce((a, b) -> a + "," + b).get());
        assertEquals(theirs(WORDS).reduce(0, (n, w) -> n + w.length(), (a, b) -> a + b),
                mine(WORDS).reduce(0, (n, w) -> n + w.length(), (a, b) -> a + b));
        List<String> none = Collections.<String>emptyList();
        assertEquals(theirs(none).reduce((a, b) -> a + b).isPresent(), mine(none).reduce((a, b) -> a + b).isPresent());
        assertEquals(theirs(none).reduce("seed", (a, b) -> a + b), mine(none).reduce("seed", (a, b) -> a + b));
    }

    @Test
    public void minMaxAndCount() {
        Comparator<String> natural = Comparator.naturalOrder();
        Comparator<String> byLength = (a, b) -> a.length() - b.length();
        assertEquals(theirs(WORDS).min(natural).get(), mine(WORDS).min(natural).get());
        assertEquals(theirs(WORDS).max(natural).get(), mine(WORDS).max(natural).get());
        // Ties: which of the equal ones is answered.
        assertSame(theirs(WORDS).min(byLength).get(), mine(WORDS).min(byLength).get());
        assertSame(theirs(WORDS).max(byLength).get(), mine(WORDS).max(byLength).get());
        assertEquals(theirs(WORDS).count(), mine(WORDS).count());
        assertEquals(theirs(WORDS).filter(w -> w.length() == 3).count(),
                mine(WORDS).filter(w -> w.length() == 3).count());
        List<String> none = Collections.<String>emptyList();
        assertEquals(theirs(none).min(natural).isPresent(), mine(none).min(natural).isPresent());
        assertEquals(theirs(none).max(natural).isPresent(), mine(none).max(natural).isPresent());
        assertEquals(0L, mine(none).count());
    }

    @Test
    public void findFirstAndFindAny() {
        assertEquals(theirs(WORDS).findFirst().get(), mine(WORDS).findFirst().get());
        assertEquals(theirs(WORDS).skip(3).findAny().get(), mine(WORDS).skip(3).findAny().get());
        assertFalse(mine(WORDS).filter(w -> w.isEmpty()).findFirst().isPresent());
        assertFalse(theirs(WORDS).filter(w -> w.isEmpty()).findFirst().isPresent());
    }

    @Test
    public void findFirstRefusesANullElement() {
        List<String> withNull = Arrays.asList(null, "a");
        try {
            theirs(withNull).findFirst();
            fail();
        } catch (NullPointerException expected) {
            // The JDK's answer.
        }
        try {
            mine(withNull).findFirst();
            fail();
        } catch (NullPointerException expected) {
            // And the shim's.
        }
    }

    @Test
    public void toArrayPlainAndTyped() {
        assertArrayEquals(theirs(WORDS).sorted().toArray(), mine(WORDS).sorted().toArray());
        String[] typed = mine(WORDS).filter(w -> w.length() > 4).toArray(String[]::new);
        assertArrayEquals(theirs(WORDS).filter(w -> w.length() > 4).toArray(String[]::new), typed);
        assertSame(String[].class, typed.getClass());
        assertEquals(0, mine(Collections.<String>emptyList()).toArray(String[]::new).length);
    }

    @Test
    public void forEachAndForEachOrderedVisitInOrder() {
        List<String> theirSeen = new ArrayList<String>();
        List<String> mySeen = new ArrayList<String>();
        theirs(WORDS).filter(w -> w.length() != 4).forEach(theirSeen::add);
        mine(WORDS).filter(w -> w.length() != 4).forEach(mySeen::add);
        assertEquals(theirSeen, mySeen);
        mySeen.clear();
        mine(WORDS).filter(w -> w.length() != 4).forEachOrdered(mySeen::add);
        assertEquals(theirSeen, mySeen);
    }

    @Test
    public void collectWithASupplierAndAccumulator() {
        assertEquals(
                theirs(WORDS).collect(StringBuilder::new, (sb, w) -> sb.append(w.charAt(0)),
                        (a, b) -> a.append(b)).toString(),
                mine(WORDS).collect(StringBuilder::new, (sb, w) -> sb.append(w.charAt(0)),
                        (a, b) -> a.append(b)).toString());
    }

    @Test
    public void theIteratorWalksThePipeline() {
        Iterator<String> theirIt = theirs(WORDS).filter(w -> w.length() > 4).map(String::toUpperCase).iterator();
        Iterator<String> myIt = mine(WORDS).filter(w -> w.length() > 4).map(String::toUpperCase).iterator();
        while (theirIt.hasNext()) {
            assertTrue(myIt.hasNext());
            assertEquals(theirIt.next(), myIt.next());
        }
        assertFalse(myIt.hasNext());
        try {
            myIt.next();
            fail();
        } catch (NoSuchElementException expected) {
            // As the JDK's does.
        }
    }

    // ---- the life of a stream ----

    @Test
    public void aStreamRunsOnce() {
        java.util.stream.Stream<String> theirStream = theirs(WORDS);
        theirStream.count();
        try {
            theirStream.count();
            fail();
        } catch (IllegalStateException expected) {
            // The JDK's answer.
        }
        Stream<String> myStream = mine(WORDS);
        myStream.count();
        try {
            myStream.count();
            fail();
        } catch (IllegalStateException expected) {
            // And the shim's.
        }
    }

    @Test
    public void aStageTakesOneOperation() {
        java.util.stream.Stream<String> theirStream = theirs(WORDS);
        theirStream.filter(w -> true);
        try {
            theirStream.map(w -> w);
            fail();
        } catch (IllegalStateException expected) {
            // The JDK's answer.
        }
        Stream<String> myStream = mine(WORDS);
        myStream.filter(w -> true);
        try {
            myStream.map(w -> w);
            fail();
        } catch (IllegalStateException expected) {
            // And the shim's.
        }
    }

    @Test
    public void theSourceIsReadWhenTheTerminalOperationStarts() {
        List<String> theirSource = new ArrayList<String>(Arrays.asList("a", "b"));
        java.util.stream.Stream<String> theirStream = theirSource.stream().map(String::toUpperCase);
        theirSource.add("c");
        List<String> mySource = new ArrayList<String>(Arrays.asList("a", "b"));
        Stream<String> myStream = JdkCollections.stream(mySource).map(String::toUpperCase);
        mySource.add("c");
        assertEquals(listed(theirStream), listed(myStream));
    }

    @Test
    public void closeRunsTheHandlersOnceInOrder() {
        List<String> theirRan = new ArrayList<String>();
        List<String> myRan = new ArrayList<String>();
        java.util.stream.Stream<String> theirStream = theirs(WORDS).onClose(() -> theirRan.add("one"))
                .filter(w -> true).onClose(() -> theirRan.add("two"));
        theirStream.close();
        theirStream.close();
        Stream<String> myStream = mine(WORDS).onClose(() -> myRan.add("one"))
                .filter(w -> true).onClose(() -> myRan.add("two"));
        myStream.close();
        myStream.close();
        assertEquals(theirRan, myRan);
        assertEquals(Arrays.asList("one", "two"), myRan);
    }

    @Test
    public void sequentialParallelAndUnordered() {
        assertEquals(theirs(WORDS).sequential().isParallel(), mine(WORDS).sequential().isParallel());
        // The shim has one thread to run on, so a parallel stream is the
        // sequential one and says so.
        assertFalse(mine(WORDS).parallel().isParallel());
        assertEquals(listed(theirs(WORDS).parallel().map(String::length)),
                listed(mine(WORDS).parallel().map(String::length)));
        assertEquals(listed(theirs(WORDS).unordered().sorted()), listed(mine(WORDS).unordered().sorted()));
    }

    @Test
    public void aNullFunctionIsRefusedAtOnce() {
        try {
            mine(WORDS).map(null);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK does.
        }
        try {
            mine(WORDS).filter(null);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK does.
        }
        try {
            mine(WORDS).sorted().forEach(null);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK does.
        }
    }

    // ---- the factories ----

    @Test
    public void ofEmptyConcatAndBuilder() {
        assertEquals(listed(java.util.stream.Stream.of("a", "b", "c")), listed(Stream.of("a", "b", "c")));
        assertEquals(listed(java.util.stream.Stream.of("solo")), listed(Stream.of("solo")));
        assertEquals(listed(java.util.stream.Stream.<String>empty()), listed(Stream.<String>empty()));
        assertEquals(
                listed(java.util.stream.Stream.concat(theirs(WORDS).limit(2), theirs(WORDS).skip(7))),
                listed(Stream.concat(mine(WORDS).limit(2), mine(WORDS).skip(7))));
        assertEquals(
                listed(java.util.stream.Stream.<String>builder().add("x").add("y").build()),
                listed(Stream.<String>builder().add("x").add("y").build()));
        Stream.Builder<String> built = Stream.builder();
        built.accept("z");
        built.build();
        try {
            built.add("late");
            fail();
        } catch (IllegalStateException expected) {
            // A built builder is finished, as the JDK's is.
        }
    }

    @Test
    public void concatIsLazyAndClosesBoth() {
        List<String> seen = new ArrayList<String>();
        List<String> closed = new ArrayList<String>();
        Stream<String> both = Stream.concat(mine(WORDS).peek(seen::add).onClose(() -> closed.add("a")),
                mine(WORDS).peek(seen::add).onClose(() -> closed.add("b")));
        assertTrue(seen.isEmpty());
        assertEquals("pear", both.findFirst().get());
        assertEquals(1, seen.size());
        both.close();
        assertEquals(Arrays.asList("a", "b"), closed);
    }

    // ---- what the JDK gained after the one this suite runs on ----

    @Test
    public void takeWhileAndDropWhile() {
        assertEquals(Arrays.asList(5, 3), listed(mine(NUMBERS).takeWhile(n -> n < 8)));
        assertEquals(Arrays.asList(8, 3, 1, 9, 2, 8, 7, 4, 6, 0), listed(mine(NUMBERS).dropWhile(n -> n < 8)));
        assertEquals(NUMBERS, listed(mine(NUMBERS).takeWhile(n -> true)));
        assertEquals(NUMBERS, listed(mine(NUMBERS).dropWhile(n -> false)));
        assertTrue(listed(mine(NUMBERS).takeWhile(n -> false)).isEmpty());
        assertTrue(listed(mine(NUMBERS).dropWhile(n -> true)).isEmpty());
        // takeWhile stops asking: an infinite stream ends.
        assertEquals(Arrays.asList(1, 2, 3, 4), listed(Stream.iterate(1, n -> n + 1).takeWhile(n -> n < 5)));
        // dropWhile stops testing after the first that fails.
        List<Integer> tested = new ArrayList<Integer>();
        listed(mine(NUMBERS).dropWhile(n -> {
            tested.add(n);
            return n != 8;
        }));
        assertEquals(Arrays.asList(5, 3, 8), tested);
    }

    @Test
    public void ofNullableAndTheBoundedIterate() {
        assertEquals(Collections.singletonList("a"), listed(Stream.ofNullable("a")));
        assertTrue(listed(Stream.<String>ofNullable(null)).isEmpty());
        assertEquals(Arrays.asList(1, 3, 9, 27), listed(Stream.iterate(1, n -> n < 50, n -> n * 3)));
        assertTrue(listed(Stream.iterate(1, n -> n < 0, n -> n * 3)).isEmpty());
    }

    @Test
    public void toListIsUnmodifiableAndTakesNull() {
        List<String> out = mine(Arrays.asList("a", null, "b")).toList();
        assertEquals(Arrays.asList("a", null, "b"), out);
        try {
            out.add("c");
            fail();
        } catch (UnsupportedOperationException expected) {
            // As documented.
        }
        try {
            out.set(0, "c");
            fail();
        } catch (UnsupportedOperationException expected) {
            // As documented.
        }
    }

    // ---- across to the primitive streams and back ----

    @Test
    public void mapToIntLongAndDouble() {
        assertEquals(theirs(WORDS).mapToInt(String::length).sum(), mine(WORDS).mapToInt(String::length).sum());
        assertEquals(theirs(WORDS).mapToLong(w -> w.length() * 10000000000L).sum(),
                mine(WORDS).mapToLong(w -> w.length() * 10000000000L).sum());
        assertEquals(theirs(WORDS).mapToDouble(w -> w.length() / 4.0).sum(),
                mine(WORDS).mapToDouble(w -> w.length() / 4.0).sum(), 0.0);
        assertEquals(listed(theirs(WORDS).mapToInt(String::length).boxed()),
                listed(mine(WORDS).mapToInt(String::length).boxed()));
        assertEquals(listed(theirs(WORDS).mapToInt(String::length).mapToObj(n -> "#" + n)),
                listed(mine(WORDS).mapToInt(String::length).mapToObj(n -> "#" + n)));
    }

    @Test
    public void primitiveStreamsAreLazyToo() {
        List<Integer> seen = new ArrayList<Integer>();
        assertEquals(8, mine(NUMBERS).peek(seen::add).mapToInt(n -> n).filter(n -> n > 5).findFirst().getAsInt());
        assertEquals(Arrays.asList(5, 3, 8), seen);
        assertEquals(boxed(new int[] {1, 2, 4, 8, 16}),
                boxed(IntStream.iterate(1, n -> n * 2).limit(5).toArray()));
    }
}
