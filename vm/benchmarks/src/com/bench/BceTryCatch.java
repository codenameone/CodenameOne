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

/**
 * Array loops meeting exception edges.
 *
 * Bounds-check elimination used to be refused for any method containing a try/catch,
 * and that was the most expensive of the three guardrails try/catch controls: 20% of
 * the self-hosting corpus' array accesses sat in a method it disabled, against 17% of
 * StringBuilder sites and 1% of methods for frameless codegen. It is now refused per
 * LOOP, and only when an exception handler lands inside one.
 *
 * The proof BCE rests on is that falling through IF_ICMPGE exit establishes
 * i &lt; a.length for the body. An exception edge is the one way into that body that
 * carries no Jump instruction, so it is invisible to the pass' existing foreign-entry
 * check -- hence the shapes below, which exist to pin the answers:
 *
 *   loopInsideTry            handler after the loop: the case the change enables
 *   throwOutOfLoop           the same, entered by a real throw mid-loop
 *   catchInsideLoop          handler lands in the body: the case still refused
 *   shorterArrayInBody       the sharp one -- b[i] must still throw AIOOBE
 *   proveThenWiden           the proven array is REPLACED in a handler
 *   nestedTry                loop between two handlers, inner and outer
 *   finallyAroundLoop        javac's duplicated finally around a counted loop
 *   handlerMovesIndex        the handler writes the induction variable
 *   multiCatch               several handlers, so several landing pads
 *   storesAndLoads           IASTORE beside IALOAD in one proven loop
 *
 * Division by zero is deliberately absent: this VM answers 0 for it rather than
 * throwing, so it would diverge here for a reason that has nothing to do with BCE.
 * So is a null dereference, which is a SIGSEGV rather than an NPE on the clean target.
 */
public class BceTryCatch {
    private static int[] fill(int n, int seed) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = (seed + i) * 31 + (i & 7);
        }
        return a;
    }

    /** The counted loop is wholly inside the try; the handler sits after it. */
    private static long loopInsideTry(int n, int seed) {
        int[] a = fill(n, seed);
        long sum = 0;
        try {
            for (int i = 0; i < a.length; i++) {
                sum += a[i];
            }
            if (sum == Long.MIN_VALUE) {
                throw new IllegalStateException("unreachable");
            }
        } catch (IllegalStateException e) {
            sum = -1;
        }
        return sum;
    }

    /** Same shape, but the throw really happens, from inside the loop. */
    private static long throwOutOfLoop(int n, int seed, int at) {
        int[] a = fill(n, seed);
        long sum = 0;
        try {
            for (int i = 0; i < a.length; i++) {
                if (i == at) {
                    throw new IllegalStateException("at " + i);
                }
                sum += a[i];
            }
        } catch (IllegalStateException e) {
            sum = sum * 2 + e.getMessage().length();
        }
        return sum;
    }

    /** The handler lands INSIDE the loop body, which is the shape still refused. */
    private static long catchInsideLoop(int n, int seed) {
        int[] a = fill(n, seed);
        long sum = 0;
        for (int i = 0; i < a.length; i++) {
            try {
                if ((a[i] & 3) == 0) {
                    throw new IllegalArgumentException("q");
                }
                sum += a[i];
            } catch (IllegalArgumentException e) {
                sum -= a[i];
            }
        }
        return sum;
    }

    /**
     * The loop is proven over a, and the body also indexes a SHORTER array. Nothing
     * proves b[i], so it must still throw -- if the pass ever widened its proof from
     * the array it measured to any array in the body, this is where that shows up.
     */
    private static long shorterArrayInBody(int n, int seed) {
        int[] a = fill(n, seed);
        int[] b = fill(n / 2, seed + 1);
        long sum = 0;
        int caught = 0;
        try {
            for (int i = 0; i < a.length; i++) {
                sum += a[i] + b[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            caught = 1;
        }
        return sum * 4 + caught;
    }

    /** A handler outside the loop swaps the array for a shorter one and re-enters. */
    private static long proveThenWiden(int n, int seed) {
        int[] a = fill(n, seed);
        long sum = 0;
        for (int round = 0; round < 2; round++) {
            try {
                for (int i = 0; i < a.length; i++) {
                    sum += a[i];
                    if (i == n - 1 && round == 0) {
                        throw new IllegalStateException("swap");
                    }
                }
            } catch (IllegalStateException e) {
                a = fill(n / 4, seed + 2);
            }
        }
        return sum;
    }

    /** Two nested try regions, the loop sitting between their handlers. */
    private static long nestedTry(int n, int seed) {
        int[] a = fill(n, seed);
        long sum = 0;
        try {
            try {
                for (int i = 0; i < a.length; i++) {
                    sum += a[i];
                }
                throw new IllegalStateException("inner");
            } catch (IllegalStateException e) {
                sum += 7;
                throw new IllegalArgumentException("outer");
            }
        } catch (IllegalArgumentException e) {
            sum += 11;
        }
        return sum;
    }

    /** javac duplicates the finally body, giving the method a catch-all handler. */
    private static long finallyAroundLoop(int n, int seed, boolean blowUp) {
        int[] a = fill(n, seed);
        long sum = 0;
        try {
            for (int i = 0; i < a.length; i++) {
                sum += a[i];
            }
            if (blowUp) {
                throw new IllegalStateException("boom");
            }
        } catch (IllegalStateException e) {
            sum += 3;
        } finally {
            sum = sum * 2 + 1;
        }
        return sum;
    }

    /** The handler writes the induction variable before control returns to the loop. */
    private static long handlerMovesIndex(int n, int seed) {
        int[] a = fill(n, seed);
        long sum = 0;
        int i = 0;
        while (i < a.length) {
            try {
                if (i == n / 2) {
                    throw new IllegalStateException("skip");
                }
                sum += a[i];
                i++;
            } catch (IllegalStateException e) {
                i += 2;
                sum += 100;
            }
        }
        return sum;
    }

    /** Several handlers, so several landing pads to resolve. */
    private static long multiCatch(int n, int seed, int mode) {
        int[] a = fill(n, seed);
        long sum = 0;
        try {
            for (int i = 0; i < a.length; i++) {
                sum += a[i];
            }
            if (mode == 0) {
                throw new IllegalStateException("s");
            }
            if (mode == 1) {
                throw new IllegalArgumentException("ar");
            }
            if (mode == 2) {
                throw new UnsupportedOperationException("uns");
            }
        } catch (IllegalStateException e) {
            sum += 1;
        } catch (IllegalArgumentException e) {
            sum += 2;
        } catch (RuntimeException e) {
            sum += 4;
        }
        return sum;
    }

    /** Loads and stores against the same proven array, inside a try. */
    private static long storesAndLoads(int n, int seed) {
        int[] a = fill(n, seed);
        long sum = 0;
        try {
            for (int i = 0; i < a.length; i++) {
                a[i] = a[i] ^ (i * 13);
            }
            for (int i = 0; i < a.length; i++) {
                sum += a[i];
            }
        } catch (RuntimeException e) {
            sum = -2;
        }
        return sum;
    }

    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        for (int n = 4; n <= 64; n *= 2) {
            out.append("n=").append(n).append('\n');
            out.append("  loopInsideTry      ").append(loopInsideTry(n, n * 3)).append('\n');
            out.append("  throwOutOfLoop     ").append(throwOutOfLoop(n, n, n / 2)).append('\n');
            out.append("  throwOutOfLoopEnd  ").append(throwOutOfLoop(n, n, n - 1)).append('\n');
            out.append("  throwOutOfLoopNone ").append(throwOutOfLoop(n, n, -1)).append('\n');
            out.append("  catchInsideLoop    ").append(catchInsideLoop(n, n + 5)).append('\n');
            out.append("  shorterArrayInBody ").append(shorterArrayInBody(n, n + 6)).append('\n');
            out.append("  proveThenWiden     ").append(proveThenWiden(n, n + 7)).append('\n');
            out.append("  nestedTry          ").append(nestedTry(n, n + 8)).append('\n');
            out.append("  finallyAroundLoopF ").append(finallyAroundLoop(n, n + 9, false)).append('\n');
            out.append("  finallyAroundLoopT ").append(finallyAroundLoop(n, n + 9, true)).append('\n');
            out.append("  handlerMovesIndex  ").append(handlerMovesIndex(n, n + 10)).append('\n');
            for (int mode = 0; mode < 4; mode++) {
                out.append("  multiCatch").append(mode).append("       ")
                   .append(multiCatch(n, n + 11, mode)).append('\n');
            }
            out.append("  storesAndLoads     ").append(storesAndLoads(n, n + 12)).append('\n');
        }
        // A longer run so the loops are hot and any wrong elimination has room to read
        // past the end of a page rather than into a neighbouring live object.
        long acc = 0;
        for (int rep = 0; rep < 200; rep++) {
            acc += loopInsideTry(257, rep);
            acc += shorterArrayInBody(257, rep);
            acc += storesAndLoads(257, rep);
            acc += catchInsideLoop(257, rep);
        }
        out.append("hot=").append(acc).append('\n');
        System.out.print(out);
    }
}
