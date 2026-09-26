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
package com.bench;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Minimal for-each shapes, one per control-flow kind, used to isolate a crash in the
 * intrinsified loop. Deliberately tiny: the full ForEachT exercises fourteen shapes at
 * once, which tells you something is wrong but not which one.
 */
public class FeMin {
    static long plain(List<Integer> l) {
        long t = 0;
        for (Integer i : l) {
            t += i.intValue();
        }
        return t;
    }

    /** Two loops in a row: javac REUSES one iterator slot for both. */
    static long seq(List<Integer> l) {
        long t = 0;
        for (Integer i : l) {
            t += i.intValue();
        }
        for (Integer i : l) {
            t += i.intValue();
        }
        return t;
    }

    static long nest(List<Integer> l) {
        long t = 0;
        for (Integer i : l) {
            for (Integer j : l) {
                t += i.intValue() * j.intValue();
            }
        }
        return t;
    }

    /** The receiver local is reassigned after the loop. */
    static long reassign(List<Integer> l) {
        long t = 0;
        for (Integer i : l) {
            t += i.intValue();
        }
        l = new ArrayList<Integer>();
        for (Integer i : l) {
            t += i.intValue();
        }
        return t;
    }

    static long inTry(List<Integer> l) {
        long t = 0;
        try {
            for (Integer i : l) {
                t += i.intValue();
            }
        } catch (RuntimeException e) {
            t = -1;
        }
        return t;
    }

    static long withBreak(List<Integer> l) {
        long t = 0;
        for (Integer i : l) {
            if (i.intValue() > 10) {
                break;
            }
            t += i.intValue();
        }
        return t;
    }

    static long withContinue(List<Integer> l) {
        long t = 0;
        for (Integer i : l) {
            if ((i.intValue() & 1) == 0) {
                continue;
            }
            t += i.intValue();
        }
        return t;
    }

    static long earlyReturn(List<Integer> l) {
        for (Integer i : l) {
            if (i.intValue() == 5) {
                return 5;
            }
        }
        return -1;
    }

    static void run(String name, List<Integer> l) {
        System.out.println(name + " plain=" + plain(l) + " seq=" + seq(l) + " nest=" + nest(l)
                + " reas=" + reassign(l) + " try=" + inTry(l) + " brk=" + withBreak(l)
                + " cont=" + withContinue(l) + " ret=" + earlyReturn(l));
    }

    private List<Integer> field;

    long viaField() {
        long t = 0;
        for (Integer i : field) {
            t += i.intValue();
        }
        return t;
    }

    public static void main(String[] args) {
        List<Integer> a = new ArrayList<Integer>();
        List<Integer> k = new LinkedList<Integer>();
        for (int i = 0; i < 32; i++) {
            a.add(Integer.valueOf(i));
            k.add(Integer.valueOf(i));
        }
        run("arraylist", a);
        run("linkedlist", k);
        run("empty", new ArrayList<Integer>());
        run("unmodifiable", java.util.Collections.unmodifiableList(a));
        run("asList", java.util.Arrays.asList(new Integer[] {Integer.valueOf(1), Integer.valueOf(2)}));
        run("sub", a.subList(0, 16));
        run("singleton", java.util.Collections.singletonList(Integer.valueOf(7)));
        FeMin f = new FeMin();
        f.field = a;
        System.out.println("field=" + f.viaField());
        // Iterable.forEach reaches the default method, whose own loop is intrinsified.
        final long[] acc = new long[1];
        a.forEach(new java.util.function.Consumer<Integer>() {
            public void accept(Integer i) {
                acc[0] += i.intValue();
            }
        });
        System.out.println("forEach=" + acc[0]);
        System.out.println("toString=" + a.toString().length());
    }
}
