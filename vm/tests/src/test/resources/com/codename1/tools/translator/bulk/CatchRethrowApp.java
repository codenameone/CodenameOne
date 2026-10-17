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

/// An exception thrown from a catch handler must leave the try statement, not
/// be caught by a sibling handler of the same try. Each case sets one bit of
/// the score; `RESULT=127` is every case right, and
/// `BulkGeneratedCodeChecks.checkOutput` requires exactly that on ParparVM.
public class CatchRethrowApp {
    static int cleanups;

    static void fail(String why) {
        throw new IllegalStateException(why);
    }

    static int rethrow() {
        try {
            fail("a");
        } catch (IllegalStateException e) {
            throw e;
        } catch (RuntimeException e) {
            return 100;
        }
        return 200;
    }

    static int throwNew() {
        try {
            fail("b");
        } catch (IllegalStateException e) {
            throw new UnsupportedOperationException("from the handler");
        } catch (RuntimeException e) {
            return 100;
        }
        return 200;
    }

    static int withFinally() {
        try {
            try {
                fail("c");
            } catch (IllegalStateException e) {
                throw e;
            } catch (RuntimeException e) {
                return 100;
            }
        } finally {
            cleanups++;
        }
        return 200;
    }

    static int nested() {
        try {
            try {
                fail("d");
            } catch (IllegalStateException e) {
                throw e;
            } catch (RuntimeException e) {
                return 100;
            }
        } catch (IllegalStateException outer) {
            return 7;
        }
        return 200;
    }

    static int touched;

    // SelfTest's long-skipped VM probe: a catch around a try/finally in the
    // same method must see what the finally rethrows.
    static String nestedFinally() {
        try {
            try {
                throw new java.io.IOException("thrown in the inner try");
            } finally {
                touched++;
            }
        } catch (Exception caught) {
            return "caught";
        }
    }

    public static void fixtureMain(String[] args) {
        int score = 0;
        try {
            rethrow();
        } catch (IllegalStateException e) {
            if ("a".equals(e.getMessage())) {
                score |= 1;
            }
        }
        try {
            throwNew();
        } catch (UnsupportedOperationException e) {
            score |= 2;
        }
        try {
            withFinally();
        } catch (IllegalStateException e) {
            score |= 4;
        }
        if (cleanups == 1) {
            score |= 8;
        }
        if (nested() == 7) {
            score |= 16;
        }
        try {
            fail("e");
        } catch (RuntimeException e) {
            score |= 32;
        }
        if ("caught".equals(nestedFinally()) && touched == 1) {
            score |= 64;
        }
        System.out.println("RESULT=" + score);
    }
}
