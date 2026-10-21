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
/// [PrivateKey]) with plain affine double-and-add. That is not constant
/// time, which is acceptable only because it runs once per key rather than
/// once per signature.
final class EcKeys {
    private static final BigInteger P = new BigInteger(
            "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff", 16);
    private static final BigInteger A = P.subtract(BigInteger.valueOf(3));
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
    static BigInteger[] multiply(BigInteger k) {
        BigInteger[] r = null;
        BigInteger[] g = {GX, GY};
        for (int i = k.bitLength() - 1; i >= 0; i--) {
            r = add(r, r);
            if (k.testBit(i)) {
                r = add(r, g);
            }
        }
        return r;
    }

    private static BigInteger[] add(BigInteger[] u, BigInteger[] v) {
        if (u == null) {
            return v;
        }
        if (v == null) {
            return u;
        }
        BigInteger lambda;
        if (u[0].equals(v[0])) {
            if (!u[1].equals(v[1]) || u[1].signum() == 0) {
                return null;
            }
            BigInteger num = u[0].multiply(u[0]).multiply(BigInteger.valueOf(3)).add(A);
            lambda = num.multiply(u[1].shiftLeft(1).modInverse(P)).mod(P);
        } else {
            lambda = v[1].subtract(u[1]).multiply(v[0].subtract(u[0]).mod(P).modInverse(P)).mod(P);
        }
        BigInteger x = lambda.multiply(lambda).subtract(u[0]).subtract(v[0]).mod(P);
        BigInteger y = lambda.multiply(u[0].subtract(x)).subtract(u[1]).mod(P);
        return new BigInteger[]{x, y};
    }
}
