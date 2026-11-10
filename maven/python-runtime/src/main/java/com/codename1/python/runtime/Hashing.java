/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

import com.codename1.util.BigInteger;

/// CPython's hash functions for the types whose hash is deterministic there:
/// numbers and tuples. Matching them is what makes a set of numbers or
/// tuples iterate (and print) in the same order as on CPython, and keeps
/// `hash(1) == hash(1.0) == hash(True)` as the language requires.
final class Hashing {
    static final int BITS = 61;
    /// 2**61 - 1, the prime CPython reduces numeric hashes by.
    static final long MODULUS = (1L << BITS) - 1;
    private static final BigInteger BIG_MODULUS = BigInteger.valueOf(MODULUS);
    private static final long INF = 314159;

    private static final long XXPRIME_1 = -7046029288634856825L;
    private static final long XXPRIME_2 = -4417276706812531889L;
    private static final long XXPRIME_5 = 2870177450012600261L;

    private Hashing() {
    }

    private static long fix(long h) {
        return h == -1 ? -2 : h;
    }

    static long ofLong(long v) {
        if (v >= 0 && v < MODULUS) {
            return v;
        }
        if (v >= 0) {
            return fix(v % MODULUS);
        }
        if (v == Long.MIN_VALUE) {
            // |MIN_VALUE| = 2**63 = 4 * 2**61, which is 4 modulo 2**61 - 1.
            return -4;
        }
        return fix(-((-v) % MODULUS));
    }

    static long ofBig(BigInteger v) {
        long r = v.abs().mod(BIG_MODULUS).longValue();
        return fix(v.signum() < 0 ? -r : r);
    }

    /// `_Py_HashDouble`. NaN hashes by identity in CPython; callers pass the
    /// object for that case.
    static long ofDouble(double v, Object identity) {
        if (Double.isNaN(v)) {
            return System.identityHashCode(identity);
        }
        if (Double.isInfinite(v)) {
            return v > 0 ? INF : -INF;
        }
        if (v == 0) {
            return 0;
        }
        int sign = 1;
        double m = v;
        if (m < 0) {
            sign = -1;
            m = -m;
        }
        // frexp: m in [0.5, 1) and v = m * 2**e.
        long bits = Double.doubleToLongBits(m);
        int exp = (int) ((bits >>> 52) & 0x7ff);
        int e;
        if (exp == 0) {
            // Subnormal: scale into the normal range first.
            m *= 18014398509481984.0; // 2**54
            bits = Double.doubleToLongBits(m);
            exp = (int) ((bits >>> 52) & 0x7ff);
            e = exp - 1022 - 54;
        } else {
            e = exp - 1022;
        }
        m = Double.longBitsToDouble((bits & 0x800fffffffffffffL) | (1022L << 52));
        long x = 0;
        while (m != 0) {
            x = ((x << 28) & MODULUS) | x >>> (BITS - 28);
            m *= 268435456.0; // 2**28
            e -= 28;
            long y = (long) m;
            m -= y;
            x += y;
            if (x >= MODULUS) {
                x -= MODULUS;
            }
        }
        e = e >= 0 ? e % BITS : BITS - 1 - ((-1 - e) % BITS);
        x = ((x << e) & MODULUS) | x >>> (BITS - e);
        return fix(x * sign);
    }

    /// The xxHash-based tuple hash of CPython 3.8+.
    static long ofTuple(Object[] items) {
        long acc = XXPRIME_5;
        for (Object item : items) {
            long lane = Py.hashOf(item);
            acc += lane * XXPRIME_2;
            acc = (acc << 31) | (acc >>> 33);
            acc *= XXPRIME_1;
        }
        acc += items.length ^ (XXPRIME_5 ^ 3527539L);
        return acc == -1 ? 1546275796L : acc;
    }
}
