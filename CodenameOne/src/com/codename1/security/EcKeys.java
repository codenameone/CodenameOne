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
package com.codename1.security;

import com.codename1.impl.EcP256Codec;
import com.codename1.util.BigInteger;

/// Completes a P-256 PKCS#8 private key with its public point.
///
/// RFC 5915 makes the `publicKey` field of ECPrivateKey optional, and
/// java.security leaves it out -- so a key generated on a server or on the
/// JavaSE port has none. Apple's SecKey cannot import an EC private key
/// without the point, and cannot derive it, so [Signature#sign] hands the
/// port this completed form. The point is computed once per key (see
/// [PrivateKey]) with a fixed-length Montgomery ladder, see [#multiply].
final class EcKeys {
    private static final BigInteger P = new BigInteger(
            "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff", 16);
    private static final BigInteger GX = new BigInteger(
            "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296", 16);
    private static final BigInteger GY = new BigInteger(
            "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5", 16);

    private EcKeys() {}

    /// Returns `pkcs8` itself when it already carries the point or is not a
    /// P-256 key [EcP256Codec] understands, otherwise the same key re-encoded
    /// with the point.
    static byte[] withPublicPoint(byte[] pkcs8) {
        byte[][] parts;
        try {
            parts = EcP256Codec.fromPkcs8(pkcs8);
        } catch (RuntimeException notP256) {
            return pkcs8;
        }
        if (parts[1] != null) {
            return pkcs8;
        }
        BigInteger[] q = multiply(new BigInteger(1, parts[0]));
        if (q == null) {
            return pkcs8;
        }
        byte[] point = new byte[EcP256Codec.POINT_LENGTH];
        point[0] = 0x04;
        putFixed(q[0], point, 1);
        putFixed(q[1], point, 33);
        return EcP256Codec.pkcs8FromScalar(parts[0], point);
    }

    private static void putFixed(BigInteger v, byte[] out, int off) {
        byte[] b = v.toByteArray();
        int start = b.length > 32 ? b.length - 32 : 0;
        int len = b.length - start;
        System.arraycopy(b, start, out, off + 32 - len, len);
    }

    /// `k * G` in affine coordinates, or null for the point at infinity.
    ///
    /// A Montgomery ladder over all 256 bits of the scalar, on projective
    /// points with the complete addition formulas of Renes, Costello and
    /// Batina ("Complete addition formulas for prime order elliptic curves",
    /// 2016, algorithm 4 for a = -3). Every bit costs the same: one addition
    /// and one doubling, both through the one formula, which has no special
    /// case for the identity or for equal points, and no field inversion until
    /// the single one at the end. The two ladder points trade places by array
    /// index, never by a branch on the bit. What the private scalar decides is
    /// therefore not which operations run, nor how many, nor whether a short
    /// key ends early.
    ///
    /// It is not constant time end to end: the big-integer arithmetic
    /// underneath (`multiply`, `mod`) takes time that depends on its operands'
    /// values and lengths, so the timing of a derivation still leaks something
    /// about the scalar to an observer who can measure it closely and many
    /// times. That is acceptable for its one use here -- deriving a stored
    /// key's public point once, on the device -- and is why a port with a
    /// native derivation should prefer it.
    ///
    /// A scalar outside `[1, n - 1]` is no private key: zero and `n` give the
    /// point at infinity, and anything larger, or negative, would alias a
    /// smaller key or fall outside the 256 bits the ladder walks. It answers
    /// null, as the point at infinity does.
    static BigInteger[] multiply(BigInteger k) {
        if (k == null || k.signum() <= 0 || k.compareTo(N) >= 0) {
            return null;
        }
        BigInteger[][] r = {{BigInteger.ZERO, BigInteger.ONE, BigInteger.ZERO},
            {GX, GY, BigInteger.ONE}};
        for (int i = 255; i >= 0; i--) {
            int bit = k.testBit(i) ? 1 : 0;
            // With bit = 1 the roles swap: r[1] is doubled and r[0] gets the sum.
            BigInteger[] lo = r[bit];
            BigInteger[] hi = r[1 - bit];
            BigInteger[] sum = add(lo, hi);
            BigInteger[] twice = add(lo, lo);
            r[bit] = twice;
            r[1 - bit] = sum;
        }
        BigInteger[] q = r[0];
        if (q[2].signum() == 0) {
            return null;
        }
        // P is prime and 0 < z < P, so the inverse exists (BigIntegerModInverseTest
        // holds modInverse to 256-bit operands).
        BigInteger zInv = q[2].mod(P).modInverse(P);
        return new BigInteger[]{q[0].multiply(zInv).mod(P), q[1].multiply(zInv).mod(P)};
    }

    /// The order of the base point.
    private static final BigInteger N = new BigInteger(
            "ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551", 16);

    /// The curve's `b`.
    private static final BigInteger B = new BigInteger(
            "5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b", 16);

    private static BigInteger m(BigInteger a, BigInteger b) {
        return a.multiply(b).mod(P);
    }

    private static BigInteger p(BigInteger a, BigInteger b) {
        return a.add(b).mod(P);
    }

    private static BigInteger s(BigInteger a, BigInteger b) {
        return a.subtract(b).mod(P);
    }

    /// Complete projective addition, `u + v`, for a = -3 (RCB algorithm 4).
    /// Valid for every pair of points, the identity (0:1:0) and `u == v`
    /// included.
    private static BigInteger[] add(BigInteger[] u, BigInteger[] v) {
        BigInteger x1 = u[0];
        BigInteger y1 = u[1];
        BigInteger z1 = u[2];
        BigInteger x2 = v[0];
        BigInteger y2 = v[1];
        BigInteger z2 = v[2];
        BigInteger t0 = m(x1, x2);
        BigInteger t1 = m(y1, y2);
        BigInteger t2 = m(z1, z2);
        BigInteger t3 = m(p(x1, y1), p(x2, y2));
        BigInteger t4 = p(t0, t1);
        t3 = s(t3, t4);
        t4 = m(p(y1, z1), p(y2, z2));
        BigInteger x3 = p(t1, t2);
        t4 = s(t4, x3);
        x3 = m(p(x1, z1), p(x2, z2));
        BigInteger y3 = p(t0, t2);
        y3 = s(x3, y3);
        BigInteger z3 = m(B, t2);
        x3 = s(y3, z3);
        z3 = p(x3, x3);
        x3 = p(x3, z3);
        z3 = s(t1, x3);
        x3 = p(t1, x3);
        y3 = m(B, y3);
        t1 = p(t2, t2);
        t2 = p(t1, t2);
        y3 = s(y3, t2);
        y3 = s(y3, t0);
        t1 = p(y3, y3);
        y3 = p(t1, y3);
        t1 = p(t0, t0);
        t0 = p(t1, t0);
        t0 = s(t0, t2);
        t1 = m(t4, y3);
        t2 = m(t0, y3);
        y3 = m(x3, z3);
        y3 = p(y3, t2);
        x3 = m(t3, x3);
        x3 = s(x3, t1);
        z3 = m(t4, z3);
        t1 = m(t3, t0);
        z3 = p(z3, t1);
        return new BigInteger[]{x3, y3, z3};
    }
}
