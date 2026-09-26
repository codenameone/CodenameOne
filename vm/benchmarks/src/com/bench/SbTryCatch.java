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
 * StringBuilders meeting exception edges.
 *
 * One try/catch anywhere in a method used to disable implicit stack allocation for
 * every builder in it -- 315 of the corpus' 1,818 sites, including builders nowhere
 * near the protected range. 300 of those 315 survive the escape analysis unchanged,
 * because that analysis was already control-flow-insensitive where it counts: a
 * builder parked in a local is validated by walking EVERY instruction that touches
 * the slot, so a use inside a handler is checked like any other.
 *
 * These are the shapes that test that claim rather than assert it:
 *
 *   builtInTry        the ordinary case -- a builder built and consumed inside a try
 *   usedInHandler     the handler reads the builder the try was filling
 *   builtInHandler    the builder is created in the catch block
 *   escapesFromTry    the builder is stored in a FIELD, so it must not be stacked
 *   escapesInHandler  the same, from the handler
 *   throughFinally    a finally block appends to it after the throw
 *   loopWithTry       a builder per iteration, try inside the loop
 *   tryInLoopBody     the builder outlives the try that filled it
 *   nestedTryBuilders one builder per nesting level, all live at once
 *   rethrowKeeps      the builder is read after a rethrow is caught outside
 *
 * A wrong answer here is a C-stack address reaching the heap, which is why the
 * verifier's escaped-stack-object check exists (run-gc-verify.sh, self-test6). What
 * this torture adds is the semantics: the strings must be what the JDK produces.
 */
public class SbTryCatch {
    private static StringBuilder kept;
    private static int counter;

    private static String builtInTry(int n) {
        StringBuilder b = new StringBuilder();
        try {
            for (int i = 0; i < n; i++) {
                b.append(i).append(':');
            }
            if (n < 0) {
                throw new IllegalStateException("never");
            }
        } catch (IllegalStateException e) {
            b.append("caught");
        }
        return b.toString();
    }

    private static String usedInHandler(int n) {
        StringBuilder b = new StringBuilder();
        try {
            for (int i = 0; i < n; i++) {
                b.append(i);
                if (i == n / 2) {
                    throw new IllegalArgumentException("mid");
                }
            }
        } catch (IllegalArgumentException e) {
            b.append('|').append(e.getMessage()).append('|').append(b.length());
        }
        return b.toString();
    }

    private static String builtInHandler(int n) {
        try {
            if (n >= 0) {
                throw new IllegalStateException("to-handler");
            }
            return "unreachable";
        } catch (IllegalStateException e) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < n; i++) {
                b.append(n - i).append('-');
            }
            return b.append(e.getMessage()).toString();
        }
    }

    /** Stored in a static: the analysis must refuse this one. */
    private static String escapesFromTry(int n) {
        StringBuilder b = new StringBuilder();
        try {
            b.append("esc").append(n);
            kept = b;
        } catch (RuntimeException e) {
            b.append("!");
        }
        return kept.toString() + "/" + b.length();
    }

    private static String escapesInHandler(int n) {
        StringBuilder b = new StringBuilder();
        try {
            b.append("h").append(n);
            throw new IllegalStateException("x");
        } catch (IllegalStateException e) {
            kept = b;
            b.append("-handled");
        }
        return kept.toString();
    }

    private static String throughFinally(int n, boolean blowUp) {
        StringBuilder b = new StringBuilder();
        try {
            b.append("try").append(n);
            if (blowUp) {
                throw new IllegalStateException("boom");
            }
            b.append("-ok");
        } catch (IllegalStateException e) {
            b.append("-caught");
        } finally {
            b.append("-finally");
        }
        return b.toString();
    }

    private static String loopWithTry(int n) {
        StringBuilder out = new StringBuilder();
        for (int round = 0; round < 3; round++) {
            StringBuilder b = new StringBuilder();
            try {
                for (int i = 0; i < n; i++) {
                    b.append(round).append(i);
                    if (i == round) {
                        throw new IllegalArgumentException("r" + round);
                    }
                }
            } catch (IllegalArgumentException e) {
                b.append('/').append(e.getMessage());
            }
            out.append(b).append(';');
        }
        return out.toString();
    }

    private static String tryInLoopBody(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            try {
                if ((i & 1) == 0) {
                    throw new IllegalStateException("even");
                }
                b.append('o');
            } catch (IllegalStateException e) {
                b.append('e');
            }
        }
        return b.append('#').append(b.length()).toString();
    }

    private static String nestedTryBuilders(int n) {
        StringBuilder outer = new StringBuilder("O");
        try {
            StringBuilder middle = new StringBuilder("M");
            try {
                StringBuilder inner = new StringBuilder("I");
                for (int i = 0; i < n; i++) {
                    inner.append(i);
                }
                middle.append(inner);
                throw new IllegalStateException(middle.toString());
            } catch (IllegalStateException e) {
                middle.append("<").append(e.getMessage().length()).append(">");
            }
            outer.append(middle);
        } catch (RuntimeException e) {
            outer.append("?");
        }
        return outer.toString();
    }

    private static String rethrowKeeps(int n) {
        StringBuilder b = new StringBuilder("R");
        try {
            try {
                b.append(n);
                throw new IllegalStateException("inner");
            } catch (IllegalStateException e) {
                b.append("-i");
                throw new IllegalArgumentException("outer");
            }
        } catch (IllegalArgumentException e) {
            b.append("-o").append(b.length());
        }
        return b.toString();
    }

    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        for (int n = 0; n <= 6; n++) {
            out.append("n=").append(n).append('\n');
            out.append("  builtInTry        ").append(builtInTry(n)).append('\n');
            out.append("  usedInHandler     ").append(usedInHandler(n)).append('\n');
            out.append("  builtInHandler    ").append(builtInHandler(n)).append('\n');
            out.append("  escapesFromTry    ").append(escapesFromTry(n)).append('\n');
            out.append("  escapesInHandler  ").append(escapesInHandler(n)).append('\n');
            out.append("  throughFinallyF   ").append(throughFinally(n, false)).append('\n');
            out.append("  throughFinallyT   ").append(throughFinally(n, true)).append('\n');
            out.append("  loopWithTry       ").append(loopWithTry(n)).append('\n');
            out.append("  tryInLoopBody     ").append(tryInLoopBody(n)).append('\n');
            out.append("  nestedTryBuilders ").append(nestedTryBuilders(n)).append('\n');
            out.append("  rethrowKeeps      ").append(rethrowKeeps(n)).append('\n');
        }
        // Hot and allocation-heavy, so collections happen while builders are live in
        // frames that have exception handlers on them.
        int acc = 0;
        for (int rep = 0; rep < 4000; rep++) {
            acc += builtInTry(rep & 31).length();
            acc += usedInHandler(8 + (rep & 15)).length();
            acc += loopWithTry(rep & 7).length();
            acc += nestedTryBuilders(rep & 15).length();
            acc += escapesFromTry(rep & 3).length();
            counter += acc & 1;
        }
        out.append("hot=").append(acc).append(" counter=").append(counter).append('\n');
        System.out.print(out);
    }
}
