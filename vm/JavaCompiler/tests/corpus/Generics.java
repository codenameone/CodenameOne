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

public class Generics {
    static class Box<T extends Comparable<T>> implements Comparable<Box<T>> {
        private final T value;

        Box(T value) {
            this.value = value;
        }

        T get() {
            return value;
        }

        <R extends Comparable<R>> Box<R> map(Function<? super T, ? extends R> f) {
            return new Box<>(f.apply(value));
        }

        public int compareTo(Box<T> o) {
            return value.compareTo(o.value);
        }

        public String toString() {
            return "Box[" + value + "]";
        }
    }

    static class Pair<A, B> {
        final A first;
        final B second;

        Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        static <A, B> Pair<A, B> of(A a, B b) {
            return new Pair<>(a, b);
        }

        <C> Pair<A, C> withSecond(C c) {
            return new Pair<>(first, c);
        }

        public String toString() {
            return "(" + first + ", " + second + ")";
        }
    }

    static <T extends Comparable<? super T>> T max(Collection<? extends T> items) {
        Iterator<? extends T> it = items.iterator();
        T best = it.next();
        while (it.hasNext()) {
            T n = it.next();
            if (n.compareTo(best) > 0) {
                best = n;
            }
        }
        return best;
    }

    static double sum(List<? extends Number> nums) {
        double s = 0;
        for (Number n : nums) {
            s += n.doubleValue();
        }
        return s;
    }

    static void fill(List<? super Integer> out, int n) {
        for (int i = 0; i < n; i++) {
            out.add(i);
        }
    }

    interface Visitor<R> {
        R visitNum(int n);

        R visitAdd(Node a, Node b);
    }

    interface Node {
        <R> R accept(Visitor<R> v);
    }

    static Node num(int n) {
        return new Node() {
            public <R> R accept(Visitor<R> v) {
                return v.visitNum(n);
            }
        };
    }

    static Node add(Node a, Node b) {
        return new Node() {
            public <R> R accept(Visitor<R> v) {
                return v.visitAdd(a, b);
            }
        };
    }

    public static void main(String[] args) {
        Box<Integer> b = new Box<>(41);
        Box<String> s = b.map(x -> "v" + (x + 1));
        System.out.println(b + " " + s + " " + s.get().length());
        List<Box<Integer>> boxes = new ArrayList<>();
        boxes.add(new Box<>(3));
        boxes.add(new Box<>(1));
        boxes.add(new Box<>(2));
        Collections.sort(boxes);
        System.out.println(boxes);
        System.out.println(max(Arrays.asList(4, 9, 2)));
        System.out.println(max(List.of("pear", "apple", "zoo")));
        System.out.println(sum(List.of(1, 2.5, 3L)));
        List<Number> out = new ArrayList<>();
        fill(out, 3);
        System.out.println(out);
        Pair<String, Integer> p = Pair.of("a", 1);
        Pair<String, List<String>> q = p.withSecond(new ArrayList<>());
        q.second.add("x");
        System.out.println(p + " " + q);
        Map<String, List<Integer>> multi = new HashMap<>();
        multi.computeIfAbsent("k", k -> new ArrayList<>()).add(5);
        multi.computeIfAbsent("k", k -> new ArrayList<>()).add(6);
        System.out.println(multi);
        Node expr = add(num(2), add(num(3), num(4)));
        int value = expr.accept(new Visitor<Integer>() {
            public Integer visitNum(int n) {
                return n;
            }

            public Integer visitAdd(Node a, Node c) {
                return a.accept(this) + c.accept(this);
            }
        });
        String shown = expr.accept(new Visitor<String>() {
            public String visitNum(int n) {
                return Integer.toString(n);
            }

            public String visitAdd(Node a, Node c) {
                return "(" + a.accept(this) + "+" + c.accept(this) + ")";
            }
        });
        System.out.println(shown + "=" + value);
        List<? extends CharSequence> seqs = List.of("ab", new StringBuilder("cde"));
        int total = 0;
        for (CharSequence cs : seqs) {
            total += cs.length();
        }
        System.out.println(total);
        Optional<Integer> o = Optional.of(5).map(x -> x * 3).filter(x -> x > 10);
        System.out.println(o.isPresent() + " " + o.get());
        Function<Integer, Function<Integer, Integer>> curried = x -> y -> x * y;
        System.out.println(curried.apply(6).apply(7));
        List<String> names = new ArrayList<>(List.of("b", "a"));
        Collections.sort(names, Collections.reverseOrder());
        System.out.println(names);
        Set<Integer> set = new TreeSet<>(Comparator.reverseOrder());
        set.addAll(List.of(1, 5, 3));
        System.out.println(set);
        Integer[] arr = {3, 1, 2};
        Arrays.sort(arr, (x, y) -> y - x);
        System.out.println(Arrays.toString(arr));
    }
}
