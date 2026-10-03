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

/// An exception unwinding through a finally must reach the caller. The
/// translator's dead-case pass stripped the case label of every catch-all
/// handler -- a finally, a synchronized block's release -- so the throw was
/// dispatched to a pc with no case, fell to "default: return" and vanished, and
/// the method returned normally. Shaped like ConnectionRequest's
/// performOperationComplete, where it dropped every network failure on the
/// browser: a typed catch that rethrows, inside a try/finally, around a call
/// that suspends.
public class JsFinallyRethrowApp {
    public static int result;
    static int cleanups;
    static Object pending;
    static final Object LOCK = new Object();

    static void fail(int n) throws java.io.IOException {
        Thread.yield();
        if (n >= 0) {
            throw new java.io.IOException("boom " + n);
        }
    }

    static boolean rethrowsThroughFinally(int n) throws java.io.IOException {
        String state = "start";
        try {
            for (int iter = 0 ; iter < 3 ; iter++) {
                state = state + iter;
                Thread.yield();
            }
            fail(n);
            return true;
        } catch (java.io.IOException err) {
            if (pending != null) {
                java.io.IOException other = (java.io.IOException) pending;
                pending = null;
                throw other;
            }
            throw err;
        } finally {
            cleanups++;
            state = null;
        }
    }

    static boolean plainFinally(int n) throws java.io.IOException {
        try {
            Thread.yield();
            fail(n);
            return true;
        } finally {
            cleanups++;
        }
    }

    static boolean throughSynchronized(int n) throws java.io.IOException {
        synchronized (LOCK) {
            Thread.yield();
            fail(n);
            return true;
        }
    }

    public static void main(String[] args) {
        int score = 0;
        try {
            rethrowsThroughFinally(1);
        } catch (java.io.IOException expected) {
            if ("boom 1".equals(expected.getMessage())) {
                score |= 1;
            }
        }
        try {
            plainFinally(2);
        } catch (java.io.IOException expected) {
            score |= 2;
        }
        try {
            throughSynchronized(3);
        } catch (java.io.IOException expected) {
            score |= 4;
        }
        try {
            if (rethrowsThroughFinally(-1)) {
                score |= 8;
            }
        } catch (java.io.IOException unexpected) {
            score = -100;
        }
        if (cleanups == 3) {
            score |= 16;
        }
        result = score;
    }
}
