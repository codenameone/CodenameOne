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
package com.codename1.util;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Holds `modInverse` to `java.math.BigInteger` on the sizes elliptic-curve
/// code uses: 256-bit moduli, odd and even, including the P-256 group order
/// and field prime.
class BigIntegerModInverseTest {

    private static final String P256_ORDER =
            "ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551";
    private static final String P256_PRIME =
            "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff";

    private static void check(java.math.BigInteger a, java.math.BigInteger m) {
        java.math.BigInteger expected;
        try {
            expected = a.modInverse(m);
        } catch (ArithmeticException e) {
            final BigInteger ca = new BigInteger(a.toString(16), 16);
            final BigInteger cm = new BigInteger(m.toString(16), 16);
            assertThrows(ArithmeticException.class, () -> ca.modInverse(cm), "a=" + a.toString(16) + " m=" + m.toString(16));
            return;
        }
        BigInteger actual = new BigInteger(a.toString(16), 16).modInverse(new BigInteger(m.toString(16), 16));
        assertEquals(expected.toString(16), actual.toString(16), "a=" + a.toString(16) + " m=" + m.toString(16));
    }

    @Test
    void namedCurveModuli() {
        Random r = new Random(1234);
        java.math.BigInteger[] mods = {new java.math.BigInteger(P256_ORDER, 16), new java.math.BigInteger(P256_PRIME, 16)};
        for (java.math.BigInteger m : mods) {
            for (int i = 0; i < 500; i++) {
                java.math.BigInteger a = new java.math.BigInteger(256, r).mod(m);
                if (a.signum() == 0) {
                    continue;
                }
                check(a, m);
                check(a.negate(), m);
            }
        }
    }

    @Test
    void randomModuli() {
        Random r = new Random(98765);
        for (int i = 0; i < 2000; i++) {
            int bits = (i % 3 == 0) ? 255 + r.nextInt(3) : 256;
            java.math.BigInteger m = new java.math.BigInteger(bits, r).setBit(bits - 1);
            if ((i & 1) == 0) {
                m = m.setBit(0);
            }
            java.math.BigInteger a = new java.math.BigInteger(256 + r.nextInt(64), r);
            if (a.signum() == 0 || m.compareTo(java.math.BigInteger.ONE) <= 0) {
                continue;
            }
            check(a, m);
        }
    }

    /// The inverse was wrong mostly because division was: `TDivision.divide`
    /// normalized with a 64-bit leading-zero count applied to a 32-bit digit.
    @Test
    void divisionAndBitCount() {
        Random r = new Random(4242);
        for (int i = 0; i < 2000; i++) {
            java.math.BigInteger a = new java.math.BigInteger(64 + r.nextInt(512), r);
            java.math.BigInteger b = new java.math.BigInteger(1 + r.nextInt(300), r).add(java.math.BigInteger.ONE);
            if ((i & 1) == 0) {
                a = a.negate();
            }
            BigInteger ca = new BigInteger(a.toString(16), 16);
            BigInteger cb = new BigInteger(b.toString(16), 16);
            BigInteger[] qr = ca.divideAndRemainder(cb);
            java.math.BigInteger[] eqr = a.divideAndRemainder(b);
            String ctx = "a=" + a.toString(16) + " b=" + b.toString(16);
            assertEquals(eqr[0].toString(16), qr[0].toString(16), ctx);
            assertEquals(eqr[1].toString(16), qr[1].toString(16), ctx);
            assertEquals(a.mod(b).toString(16), ca.mod(cb).toString(16), ctx);
            assertEquals(a.bitCount(), ca.bitCount(), ctx);
            assertEquals(a.gcd(b).toString(16), ca.gcd(cb).toString(16), ctx);
        }
    }
}
