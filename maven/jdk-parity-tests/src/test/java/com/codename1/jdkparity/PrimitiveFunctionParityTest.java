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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The defaults of the primitive java.util.function interfaces in vm/JavaAPI
 * against the JDK's. The object-typed ones (Function, Predicate, Comparator...)
 * cannot be renamed apart from the JDK collections that call them; vm/tests'
 * FunctionEdgeApp runs those on ParparVM.
 */
class PrimitiveFunctionParityTest {

    @Test
    void predicates() {
        java.util.function.IntPredicate theirEven = n -> n % 2 == 0;
        java.util.function.IntPredicate theirBig = n -> n > 5;
        jdkparity.java.util.function.IntPredicate myEven = n -> n % 2 == 0;
        jdkparity.java.util.function.IntPredicate myBig = n -> n > 5;
        java.util.function.LongPredicate theirLong = n -> n > 5L;
        jdkparity.java.util.function.LongPredicate myLong = n -> n > 5L;
        java.util.function.DoublePredicate theirDouble = d -> d > 0.5;
        jdkparity.java.util.function.DoublePredicate myDouble = d -> d > 0.5;
        for (int n = -2; n < 12; n++) {
            assertEquals(theirEven.and(theirBig).test(n), myEven.and(myBig).test(n));
            assertEquals(theirEven.or(theirBig).test(n), myEven.or(myBig).test(n));
            assertEquals(theirEven.negate().test(n), myEven.negate().test(n));
            assertEquals(theirLong.and(x -> x < 9L).test(n), myLong.and(x -> x < 9L).test(n));
            assertEquals(theirLong.or(x -> x == 0L).test(n), myLong.or(x -> x == 0L).test(n));
            assertEquals(theirLong.negate().test(n), myLong.negate().test(n));
            double d = n / 8.0;
            assertEquals(theirDouble.and(x -> x < 1).test(d), myDouble.and(x -> x < 1).test(d));
            assertEquals(theirDouble.or(x -> x < 0).test(d), myDouble.or(x -> x < 0).test(d));
            assertEquals(theirDouble.negate().test(d), myDouble.negate().test(d));
        }
        java.util.function.BiPredicate<String, Integer> theirs = (s, n) -> s.length() > n;
        jdkparity.java.util.function.BiPredicate<String, Integer> mine = (s, n) -> s.length() > n;
        for (String s : Arrays.asList("", "a", "abc")) {
            for (int n = -1; n < 4; n++) {
                assertEquals(theirs.and((x, y) -> y > 0).test(s, n), mine.and((x, y) -> y > 0).test(s, n));
                assertEquals(theirs.or((x, y) -> y > 2).test(s, n), mine.or((x, y) -> y > 2).test(s, n));
                assertEquals(theirs.negate().test(s, n), mine.negate().test(s, n));
            }
        }
    }

    @Test
    void combinatorsAskTheSecondOnlyWhenTheyMust() {
        int[] asked = new int[1];
        jdkparity.java.util.function.IntPredicate counted = n -> {
            asked[0]++;
            return true;
        };
        jdkparity.java.util.function.IntPredicate never = n -> false;
        jdkparity.java.util.function.IntPredicate always = n -> true;
        never.and(counted).test(1);
        always.or(counted).test(1);
        assertEquals(0, asked[0]);
        never.or(counted).test(1);
        always.and(counted).test(1);
        assertEquals(2, asked[0]);
    }

    @Test
    void nullArgumentsAreRefusedWhenComposing() {
        jdkparity.java.util.function.IntPredicate p = n -> true;
        jdkparity.java.util.function.IntUnaryOperator op = n -> n;
        jdkparity.java.util.function.LongConsumer c = n -> { };
        jdkparity.java.util.function.BiPredicate<String, String> bp = (a, b) -> true;
        assertThrows(NullPointerException.class, () -> p.and(null));
        assertThrows(NullPointerException.class, () -> p.or(null));
        assertThrows(NullPointerException.class, () -> op.andThen(null));
        assertThrows(NullPointerException.class, () -> op.compose(null));
        assertThrows(NullPointerException.class, () -> c.andThen(null));
        assertThrows(NullPointerException.class, () -> bp.and(null));
    }

    @Test
    void operators() {
        java.util.function.IntUnaryOperator theirTwice = n -> n * 2;
        jdkparity.java.util.function.IntUnaryOperator myTwice = n -> n * 2;
        java.util.function.LongUnaryOperator theirLong = n -> n * 3;
        jdkparity.java.util.function.LongUnaryOperator myLong = n -> n * 3;
        java.util.function.DoubleUnaryOperator theirDouble = d -> d / 2;
        jdkparity.java.util.function.DoubleUnaryOperator myDouble = d -> d / 2;
        for (int n = -3; n < 4; n++) {
            assertEquals(theirTwice.andThen(x -> x + 3).applyAsInt(n), myTwice.andThen(x -> x + 3).applyAsInt(n));
            assertEquals(theirTwice.compose(x -> x + 3).applyAsInt(n), myTwice.compose(x -> x + 3).applyAsInt(n));
            assertEquals(java.util.function.IntUnaryOperator.identity().applyAsInt(n),
                    jdkparity.java.util.function.IntUnaryOperator.identity().applyAsInt(n));
            assertEquals(theirLong.andThen(x -> x - 1).applyAsLong(n), myLong.andThen(x -> x - 1).applyAsLong(n));
            assertEquals(theirLong.compose(x -> x - 1).applyAsLong(n), myLong.compose(x -> x - 1).applyAsLong(n));
            assertEquals(java.util.function.LongUnaryOperator.identity().applyAsLong(n),
                    jdkparity.java.util.function.LongUnaryOperator.identity().applyAsLong(n));
            assertEquals(theirDouble.andThen(x -> x + 1).applyAsDouble(n),
                    myDouble.andThen(x -> x + 1).applyAsDouble(n), 0.0);
            assertEquals(theirDouble.compose(x -> x + 1).applyAsDouble(n),
                    myDouble.compose(x -> x + 1).applyAsDouble(n), 0.0);
            assertEquals(java.util.function.DoubleUnaryOperator.identity().applyAsDouble(n),
                    jdkparity.java.util.function.DoubleUnaryOperator.identity().applyAsDouble(n), 0.0);
        }
    }

    @Test
    void consumersRunInOrderAndStopAtAFailure() {
        List<Object> theirs = new ArrayList<Object>();
        List<Object> mine = new ArrayList<Object>();
        java.util.function.IntConsumer ti = theirs::add;
        jdkparity.java.util.function.IntConsumer mi = mine::add;
        ti.andThen(n -> theirs.add(-n)).accept(4);
        mi.andThen(n -> mine.add(-n)).accept(4);
        java.util.function.LongConsumer tl = theirs::add;
        jdkparity.java.util.function.LongConsumer ml = mine::add;
        tl.andThen(n -> theirs.add(n * 2)).accept(5L);
        ml.andThen(n -> mine.add(n * 2)).accept(5L);
        java.util.function.DoubleConsumer td = theirs::add;
        jdkparity.java.util.function.DoubleConsumer md = mine::add;
        td.andThen(n -> theirs.add(n / 2)).accept(3.0);
        md.andThen(n -> mine.add(n / 2)).accept(3.0);
        assertEquals(theirs, mine);

        jdkparity.java.util.function.IntConsumer failing = n -> {
            throw new IllegalStateException();
        };
        assertThrows(IllegalStateException.class, () -> failing.andThen(n -> mine.add("ran")).accept(1));
        assertEquals(theirs, mine);
    }

    @Test
    void theRestAreTheirOneMethod() {
        jdkparity.java.util.function.IntBinaryOperator ib = (a, b) -> a * b;
        jdkparity.java.util.function.LongBinaryOperator lb = (a, b) -> a - b;
        jdkparity.java.util.function.DoubleBinaryOperator db = (a, b) -> a / b;
        jdkparity.java.util.function.IntFunction<String> ifn = x -> "#" + x;
        jdkparity.java.util.function.LongFunction<String> lfn = x -> "L" + x;
        jdkparity.java.util.function.DoubleFunction<String> dfn = x -> "D" + x;
        jdkparity.java.util.function.IntToLongFunction itl = x -> x * 1000000000L;
        jdkparity.java.util.function.IntToDoubleFunction itd = x -> x / 4.0;
        jdkparity.java.util.function.LongToIntFunction lti = x -> (int) (x >> 32);
        jdkparity.java.util.function.LongToDoubleFunction ltd = x -> x / 8.0;
        jdkparity.java.util.function.DoubleToIntFunction dti = x -> (int) Math.floor(x);
        jdkparity.java.util.function.DoubleToLongFunction dtl = Math::round;
        jdkparity.java.util.function.ToIntFunction<String> tif = String::length;
        jdkparity.java.util.function.ToLongFunction<String> tlf = s -> s.length() * 10L;
        jdkparity.java.util.function.ToDoubleFunction<String> tdf = s -> s.length() / 2.0;
        jdkparity.java.util.function.ToIntBiFunction<String, String> tibf = (a, b) -> a.length() + b.length();
        jdkparity.java.util.function.ToLongBiFunction<String, String> tlbf = (a, b) -> a.length() * (long) b.length();
        jdkparity.java.util.function.ToDoubleBiFunction<String, String> tdbf = (a, b) -> a.length() / (double) b.length();
        jdkparity.java.util.function.IntSupplier is = () -> 42;
        jdkparity.java.util.function.LongSupplier ls = () -> 43L;
        jdkparity.java.util.function.DoubleSupplier ds = () -> 4.5;
        jdkparity.java.util.function.BooleanSupplier bs = () -> true;
        List<Object> seen = new ArrayList<Object>();
        jdkparity.java.util.function.ObjIntConsumer<String> oic = (s, x) -> seen.add(s + x);
        jdkparity.java.util.function.ObjLongConsumer<String> olc = (s, x) -> seen.add(s + x);
        jdkparity.java.util.function.ObjDoubleConsumer<String> odc = (s, x) -> seen.add(s + x);
        oic.accept("a", 1);
        olc.accept("b", 2L);
        odc.accept("c", 3.5);
        assertEquals(42, ib.applyAsInt(6, 7));
        assertEquals(-1L, lb.applyAsLong(1, 2));
        assertEquals(0.25, db.applyAsDouble(1, 4), 0.0);
        assertEquals("#1L2D0.5", ifn.apply(1) + lfn.apply(2) + dfn.apply(0.5));
        assertEquals(3000000000L, itl.applyAsLong(3));
        assertEquals(0.75, itd.applyAsDouble(3), 0.0);
        assertEquals(2, lti.applyAsInt(1L << 33));
        assertEquals(0.375, ltd.applyAsDouble(3), 0.0);
        assertEquals(-2, dti.applyAsInt(-1.5));
        assertEquals(3L, dtl.applyAsLong(2.5));
        assertEquals(3, tif.applyAsInt("abc"));
        assertEquals(20L, tlf.applyAsLong("ab"));
        assertEquals(1.5, tdf.applyAsDouble("abc"), 0.0);
        assertEquals(3, tibf.applyAsInt("a", "bc"));
        assertEquals(6L, tlbf.applyAsLong("ab", "cde"));
        assertEquals(1 / 3.0, tdbf.applyAsDouble("a", "bcd"), 0.0);
        assertEquals("42434.5true", "" + is.getAsInt() + ls.getAsLong() + ds.getAsDouble() + bs.getAsBoolean());
        assertEquals(Arrays.<Object>asList("a1", "b2", "c3.5"), seen);
    }
}
