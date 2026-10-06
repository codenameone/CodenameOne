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

public class PatternsSwitch {
    sealed interface Shape permits Circle, Rect, Tri {
    }

    record Circle(double r) implements Shape {
    }

    record Rect(double w, double h) implements Shape {
    }

    record Tri(double b, double h) implements Shape {
    }

    record Pair(Object a, Object b) {
    }

    sealed interface Expr {
    }

    record Num(int v) implements Expr {
    }

    record Add(Expr l, Expr r) implements Expr {
    }

    record Neg(Expr e) implements Expr {
    }

    static int eval(Expr e) {
        return switch (e) {
            case Num n -> n.v();
            case Add(Expr l, Expr r) -> eval(l) + eval(r);
            case Neg(Num(int v)) -> -v;
            case Neg(var inner) -> -eval(inner);
        };
    }

    static double area(Shape s) {
        return switch (s) {
            case Circle c when c.r() == 0 -> 0;
            case Circle c -> Math.PI * c.r() * c.r();
            case Rect(double w, double h) -> w * h;
            case Tri t -> 0.5 * t.b() * t.h();
        };
    }

    static String classify(Object o) {
        return switch (o) {
            case null -> "null!";
            case Integer i when i > 10 -> "big int " + i;
            case Integer i -> "int " + i;
            case String s when s.isEmpty() -> "empty string";
            case String s -> "string " + s.length();
            case int[] arr -> "ints " + arr.length;
            case Pair(String a, Integer b) -> "pair " + a + b;
            case Pair(var a, var b) -> "other pair " + a + "/" + b;
            default -> "object " + o.getClass().getSimpleName();
        };
    }

    static String days(int d) {
        String r;
        switch (d) {
            case 1:
            case 7:
                r = "weekend";
                break;
            case 2:
                r = "monday";
                break;
            default:
                r = "weekday";
        }
        return r;
    }

    static int fallthrough(int x) {
        int acc = 0;
        switch (x) {
            case 1:
                acc += 1;
            case 2:
                acc += 10;
            case 3:
                acc += 100;
                break;
            case 1000:
                acc = -1;
        }
        return acc;
    }

    static String str(String s) {
        switch (s) {
            case "a":
                return "A";
            case "b":
            case "c":
                return "BC";
            default:
                return "?" + s;
        }
    }

    static int yields(String s) {
        return switch (s) {
            case "one" -> 1;
            case "two" -> {
                int t = 1;
                t++;
                yield t;
            }
            default -> {
                if (s.length() > 3) {
                    yield 100;
                }
                yield -1;
            }
        };
    }

    static String charSwitch(char c) {
        return switch (c) {
            case 'a', 'e', 'i', 'o', 'u' -> "vowel";
            case ' ' -> "space";
            default -> "consonant";
        };
    }

    static int oldStyleYield(int k) {
        int v = switch (k) {
            case 0:
                yield 10;
            case 1:
            case 2:
                yield 20;
            default:
                yield k * 3;
        };
        return v;
    }

    public static void main(String[] args) {
        System.out.println(eval(new Add(new Num(3), new Neg(new Num(4)))));
        System.out.println(eval(new Neg(new Add(new Num(1), new Num(2)))));
        for (Shape s : List.of(new Circle(1), new Circle(0), new Rect(2, 3), new Tri(4, 5))) {
            System.out.printf("%.3f%n", area(s));
        }
        Object[] things = {null, 5, 50, "", "hey", new int[3], new Pair("x", 1), new Pair(1, 2), 3.5};
        for (Object o : things) {
            System.out.println(classify(o));
        }
        for (int i = 0; i <= 8; i++) {
            System.out.print(days(i) + " ");
        }
        System.out.println();
        System.out.println(fallthrough(1) + " " + fallthrough(2) + " " + fallthrough(3) + " " + fallthrough(1000) + " " + fallthrough(9));
        System.out.println(str("a") + str("c") + str("zz"));
        System.out.println(yields("one") + " " + yields("two") + " " + yields("three") + " " + yields("x"));
        System.out.println(charSwitch('e') + charSwitch(' ') + charSwitch('z'));
        System.out.println(oldStyleYield(0) + oldStyleYield(2) + oldStyleYield(5));
        Object o = "pattern";
        if (o instanceof String s && s.length() > 3) {
            System.out.println("long " + s);
        }
        if (!(o instanceof Integer i)) {
            System.out.println("not int");
        } else {
            System.out.println(i + 1);
        }
        Object q = 42;
        if (!(q instanceof Integer n)) {
            return;
        }
        System.out.println("n=" + (n + 1));
        Object pr = new Pair(new Pair(1, "in"), 2);
        if (pr instanceof Pair(Pair(Integer a, String b), Integer c)) {
            System.out.println(a + b + c);
        }
        Integer boxed = 7;
        switch (boxed) {
            case 7 -> System.out.println("seven");
            default -> System.out.println("other");
        }
        try {
            String nul = null;
            switch (nul) {
                case "a" -> System.out.println("a");
                default -> System.out.println("d");
            }
        } catch (NullPointerException e) {
            System.out.println("NPE");
        }
    }
}
