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

public class Classes {
    interface Animal {
        String name();

        default String speak() {
            return name() + " makes " + sound();
        }

        String sound();

        static Animal of(String n, String s) {
            return new Animal() {
                public String name() {
                    return n;
                }

                public String sound() {
                    return s;
                }
            };
        }

        private String hidden() {
            return "hidden";
        }

        default String reveal() {
            return hidden();
        }
    }

    static abstract class Base implements Animal {
        protected int legs;
        static int created;

        Base(int legs) {
            this.legs = legs;
            created++;
            init();
        }

        void init() {
        }

        public String sound() {
            return "...";
        }

        public String toString() {
            return name() + "/" + legs;
        }
    }

    static class Dog extends Base {
        String extra = "wag";
        { extra += "!"; }

        Dog() {
            super(4);
        }

        public String name() {
            return "dog";
        }

        @Override
        public String sound() {
            return "woof " + super.sound();
        }

        @Override
        void init() {
            System.out.println("init sees extra=" + extra);
        }
    }

    static class Puppy extends Dog {
        Puppy() {
            this("tiny");
        }

        Puppy(String size) {
            super();
            legs = size.length();
        }

        public String name() {
            return "puppy";
        }
    }

    static int staticCounter;
    static final List<String> LOG = new ArrayList<>();

    static {
        staticCounter = 5;
        LOG.add("static init");
    }

    int instanceValue;

    {
        instanceValue = staticCounter * 2;
    }

    Classes() {
        LOG.add("ctor " + instanceValue);
    }

    static class Counter implements Iterable<Integer> {
        private final int max;

        Counter(int max) {
            this.max = max;
        }

        public Iterator<Integer> iterator() {
            return new Iterator<>() {
                int i;

                public boolean hasNext() {
                    return i < max;
                }

                public Integer next() {
                    return i++;
                }
            };
        }
    }

    static class Matrix {
        final double[][] m;

        Matrix(double[][] m) {
            this.m = m;
        }

        Matrix times(Matrix o) {
            int n = m.length;
            double[][] r = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    for (int k = 0; k < n; k++) {
                        r[i][j] += m[i][k] * o.m[k][j];
                    }
                }
            }
            return new Matrix(r);
        }
    }

    static String varargs(String first, Object... rest) {
        return first + rest.length + Arrays.toString(rest);
    }

    static int sum(int... xs) {
        int s = 0;
        for (int x : xs) {
            s += x;
        }
        return s;
    }

    public static void main(String[] args) {
        Dog d = new Dog();
        System.out.println(d + " " + d.speak() + " " + d.extra);
        Puppy p = new Puppy();
        System.out.println(p + " " + p.speak() + " " + Base.created);
        Animal cat = Animal.of("cat", "meow");
        System.out.println(cat.speak() + " " + cat.reveal());
        new Classes();
        System.out.println(LOG);
        int total = 0;
        for (int i : new Counter(4)) {
            total += i;
        }
        System.out.println(total);
        Matrix a = new Matrix(new double[][]{{1, 2}, {3, 4}});
        System.out.println(Arrays.deepToString(a.times(a).m));
        System.out.println(varargs("x") + varargs("y", 1, "two") + varargs("z", (Object[]) new String[]{"q"}));
        System.out.println(sum() + sum(1) + sum(1, 2, 3) + sum(new int[]{4, 5}));
        Object o = d;
        System.out.println((o instanceof Animal) + " " + (o instanceof Base) + " " + (o instanceof Puppy));
        Animal[] zoo = {d, p, cat};
        for (Animal an : zoo) {
            System.out.println(an.name());
        }
        StringBuilder sb = new StringBuilder();
        for (char c = 'a'; c <= 'e'; c++) {
            sb.insert(0, c);
        }
        System.out.println(sb.reverse());
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("one", 1);
        m.put("two", 2);
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            System.out.println(e.getKey() + "->" + e.getValue());
        }
        var list = new ArrayList<Map.Entry<String, Integer>>(m.entrySet());
        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        System.out.println(list.get(0).getKey());
        String text = """
            {
              "a": 1,
              "b": "two\tx"
            }
            """;
        System.out.print(text);
        int[] data = {5, 2, 8};
        int idx = 0;
        while (idx < data.length && data[idx] != 8) {
            idx++;
        }
        System.out.println(idx);
        System.out.println(Objects.hash(1, "a") == Objects.hash(1, "a"));
        System.out.println(String.format("%05d|%-4s|%.1f", 42, "ab", 3.14159));
        char grade = 'B';
        String msg = grade == 'A' ? "top" : grade == 'B' ? "good" : "other";
        System.out.println(msg);
    }
}
