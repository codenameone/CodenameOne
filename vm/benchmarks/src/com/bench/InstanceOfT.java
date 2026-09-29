/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.bench;

/**
 * Differential torture for instanceof.
 *
 * The translator answers a non-array instanceof from a precomputed bitmap: every type
 * tested anywhere in the application gets a dense bit index, and every class carries a
 * row of the types it is an instance of (Parser.typeTestIds, BC_INSTANCEOF_FAST). That
 * replaces a walk of the supertype list, so it has to agree with the walk on exactly
 * the cases the walk used to decide -- and on the ones it never reached, since the old
 * path answered identity before the list and arrays before that again.
 *
 * What is deliberately covered:
 *
 *  - identity, which the scan never saw because instanceofFunction returned first;
 *  - a deep single-inheritance chain, tested from every level against every level,
 *    which is the case a row bit either has or has not;
 *  - interfaces reached through a superclass, and interfaces that extend interfaces,
 *    because those enter the row through appendClassOffset's second recursion rather
 *    than the first;
 *  - a type tested but never instantiated, and a type instantiated but never tested,
 *    which are the two ways a bit index and a row can disagree about who owns it;
 *  - arrays of every dimension and element kind, which have no row at all and must
 *    fall back -- an array id is above CN1_TYPETEST_ROWS, and reading a row for one
 *    would be out of bounds rather than merely wrong;
 *  - null, which never reaches the bitmap.
 *
 * Every answer is printed, so the harness compares against a real JDK rather than
 * against an expectation written here.
 */
public class InstanceOfT {
    interface Alpha { }
    interface Beta { }
    interface Gamma extends Alpha { }
    interface Delta { }

    static class A { }
    static class B extends A implements Alpha { }
    static class C extends B implements Beta { }
    static class D extends C { }
    static class E extends D implements Gamma { }

    static class Lonely implements Delta { }
    /** Tested against, never instantiated. */
    static class NeverMade extends A { }
    /** Instantiated, never tested against by name. */
    static class NeverTested extends E { }

    private static void p(String label, boolean v) {
        System.out.println(label + "=" + v);
    }

    private static void probe(String name, Object o) {
        System.out.println("-- " + name);
        p("Object", o instanceof Object);
        p("A", o instanceof A);
        p("B", o instanceof B);
        p("C", o instanceof C);
        p("D", o instanceof D);
        p("E", o instanceof E);
        p("Alpha", o instanceof Alpha);
        p("Beta", o instanceof Beta);
        p("Gamma", o instanceof Gamma);
        p("Delta", o instanceof Delta);
        p("Lonely", o instanceof Lonely);
        p("NeverMade", o instanceof NeverMade);
        p("String", o instanceof String);
        p("CharSequence", o instanceof CharSequence);
        p("Number", o instanceof Number);
        p("Integer", o instanceof Integer);
        p("Runnable", o instanceof Runnable);
        p("int[]", o instanceof int[]);
        p("byte[]", o instanceof byte[]);
        p("char[]", o instanceof char[]);
        p("long[]", o instanceof long[]);
        p("double[]", o instanceof double[]);
        p("Object[]", o instanceof Object[]);
        p("String[]", o instanceof String[]);
        p("A[]", o instanceof A[]);
        p("int[][]", o instanceof int[][]);
        p("Object[][]", o instanceof Object[][]);
        p("A[][]", o instanceof A[][]);
        p("A[][][]", o instanceof A[][][]);
    }

    public static void main(String[] args) {
        probe("null", null);
        probe("new Object", new Object());
        probe("A", new A());
        probe("B", new B());
        probe("C", new C());
        probe("D", new D());
        probe("E", new E());
        probe("Lonely", new Lonely());
        probe("NeverTested", new NeverTested());
        probe("String", "hello");
        probe("Integer", Integer.valueOf(7));
        probe("Long", Long.valueOf(7L));
        probe("StringBuilder", new StringBuilder("x"));
        probe("int[0]", new int[0]);
        probe("int[3]", new int[3]);
        probe("byte[3]", new byte[3]);
        probe("char[3]", new char[3]);
        probe("long[3]", new long[3]);
        probe("double[3]", new double[3]);
        probe("Object[2]", new Object[2]);
        probe("String[2]", new String[2]);
        probe("A[2]", new A[2]);
        probe("E[2]", new E[2]);
        probe("int[2][2]", new int[2][2]);
        probe("Object[2][2]", new Object[2][2]);
        probe("A[2][2]", new A[2][2]);
        probe("A[2][2][2]", new A[2][2][2]);

        // The same tests under churn, so a bitmap row read through a moved or
        // freshly allocated object is exercised rather than only a settled one.
        int acc = 0;
        for(int round = 0 ; round < 200 ; round++) {
            Object[] mix = new Object[] {
                new A(), new B(), new C(), new D(), new E(), new Lonely(),
                new NeverTested(), "s" + round, Integer.valueOf(round),
                new int[round % 4], new Object[round % 3], new A[round % 3],
                new A[1][1], null
            };
            for(int i = 0 ; i < mix.length ; i++) {
                Object o = mix[i];
                if(o instanceof E) { acc += 1; }
                if(o instanceof D) { acc += 2; }
                if(o instanceof C) { acc += 4; }
                if(o instanceof B) { acc += 8; }
                if(o instanceof A) { acc += 16; }
                if(o instanceof Alpha) { acc += 32; }
                if(o instanceof Beta) { acc += 64; }
                if(o instanceof Gamma) { acc += 128; }
                if(o instanceof Delta) { acc += 256; }
                if(o instanceof CharSequence) { acc += 512; }
                if(o instanceof Number) { acc += 1024; }
                if(o instanceof int[]) { acc += 2048; }
                if(o instanceof Object[]) { acc += 4096; }
                if(o instanceof A[]) { acc += 8192; }
                if(o instanceof A[][]) { acc += 16384; }
            }
        }
        System.out.println("churn=" + acc);
    }
}
