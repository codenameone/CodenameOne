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

    /// SEQUENCE { OID id-ecPublicKey, OID prime256v1 }
    private static final byte[] P256_ALG_ID = {
        0x30, 0x13,
        0x06, 0x07, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x02, 0x01,
        0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x03, 0x01, 0x07
    };

    private EcKeys() {}

    /// Returns `pkcs8` itself when it already carries the point or is not a
    /// P-256 key this parser understands, otherwise the same key re-encoded
    /// with the point.
    static byte[] withPublicPoint(byte[] pkcs8) {
        byte[] d = privateScalarWithoutPoint(pkcs8);
        if (d == null) {
            return pkcs8;
        }
        BigInteger[] q = multiply(new BigInteger(1, d));
        if (q == null) {
            return pkcs8;
        }
        byte[] out = new byte[138];
        int p = 0;
        byte[] head = {0x30, (byte) 0x81, (byte) 0x87, 0x02, 0x01, 0x00};
        System.arraycopy(head, 0, out, p, head.length);
        p += head.length;
        System.arraycopy(P256_ALG_ID, 0, out, p, P256_ALG_ID.length);
        p += P256_ALG_ID.length;
        byte[] mid = {0x04, 0x6d, 0x30, 0x6b, 0x02, 0x01, 0x01, 0x04, 0x20};
        System.arraycopy(mid, 0, out, p, mid.length);
        p += mid.length;
        System.arraycopy(d, 0, out, p + 32 - d.length, d.length);
        p += 32;
        byte[] tail = {(byte) 0xa1, 0x44, 0x03, 0x42, 0x00, 0x04};
        System.arraycopy(tail, 0, out, p, tail.length);
        p += tail.length;
        putFixed(q[0], out, p);
        putFixed(q[1], out, p + 32);
        return out;
    }

    private static void putFixed(BigInteger v, byte[] out, int off) {
        byte[] b = v.toByteArray();
        int start = b.length > 32 ? b.length - 32 : 0;
        int len = b.length - start;
        System.arraycopy(b, start, out, off + 32 - len, len);
    }

    /// The private scalar of a P-256 PKCS#8 key that has no public point, or
    /// null for anything else.
    private static byte[] privateScalarWithoutPoint(byte[] b) {
        int[] p = {0};
        if (enter(b, b.length, p, 0x30) < 0) {
            return null;
        }
        int len = enter(b, b.length, p, 0x02);
        if (len < 0) {
            return null;
        }
        p[0] += len;
        if (p[0] + P256_ALG_ID.length > b.length) {
            return null;
        }
        for (int i = 0; i < P256_ALG_ID.length; i++) {
            if (b[p[0] + i] != P256_ALG_ID[i]) {
                return null;
            }
        }
        p[0] += P256_ALG_ID.length;
        if (enter(b, b.length, p, 0x04) < 0) {
            return null;
        }
        len = enter(b, b.length, p, 0x30);
        if (len < 0) {
            return null;
        }
        int end = p[0] + len;
        len = enter(b, end, p, 0x02);
        if (len < 0) {
            return null;
        }
        p[0] += len;
        int dLen = enter(b, end, p, 0x04);
        if (dLen < 1 || dLen > 32) {
            return null;
        }
        byte[] d = new byte[dLen];
        System.arraycopy(b, p[0], d, 0, dLen);
        p[0] += dLen;
        if (p[0] < end && (b[p[0]] & 0xff) == 0xa0) {
            len = enter(b, end, p, 0xa0);
            if (len < 0) {
                return null;
            }
            p[0] += len;
        }
        if (p[0] < end && (b[p[0]] & 0xff) == 0xa1) {
            return null;
        }
        return d;
    }

    /// Reads a tag and length at `p[0]`, leaving `p[0]` on the content.
    /// Returns the content length, or -1 when the tag differs or the content
    /// does not fit before `n`.
    private static int enter(byte[] b, int n, int[] p, int tag) {
        if (p[0] + 2 > n || (b[p[0]] & 0xff) != tag) {
            return -1;
        }
        p[0]++;
        int first = b[p[0]++] & 0xff;
        int len;
        if ((first & 0x80) == 0) {
            len = first;
        } else {
            int sl = first & 0x7f;
            if (sl < 1 || sl > 3 || p[0] + sl > n) {
                return -1;
            }
            len = 0;
            for (int i = 0; i < sl; i++) {
                len = (len << 8) | (b[p[0]++] & 0xff);
            }
        }
        if (p[0] + len > n) {
            return -1;
        }
        return len;
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
    /// key ends early. The big-integer arithmetic underneath is not itself
    /// constant time; a port with a native derivation should prefer it.
    static BigInteger[] multiply(BigInteger k) {
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
