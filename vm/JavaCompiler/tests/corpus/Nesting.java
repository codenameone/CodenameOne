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
import static java.lang.Math.max;
import static java.util.Collections.*;

public class Nesting<T> {
    private T item;
    private static int secret = 42;
    private int privateField = 1;

    Nesting(T item) {
        this.item = item;
    }

    private String privateMethod() {
        return "pm" + privateField;
    }

    class Holder {
        private int innerPrivate = 9;

        T get() {
            return item;
        }

        Supplier<String> viaLambda() {
            return () -> {
                Runnable r = new Runnable() {
                    public void run() {
                        privateField += innerPrivate;
                    }
                };
                r.run();
                return privateMethod() + item;
            };
        }
    }

    static class Base {
        String who() {
            return "base";
        }
    }

    class InnerBase {
        String tag = "ib" + privateField;
    }

    class InnerSub extends InnerBase {
        String both() {
            return tag + secret;
        }
    }

    static class Outsider extends Nesting<String>.InnerBase {
        Outsider(Nesting<String> n) {
            n.super();
        }
    }

    int readInner(Holder h) {
        return h.innerPrivate;
    }

    List<Supplier<Integer>> captureLoop() {
        List<Supplier<Integer>> out = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final int copy = i;
            out.add(() -> copy * 10);
        }
        for (String s : List.of("a", "bb")) {
            out.add(() -> s.length());
        }
        return out;
    }

    static class Sub extends Base {
        String who() {
            return "sub";
        }

        Supplier<String> sup() {
            return super::who;
        }

        Supplier<String> mine() {
            return this::who;
        }
    }

    static Function<Integer, Integer> fib;

    static {
        fib = n -> n < 2 ? n : fib.apply(n - 1) + fib.apply(n - 2);
    }

    final Runnable fieldLambda = () -> System.out.println("field lambda " + item);

    public static void main(String[] args) {
        Nesting<String> n = new Nesting<>("X");
        Nesting<String>.Holder h = n.new Holder();
        System.out.println(h.get() + h.viaLambda().get() + n.privateField + n.readInner(h));
        System.out.println(n.new InnerSub().both());
        System.out.println(new Outsider(n).tag);
        for (Supplier<Integer> s : n.captureLoop()) {
            System.out.print(s.get() + " ");
        }
        System.out.println();
        Sub sub = new Sub();
        System.out.println(sub.sup().get() + " " + sub.mine().get());
        System.out.println(fib.apply(15));
        n.fieldLambda.run();
        System.out.println(max(3, 9) + " " + emptyList().size());
        List<String> sorted = new ArrayList<>(List.of("b", "c", "a"));
        sort(sorted);
        System.out.println(sorted + " " + Collections.<String>emptyList());
        int base = 5;
        class Counter {
            int count;

            Supplier<Integer> next() {
                return () -> ++count + base;
            }
        }
        Counter c = new Counter();
        Supplier<Integer> next = c.next();
        next.get();
        System.out.println(next.get());
        Function<Integer, Counter> mk = x -> {
            Counter k = new Counter();
            k.count = x;
            return k;
        };
        System.out.println(mk.apply(10).next().get());
        Object o = new Object() {
            int hidden = 3;

            @Override
            public String toString() {
                return "anon" + hidden + secret;
            }
        };
        System.out.println(o);
        var anon = new Object() {
            int field = 7;

            int twice() {
                return field * 2;
            }
        };
        System.out.println(anon.field + anon.twice());
        Runnable[] tasks = new Runnable[3];
        for (int i = 0; i < tasks.length; i++) {
            int id = i;
            tasks[i] = () -> System.out.print("t" + id);
        }
        for (Runnable t : tasks) {
            t.run();
        }
        System.out.println();
        Runnable both = (Runnable & java.io.Serializable) () -> System.out.println("intersection");
        both.run();
        BiFunction<Integer, Integer, Integer> m = Math::max;
        System.out.println(m.apply(4, 8));
        Function<String[], List<String>> asList = Arrays::asList;
        System.out.println(asList.apply(new String[]{"x", "y"}));
    }
}
