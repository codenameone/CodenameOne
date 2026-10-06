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

public class EnumsRecords {
    enum Op {
        ADD("+") {
            int apply(int a, int b) {
                return a + b;
            }
        },
        MUL("*") {
            int apply(int a, int b) {
                return a * b;
            }
        };

        private final String symbol;

        Op(String symbol) {
            this.symbol = symbol;
        }

        abstract int apply(int a, int b);

        String symbol() {
            return symbol;
        }
    }

    enum Planet {
        MERCURY(3.303e+23, 2.4397e6), EARTH(5.976e+24, 6.37814e6);

        final double mass;
        final double radius;
        static final double G = 6.67300E-11;

        Planet(double mass, double radius) {
            this.mass = mass;
            this.radius = radius;
        }

        double gravity() {
            return G * mass / (radius * radius);
        }
    }

    enum Level { LOW, MEDIUM, HIGH }

    record Point(int x, int y) {
        Point {
            if (x < 0) {
                throw new IllegalArgumentException("negative x");
            }
        }

        Point(int v) {
            this(v, v);
        }

        static Point origin() {
            return new Point(0, 0);
        }

        double dist() {
            return Math.sqrt(x * x + y * y);
        }
    }

    record Named<T>(String name, T value) {
    }

    record Range(int lo, int hi) {
        Range(int lo, int hi) {
            this.lo = Math.min(lo, hi);
            this.hi = Math.max(lo, hi);
        }
    }

    record Flags(boolean on, char c, long l, float f, double d, byte b, short s) {
    }

    public static void main(String[] args) {
        for (Op op : Op.values()) {
            System.out.println(op + " " + op.symbol() + " " + op.apply(6, 7) + " " + op.ordinal() + " " + op.name());
        }
        System.out.println(Op.valueOf("MUL").apply(2, 3));
        for (Planet p : Planet.values()) {
            System.out.printf("%s %.2f%n", p, p.gravity());
        }
        Level lv = Level.MEDIUM;
        switch (lv) {
            case LOW -> System.out.println("low");
            case MEDIUM -> System.out.println("medium");
            case HIGH -> System.out.println("high");
        }
        String desc = switch (lv) {
            case LOW, MEDIUM -> "not high";
            case HIGH -> "high";
        };
        System.out.println(desc);
        EnumMap<Level, Integer> em = new EnumMap<>(Level.class);
        em.put(Level.HIGH, 3);
        em.put(Level.LOW, 1);
        System.out.println(em);
        System.out.println(Level.HIGH.compareTo(Level.LOW) > 0);
        Point p = new Point(3, 4);
        System.out.println(p + " " + p.x() + " " + p.dist() + " " + p.equals(new Point(3, 4)) + " " + (p.hashCode() == new Point(3, 4).hashCode()));
        System.out.println(new Point(5) + " " + Point.origin());
        try {
            new Point(-1, 0);
        } catch (IllegalArgumentException e) {
            System.out.println("caught " + e.getMessage());
        }
        Named<List<String>> n = new Named<>("list", List.of("a"));
        System.out.println(n + " " + n.value().get(0));
        System.out.println(new Range(9, 2));
        Flags f = new Flags(true, 'z', 5L, 1.5f, 2.25, (byte) 7, (short) 300);
        System.out.println(f + " " + f.equals(new Flags(true, 'z', 5L, 1.5f, 2.25, (byte) 7, (short) 300)));
        System.out.println(new Flags(true, 'z', 5L, 1.5f, 2.25, (byte) 7, (short) 300).hashCode() == f.hashCode());
        Set<Point> points = new HashSet<>();
        points.add(new Point(1, 1));
        points.add(new Point(1, 1));
        System.out.println(points.size());
        record Local(String a, int b) {
        }
        System.out.println(new Local("loc", 2));
        enum Dir { N, S }
        System.out.println(Dir.S.ordinal() + " " + Arrays.toString(Dir.values()));
    }
}
