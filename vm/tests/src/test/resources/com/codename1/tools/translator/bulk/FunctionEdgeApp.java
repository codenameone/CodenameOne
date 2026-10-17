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
import java.util.List;
import java.util.function.*;

/// The default and static members of java.util.function and java.util.Comparator,
/// printed as one line that the JVM and ParparVM must agree on.
public class FunctionEdgeApp {
    private static final StringBuilder OUT = new StringBuilder();

    private static void out(Object label, Object value) {
        OUT.append(label).append('=').append(value).append(';');
    }

    static final class Person {
        final String name;
        final int age;
        final long id;
        final double score;

        Person(String name, int age, long id, double score) {
            this.name = name;
            this.age = age;
            this.id = id;
            this.score = score;
        }

        public String toString() {
            return name;
        }
    }

    public static void fixtureMain(String[] args) {
        Function<Integer, Integer> inc = x -> x + 1;
        Function<Integer, Integer> dbl = x -> x * 2;
        out("compose", inc.compose(dbl).apply(5));
        out("andThen", inc.andThen(dbl).apply(5));
        out("identity", Function.<String>identity().apply("same"));
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        out("biAndThen", add.andThen(dbl).apply(3, 4));
        BinaryOperator<String> min = BinaryOperator.minBy(Comparator.<String>naturalOrder());
        BinaryOperator<String> max = BinaryOperator.maxBy(Comparator.<String>naturalOrder());
        out("minBy", min.apply("b", "a") + min.apply("a", "a"));
        out("maxBy", max.apply("b", "a") + max.apply("a", "b"));
        BiFunction<String, String, String> asBi = min;
        out("binaryIsBi", asBi.apply("z", "y"));
        out("unaryIdentity", UnaryOperator.<String>identity().apply("u"));

        final StringBuilder trace = new StringBuilder();
        Consumer<String> first = s -> trace.append("1").append(s);
        Consumer<String> second = s -> trace.append("2").append(s);
        first.andThen(second).accept("x");
        BiConsumer<String, Integer> b1 = (s, i) -> trace.append("[").append(s).append(i);
        b1.andThen((s, i) -> trace.append(i).append(s).append("]")).accept("k", 9);
        out("consumers", trace);
        try {
            first.andThen(null);
            out("andThenNull", "accepted");
        } catch (NullPointerException e) {
            out("andThenNull", "npe");
        }

        Predicate<Integer> even = x -> x % 2 == 0;
        Predicate<Integer> big = x -> x > 10;
        out("and", even.and(big).test(12) + "," + even.and(big).test(8));
        out("or", even.or(big).test(11) + "," + even.or(big).test(7));
        out("negate", even.negate().test(3));
        out("not", Predicate.not(even).test(4));
        out("isEqual", Predicate.isEqual("a").test("a") + "," + Predicate.isEqual(null).test(null)
                + "," + Predicate.isEqual(null).test("x"));
        final int[] asked = new int[1];
        Predicate<Integer> counting = x -> {
            asked[0]++;
            return true;
        };
        even.and(counting).test(3);
        even.or(counting).test(4);
        out("shortCircuit", asked[0]);
        BiPredicate<String, Integer> longer = (s, n) -> s.length() > n;
        out("biPredicate", longer.test("abc", 2) + "," + longer.negate().test("abc", 2) + ","
                + longer.and((s, n) -> n > 0).test("abc", 0) + "," + longer.or((s, n) -> n == 5).test("a", 5));

        IntUnaryOperator i1 = x -> x + 3;
        out("intUnary", i1.andThen(x -> x * 10).applyAsInt(1) + "," + i1.compose(x -> x * 10).applyAsInt(1)
                + "," + IntUnaryOperator.identity().applyAsInt(7));
        LongUnaryOperator l1 = x -> x - 1;
        out("longUnary", l1.andThen(x -> x * 3L).applyAsLong(Long.MAX_VALUE / 4) + ","
                + LongUnaryOperator.identity().applyAsLong(-5L) + "," + l1.compose(x -> x * 2).applyAsLong(4));
        DoubleUnaryOperator d1 = x -> x / 2;
        out("doubleUnary", d1.andThen(x -> x + 0.25).applyAsDouble(3) + "," + d1.compose(x -> x * 4).applyAsDouble(1)
                + "," + DoubleUnaryOperator.identity().applyAsDouble(-0.0));
        IntPredicate ip = x -> x > 0;
        LongPredicate lp = x -> x > 0;
        DoublePredicate dp = x -> x > 0;
        out("primPredicates", ip.and(x -> x < 5).test(3) + "," + ip.or(x -> x == -1).test(-1) + "," + ip.negate().test(1)
                + "," + lp.and(x -> x < 5).test(9) + "," + lp.or(x -> x == -1).test(-1) + "," + lp.negate().test(0)
                + "," + dp.and(x -> x < 5).test(4.5) + "," + dp.or(x -> Double.isNaN(x)).test(Double.NaN) + ","
                + dp.negate().test(Double.NaN));
        final StringBuilder prim = new StringBuilder();
        IntConsumer ic = x -> prim.append('i').append(x);
        LongConsumer lc = x -> prim.append('l').append(x);
        DoubleConsumer dc = x -> prim.append('d').append(x);
        ic.andThen(x -> prim.append('I')).accept(1);
        lc.andThen(x -> prim.append('L')).accept(2L);
        dc.andThen(x -> prim.append('D')).accept(2.5);
        out("primConsumers", prim);
        IntBinaryOperator ib = (a, b) -> a * b;
        LongBinaryOperator lb = (a, b) -> a - b;
        DoubleBinaryOperator db = (a, b) -> a / b;
        IntFunction<String> ifn = x -> "#" + x;
        LongFunction<String> lfn = x -> "L" + x;
        DoubleFunction<String> dfn = x -> "D" + x;
        IntToLongFunction itl = x -> x * 1000000000L;
        IntToDoubleFunction itd = x -> x / 4.0;
        LongToIntFunction lti = x -> (int) (x >> 32);
        LongToDoubleFunction ltd = x -> x / 8.0;
        DoubleToIntFunction dti = x -> (int) Math.floor(x);
        DoubleToLongFunction dtl = x -> Math.round(x);
        ToIntFunction<String> tif = String::length;
        ToLongFunction<String> tlf = s -> s.length() * 10L;
        ToDoubleFunction<String> tdf = s -> s.length() / 2.0;
        ToIntBiFunction<String, String> tibf = (a, b) -> a.length() + b.length();
        ToLongBiFunction<String, String> tlbf = (a, b) -> a.length() * (long) b.length();
        ToDoubleBiFunction<String, String> tdbf = (a, b) -> a.length() / (double) b.length();
        IntSupplier is = () -> 42;
        LongSupplier ls = () -> 43L;
        DoubleSupplier ds = () -> 4.5;
        BooleanSupplier bs = () -> true;
        final StringBuilder objPrim = new StringBuilder();
        ObjIntConsumer<String> oic = (s, x) -> objPrim.append(s).append(x);
        ObjLongConsumer<String> olc = (s, x) -> objPrim.append(s).append(x);
        ObjDoubleConsumer<String> odc = (s, x) -> objPrim.append(s).append(x);
        oic.accept("a", 1);
        olc.accept("b", 2L);
        odc.accept("c", 3.5);
        out("plain", ib.applyAsInt(6, 7) + "," + lb.applyAsLong(1, 2) + "," + db.applyAsDouble(1, 4) + ","
                + ifn.apply(1) + lfn.apply(2) + dfn.apply(0.5) + "," + itl.applyAsLong(3) + "," + itd.applyAsDouble(3)
                + "," + lti.applyAsInt(1L << 33) + "," + ltd.applyAsDouble(3) + "," + dti.applyAsInt(-1.5) + ","
                + dtl.applyAsLong(2.5) + "," + tif.applyAsInt("abc") + "," + tlf.applyAsLong("ab") + ","
                + tdf.applyAsDouble("abc") + "," + tibf.applyAsInt("a", "bc") + "," + tlbf.applyAsLong("ab", "cde")
                + "," + tdbf.applyAsDouble("a", "bcd") + "," + is.getAsInt() + ls.getAsLong() + ds.getAsDouble()
                + bs.getAsBoolean() + "," + objPrim);

        List<Person> people = new ArrayList<Person>(Arrays.asList(
                new Person("cat", 30, 5L, 1.5), new Person("ann", 25, 9L, Double.NaN),
                new Person("bob", 30, 1L, -0.0), new Person("dan", 25, 9L, 0.0)));
        List<Person> sorted = new ArrayList<Person>(people);
        Collections.sort(sorted, Comparator.comparingInt((Person p) -> p.age).thenComparing(p -> p.name));
        out("byAgeName", sorted);
        Collections.sort(sorted, Comparator.comparingLong((Person p) -> p.id).thenComparingInt(p -> p.age)
                .thenComparing(p -> p.name, Comparator.<String>reverseOrder()));
        out("byIdAgeNameDesc", sorted);
        Collections.sort(sorted, Comparator.comparingDouble((Person p) -> p.score));
        out("byScore", sorted);
        Collections.sort(sorted, Comparator.comparing((Person p) -> p.name).reversed());
        out("byNameDesc", sorted);
        Collections.sort(sorted, Comparator.comparing((Person p) -> p.age, Comparator.<Integer>reverseOrder())
                .thenComparingLong(p -> p.id).thenComparingDouble(p -> p.score));
        out("byAgeDescIdScore", sorted);

        List<String> withNulls = new ArrayList<String>(Arrays.asList("b", null, "a", null, "c"));
        Collections.sort(withNulls, Comparator.nullsFirst(Comparator.<String>naturalOrder()));
        out("nullsFirst", withNulls);
        Collections.sort(withNulls, Comparator.nullsLast(Comparator.<String>naturalOrder()));
        out("nullsLast", withNulls);
        Collections.sort(withNulls, Comparator.nullsFirst(Comparator.<String>naturalOrder()).reversed());
        out("nullsFirstReversed", withNulls);
        Collections.sort(withNulls, Comparator.<String>nullsLast(null));
        out("nullsLastNoComparator", withNulls);
        Collections.sort(withNulls, Comparator.nullsLast(Comparator.<String>naturalOrder())
                .thenComparing(Comparator.<String>reverseOrder()));
        out("nullsLastThen", withNulls);
        try {
            Comparator.comparingInt(null);
            out("comparingIntNull", "accepted");
        } catch (NullPointerException e) {
            out("comparingIntNull", "npe");
        }
        System.out.println("RESULT=" + OUT);
    }
}
