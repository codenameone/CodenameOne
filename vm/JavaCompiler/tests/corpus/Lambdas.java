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

public class Lambdas {
    private int field = 7;
    private String name = "L";

    static int twice(int x) {
        return x * 2;
    }

    String hello(String who) {
        return name + who;
    }

    static String join(String... parts) {
        return String.join("|", parts);
    }

    Supplier<String> capturesThis() {
        return () -> name + field;
    }

    Function<Integer, Integer> nested(int k) {
        return x -> {
            Function<Integer, Integer> inner = y -> y + k + field;
            return inner.apply(x) * 2;
        };
    }

    public static void main(String[] args) {
        Lambdas l = new Lambdas();
        int local = 5;
        IntBinaryOperator add = (a, b) -> a + b + local;
        System.out.println(add.applyAsInt(1, 2));
        Function<Integer, Integer> f = Lambdas::twice;
        System.out.println(f.apply(21));
        Function<String, String> bound = l::hello;
        System.out.println(bound.apply("x"));
        BiFunction<Lambdas, String, String> unbound = Lambdas::hello;
        System.out.println(unbound.apply(l, "y"));
        Supplier<List<String>> ctor = ArrayList::new;
        List<String> made = ctor.get();
        made.add("z");
        System.out.println(made);
        Function<Integer, int[]> arr = int[]::new;
        System.out.println(arr.apply(4).length);
        IntFunction<String[]> sarr = String[]::new;
        System.out.println(sarr.apply(2).length);
        Function<String, Integer> len = String::length;
        System.out.println(len.apply("four"));
        System.out.println(l.capturesThis().get());
        System.out.println(l.nested(3).apply(4));
        Comparator<String> byLen = Comparator.comparing(String::length);
        List<String> words = new ArrayList<>(List.of("ccc", "a", "bb"));
        words.sort(byLen.reversed());
        System.out.println(words);
        words.sort(Comparator.comparing((String s) -> s.charAt(0)).thenComparing(String::length));
        System.out.println(words);
        System.out.println(words.stream().map(String::toUpperCase).collect(Collectors.joining(",")));
        System.out.println(IntStream.rangeClosed(1, 5).map(x -> x * x).sum());
        Map<Integer, Long> lenCounts = Stream.of("a", "bb", "cc", "ddd").collect(Collectors.groupingBy(String::length, TreeMap::new, Collectors.counting()));
        System.out.println(lenCounts);
        Optional<String> first = words.stream().filter(w -> w.length() > 1).findFirst();
        System.out.println(first.orElse("none"));
        Runnable r = () -> System.out.println("runnable " + local);
        r.run();
        Function<String, String> vararg = Lambdas::join;
        System.out.println(vararg.apply("one"));
        BinaryOperator<String> vararg2 = Lambdas::join;
        System.out.println(vararg2.apply("a", "b"));
        Predicate<String> isEmpty = String::isEmpty;
        System.out.println(isEmpty.negate().test(""));
        UnaryOperator<String> up = s -> s + "!";
        System.out.println(up.andThen(up).apply("hey"));
        ToIntFunction<String> parse = Integer::parseInt;
        System.out.println(parse.applyAsInt("42") + 1);
        Supplier<Supplier<String>> ss = () -> () -> "deep";
        System.out.println(ss.get().get());
        Map<String, Integer> counts = new TreeMap<>();
        for (String w : new String[]{"x", "y", "x"}) {
            counts.merge(w, 1, Integer::sum);
        }
        System.out.println(counts);
        counts.forEach((k, v) -> System.out.println(k + "=" + v));
        List<Integer> nums = Arrays.asList(3, 1, 2);
        System.out.println(nums.stream().sorted().map(String::valueOf).collect(Collectors.toList()));
        double avg = nums.stream().mapToInt(Integer::intValue).average().orElse(0);
        System.out.println(avg);
        BiConsumer<String, Integer> printer = (s, i) -> System.out.println(s.repeat(i));
        printer.accept("ab", 3);
        Callable c = () -> "callable";
        try {
            System.out.println(c.call());
        } catch (Exception e) {
            System.out.println("err");
        }
    }

    interface Callable {
        Object call() throws Exception;
    }
}
