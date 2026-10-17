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
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/// java.util.stream's IntStream, LongStream and DoubleStream, their
/// optionals and their summary statistics.
public final class PrimitiveStreamChecks {
    private static final int[] INTS = {5, 3, 8, 3, 1, 9, 2, 8, 7, 4, 6, 0, -4};
    private static final long[] LONGS = {5000000000L, 3, -8000000000L, 3, 1, 9, 2, 5000000000L, 7};
    private static final double[] DOUBLES = {0.1, 2.5, -3.75, 0.1, 1e10, 0.2, 0.3, -1e10, 7.0};

    private PrimitiveStreamChecks() {
    }

    private static IntStream ints() {
        return Arrays.stream(INTS);
    }

    private static void opt(String key, OptionalInt o) {
        Bulk.p(key, o.isPresent() + " " + o + " " + (o.isPresent() ? o.getAsInt() : 0) + " " + o.orElse(-77));
    }

    private static void opt(String key, OptionalLong o) {
        Bulk.p(key, o.isPresent() + " " + o + " " + o.orElse(-77L));
    }

    private static void opt(String key, OptionalDouble o) {
        Bulk.p(key, o.isPresent() + " " + o + " " + o.orElse(-77.5));
    }

    /// The bit patterns of `values`, so a signed zero or a NaN payload shows.
    private static String bits(double v) {
        return v + "/" + Long.toHexString(Double.doubleToLongBits(v));
    }

    /// Runs the group.
    public static void run() {
        ranges();
        intStreams();
        iteratorsAndLife();
        longsAndDoubles();
        statistics();
        optionals();
    }

    private static void ranges() {
        Bulk.check("PrimitiveStream.intRangeAtEveryBoundary", () -> {
            int max = Integer.MAX_VALUE;
            int min = Integer.MIN_VALUE;
            int[][] bounds = {{0, 0}, {0, 1}, {0, 5}, {5, 2}, {-3, 3}, {5, 5}, {5, 4}, {max - 2, max}, {max, max},
                {min, min + 2}, {min, min}, {max, min}, {-1, 0}};
            for (int[] b : bounds) {
                String at = b[0] + ".." + b[1];
                Bulk.p("range " + at, IntStream.range(b[0], b[1]).toArray());
                Bulk.p("rangeClosed " + at, IntStream.rangeClosed(b[0], b[1]).toArray());
            }
            // The widest ranges, counted without being walked to the end.
            Bulk.p("widestLimitSum", IntStream.rangeClosed(min, max).limit(3).sum());
            Bulk.p("widestSkipFirst", IntStream.range(min, max).skip(4).findFirst().getAsInt());
        });
        Bulk.check("PrimitiveStream.longRangeAtEveryBoundary", () -> {
            long max = Long.MAX_VALUE;
            long min = Long.MIN_VALUE;
            long[][] bounds = {{0, 0}, {0, 1}, {0, 5}, {5, 2}, {-3, 3}, {5, 5}, {5, 4}, {max - 2, max}, {max, max},
                {min, min + 2}, {min, min}, {max, min}, {4000000000L, 4000000003L}};
            for (long[] b : bounds) {
                String at = b[0] + ".." + b[1];
                Bulk.p("range " + at, LongStream.range(b[0], b[1]).toArray());
                Bulk.p("rangeClosed " + at, LongStream.rangeClosed(b[0], b[1]).toArray());
            }
            Bulk.p("widestLimitSum", LongStream.rangeClosed(min, max).limit(3).sum());
        });
    }

    private static void intStreams() {
        Bulk.check("PrimitiveStream.intIntermediateOperations", () -> {
            Bulk.p("filterMap", ints().filter(n -> n % 2 == 1).map(n -> n * 10).toArray());
            Bulk.p("distinct", ints().distinct().toArray());
            Bulk.p("sorted", ints().sorted().toArray());
            Bulk.p("skipLimit", ints().skip(2).limit(4).toArray());
            Bulk.p("flatMap", ints().flatMap(n -> IntStream.range(0, n)).toArray());
            Bulk.p("flatMapNull", ints().flatMap(n -> n > 5 ? null : IntStream.of(n)).toArray());
            List<Integer> seen = new ArrayList<Integer>();
            Bulk.p("peekLimit", ints().peek(seen::add).limit(3).toArray());
            Bulk.p("seen", seen);
        });
        Bulk.check("PrimitiveStream.intTerminalOperations", () -> {
            Bulk.p("sum", ints().sum());
            Bulk.p("count", ints().count());
            opt("min", ints().min());
            opt("max", ints().max());
            opt("average", ints().average());
            opt("findFirst", ints().findFirst());
            opt("findAny", ints().skip(4).findAny());
            opt("reduce", ints().reduce((a, b) -> a * 3 + b));
            Bulk.p("reduceIdentity", ints().reduce(7, (a, b) -> a ^ b));
            Bulk.p("any", ints().anyMatch(n -> n == 9));
            Bulk.p("all", ints().allMatch(n -> n >= 0));
            Bulk.p("none", ints().noneMatch(n -> n > 100));
            Bulk.p("collect", ints().collect(StringBuilder::new, (sb, n) -> sb.append(n).append(';'),
                    (a, b) -> a.append(b)).toString());
            List<Integer> seen = new ArrayList<Integer>();
            ints().forEach(seen::add);
            Bulk.p("forEach", seen);
            List<Integer> ordered = new ArrayList<Integer>();
            ints().forEachOrdered(ordered::add);
            Bulk.p("forEachOrdered", ordered);
        });
        Bulk.check("PrimitiveStream.intSumWrapsAsTheJdkDoes", () -> {
            int[] big = {Integer.MAX_VALUE, Integer.MAX_VALUE, 5};
            Bulk.p("sum", IntStream.of(big).sum());
            // The average does not wrap: it is summed as a long.
            opt("average", IntStream.of(big).average());
        });
        Bulk.check("PrimitiveStream.anEmptyIntStreamHasNoMinMaxOrAverage", () -> {
            opt("min", IntStream.empty().min());
            opt("max", IntStream.empty().max());
            opt("average", IntStream.empty().average());
            opt("findFirst", IntStream.empty().findFirst());
            opt("reduce", IntStream.empty().reduce((a, b) -> a + b));
            Bulk.p("sum", IntStream.empty().sum());
            Bulk.pThrown("maxGet", NoSuchElementException.class, () -> IntStream.empty().max().getAsInt());
            Bulk.pThrown("averageGet", NoSuchElementException.class, () -> IntStream.empty().average().getAsDouble());
        });
        Bulk.check("PrimitiveStream.intConversions", () -> {
            Bulk.p("asLong", ints().asLongStream().map(n -> n * 3000000000L).toArray());
            Bulk.p("asDouble", ints().asDoubleStream().map(n -> n / 4.0).toArray());
            Bulk.p("mapToLong", ints().mapToLong(n -> n * 3000000000L).toArray());
            Bulk.p("mapToDouble", ints().mapToDouble(n -> n / 8.0).toArray());
            Bulk.p("mapToObj", ints().mapToObj(Integer::toHexString).collect(Collectors.joining("/")));
            Bulk.p("boxed", ints().boxed().collect(Collectors.toList()));
        });
        Bulk.check("PrimitiveStream.intFactories", () -> {
            Bulk.p("ofOne", IntStream.of(4).toArray());
            Bulk.p("ofMany", IntStream.of(4, 5, 6).toArray());
            Bulk.p("iterate", IntStream.iterate(1, n -> n * 3).limit(6).toArray());
            final int[] next = {0};
            Bulk.p("generate", IntStream.generate(() -> next[0]++).limit(4).toArray());
            Bulk.p("concat", IntStream.concat(IntStream.range(0, 3), ints().limit(2)).toArray());
            Bulk.p("boundedIterate", IntStream.iterate(1, n -> n < 50, n -> n * 3).toArray());
            Bulk.p("takeWhile", ints().takeWhile(n -> n < 8).toArray());
            Bulk.p("dropWhile", ints().dropWhile(n -> n != 0).toArray());
        });
        // Arrays.stream(array, from, to): the values, not the bounds check.
        Bulk.check("PrimitiveStream.arraysStreamRanges", () -> {
            Bulk.p("ints", Arrays.stream(INTS, 2, 6).toArray());
            Bulk.p("intsEmpty", Arrays.stream(INTS, 4, 4).toArray());
            Bulk.p("intsWhole", Arrays.stream(INTS, 0, INTS.length).toArray());
            Bulk.p("longs", Arrays.stream(LONGS, 1, 5).toArray());
            Bulk.p("doubles", Arrays.stream(DOUBLES, 1, 5).toArray());
            String[] words = {"a", "b", "c", "d"};
            Bulk.p("objects", Arrays.stream(words, 1, 3).toArray());
            Bulk.p("objectsWhole", Arrays.stream(words).toArray());
            // And the same values through what JavaAPI's Arrays builds them from.
            Bulk.p("intsOfSkipLimit", IntStream.of(INTS).skip(2).limit(4).toArray());
            Bulk.p("objectsOfSkipLimit", Stream.of(words).skip(1).limit(2).toArray());
        });
    }

    private static void iteratorsAndLife() {
        Bulk.check("PrimitiveStream.thePrimitiveIterators", () -> {
            PrimitiveIterator.OfInt it = ints().filter(n -> n > 4).iterator();
            List<Integer> walked = new ArrayList<Integer>();
            while (it.hasNext()) {
                walked.add(it.nextInt());
            }
            Bulk.p("walked", walked);
            Bulk.p("hasNext", it.hasNext());
            Bulk.pThrown("nextAtEnd", NoSuchElementException.class, it::nextInt);
            PrimitiveIterator.OfLong longs = LongStream.of(4L, 5L).iterator();
            Bulk.p("nextLong", longs.nextLong());
            Bulk.p("nextBoxed", longs.next());
            PrimitiveIterator.OfDouble doubles = DoubleStream.of(0.5).iterator();
            Bulk.p("nextDouble", doubles.nextDouble());
            Bulk.p("doublesHasNext", doubles.hasNext());
        });
        Bulk.check("PrimitiveStream.aPrimitiveStreamRunsOnce", () -> {
            IntStream stream = ints();
            Bulk.p("sum", stream.sum());
            Bulk.pThrown("again", IllegalStateException.class, stream::sum);
            IntStream staged = ints();
            staged.map(n -> n);
            Bulk.pThrown("secondStage", IllegalStateException.class, () -> staged.filter(n -> true));
        });
    }

    private static void longsAndDoubles() {
        Bulk.check("PrimitiveStream.longOperations", () -> {
            Bulk.p("pipeline", LongStream.of(LONGS).filter(n -> n > 2).map(n -> n * 2).sorted().distinct().toArray());
            Bulk.p("sum", LongStream.of(LONGS).sum());
            Bulk.p("count", LongStream.of(LONGS).count());
            opt("min", LongStream.of(LONGS).min());
            opt("max", LongStream.of(LONGS).max());
            opt("average", LongStream.of(LONGS).average());
            opt("reduce", LongStream.of(LONGS).reduce((a, b) -> a - b));
            opt("findFirst", LongStream.of(LONGS).skip(1).limit(2).findFirst());
            Bulk.p("reduceIdentity", LongStream.of(LONGS).reduce(1L, (a, b) -> a + b));
            Bulk.p("mapToInt", LongStream.of(LONGS).mapToInt(n -> (int) (n % 7)).toArray());
            Bulk.p("asDouble", LongStream.of(LONGS).asDoubleStream().toArray());
            Bulk.p("mapToDouble", LongStream.of(LONGS).mapToDouble(n -> n / 2.0).toArray());
            Bulk.p("boxed", LongStream.of(LONGS).boxed().collect(Collectors.toList()));
            Bulk.p("mapToObj", LongStream.of(LONGS).mapToObj(Long::toString).collect(Collectors.joining()));
            Bulk.p("flatMap", LongStream.of(LONGS).flatMap(n -> LongStream.of(n, n)).limit(5).toArray());
            Bulk.p("any", LongStream.of(LONGS).anyMatch(n -> n < 0));
            Bulk.p("all", LongStream.of(LONGS).allMatch(n -> n < 0));
            Bulk.p("none", LongStream.of(LONGS).noneMatch(n -> n == 0));
            opt("emptyMin", LongStream.empty().min());
            opt("emptyAverage", LongStream.empty().average());
            Bulk.p("iterate", LongStream.iterate(1, n -> n * 5).limit(20).toArray());
            Bulk.p("concat", LongStream.concat(LongStream.of(1), LongStream.of(2, 3)).toArray());
        });
        Bulk.check("PrimitiveStream.doubleOperations", () -> {
            Bulk.p("pipeline", DoubleStream.of(DOUBLES).filter(d -> d > 0).map(d -> d * 3).sorted().distinct()
                    .toArray());
            Bulk.p("count", DoubleStream.of(DOUBLES).count());
            opt("min", DoubleStream.of(DOUBLES).min());
            opt("max", DoubleStream.of(DOUBLES).max());
            opt("findFirst", DoubleStream.of(DOUBLES).findFirst());
            opt("reduce", DoubleStream.of(DOUBLES).reduce((a, b) -> a / 2 + b));
            Bulk.p("reduceIdentity", DoubleStream.of(DOUBLES).reduce(1.5, (a, b) -> a * 0.5 + b));
            Bulk.p("mapToInt", DoubleStream.of(DOUBLES).mapToInt(d -> (int) d).toArray());
            Bulk.p("mapToLong", DoubleStream.of(DOUBLES).mapToLong(d -> (long) (d * 10)).toArray());
            Bulk.p("boxed", DoubleStream.of(DOUBLES).boxed().collect(Collectors.toList()));
            Bulk.p("mapToObj", DoubleStream.of(DOUBLES).mapToObj(Double::toString).collect(Collectors.joining(" ")));
            Bulk.p("flatMap", DoubleStream.of(DOUBLES).flatMap(d -> DoubleStream.of(d, -d)).skip(3).limit(5)
                    .toArray());
            Bulk.p("any", DoubleStream.of(DOUBLES).anyMatch(d -> d < 0));
            Bulk.p("all", DoubleStream.of(DOUBLES).allMatch(d -> d < 0));
            Bulk.p("none", DoubleStream.of(DOUBLES).noneMatch(d -> d == 0));
            opt("emptyMin", DoubleStream.empty().min());
            opt("emptyMax", DoubleStream.empty().max());
            opt("emptyAverage", DoubleStream.empty().average());
            Bulk.p("emptySum", DoubleStream.empty().sum());
            Bulk.p("iterate", DoubleStream.iterate(1, d -> d / 3).limit(8).toArray());
        });
        Bulk.check("PrimitiveStream.doubleSumAndAverageAreCompensatedLikeTheJdks", () -> {
            double[][] cases = {DOUBLES, {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1}, {1e100, 1.0, -1e100},
                {1.0, 1e-16, 1e-16, 1e-16, 1e-16}, {}, {3.5}, {-0.0}, {-0.0, -0.0}};
            for (double[] values : cases) {
                String at = Arrays.toString(values);
                Bulk.p("sum " + at, bits(DoubleStream.of(values).sum()));
                opt("average " + at, DoubleStream.of(values).average());
            }
        });
        Bulk.check("PrimitiveStream.doubleSumOfInfinitiesAndNaN", () -> {
            double inf = Double.POSITIVE_INFINITY;
            double[][] cases = {{inf, 1.0}, {1.0, inf, 2.0}, {-inf, 1.0}, {inf, -inf}, {inf, inf}, {Double.NaN, 1.0},
                {1.0, Double.NaN, inf}, {Double.MAX_VALUE, Double.MAX_VALUE}, {Double.MAX_VALUE, Double.MAX_VALUE, -inf}};
            for (double[] values : cases) {
                String at = Arrays.toString(values);
                Bulk.p("sum " + at, bits(DoubleStream.of(values).sum()));
                opt("min " + at, DoubleStream.of(values).min());
                opt("max " + at, DoubleStream.of(values).max());
            }
        });
        Bulk.check("PrimitiveStream.doubleMinAndMaxOrderTheZeros", () -> {
            double[] zeros = {0.0, -0.0, 0.0};
            Bulk.p("min", bits(DoubleStream.of(zeros).min().getAsDouble()));
            Bulk.p("max", bits(DoubleStream.of(zeros).max().getAsDouble()));
            Bulk.p("sorted", DoubleStream.of(zeros).sorted().toArray());
            Bulk.p("distinct", DoubleStream.of(zeros).distinct().count());
        });
    }

    private static void statistics() {
        Bulk.check("PrimitiveStream.summaryStatistics", () -> {
            IntSummaryStatistics i = ints().summaryStatistics();
            Bulk.p("int", i.getCount() + " " + i.getSum() + " " + i.getMin() + " " + i.getMax() + " " + i.getAverage());
            LongSummaryStatistics l = LongStream.of(LONGS).summaryStatistics();
            Bulk.p("long", l.getCount() + " " + l.getSum() + " " + l.getMin() + " " + l.getMax() + " "
                    + l.getAverage());
            DoubleSummaryStatistics d = DoubleStream.of(DOUBLES).summaryStatistics();
            Bulk.p("double", d.getCount() + " " + d.getSum() + " " + d.getMin() + " " + d.getMax() + " "
                    + d.getAverage());
        });
        Bulk.check("PrimitiveStream.emptyStatisticsAnswerTheJdksSentinels", () -> {
            IntSummaryStatistics i = new IntSummaryStatistics();
            Bulk.p("int", i.getCount() + " " + i.getMin() + " " + i.getMax() + " " + i.getAverage());
            LongSummaryStatistics l = new LongSummaryStatistics();
            Bulk.p("long", l.getMin() + " " + l.getMax() + " " + l.getAverage());
            DoubleSummaryStatistics d = new DoubleSummaryStatistics();
            Bulk.p("double", d.getMin() + " " + d.getMax() + " " + d.getSum() + " " + d.getAverage());
        });
        Bulk.check("PrimitiveStream.statisticsCombine", () -> {
            IntSummaryStatistics a = IntStream.of(1, 9).summaryStatistics();
            a.combine(IntStream.of(-5, 20, 3).summaryStatistics());
            a.accept(4);
            Bulk.p("int", a.getCount() + " " + a.getSum() + " " + a.getMin() + " " + a.getMax());
            DoubleSummaryStatistics d = DoubleStream.of(0.1, 0.2).summaryStatistics();
            d.combine(DoubleStream.of(0.3, 1e10, -1e10).summaryStatistics());
            Bulk.p("double", d.getCount() + " " + d.getSum() + " " + d.getAverage());
        });
    }

    private static void optionals() {
        Bulk.check("PrimitiveStream.optionalEqualityAndFallbacks", () -> {
            Bulk.p("intEquals", OptionalInt.of(4).equals(OptionalInt.of(4)));
            Bulk.p("intHash", OptionalInt.of(4).hashCode());
            Bulk.p("intNotEquals", OptionalInt.of(4).equals(OptionalInt.of(5)));
            Bulk.p("zeroVsEmpty", OptionalInt.of(0).equals(OptionalInt.empty()));
            Bulk.p("emptyEquals", OptionalInt.empty().equals(OptionalInt.empty()));
            Bulk.p("longHash", OptionalLong.of(1L << 40).hashCode());
            Bulk.p("doubleHash", OptionalDouble.of(2.5).hashCode());
            Bulk.p("nanEquals", OptionalDouble.of(Double.NaN).equals(OptionalDouble.of(Double.NaN)));
            Bulk.p("orElseGetEmpty", OptionalInt.empty().orElseGet(() -> 9));
            Bulk.p("orElseGetPresent", OptionalInt.of(3).orElseGet(() -> 9));
            final int[] got = {0};
            OptionalInt.of(6).ifPresent(v -> got[0] = v);
            OptionalInt.empty().ifPresent(v -> got[0] = -1);
            Bulk.p("ifPresent", got[0]);
            String message = "none";
            try {
                OptionalLong.empty().orElseThrow(() -> new IllegalStateException("none thrown"));
            } catch (IllegalStateException expected) {
                message = expected.getMessage();
            }
            Bulk.p("orElseThrow", message);
        });
        // A call compiled against the JDK is linked by descriptor, and the
        // JDK's primitive streams declare parallel() and sequential()
        // answering their own type. Assigning each to its own type below
        // only compiles if JavaAPI declares them the same way, and links on
        // both runs only if the descriptor is the JDK's.
        Bulk.check("PrimitiveStream.parallelAndSequentialAnswerTheStreamsOwnType", () -> {
            IntStream ip = IntStream.range(0, 5).parallel();
            IntStream is = ip.sequential();
            Bulk.p("int", is.sum());
            LongStream lp = LongStream.range(0, 5).parallel();
            LongStream ls = lp.sequential();
            Bulk.p("long", ls.sum());
            DoubleStream dp = DoubleStream.of(0.5, 1.5).parallel();
            DoubleStream ds = dp.sequential();
            Bulk.p("double", ds.sum());
        });
    }
}
