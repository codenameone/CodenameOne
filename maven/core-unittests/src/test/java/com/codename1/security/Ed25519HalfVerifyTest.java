/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The half-length verify (`Ed25519Math.verifyHalf`) against the plain check it
/// replaced, encode([s]B - [k]A) == R, on keys and R values with and without a
/// small-order component, plus the lattice reduction it rests on.
class Ed25519HalfVerifyTest {

    private static final BigInteger L = BigInteger.ONE.shiftLeft(252)
            .add(new BigInteger("27742317777372353535851937790883648493"));
    private static final BigInteger EIGHT_L = L.shiftLeft(3);

    // a point of order 8
    private static final byte[] T8 = Hash.fromHex("26e8958fc2b227b045c3f489f2ef98f0d5dfac05d3c63339b13802886d53fc05");

    private static BigInteger le(byte[] b) {
        byte[] be = new byte[b.length + 1];
        for (int i = 0; i < b.length; i++) {
            be[b.length - i] = b[i];
        }
        return new BigInteger(be);
    }

    private static byte[] scalar(Random rnd) {
        byte[] wide = new byte[64];
        rnd.nextBytes(wide);
        return Ed25519Math.reduce64(wide);
    }

    private static Ed25519Math.P3 sum(Ed25519Math.P3 p, Ed25519Math.P3 q) {
        Ed25519Math.Cached qc = new Ed25519Math.Cached();
        Ed25519Math.toCached(qc, q);
        Ed25519Math.P3 r = new Ed25519Math.P3();
        Ed25519Math.add(new Ed25519Math.Ctx(), r, p, qc);
        return r;
    }

    private static Ed25519Math.P3 times(Ed25519Math.P3 p, byte[] k) {
        Ed25519Math.Ctx c = new Ed25519Math.Ctx();
        Ed25519Math.Cached pc = new Ed25519Math.Cached();
        Ed25519Math.toCached(pc, p);
        Ed25519Math.P3 r = new Ed25519Math.P3();
        r.identity();
        for (int i = 255; i >= 0; i--) {
            Ed25519Math.dbl(c, r, r);
            if (((k[i >> 3] >> (i & 7)) & 1) != 0) {
                Ed25519Math.add(c, r, r, pc);
            }
        }
        return r;
    }

    private static Ed25519Math.P3 base(byte[] k) {
        Ed25519Math.P3 r = new Ed25519Math.P3();
        Ed25519Math.mulBase(r, k);
        return r;
    }

    private static byte[] enc(Ed25519Math.P3 p) {
        byte[] out = new byte[32];
        Ed25519Math.encode(out, 0, p);
        return out;
    }

    private static Ed25519Math.P3 dec(byte[] b) {
        Ed25519Math.P3 p = new Ed25519Math.P3();
        assertTrue(Ed25519Math.decode(p, b, 0));
        return p;
    }

    private static byte[] challenge(byte[] r, byte[] a, byte[] m) {
        Hash d = Hash.create(Hash.SHA512);
        d.update(r);
        d.update(a);
        d.update(m);
        return Ed25519Math.reduce64(d.digest());
    }

    // the check verify made before: encode([s]B - [k]A) == R
    private static boolean reference(byte[] m, byte[] sig, byte[] pub) {
        if (!Ed25519Math.isCanonicalScalar(sig, 32)) {
            return false;
        }
        Ed25519Math.P3 a = new Ed25519Math.P3();
        if (!Ed25519Math.decode(a, pub, 0)) {
            return false;
        }
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(sig, 0, r, 0, 32);
        System.arraycopy(sig, 32, s, 0, 32);
        Ed25519Math.P3 ka = times(a, challenge(r, pub, m));
        Ed25519Math.Cached kac = new Ed25519Math.Cached();
        Ed25519Math.toCached(kac, ka);
        Ed25519Math.P3 p = new Ed25519Math.P3();
        Ed25519Math.sub(new Ed25519Math.Ctx(), p, base(s), kac);
        byte[] e = enc(p);
        for (int i = 0; i < 32; i++) {
            if (e[i] != r[i]) {
                return false;
            }
        }
        return true;
    }

    @Test
    void karatsubaProductMatchesRef10() {
        // both paths on every machine: mul picks one of them by architecture
        Random rnd = new Random(5);
        for (int trial = 0; trial < 200000; trial++) {
            int[] f = new int[10];
            int[] g = new int[10];
            for (int i = 0; i < 10; i++) {
                int bound = (i % 2 == 0 ? 1 << 25 : 1 << 24) * (1 + trial % 3);
                f[i] = trial % 7 == 0 ? (i % 2 == 0 ? bound : -bound) : rnd.nextInt(2 * bound + 1) - bound;
                g[i] = trial % 11 == 0 ? -bound : rnd.nextInt(2 * bound + 1) - bound;
                if (trial % 13 == 0) {
                    // the worst case: every limb of both at the same-signed maximum
                    int sign = trial % 26 == 0 ? 1 : -1;
                    f[i] = sign * bound;
                    g[i] = sign * bound;
                }
            }
            int[] h1 = new int[10];
            int[] h2 = new int[10];
            Ed25519Math.mul10(h1, f, g);
            Ed25519Math.kmul(h2, f, g);
            byte[] b1 = new byte[32];
            byte[] b2 = new byte[32];
            Ed25519Math.toBytes(b1, 0, h1);
            Ed25519Math.toBytes(b2, 0, h2);
            assertArrayEquals(b1, b2, "trial " + trial);
        }
    }

    @Test
    void theTestPointHasOrderEight() {
        Ed25519Math.P3 t = dec(T8);
        byte[] four = new byte[32];
        four[0] = 4;
        byte[] eight = new byte[32];
        eight[0] = 8;
        assertFalse(java.util.Arrays.equals(enc(times(t, four)), enc(times(t, new byte[32]))));
        assertArrayEquals(enc(times(t, new byte[32])), enc(times(t, eight)));
    }

    @Test
    void reductionFindsAShortOddRow() {
        Random rnd = new Random(7);
        byte[][] ks = new byte[203][];
        ks[0] = new byte[32];
        ks[1] = new byte[32];
        ks[1][0] = 1;
        byte[] lm1 = L.subtract(BigInteger.ONE).toByteArray();
        ks[2] = new byte[32];
        for (int i = 0; i < lm1.length && i < 32; i++) {
            ks[2][i] = lm1[lm1.length - 1 - i];
        }
        for (int i = 3; i < ks.length; i++) {
            ks[i] = scalar(rnd);
        }
        for (byte[] k : ks) {
            byte[] v0 = new byte[32];
            byte[] v1 = new byte[32];
            boolean neg = Ed25519Math.reduceBasis(k, v0, v1);
            BigInteger a = le(v0);
            BigInteger b = le(v1);
            if (neg) {
                a = a.negate();
            }
            assertTrue(b.testBit(0), "v1 odd");
            assertEquals(a.mod(EIGHT_L), b.multiply(le(k)).mod(EIGHT_L));
            // the edge cases may take the full-length row (k, 1); a random k gets a row near
            // 2^127, a few bits longer when the shortest one has an even v1
            assertTrue(k == ks[0] || k == ks[1] || k == ks[2]
                    || b.bitLength() <= 140 && a.abs().bitLength() <= 140,
                    "short: " + a.bitLength() + " " + b.bitLength());
        }
    }

    // encode([s]B - [k]A) == encode(R) for a k of the caller's choosing
    private static boolean referenceWithK(byte[] k, byte[] s, Ed25519Math.P3 a, Ed25519Math.P3 r) {
        Ed25519Math.Cached kac = new Ed25519Math.Cached();
        Ed25519Math.toCached(kac, times(a, k));
        Ed25519Math.P3 p = new Ed25519Math.P3();
        Ed25519Math.sub(new Ed25519Math.Ctx(), p, base(s), kac);
        return java.util.Arrays.equals(enc(p), enc(r));
    }

    @Test
    void theFullLengthRowVerifiesToo() {
        // challenges a hash cannot be steered to, whose rows are the fallback (k, 1)
        Random rnd = new Random(9);
        Ed25519Math.P3 t = dec(T8);
        byte[] lm1 = L.subtract(BigInteger.ONE).toByteArray();
        byte[] k = new byte[32];
        for (int i = 0; i < 32; i++) {
            k[i] = lm1[lm1.length - 1 - i];
        }
        byte[] v0 = new byte[32];
        byte[] v1 = new byte[32];
        Ed25519Math.reduceBasis(k, v0, v1);
        assertArrayEquals(k, v0);
        int valid = 0;
        for (int trial = 0; trial < 16; trial++) {
            byte[] a = scalar(rnd);
            byte[] r = scalar(rnd);
            Ed25519Math.P3 ap = trial % 2 == 0 ? base(a) : sum(base(a), t);
            Ed25519Math.P3 rp = trial % 4 < 2 ? base(r) : sum(base(r), t);
            byte[] s = Ed25519Math.mulAdd(k, a, r);
            if (trial % 8 == 7) {
                s[3] ^= 0x10;
            }
            Ed25519Math.P3 ad = dec(enc(ap));
            Ed25519Math.P3 rd = dec(enc(rp));
            boolean want = referenceWithK(k, s, ad, rd);
            assertEquals(want, Ed25519Math.verifyHalf(k, s, ad, rd), "trial " + trial);
            if (want) {
                valid++;
            }
        }
        assertTrue(valid > 0 && valid < 16, valid + " valid");
    }

    @Test
    void agreesWithThePlainCheckWithAndWithoutTorsion() {
        Random rnd = new Random(42);
        Ed25519Math.P3 t = dec(T8);
        Ed25519Math.P3[] torsion = new Ed25519Math.P3[8];
        for (int i = 0; i < 8; i++) {
            byte[] n = new byte[32];
            n[0] = (byte) i;
            torsion[i] = times(t, n);
        }
        int valid = 0;
        int validWithTorsion = 0;
        int invalid = 0;
        for (int trial = 0; trial < 120; trial++) {
            byte[] a = scalar(rnd);
            byte[] r = scalar(rnd);
            byte[] m = new byte[1 + rnd.nextInt(40)];
            rnd.nextBytes(m);
            int ta = trial % 8;
            byte[] pub = enc(sum(base(a), torsion[ta]));
            for (int tr = 0; tr < 8; tr++) {
                byte[] rEnc = enc(sum(base(r), torsion[tr]));
                byte[] k = challenge(rEnc, pub, m);
                byte[] s = Ed25519Math.mulAdd(k, a, r);
                byte[] sig = new byte[64];
                System.arraycopy(rEnc, 0, sig, 0, 32);
                System.arraycopy(s, 0, sig, 32, 32);
                if (trial % 5 == 4) {
                    sig[32 + rnd.nextInt(16)] ^= (byte) (1 << rnd.nextInt(8));
                }
                boolean want = reference(m, sig, pub);
                assertEquals(want, Ed25519.verify(m, sig, pub), "trial " + trial + " A+" + ta + "T R+" + tr + "T");
                // and directly, so an engine installed in this JVM cannot hide it
                byte[] sOnly = new byte[32];
                System.arraycopy(sig, 32, sOnly, 0, 32);
                assertEquals(want, Ed25519Math.isCanonicalScalar(sig, 32)
                        && Ed25519Math.verifyHalf(k, sOnly, dec(pub), dec(rEnc)));
                if (want) {
                    valid++;
                    if (ta != 0 || tr != 0) {
                        validWithTorsion++;
                    }
                } else {
                    invalid++;
                }
            }
        }
        assertTrue(valid > 0 && validWithTorsion > 0 && invalid > 0,
                valid + " valid, " + validWithTorsion + " with torsion, " + invalid + " invalid");
    }
}
