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
package com.codename1.python.runtime;

/// Argument arrays lent to a callee that reads them and lets them go.
///
/// A builtin written over an array -- `str.format`, a dataclass `__init__`,
/// a module function taking ten options -- was handed a new one on every
/// call. Here one array per length is kept, taken for the call and given
/// back once it returns: a nested call finds the slot empty and gets an array
/// of its own, and one that raises simply never gives its array back, so no
/// handler is needed. Python code here runs on one thread at a time.
///
/// ONLY for a callee that keeps nothing of the array after it returns -- not
/// the array, and not a view over it. One that stores it (a tuple of the
/// arguments, an exception's `args`) must be handed a new array instead.
final class ArgArrays {
    private static final int MAX = 16;
    private static final Object[][] SPARE = new Object[MAX + 1][];

    private ArgArrays() {
    }

    /// An array of exactly `n` elements, every one null.
    static Object[] take(int n) {
        if (n > MAX) {
            return new Object[n];
        }
        Object[] a = SPARE[n];
        if (a == null) {
            return new Object[n];
        }
        SPARE[n] = null;
        return a;
    }

    /// Hands `a` back, cleared so it holds on to nothing.
    static void give(Object[] a) {
        int n = a.length;
        if (n > MAX) {
            return;
        }
        for (int i = 0; i < n; i++) {
            a[i] = null;
        }
        SPARE[n] = a;
    }

    /// How many calls have built an argument array because the callee had no
    /// fixed-arity path for them. Read by a test that holds the common call
    /// shapes at zero; the count costs one increment on a path that already
    /// allocates.
    static long fallbacks;

    /// Counts `a` as one [#fallbacks] and hands it back.
    static Object[] fallback(Object[] a) {
        fallbacks++;
        return a;
    }

}
