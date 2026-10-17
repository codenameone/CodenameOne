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
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;

/// The defaults of the primitive java.util.function interfaces, and the
/// single method of the rest.
public final class PrimitiveFunctionChecks {
    private PrimitiveFunctionChecks() {
    }

    private static char b(boolean v) {
        return v ? 'T' : 'F';
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("PrimitiveFunction.predicates", () -> {
            IntPredicate even = n -> n % 2 == 0;
            IntPredicate big = n -> n > 5;
            LongPredicate longP = n -> n > 5L;
            DoublePredicate doubleP = d -> d > 0.5;
            StringBuilder sb = new StringBuilder();
            for (int n = -2; n < 12; n++) {
                double d = n / 8.0;
                sb.append(n).append(':')
                        .append(b(even.and(big).test(n))).append(b(even.or(big).test(n)))
                        .append(b(even.negate().test(n)))
                        .append(b(longP.and(x -> x < 9L).test(n))).append(b(longP.or(x -> x == 0L).test(n)))
                        .append(b(longP.negate().test(n)))
                        .append(b(doubleP.and(x -> x < 1).test(d))).append(b(doubleP.or(x -> x < 0).test(d)))
                        .append(b(doubleP.negate().test(d))).append(' ');
            }
            Bulk.p("primitive", sb.toString());
            BiPredicate<String, Integer> longer = (s, n) -> s.length() > n;
            StringBuilder bi = new StringBuilder();
            for (String s : Arrays.asList("", "a", "abc")) {
                for (int n = -1; n < 4; n++) {
                    bi.append(s).append(n).append(':')
                            .append(b(longer.and((x, y) -> y > 0).test(s, n)))
                            .append(b(longer.or((x, y) -> y > 2).test(s, n)))
                            .append(b(longer.negate().test(s, n))).append(' ');
                }
            }
            Bulk.p("bi", bi.toString());
        });
        Bulk.check("PrimitiveFunction.combinatorsAskTheSecondOnlyWhenTheyMust", () -> {
            int[] asked = new int[1];
            IntPredicate counted = n -> {
                asked[0]++;
                return true;
            };
            IntPredicate never = n -> false;
            IntPredicate always = n -> true;
            never.and(counted).test(1);
            always.or(counted).test(1);
            Bulk.p("shortCircuited", asked[0]);
            never.or(counted).test(1);
            always.and(counted).test(1);
            Bulk.p("asked", asked[0]);
        });
        Bulk.check("PrimitiveFunction.nullArgumentsAreRefusedWhenComposing", () -> {
            IntPredicate p = n -> true;
            IntUnaryOperator op = n -> n;
            LongConsumer c = n -> { };
            BiPredicate<String, String> bp = (x, y) -> true;
            Bulk.pThrown("and", NullPointerException.class, () -> p.and(null));
            Bulk.pThrown("or", NullPointerException.class, () -> p.or(null));
            Bulk.pThrown("andThen", NullPointerException.class, () -> op.andThen(null));
            Bulk.pThrown("compose", NullPointerException.class, () -> op.compose(null));
            Bulk.pThrown("consumerAndThen", NullPointerException.class, () -> c.andThen(null));
            Bulk.pThrown("biAnd", NullPointerException.class, () -> bp.and(null));
        });
        Bulk.check("PrimitiveFunction.operators", () -> {
            IntUnaryOperator twice = n -> n * 2;
            LongUnaryOperator thrice = n -> n * 3;
            DoubleUnaryOperator half = d -> d / 2;
            StringBuilder sb = new StringBuilder();
            for (int n = -3; n < 4; n++) {
                sb.append(n).append(':')
                        .append(twice.andThen(x -> x + 3).applyAsInt(n)).append(',')
                        .append(twice.compose(x -> x + 3).applyAsInt(n)).append(',')
                        .append(IntUnaryOperator.identity().applyAsInt(n)).append(',')
                        .append(thrice.andThen(x -> x - 1).applyAsLong(n)).append(',')
                        .append(thrice.compose(x -> x - 1).applyAsLong(n)).append(',')
                        .append(LongUnaryOperator.identity().applyAsLong(n)).append(',')
                        .append(half.andThen(x -> x + 1).applyAsDouble(n)).append(',')
                        .append(half.compose(x -> x + 1).applyAsDouble(n)).append(',')
                        .append(DoubleUnaryOperator.identity().applyAsDouble(n)).append(' ');
            }
            Bulk.p("operators", sb.toString());
        });
        Bulk.check("PrimitiveFunction.consumersRunInOrderAndStopAtAFailure", () -> {
            List<Object> seen = new ArrayList<Object>();
            IntConsumer ic = seen::add;
            ic.andThen(n -> seen.add(-n)).accept(4);
            LongConsumer lc = seen::add;
            lc.andThen(n -> seen.add(n * 2)).accept(5L);
            DoubleConsumer dc = seen::add;
            dc.andThen(n -> seen.add(n / 2)).accept(3.0);
            Bulk.p("seen", seen);
            IntConsumer failing = n -> {
                throw new IllegalStateException();
            };
            Bulk.pThrown("failing", IllegalStateException.class, () -> failing.andThen(n -> seen.add("ran")).accept(1));
            Bulk.p("seenAfter", seen);
        });
        Bulk.check("PrimitiveFunction.theRestAreTheirOneMethod", () -> {
            IntBinaryOperator ib = (x, y) -> x * y;
            LongBinaryOperator lb = (x, y) -> x - y;
            DoubleBinaryOperator db = (x, y) -> x / y;
            IntFunction<String> ifn = x -> "#" + x;
            LongFunction<String> lfn = x -> "L" + x;
            DoubleFunction<String> dfn = x -> "D" + x;
            IntToLongFunction itl = x -> x * 1000000000L;
            IntToDoubleFunction itd = x -> x / 4.0;
            LongToIntFunction lti = x -> (int) (x >> 32);
            LongToDoubleFunction ltd = x -> x / 8.0;
            DoubleToIntFunction dti = x -> (int) Math.floor(x);
            DoubleToLongFunction dtl = Math::round;
            ToIntFunction<String> tif = String::length;
            ToLongFunction<String> tlf = s -> s.length() * 10L;
            ToDoubleFunction<String> tdf = s -> s.length() / 2.0;
            ToIntBiFunction<String, String> tibf = (x, y) -> x.length() + y.length();
            ToLongBiFunction<String, String> tlbf = (x, y) -> x.length() * (long) y.length();
            ToDoubleBiFunction<String, String> tdbf = (x, y) -> x.length() / (double) y.length();
            IntSupplier is = () -> 42;
            LongSupplier ls = () -> 43L;
            DoubleSupplier ds = () -> 4.5;
            BooleanSupplier bs = () -> true;
            List<Object> seen = new ArrayList<Object>();
            ObjIntConsumer<String> oic = (s, x) -> seen.add(s + x);
            ObjLongConsumer<String> olc = (s, x) -> seen.add(s + x);
            ObjDoubleConsumer<String> odc = (s, x) -> seen.add(s + x);
            oic.accept("a", 1);
            olc.accept("b", 2L);
            odc.accept("c", 3.5);
            Bulk.p("intBinary", ib.applyAsInt(6, 7));
            Bulk.p("longBinary", lb.applyAsLong(1, 2));
            Bulk.p("doubleBinary", db.applyAsDouble(1, 4));
            Bulk.p("functions", ifn.apply(1) + lfn.apply(2) + dfn.apply(0.5));
            Bulk.p("intToLong", itl.applyAsLong(3));
            Bulk.p("intToDouble", itd.applyAsDouble(3));
            Bulk.p("longToInt", lti.applyAsInt(1L << 33));
            Bulk.p("longToDouble", ltd.applyAsDouble(3));
            Bulk.p("doubleToInt", dti.applyAsInt(-1.5));
            Bulk.p("doubleToLong", dtl.applyAsLong(2.5));
            Bulk.p("toInt", tif.applyAsInt("abc"));
            Bulk.p("toLong", tlf.applyAsLong("ab"));
            Bulk.p("toDouble", tdf.applyAsDouble("abc"));
            Bulk.p("toIntBi", tibf.applyAsInt("a", "bc"));
            Bulk.p("toLongBi", tlbf.applyAsLong("ab", "cde"));
            Bulk.p("toDoubleBi", tdbf.applyAsDouble("a", "bcd"));
            Bulk.p("suppliers", "" + is.getAsInt() + ls.getAsLong() + ds.getAsDouble() + bs.getAsBoolean());
            Bulk.p("objConsumers", seen);
        });
    }
}
