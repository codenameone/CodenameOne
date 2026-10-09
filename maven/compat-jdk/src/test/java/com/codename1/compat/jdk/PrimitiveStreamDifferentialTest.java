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
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds `IntStream`, `LongStream`, `DoubleStream`, their optionals and
/// their statistics against the JDK's.
public class PrimitiveStreamDifferentialTest {

    private static final int[] INTS = {5, 3, 8, 3, 1, 9, 2, 8, 7, 4, 6, 0, -4};
    private static final long[] LONGS = {5000000000L, 3, -8000000000L, 3, 1, 9, 2, 5000000000L, 7};
    private static final double[] DOUBLES = {0.1, 2.5, -3.75, 0.1, 1e10, 0.2, 0.3, -1e10, 7.0};

    private static java.util.stream.IntStream theirs() {
        return Arrays.stream(INTS);
    }

    private static IntStream mine() {
        return JdkCollections.stream(INTS);
    }

    private static void same(java.util.OptionalInt theirs, OptionalInt mine) {
        assertEquals(theirs.isPresent(), mine.isPresent());
        assertEquals(theirs.toString(), mine.toString());
        if (theirs.isPresent()) {
            assertEquals(theirs.getAsInt(), mine.getAsInt());
        }
        assertEquals(theirs.orElse(-77), mine.orElse(-77));
    }

    private static void same(java.util.OptionalLong theirs, OptionalLong mine) {
        assertEquals(theirs.isPresent(), mine.isPresent());
        assertEquals(theirs.toString(), mine.toString());
        assertEquals(theirs.orElse(-77L), mine.orElse(-77L));
    }

    private static void same(java.util.OptionalDouble theirs, OptionalDouble mine) {
        assertEquals(theirs.isPresent(), mine.isPresent());
        assertEquals(theirs.toString(), mine.toString());
        assertEquals(theirs.orElse(-77.5), mine.orElse(-77.5), 0.0);
    }

    // ---- ranges ----

    @Test
    public void intRangeAtEveryBoundary() {
        int max = Integer.MAX_VALUE;
        int min = Integer.MIN_VALUE;
        int[][] bounds = {{0, 0}, {0, 1}, {0, 5}, {5, 2}, {-3, 3}, {5, 5}, {5, 4}, {max - 2, max}, {max, max},
            {min, min + 2}, {min, min}, {max, min}, {-1, 0}};
        for (int[] b : bounds) {
            String at = b[0] + ".." + b[1];
            assertArrayEquals(at, java.util.stream.IntStream.range(b[0], b[1]).toArray(),
                    IntStream.range(b[0], b[1]).toArray());
            assertArrayEquals(at, java.util.stream.IntStream.rangeClosed(b[0], b[1]).toArray(),
                    IntStream.rangeClosed(b[0], b[1]).toArray());
        }
        // The widest ranges, counted without being walked to the end.
        assertEquals(java.util.stream.IntStream.rangeClosed(min, max).limit(3).sum(),
                IntStream.rangeClosed(min, max).limit(3).sum());
        assertEquals(java.util.stream.IntStream.range(min, max).skip(4).findFirst().getAsInt(),
                IntStream.range(min, max).skip(4).findFirst().getAsInt());
    }

    @Test
    public void longRangeAtEveryBoundary() {
        long max = Long.MAX_VALUE;
        long min = Long.MIN_VALUE;
        long[][] bounds = {{0, 0}, {0, 1}, {0, 5}, {5, 2}, {-3, 3}, {5, 5}, {5, 4}, {max - 2, max}, {max, max},
            {min, min + 2}, {min, min}, {max, min}, {4000000000L, 4000000003L}};
        for (long[] b : bounds) {
            String at = b[0] + ".." + b[1];
            assertArrayEquals(at, java.util.stream.LongStream.range(b[0], b[1]).toArray(),
                    LongStream.range(b[0], b[1]).toArray());
            assertArrayEquals(at, java.util.stream.LongStream.rangeClosed(b[0], b[1]).toArray(),
                    LongStream.rangeClosed(b[0], b[1]).toArray());
        }
        assertEquals(java.util.stream.LongStream.rangeClosed(min, max).limit(3).sum(),
                LongStream.rangeClosed(min, max).limit(3).sum());
    }

    // ---- IntStream ----

    @Test
    public void intIntermediateOperations() {
        assertArrayEquals(theirs().filter(n -> n % 2 == 1).map(n -> n * 10).toArray(),
                mine().filter(n -> n % 2 == 1).map(n -> n * 10).toArray());
        assertArrayEquals(theirs().distinct().toArray(), mine().distinct().toArray());
        assertArrayEquals(theirs().sorted().toArray(), mine().sorted().toArray());
        assertArrayEquals(theirs().skip(2).limit(4).toArray(), mine().skip(2).limit(4).toArray());
        assertArrayEquals(theirs().flatMap(n -> java.util.stream.IntStream.range(0, n)).toArray(),
                mine().flatMap(n -> IntStream.range(0, n)).toArray());
        assertArrayEquals(theirs().flatMap(n -> n > 5 ? null : java.util.stream.IntStream.of(n)).toArray(),
                mine().flatMap(n -> n > 5 ? null : IntStream.of(n)).toArray());
        List<Integer> theirSeen = new ArrayList<Integer>();
        List<Integer> mySeen = new ArrayList<Integer>();
        assertArrayEquals(theirs().peek(theirSeen::add).limit(3).toArray(),
                mine().peek(mySeen::add).limit(3).toArray());
        assertEquals(theirSeen, mySeen);
    }

    @Test
    public void intTerminalOperations() {
        assertEquals(theirs().sum(), mine().sum());
        assertEquals(theirs().count(), mine().count());
        same(theirs().min(), mine().min());
        same(theirs().max(), mine().max());
        same(theirs().average(), mine().average());
        same(theirs().findFirst(), mine().findFirst());
        same(theirs().skip(4).findAny(), mine().skip(4).findAny());
        same(theirs().reduce((a, b) -> a * 3 + b), mine().reduce((a, b) -> a * 3 + b));
        assertEquals(theirs().reduce(7, (a, b) -> a ^ b), mine().reduce(7, (a, b) -> a ^ b));
        assertEquals(theirs().anyMatch(n -> n == 9), mine().anyMatch(n -> n == 9));
        assertEquals(theirs().allMatch(n -> n >= 0), mine().allMatch(n -> n >= 0));
        assertEquals(theirs().noneMatch(n -> n > 100), mine().noneMatch(n -> n > 100));
        assertEquals(
                theirs().collect(StringBuilder::new, (sb, n) -> sb.append(n).append(';'),
                        (a, b) -> a.append(b)).toString(),
                mine().collect(StringBuilder::new, (sb, n) -> sb.append(n).append(';'),
                        (a, b) -> a.append(b)).toString());
        List<Integer> theirSeen = new ArrayList<Integer>();
        List<Integer> mySeen = new ArrayList<Integer>();
        theirs().forEach(theirSeen::add);
        mine().forEachOrdered(mySeen::add);
        assertEquals(theirSeen, mySeen);
    }

    @Test
    public void intSumWrapsAsTheJdkDoes() {
        int[] big = {Integer.MAX_VALUE, Integer.MAX_VALUE, 5};
        assertEquals(Arrays.stream(big).sum(), JdkCollections.stream(big).sum());
        // The average does not wrap: it is summed as a long.
        same(Arrays.stream(big).average(), JdkCollections.stream(big).average());
    }

    @Test
    public void anEmptyIntStreamHasNoMinMaxOrAverage() {
        same(java.util.stream.IntStream.empty().min(), IntStream.empty().min());
        same(java.util.stream.IntStream.empty().max(), IntStream.empty().max());
        same(java.util.stream.IntStream.empty().average(), IntStream.empty().average());
        same(java.util.stream.IntStream.empty().findFirst(), IntStream.empty().findFirst());
        same(java.util.stream.IntStream.empty().reduce((a, b) -> a + b), IntStream.empty().reduce((a, b) -> a + b));
        assertEquals(java.util.stream.IntStream.empty().sum(), IntStream.empty().sum());
        assertFalse(IntStream.empty().average().isPresent());
        try {
            IntStream.empty().max().getAsInt();
            fail();
        } catch (NoSuchElementException expected) {
            // As the JDK's optional answers.
        }
        try {
            IntStream.empty().average().getAsDouble();
            fail();
        } catch (NoSuchElementException expected) {
            // As the JDK's optional answers.
        }
    }

    @Test
    public void intConversions() {
        assertArrayEquals(theirs().asLongStream().map(n -> n * 3000000000L).toArray(),
                mine().asLongStream().map(n -> n * 3000000000L).toArray());
        assertArrayEquals(theirs().asDoubleStream().map(n -> n / 4.0).toArray(),
                mine().asDoubleStream().map(n -> n / 4.0).toArray(), 0.0);
        assertArrayEquals(theirs().mapToLong(n -> n * 3000000000L).toArray(),
                mine().mapToLong(n -> n * 3000000000L).toArray());
        assertArrayEquals(theirs().mapToDouble(n -> n / 8.0).toArray(),
                mine().mapToDouble(n -> n / 8.0).toArray(), 0.0);
        assertEquals(theirs().mapToObj(Integer::toHexString).collect(java.util.stream.Collectors.joining("/")),
                mine().mapToObj(Integer::toHexString).collect(Collectors.joining("/")));
        assertEquals(theirs().boxed().collect(java.util.stream.Collectors.toList()),
                mine().boxed().collect(Collectors.toList()));
    }

    @Test
    public void intFactories() {
        assertArrayEquals(java.util.stream.IntStream.of(4).toArray(), IntStream.of(4).toArray());
        assertArrayEquals(java.util.stream.IntStream.of(4, 5, 6).toArray(), IntStream.of(4, 5, 6).toArray());
        assertArrayEquals(java.util.stream.IntStream.iterate(1, n -> n * 3).limit(6).toArray(),
                IntStream.iterate(1, n -> n * 3).limit(6).toArray());
        final int[] theirNext = {0};
        final int[] myNext = {0};
        assertArrayEquals(java.util.stream.IntStream.generate(() -> theirNext[0]++).limit(4).toArray(),
                IntStream.generate(() -> myNext[0]++).limit(4).toArray());
        assertArrayEquals(
                java.util.stream.IntStream.concat(java.util.stream.IntStream.range(0, 3), theirs().limit(2)).toArray(),
                IntStream.concat(IntStream.range(0, 3), mine().limit(2)).toArray());
        // Later JDKs.
        assertArrayEquals(new int[] {1, 3, 9, 27}, IntStream.iterate(1, n -> n < 50, n -> n * 3).toArray());
        assertArrayEquals(new int[] {5, 3}, mine().takeWhile(n -> n < 8).toArray());
        assertArrayEquals(new int[] {0, -4}, mine().dropWhile(n -> n != 0).toArray());
    }

    @Test
    public void arraysStreamRanges() {
        assertArrayEquals(Arrays.stream(INTS, 2, 6).toArray(), JdkCollections.stream(INTS, 2, 6).toArray());
        assertArrayEquals(Arrays.stream(INTS, 4, 4).toArray(), JdkCollections.stream(INTS, 4, 4).toArray());
        assertArrayEquals(Arrays.stream(INTS, 0, INTS.length).toArray(),
                JdkCollections.stream(INTS, 0, INTS.length).toArray());
        assertArrayEquals(Arrays.stream(LONGS, 1, 5).toArray(), JdkCollections.stream(LONGS, 1, 5).toArray());
        assertArrayEquals(Arrays.stream(DOUBLES, 1, 5).toArray(), JdkCollections.stream(DOUBLES, 1, 5).toArray(), 0.0);
        String[] words = {"a", "b", "c", "d"};
        assertArrayEquals(Arrays.stream(words, 1, 3).toArray(), JdkCollections.stream(words, 1, 3).toArray());
        assertArrayEquals(Arrays.stream(words).toArray(), JdkCollections.stream(words).toArray());
        int[][] bad = {{-1, 2}, {3, 2}, {0, INTS.length + 1}};
        for (int[] b : bad) {
            try {
                Arrays.stream(INTS, b[0], b[1]);
                fail();
            } catch (ArrayIndexOutOfBoundsException expected) {
                // The JDK's answer.
            }
            try {
                JdkCollections.stream(INTS, b[0], b[1]);
                fail();
            } catch (ArrayIndexOutOfBoundsException expected) {
                // And the shim's.
            }
            try {
                JdkCollections.stream(words, b[0], b[1] > 4 ? 5 : b[1]);
                fail();
            } catch (ArrayIndexOutOfBoundsException expected) {
                // And for an object array.
            }
        }
    }

    @Test
    public void thePrimitiveIterators() {
        java.util.PrimitiveIterator.OfInt theirIt = theirs().filter(n -> n > 4).iterator();
        PrimitiveIterator.OfInt myIt = mine().filter(n -> n > 4).iterator();
        while (theirIt.hasNext()) {
            assertTrue(myIt.hasNext());
            assertEquals(theirIt.nextInt(), myIt.nextInt());
        }
        assertFalse(myIt.hasNext());
        try {
            myIt.nextInt();
            fail();
        } catch (NoSuchElementException expected) {
            // As the JDK's does.
        }
        PrimitiveIterator.OfLong longs = LongStream.of(4L, 5L).iterator();
        assertEquals(4L, longs.nextLong());
        assertEquals(Long.valueOf(5L), longs.next());
        PrimitiveIterator.OfDouble doubles = DoubleStream.of(0.5).iterator();
        assertEquals(0.5, doubles.nextDouble(), 0.0);
        assertFalse(doubles.hasNext());
    }

    @Test
    public void aPrimitiveStreamRunsOnce() {
        IntStream stream = mine();
        stream.sum();
        try {
            stream.sum();
            fail();
        } catch (IllegalStateException expected) {
            // As the JDK's does.
        }
        IntStream staged = mine();
        staged.map(n -> n);
        try {
            staged.filter(n -> true);
            fail();
        } catch (IllegalStateException expected) {
            // As the JDK's does.
        }
    }

    // ---- LongStream ----

    @Test
    public void longOperations() {
        assertArrayEquals(Arrays.stream(LONGS).filter(n -> n > 2).map(n -> n * 2).sorted().distinct().toArray(),
                JdkCollections.stream(LONGS).filter(n -> n > 2).map(n -> n * 2).sorted().distinct().toArray());
        assertEquals(Arrays.stream(LONGS).sum(), JdkCollections.stream(LONGS).sum());
        assertEquals(Arrays.stream(LONGS).count(), JdkCollections.stream(LONGS).count());
        same(Arrays.stream(LONGS).min(), JdkCollections.stream(LONGS).min());
        same(Arrays.stream(LONGS).max(), JdkCollections.stream(LONGS).max());
        same(Arrays.stream(LONGS).average(), JdkCollections.stream(LONGS).average());
        same(Arrays.stream(LONGS).reduce((a, b) -> a - b), JdkCollections.stream(LONGS).reduce((a, b) -> a - b));
        same(Arrays.stream(LONGS).skip(1).limit(2).findFirst(), JdkCollections.stream(LONGS).skip(1).limit(2).findFirst());
        assertEquals(Arrays.stream(LONGS).reduce(1L, (a, b) -> a + b), JdkCollections.stream(LONGS).reduce(1L, (a, b) -> a + b));
        assertArrayEquals(Arrays.stream(LONGS).mapToInt(n -> (int) (n % 7)).toArray(),
                JdkCollections.stream(LONGS).mapToInt(n -> (int) (n % 7)).toArray());
        assertArrayEquals(Arrays.stream(LONGS).asDoubleStream().toArray(),
                JdkCollections.stream(LONGS).asDoubleStream().toArray(), 0.0);
        assertArrayEquals(Arrays.stream(LONGS).mapToDouble(n -> n / 2.0).toArray(),
                JdkCollections.stream(LONGS).mapToDouble(n -> n / 2.0).toArray(), 0.0);
        assertEquals(Arrays.stream(LONGS).boxed().collect(java.util.stream.Collectors.toList()),
                JdkCollections.stream(LONGS).boxed().collect(Collectors.toList()));
        assertEquals(Arrays.stream(LONGS).mapToObj(Long::toString).collect(java.util.stream.Collectors.joining()),
                JdkCollections.stream(LONGS).mapToObj(Long::toString).collect(Collectors.joining()));
        assertArrayEquals(
                Arrays.stream(LONGS).flatMap(n -> java.util.stream.LongStream.of(n, n)).limit(5).toArray(),
                JdkCollections.stream(LONGS).flatMap(n -> LongStream.of(n, n)).limit(5).toArray());
        assertEquals(Arrays.stream(LONGS).anyMatch(n -> n < 0), JdkCollections.stream(LONGS).anyMatch(n -> n < 0));
        assertEquals(Arrays.stream(LONGS).allMatch(n -> n < 0), JdkCollections.stream(LONGS).allMatch(n -> n < 0));
        assertEquals(Arrays.stream(LONGS).noneMatch(n -> n == 0), JdkCollections.stream(LONGS).noneMatch(n -> n == 0));
        same(java.util.stream.LongStream.empty().min(), LongStream.empty().min());
        same(java.util.stream.LongStream.empty().average(), LongStream.empty().average());
        assertArrayEquals(java.util.stream.LongStream.iterate(1, n -> n * 5).limit(20).toArray(),
                LongStream.iterate(1, n -> n * 5).limit(20).toArray());
        assertArrayEquals(
                java.util.stream.LongStream.concat(java.util.stream.LongStream.of(1), java.util.stream.LongStream.of(2, 3))
                        .toArray(),
                LongStream.concat(LongStream.of(1), LongStream.of(2, 3)).toArray());
    }

    // ---- DoubleStream ----

    @Test
    public void doubleOperations() {
        assertArrayEquals(Arrays.stream(DOUBLES).filter(d -> d > 0).map(d -> d * 3).sorted().distinct().toArray(),
                JdkCollections.stream(DOUBLES).filter(d -> d > 0).map(d -> d * 3).sorted().distinct().toArray(), 0.0);
        assertEquals(Arrays.stream(DOUBLES).count(), JdkCollections.stream(DOUBLES).count());
        same(Arrays.stream(DOUBLES).min(), JdkCollections.stream(DOUBLES).min());
        same(Arrays.stream(DOUBLES).max(), JdkCollections.stream(DOUBLES).max());
        same(Arrays.stream(DOUBLES).findFirst(), JdkCollections.stream(DOUBLES).findFirst());
        same(Arrays.stream(DOUBLES).reduce((a, b) -> a / 2 + b), JdkCollections.stream(DOUBLES).reduce((a, b) -> a / 2 + b));
        assertEquals(Arrays.stream(DOUBLES).reduce(1.5, (a, b) -> a * 0.5 + b),
                JdkCollections.stream(DOUBLES).reduce(1.5, (a, b) -> a * 0.5 + b), 0.0);
        assertArrayEquals(Arrays.stream(DOUBLES).mapToInt(d -> (int) d).toArray(),
                JdkCollections.stream(DOUBLES).mapToInt(d -> (int) d).toArray());
        assertArrayEquals(Arrays.stream(DOUBLES).mapToLong(d -> (long) (d * 10)).toArray(),
                JdkCollections.stream(DOUBLES).mapToLong(d -> (long) (d * 10)).toArray());
        assertEquals(Arrays.stream(DOUBLES).boxed().collect(java.util.stream.Collectors.toList()),
                JdkCollections.stream(DOUBLES).boxed().collect(Collectors.toList()));
        assertEquals(Arrays.stream(DOUBLES).mapToObj(Double::toString).collect(java.util.stream.Collectors.joining(" ")),
                JdkCollections.stream(DOUBLES).mapToObj(Double::toString).collect(Collectors.joining(" ")));
        assertArrayEquals(
                Arrays.stream(DOUBLES).flatMap(d -> java.util.stream.DoubleStream.of(d, -d)).skip(3).limit(5).toArray(),
                JdkCollections.stream(DOUBLES).flatMap(d -> DoubleStream.of(d, -d)).skip(3).limit(5).toArray(), 0.0);
        assertEquals(Arrays.stream(DOUBLES).anyMatch(d -> d < 0), JdkCollections.stream(DOUBLES).anyMatch(d -> d < 0));
        assertEquals(Arrays.stream(DOUBLES).allMatch(d -> d < 0), JdkCollections.stream(DOUBLES).allMatch(d -> d < 0));
        assertEquals(Arrays.stream(DOUBLES).noneMatch(d -> d == 0), JdkCollections.stream(DOUBLES).noneMatch(d -> d == 0));
        same(java.util.stream.DoubleStream.empty().min(), DoubleStream.empty().min());
        same(java.util.stream.DoubleStream.empty().max(), DoubleStream.empty().max());
        same(java.util.stream.DoubleStream.empty().average(), DoubleStream.empty().average());
        assertEquals(java.util.stream.DoubleStream.empty().sum(), DoubleStream.empty().sum(), 0.0);
        assertArrayEquals(java.util.stream.DoubleStream.iterate(1, d -> d / 3).limit(8).toArray(),
                DoubleStream.iterate(1, d -> d / 3).limit(8).toArray(), 0.0);
    }

    @Test
    public void doubleSumAndAverageAreCompensatedLikeTheJdks() {
        double[][] cases = {DOUBLES, {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1}, {1e100, 1.0, -1e100},
            {1.0, 1e-16, 1e-16, 1e-16, 1e-16}, {}, {3.5}, {-0.0}, {-0.0, -0.0}};
        for (double[] values : cases) {
            String at = Arrays.toString(values);
            assertEquals(at, Double.doubleToLongBits(Arrays.stream(values).sum()),
                    Double.doubleToLongBits(JdkCollections.stream(values).sum()));
            same(Arrays.stream(values).average(), JdkCollections.stream(values).average());
        }
    }

    @Test
    public void doubleSumOfInfinitiesAndNaN() {
        double inf = Double.POSITIVE_INFINITY;
        double[][] cases = {{inf, 1.0}, {1.0, inf, 2.0}, {-inf, 1.0}, {inf, -inf}, {inf, inf}, {Double.NaN, 1.0},
            {1.0, Double.NaN, inf}, {Double.MAX_VALUE, Double.MAX_VALUE}, {Double.MAX_VALUE, Double.MAX_VALUE, -inf}};
        for (double[] values : cases) {
            String at = Arrays.toString(values);
            assertEquals(at, Double.doubleToLongBits(Arrays.stream(values).sum()),
                    Double.doubleToLongBits(JdkCollections.stream(values).sum()));
            same(Arrays.stream(values).min(), JdkCollections.stream(values).min());
            same(Arrays.stream(values).max(), JdkCollections.stream(values).max());
        }
    }

    @Test
    public void doubleMinAndMaxOrderTheZeros() {
        double[] zeros = {0.0, -0.0, 0.0};
        assertEquals(Double.doubleToLongBits(Arrays.stream(zeros).min().getAsDouble()),
                Double.doubleToLongBits(JdkCollections.stream(zeros).min().getAsDouble()));
        assertEquals(Double.doubleToLongBits(Arrays.stream(zeros).max().getAsDouble()),
                Double.doubleToLongBits(JdkCollections.stream(zeros).max().getAsDouble()));
        assertArrayEquals(Arrays.stream(zeros).sorted().toArray(), JdkCollections.stream(zeros).sorted().toArray(), 0.0);
        assertEquals(Arrays.stream(zeros).distinct().count(), JdkCollections.stream(zeros).distinct().count());
    }

    // ---- statistics ----

    @Test
    public void summaryStatistics() {
        java.util.IntSummaryStatistics theirInts = theirs().summaryStatistics();
        IntSummaryStatistics myInts = mine().summaryStatistics();
        assertEquals(theirInts.getCount(), myInts.getCount());
        assertEquals(theirInts.getSum(), myInts.getSum());
        assertEquals(theirInts.getMin(), myInts.getMin());
        assertEquals(theirInts.getMax(), myInts.getMax());
        assertEquals(theirInts.getAverage(), myInts.getAverage(), 0.0);

        java.util.LongSummaryStatistics theirLongs = Arrays.stream(LONGS).summaryStatistics();
        LongSummaryStatistics myLongs = JdkCollections.stream(LONGS).summaryStatistics();
        assertEquals(theirLongs.getCount(), myLongs.getCount());
        assertEquals(theirLongs.getSum(), myLongs.getSum());
        assertEquals(theirLongs.getMin(), myLongs.getMin());
        assertEquals(theirLongs.getMax(), myLongs.getMax());
        assertEquals(theirLongs.getAverage(), myLongs.getAverage(), 0.0);

        java.util.DoubleSummaryStatistics theirDoubles = Arrays.stream(DOUBLES).summaryStatistics();
        DoubleSummaryStatistics myDoubles = JdkCollections.stream(DOUBLES).summaryStatistics();
        assertEquals(theirDoubles.getCount(), myDoubles.getCount());
        assertEquals(theirDoubles.getSum(), myDoubles.getSum(), 0.0);
        assertEquals(theirDoubles.getMin(), myDoubles.getMin(), 0.0);
        assertEquals(theirDoubles.getMax(), myDoubles.getMax(), 0.0);
        assertEquals(theirDoubles.getAverage(), myDoubles.getAverage(), 0.0);
    }

    @Test
    public void emptyStatisticsAnswerTheJdksSentinels() {
        java.util.IntSummaryStatistics theirInts = new java.util.IntSummaryStatistics();
        IntSummaryStatistics myInts = new IntSummaryStatistics();
        assertEquals(theirInts.getCount(), myInts.getCount());
        assertEquals(theirInts.getMin(), myInts.getMin());
        assertEquals(theirInts.getMax(), myInts.getMax());
        assertEquals(theirInts.getAverage(), myInts.getAverage(), 0.0);
        java.util.LongSummaryStatistics theirLongs = new java.util.LongSummaryStatistics();
        LongSummaryStatistics myLongs = new LongSummaryStatistics();
        assertEquals(theirLongs.getMin(), myLongs.getMin());
        assertEquals(theirLongs.getMax(), myLongs.getMax());
        assertEquals(theirLongs.getAverage(), myLongs.getAverage(), 0.0);
        java.util.DoubleSummaryStatistics theirDoubles = new java.util.DoubleSummaryStatistics();
        DoubleSummaryStatistics myDoubles = new DoubleSummaryStatistics();
        assertEquals(theirDoubles.getMin(), myDoubles.getMin(), 0.0);
        assertEquals(theirDoubles.getMax(), myDoubles.getMax(), 0.0);
        assertEquals(theirDoubles.getSum(), myDoubles.getSum(), 0.0);
        assertEquals(theirDoubles.getAverage(), myDoubles.getAverage(), 0.0);
    }

    @Test
    public void statisticsCombine() {
        java.util.IntSummaryStatistics theirA = java.util.stream.IntStream.of(1, 9).summaryStatistics();
        theirA.combine(java.util.stream.IntStream.of(-5, 20, 3).summaryStatistics());
        theirA.accept(4);
        IntSummaryStatistics myA = IntStream.of(1, 9).summaryStatistics();
        myA.combine(IntStream.of(-5, 20, 3).summaryStatistics());
        myA.accept(4);
        assertEquals(theirA.getCount(), myA.getCount());
        assertEquals(theirA.getSum(), myA.getSum());
        assertEquals(theirA.getMin(), myA.getMin());
        assertEquals(theirA.getMax(), myA.getMax());

        java.util.DoubleSummaryStatistics theirD = java.util.stream.DoubleStream.of(0.1, 0.2).summaryStatistics();
        theirD.combine(java.util.stream.DoubleStream.of(0.3, 1e10, -1e10).summaryStatistics());
        DoubleSummaryStatistics myD = DoubleStream.of(0.1, 0.2).summaryStatistics();
        myD.combine(DoubleStream.of(0.3, 1e10, -1e10).summaryStatistics());
        assertEquals(theirD.getCount(), myD.getCount());
        assertEquals(theirD.getSum(), myD.getSum(), 0.0);
        assertEquals(theirD.getAverage(), myD.getAverage(), 0.0);
    }

    // ---- the optionals ----

    @Test
    public void optionalEqualityAndFallbacks() {
        assertEquals(OptionalInt.of(4), OptionalInt.of(4));
        assertEquals(OptionalInt.of(4).hashCode(), java.util.OptionalInt.of(4).hashCode());
        assertFalse(OptionalInt.of(4).equals(OptionalInt.of(5)));
        assertFalse(OptionalInt.of(0).equals(OptionalInt.empty()));
        assertEquals(OptionalInt.empty(), OptionalInt.empty());
        assertEquals(java.util.OptionalLong.of(1L << 40).hashCode(), OptionalLong.of(1L << 40).hashCode());
        assertEquals(java.util.OptionalDouble.of(2.5).hashCode(), OptionalDouble.of(2.5).hashCode());
        assertEquals(OptionalDouble.of(Double.NaN), OptionalDouble.of(Double.NaN));
        assertEquals(9, OptionalInt.empty().orElseGet(() -> 9));
        assertEquals(3, OptionalInt.of(3).orElseGet(() -> 9));
        final int[] got = {0};
        OptionalInt.of(6).ifPresent(v -> got[0] = v);
        OptionalInt.empty().ifPresent(v -> got[0] = -1);
        assertEquals(6, got[0]);
        try {
            OptionalLong.empty().orElseThrow(() -> new IllegalStateException("none"));
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("none", expected.getMessage());
        }
    }
}
