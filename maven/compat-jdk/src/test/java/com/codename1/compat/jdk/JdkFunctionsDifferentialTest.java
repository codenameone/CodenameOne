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
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds `JdkFunctions` -- the default and static methods of `Comparator`
/// and the function interfaces -- against the JDK's own, and the primitive
/// function interfaces' defaults against theirs.
public class JdkFunctionsDifferentialTest {

    private static final List<String> WORDS = Collections.unmodifiableList(Arrays.asList(
            "pear", "Apple", "fig", "banana", "apple", "Kiwi", "cherry", "Fig", "date", "avocado", "FIG"));

    private static List<String> sorted(List<String> words, Comparator<? super String> comparator) {
        List<String> out = new ArrayList<String>(words);
        Collections.sort(out, comparator);
        return out;
    }

    private static void same(Comparator<? super String> theirs, Comparator<? super String> mine) {
        assertEquals(sorted(WORDS, theirs), sorted(WORDS, mine));
        for (String a : WORDS) {
            for (String b : WORDS) {
                assertEquals(a + " " + b, Integer.signum(theirs.compare(a, b)), Integer.signum(mine.compare(a, b)));
            }
        }
    }

    // ---- Comparator ----

    @Test
    public void naturalAndReverseOrder() {
        same(Comparator.<String>naturalOrder(), JdkFunctions.<String>naturalOrder());
        same(Comparator.<String>reverseOrder(), JdkFunctions.<String>reverseOrder());
        try {
            JdkFunctions.<String>naturalOrder().compare(null, "a");
            fail();
        } catch (NullPointerException expected) {
            // As the JDK's.
        }
    }

    @Test
    public void reversed() {
        Comparator<String> byLength = (a, b) -> a.length() - b.length();
        same(byLength.reversed(), JdkFunctions.reversed(byLength));
        same(byLength.reversed().reversed(), JdkFunctions.reversed(JdkFunctions.reversed(byLength)));
        same(String.CASE_INSENSITIVE_ORDER.reversed(), JdkFunctions.reversed(String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void comparingByAKey() {
        same(Comparator.comparing(String::length), JdkFunctions.comparing(String::length));
        same(Comparator.comparing(s -> s.toLowerCase(), Comparator.<String>reverseOrder()),
                JdkFunctions.comparing(s -> s.toLowerCase(), Comparator.<String>reverseOrder()));
        same(Comparator.comparingInt(String::length), JdkFunctions.comparingInt(String::length));
        same(Comparator.comparingLong(s -> -(long) s.charAt(0)), JdkFunctions.comparingLong(s -> -(long) s.charAt(0)));
        same(Comparator.comparingDouble(s -> 1.0 / s.length()), JdkFunctions.comparingDouble(s -> 1.0 / s.length()));
    }

    @Test
    public void comparingIntDoesNotOverflow() {
        Comparator<Integer> theirs = Comparator.comparingInt(n -> n);
        Comparator<Integer> mine = JdkFunctions.comparingInt(n -> n);
        assertEquals(Integer.signum(theirs.compare(Integer.MIN_VALUE, 1)),
                Integer.signum(mine.compare(Integer.MIN_VALUE, 1)));
        assertEquals(Integer.signum(theirs.compare(Integer.MAX_VALUE, -1)),
                Integer.signum(mine.compare(Integer.MAX_VALUE, -1)));
        assertTrue(mine.compare(Integer.MIN_VALUE, 1) < 0);
    }

    @Test
    public void thenComparing() {
        Comparator<String> byLength = (a, b) -> a.length() - b.length();
        same(byLength.thenComparing(Comparator.<String>naturalOrder()),
                JdkFunctions.thenComparing(byLength, Comparator.<String>naturalOrder()));
        same(byLength.thenComparing(s -> s.toLowerCase()),
                JdkFunctions.thenComparing(byLength, (Function<String, String>) s -> s.toLowerCase()));
        same(byLength.thenComparing(s -> s.toLowerCase(), Comparator.<String>reverseOrder()),
                JdkFunctions.thenComparing(byLength, s -> s.toLowerCase(), Comparator.<String>reverseOrder()));
        same(byLength.thenComparingInt(s -> s.charAt(0)), JdkFunctions.thenComparingInt(byLength, s -> s.charAt(0)));
        same(byLength.thenComparingLong(s -> -s.charAt(1)), JdkFunctions.thenComparingLong(byLength, s -> -s.charAt(1)));
        same(byLength.thenComparingDouble(s -> s.charAt(2) / 3.0),
                JdkFunctions.thenComparingDouble(byLength, s -> s.charAt(2) / 3.0));
    }

    @Test
    public void thenComparingAsksTheSecondOnlyOnATie() {
        final int[] asked = {0};
        Comparator<String> byLength = (a, b) -> a.length() - b.length();
        Comparator<String> counting = (a, b) -> {
            asked[0]++;
            return a.compareTo(b);
        };
        Comparator<String> mine = JdkFunctions.thenComparing(byLength, counting);
        assertTrue(mine.compare("a", "bb") < 0);
        assertEquals(0, asked[0]);
        assertTrue(mine.compare("b", "a") > 0);
        assertEquals(1, asked[0]);
    }

    @Test
    public void nullsFirstAndLast() {
        List<String> withNulls = Arrays.asList("b", null, "a", null, "c");
        Comparator<String> natural = Comparator.naturalOrder();
        assertEquals(sorted(withNulls, Comparator.nullsFirst(natural)), sorted(withNulls, JdkFunctions.nullsFirst(natural)));
        assertEquals(sorted(withNulls, Comparator.nullsLast(natural)), sorted(withNulls, JdkFunctions.nullsLast(natural)));
        // A null comparator: the non-null ones count as equal.
        assertEquals(sorted(withNulls, Comparator.<String>nullsFirst(null)),
                sorted(withNulls, JdkFunctions.<String>nullsFirst(null)));
        assertEquals(sorted(withNulls, Comparator.<String>nullsLast(null)),
                sorted(withNulls, JdkFunctions.<String>nullsLast(null)));
        assertEquals(sorted(withNulls, Comparator.nullsFirst(natural).reversed()),
                sorted(withNulls, JdkFunctions.reversed(JdkFunctions.nullsFirst(natural))));
        assertEquals(0, JdkFunctions.nullsFirst(natural).compare(null, null));
    }

    @Test
    public void aNullArgumentIsRefused() {
        Comparator<String> natural = Comparator.naturalOrder();
        Runnable[] calls = {
            () -> JdkFunctions.comparing((Function<String, String>) null),
            () -> JdkFunctions.comparing(String::length, null),
            () -> JdkFunctions.comparingInt(null),
            () -> JdkFunctions.thenComparing(natural, (Comparator<String>) null),
            () -> JdkFunctions.and(s -> true, null),
            () -> JdkFunctions.or(s -> true, null),
            () -> JdkFunctions.not(null),
            () -> JdkFunctions.andThen((Function<String, String>) s -> s, null),
            () -> JdkFunctions.compose((Function<String, String>) s -> s, null),
            () -> JdkFunctions.andThen((Consumer<String>) s -> { }, null),
            () -> JdkFunctions.minBy(null),
            () -> JdkFunctions.maxBy(null),
        };
        for (int i = 0; i < calls.length; i++) {
            try {
                calls[i].run();
                fail("call " + i);
            } catch (NullPointerException expected) {
                // As the JDK's.
            }
        }
    }

    // ---- Predicate ----

    @Test
    public void predicateCombinators() {
        Predicate<String> isShort = s -> s.length() < 5;
        Predicate<String> lower = s -> Character.isLowerCase(s.charAt(0));
        for (String w : WORDS) {
            assertEquals(isShort.and(lower).test(w), JdkFunctions.and(isShort, lower).test(w));
            assertEquals(isShort.or(lower).test(w), JdkFunctions.or(isShort, lower).test(w));
            assertEquals(isShort.negate().test(w), JdkFunctions.negate(isShort).test(w));
            assertEquals(!isShort.test(w), JdkFunctions.not(isShort).test(w));
            assertEquals(Predicate.isEqual("fig").test(w), JdkFunctions.isEqual("fig").test(w));
        }
        assertEquals(Predicate.isEqual(null).test(null), JdkFunctions.isEqual(null).test(null));
        assertEquals(Predicate.isEqual(null).test("a"), JdkFunctions.isEqual(null).test("a"));
        assertEquals(Predicate.isEqual("a").test(null), JdkFunctions.isEqual("a").test(null));
    }

    @Test
    public void predicatesShortCircuit() {
        final int[] asked = {0};
        Predicate<String> counting = s -> {
            asked[0]++;
            return true;
        };
        assertFalse(JdkFunctions.and(s -> false, counting).test("x"));
        assertTrue(JdkFunctions.or(s -> true, counting).test("x"));
        assertEquals(0, asked[0]);
        assertTrue(JdkFunctions.and(s -> true, counting).test("x"));
        assertEquals(1, asked[0]);
    }

    // ---- Function and friends ----

    @Test
    public void functionComposition() {
        Function<Integer, Integer> twice = n -> n * 2;
        Function<Integer, Integer> plusThree = n -> n + 3;
        assertEquals(twice.andThen(plusThree).apply(5), JdkFunctions.andThen(twice, plusThree).apply(5));
        assertEquals(twice.compose(plusThree).apply(5), JdkFunctions.compose(twice, plusThree).apply(5));
        assertEquals(Function.<String>identity().apply("same"), JdkFunctions.<String>identity().apply("same"));
        String value = "identical";
        assertSame(value, JdkFunctions.<String>identity().apply(value));
        assertSame(value, JdkFunctions.<String>unaryIdentity().apply(value));
        UnaryOperator<String> upper = String::toUpperCase;
        assertEquals(upper.andThen(String::length).apply("abc"), JdkFunctions.andThen(upper, String::length).apply("abc"));

        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        assertEquals(add.andThen(twice).apply(3, 4), JdkFunctions.andThen(add, twice).apply(3, 4));
    }

    @Test
    public void consumersRunInOrder() {
        List<String> theirs = new ArrayList<String>();
        List<String> mine = new ArrayList<String>();
        Consumer<String> theirFirst = s -> theirs.add("first " + s);
        theirFirst.andThen(s -> theirs.add("second " + s)).accept("x");
        Consumer<String> myFirst = s -> mine.add("first " + s);
        JdkFunctions.andThen(myFirst, s -> mine.add("second " + s)).accept("x");
        assertEquals(theirs, mine);

        BiConsumer<String, Integer> theirPair = (s, n) -> theirs.add(s + n);
        theirPair.andThen((s, n) -> theirs.add(n + s)).accept("p", 1);
        BiConsumer<String, Integer> myPair = (s, n) -> mine.add(s + n);
        JdkFunctions.andThen(myPair, (s, n) -> mine.add(n + s)).accept("p", 1);
        assertEquals(theirs, mine);
        assertEquals(4, mine.size());
    }

    @Test
    public void minByAndMaxBy() {
        Comparator<String> byLength = (a, b) -> a.length() - b.length();
        BinaryOperator<String> theirMin = BinaryOperator.minBy(byLength);
        BinaryOperator<String> myMin = JdkFunctions.minBy(byLength);
        BinaryOperator<String> theirMax = BinaryOperator.maxBy(byLength);
        BinaryOperator<String> myMax = JdkFunctions.maxBy(byLength);
        for (String a : WORDS) {
            for (String b : WORDS) {
                // On a tie the same one of the two, not merely an equal one.
                assertSame(theirMin.apply(a, b), myMin.apply(a, b));
                assertSame(theirMax.apply(a, b), myMax.apply(a, b));
            }
        }
    }

    // ---- the primitive interfaces' own defaults ----

    @Test
    public void primitivePredicates() {
        java.util.function.IntPredicate theirEven = n -> n % 2 == 0;
        java.util.function.IntPredicate theirBig = n -> n > 5;
        IntPredicate myEven = n -> n % 2 == 0;
        IntPredicate myBig = n -> n > 5;
        for (int n = 0; n < 12; n++) {
            assertEquals(theirEven.and(theirBig).test(n), myEven.and(myBig).test(n));
            assertEquals(theirEven.or(theirBig).test(n), myEven.or(myBig).test(n));
            assertEquals(theirEven.negate().test(n), myEven.negate().test(n));
        }
        LongPredicate longs = n -> n > 5L;
        assertTrue(longs.and(n -> n < 9L).test(7L));
        assertFalse(longs.or(n -> n == 0L).test(3L));
        DoublePredicate doubles = d -> d > 0.5;
        assertTrue(doubles.negate().test(0.25));
        BiPredicate<String, Integer> longer = (s, n) -> s.length() > n;
        assertTrue(longer.and((s, n) -> n > 0).test("abc", 2));
        assertTrue(longer.or((s, n) -> n > 0).test("", 2));
        assertTrue(longer.negate().test("", 2));
    }

    @Test
    public void primitiveOperators() {
        java.util.function.IntUnaryOperator theirTwice = n -> n * 2;
        IntUnaryOperator myTwice = n -> n * 2;
        assertEquals(theirTwice.andThen(n -> n + 3).applyAsInt(5), myTwice.andThen(n -> n + 3).applyAsInt(5));
        assertEquals(theirTwice.compose(n -> n + 3).applyAsInt(5), myTwice.compose(n -> n + 3).applyAsInt(5));
        assertEquals(java.util.function.IntUnaryOperator.identity().applyAsInt(7),
                IntUnaryOperator.identity().applyAsInt(7));
        LongUnaryOperator longs = n -> n * 2;
        assertEquals(16L, longs.andThen(n -> n + 6).applyAsLong(5));
        assertEquals(22L, longs.compose(n -> n + 6).applyAsLong(5));
        DoubleUnaryOperator doubles = d -> d / 2;
        assertEquals(3.0, doubles.andThen(d -> d + 1).applyAsDouble(4), 0.0);
        assertEquals(2.5, doubles.compose(d -> d + 1).applyAsDouble(4), 0.0);

        List<Integer> seen = new ArrayList<Integer>();
        IntConsumer first = n -> seen.add(n);
        first.andThen(n -> seen.add(-n)).accept(4);
        assertEquals(Arrays.asList(4, -4), seen);
    }
}
