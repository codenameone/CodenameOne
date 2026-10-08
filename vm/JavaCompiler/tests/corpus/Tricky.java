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

public class Tricky {
    static abstract class Animal implements Comparable<Animal> {
        abstract int rank();

        public int compareTo(Animal o) {
            return Integer.compare(rank(), o.rank());
        }
    }

    static class Cat extends Animal implements Cloneable {
        int lives = 9;

        int rank() {
            return lives;
        }

        @Override
        public Cat clone() {
            try {
                return (Cat) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    static abstract class Transformer<A, B> {
        abstract B apply(A a);

        B twice(A a, Function<B, A> back) {
            return apply(back.apply(apply(a)));
        }
    }

    static class Lengther extends Transformer<String, Integer> {
        Integer apply(String s) {
            return s.length();
        }
    }

    interface Shape {
        double area();

        default String describe() {
            return getClass().getSimpleName() + String.format("(%.1f)", area());
        }
    }

    static String sw(Object o) {
        return switch (o) {
            case Integer i when i > 0 -> {
                String r = "";
                for (int k = 0; k < i; k++) {
                    r += k;
                }
                yield r;
            }
            case Integer i -> "nonpos";
            default -> {
                try {
                    yield "len" + o.toString().length();
                } finally {
                    System.out.print("[fin]");
                }
            }
        };
    }

    static int hashCollide(String s) {
        switch (s) {
            case "Aa":
                return 1;
            case "BB":
                return 2;
            default:
                return 0;
        }
    }

    static int sumWithTry(int[] xs) {
        int s = 0;
        for (int x : xs) {
            try {
                if (x < 0) {
                    throw new IllegalArgumentException();
                }
                s += x;
            } catch (IllegalArgumentException e) {
                s -= 100;
                continue;
            } finally {
                s += 1000;
            }
            s++;
        }
        return s;
    }

    static String finallyInLoop() {
        StringBuilder b = new StringBuilder();
        outer:
        while (true) {
            for (int i = 0; ; i++) {
                try {
                    try {
                        if (i == 2) {
                            break outer;
                        }
                        b.append(i);
                    } finally {
                        b.append('f');
                    }
                } finally {
                    b.append('F');
                }
            }
        }
        return b.toString();
    }

    public static void main(String[] args) {
        List<Animal> cats = new ArrayList<>();
        Cat a = new Cat();
        Cat b = a.clone();
        b.lives = 3;
        cats.add(a);
        cats.add(b);
        Collections.sort(cats);
        System.out.println(((Cat) cats.get(0)).lives + " " + (a != b));
        Transformer<String, Integer> t = new Lengther();
        System.out.println(t.apply("hello") + " " + t.twice("abc", n -> "x".repeat(n * 2)));
        Shape sq = () -> 4.0;
        System.out.println(sq.area() + " " + sq.describe().endsWith("(4.0)"));
        System.out.println(sw(3) + " " + sw(-1) + " " + sw("abcd"));
        System.out.println(hashCollide("Aa") + "" + hashCollide("BB") + hashCollide("CC"));
        System.out.println(sumWithTry(new int[]{1, -1, 2}));
        System.out.println(finallyInLoop());
        boolean cond = args.length == 0;
        char ch = cond ? 'a' : 0;
        int ci = cond ? 1 : 'a';
        System.out.println(ch + " " + ci + " " + (cond ? 1 : 2.0) + " " + (cond ? 'x' : "str"));
        Integer nul = null;
        try {
            int bad = cond ? nul : 0;
            System.out.println(bad);
        } catch (NullPointerException e) {
            System.out.println("unboxing NPE");
        }
        Integer i1 = 1000;
        int i2 = 1000;
        System.out.println(i1 == i2);
        Object[] objs = new String[1];
        try {
            objs[0] = 1;
        } catch (ArrayStoreException e) {
            System.out.println("ASE");
        }
        int[] orig = {1, 2, 3};
        int[] copy = orig.clone();
        copy[0] = 9;
        System.out.println(orig[0] + "" + copy[0] + copy.length);
        Object ol = new ArrayList<>(List.of(1, 2));
        if (ol instanceof List<?> lst && !lst.isEmpty()) {
            System.out.println("list of " + lst.size());
        }
        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        nums.removeIf(x -> x % 2 == 0);
        Iterator<Integer> it = nums.iterator();
        while (it.hasNext()) {
            if (it.next() == 3) {
                it.remove();
            }
        }
        System.out.println(nums);
        long big = Integer.MAX_VALUE;
        big += 1;
        System.out.println(big + " " + (big > Integer.MAX_VALUE) + " " + Long.compare(big, 5L));
        float f = 0.1f;
        double d = f;
        System.out.println(d != 0.1 ? "diff" : "same");
        String tb = """
            a\
            b
            c\sd
            "quoted" \"""
            """;
        System.out.print(tb);
        StringBuilder sb = new StringBuilder("x");
        sb.append(1).append('c').append(2.0f).append(true).append((Object) null).append(new char[]{'z'});
        System.out.println(sb);
        String s = "s";
        s += 'c';
        s += 1 + 2;
        s += null;
        System.out.println(s);
        char c = 'a';
        c *= 1;
        c += 1.7;
        System.out.println(c);
        System.out.println(Objects.requireNonNullElse(null, "default"));
        int x = 0;
        x += x++ + ++x;
        System.out.println(x);
        System.out.println(010 + 0x10 + 0b10 + 1_000);
        System.out.println('A' + "\t|" + "\u00e9".length());
    }
}
