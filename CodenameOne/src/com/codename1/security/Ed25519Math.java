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

/// Field, group and scalar arithmetic for Ed25519 ([Ed25519]).
///
/// The field element representation and the group formulas follow the
/// public-domain "ref10" implementation by Bernstein, Duif, Lange, Schwabe
/// and Yang: ten signed limbs alternating 26 and 25 bits, extended twisted
/// Edwards coordinates, and a fixed-base table of 32 x 8 points for signing.
/// Verification differs: it is a variable-time Strauss double-scalar
/// multiplication over width-5 and width-8 NAFs.
@com.codename1.impl.SharedWithBackend
final class Ed25519Math {
    private Ed25519Math() {
    }

    static final int[] D = {56195235, 13857412, 51736253, 6949390, 114729, 24766616, 60832955, 30306712, 48412415, 21499315};
    static final int[] D2 = {45281625, 27714825, 36363642, 13898781, 229458, 15978800, 54557047, 27058993, 29715967, 9444199};
    static final int[] SQRTM1 = {34513072, 25610706, 9377949, 3500415, 12389472, 33281959, 41962654, 31548777, 326685, 11406482};
    static final int[] BX = {52811034, 25909283, 16144682, 17082669, 27570973, 30858332, 40966398, 8378388, 20764389, 8758491};
    static final int[] BY = {40265304, 26843545, 13421772, 20132659, 26843545, 6710886, 53687091, 13421772, 40265318, 26843545};

    // ------------------------------------------------------------------
    // field elements mod 2^255 - 19: int[10], limb i weighs 2^ceil(25.5 i)

    static int[] fe() {
        return new int[10];
    }

    static void one(int[] h) {
        h[0] = 1;
        for (int i = 1; i < 10; i++) {
            h[i] = 0;
        }
    }

    static void zero(int[] h) {
        for (int i = 0; i < 10; i++) {
            h[i] = 0;
        }
    }

    static void copy(int[] h, int[] f) {
        System.arraycopy(f, 0, h, 0, 10);
    }

    static void add(int[] h, int[] f, int[] g) {
        for (int i = 0; i < 10; i++) {
            h[i] = f[i] + g[i];
        }
    }

    static void sub(int[] h, int[] f, int[] g) {
        for (int i = 0; i < 10; i++) {
            h[i] = f[i] - g[i];
        }
    }

    static void neg(int[] h, int[] f) {
        for (int i = 0; i < 10; i++) {
            h[i] = -f[i];
        }
    }

    // brings every limb back into its 26 or 25 bits, so an element that was
    // the sum or difference of several can go into another multiplication
    static void carry(int[] h) {
        int c;
        for (int i = 0; i < 9; i++) {
            int bits = (i & 1) == 0 ? 26 : 25;
            c = h[i] >> bits;
            h[i + 1] += c;
            h[i] -= c << bits;
        }
        c = h[9] >> 25;
        h[9] -= c << 25;
        h[0] += c * 19;
        c = h[0] >> 26;
        h[0] -= c << 26;
        h[1] += c;
    }

    // conditional move: h = f when flag is 1, unchanged when 0
    static void cmov(int[] h, int[] f, int flag) {
        int mask = -flag;
        for (int i = 0; i < 10; i++) {
            h[i] ^= (h[i] ^ f[i]) & mask;
        }
    }

    static void mul(int[] h, int[] f, int[] g) {
        long f0 = f[0];
        long f1 = f[1];
        long f2 = f[2];
        long f3 = f[3];
        long f4 = f[4];
        long f5 = f[5];
        long f6 = f[6];
        long f7 = f[7];
        long f8 = f[8];
        long f9 = f[9];
        long g0 = g[0];
        long g1 = g[1];
        long g2 = g[2];
        long g3 = g[3];
        long g4 = g[4];
        long g5 = g[5];
        long g6 = g[6];
        long g7 = g[7];
        long g8 = g[8];
        long g9 = g[9];
        long g1_19 = 19 * g1;
        long g2_19 = 19 * g2;
        long g3_19 = 19 * g3;
        long g4_19 = 19 * g4;
        long g5_19 = 19 * g5;
        long g6_19 = 19 * g6;
        long g7_19 = 19 * g7;
        long g8_19 = 19 * g8;
        long g9_19 = 19 * g9;
        long f1_2 = 2 * f1;
        long f3_2 = 2 * f3;
        long f5_2 = 2 * f5;
        long f7_2 = 2 * f7;
        long f9_2 = 2 * f9;
        long h0 = f0 * g0 + f1_2 * g9_19 + f2 * g8_19 + f3_2 * g7_19 + f4 * g6_19 + f5_2 * g5_19 + f6 * g4_19 + f7_2 * g3_19 + f8 * g2_19 + f9_2 * g1_19;
        long h1 = f0 * g1 + f1 * g0 + f2 * g9_19 + f3 * g8_19 + f4 * g7_19 + f5 * g6_19 + f6 * g5_19 + f7 * g4_19 + f8 * g3_19 + f9 * g2_19;
        long h2 = f0 * g2 + f1_2 * g1 + f2 * g0 + f3_2 * g9_19 + f4 * g8_19 + f5_2 * g7_19 + f6 * g6_19 + f7_2 * g5_19 + f8 * g4_19 + f9_2 * g3_19;
        long h3 = f0 * g3 + f1 * g2 + f2 * g1 + f3 * g0 + f4 * g9_19 + f5 * g8_19 + f6 * g7_19 + f7 * g6_19 + f8 * g5_19 + f9 * g4_19;
        long h4 = f0 * g4 + f1_2 * g3 + f2 * g2 + f3_2 * g1 + f4 * g0 + f5_2 * g9_19 + f6 * g8_19 + f7_2 * g7_19 + f8 * g6_19 + f9_2 * g5_19;
        long h5 = f0 * g5 + f1 * g4 + f2 * g3 + f3 * g2 + f4 * g1 + f5 * g0 + f6 * g9_19 + f7 * g8_19 + f8 * g7_19 + f9 * g6_19;
        long h6 = f0 * g6 + f1_2 * g5 + f2 * g4 + f3_2 * g3 + f4 * g2 + f5_2 * g1 + f6 * g0 + f7_2 * g9_19 + f8 * g8_19 + f9_2 * g7_19;
        long h7 = f0 * g7 + f1 * g6 + f2 * g5 + f3 * g4 + f4 * g3 + f5 * g2 + f6 * g1 + f7 * g0 + f8 * g9_19 + f9 * g8_19;
        long h8 = f0 * g8 + f1_2 * g7 + f2 * g6 + f3_2 * g5 + f4 * g4 + f5_2 * g3 + f6 * g2 + f7_2 * g1 + f8 * g0 + f9_2 * g9_19;
        long h9 = f0 * g9 + f1 * g8 + f2 * g7 + f3 * g6 + f4 * g5 + f5 * g4 + f6 * g3 + f7 * g2 + f8 * g1 + f9 * g0;
        long c;
        c = h0 >> 26; h1 += c; h0 -= c << 26;
        c = h4 >> 26; h5 += c; h4 -= c << 26;
        c = h1 >> 25; h2 += c; h1 -= c << 25;
        c = h5 >> 25; h6 += c; h5 -= c << 25;
        c = h2 >> 26; h3 += c; h2 -= c << 26;
        c = h6 >> 26; h7 += c; h6 -= c << 26;
        c = h3 >> 25; h4 += c; h3 -= c << 25;
        c = h7 >> 25; h8 += c; h7 -= c << 25;
        c = h4 >> 26; h5 += c; h4 -= c << 26;
        c = h8 >> 26; h9 += c; h8 -= c << 26;
        c = h9 >> 25; h0 += c * 19; h9 -= c << 25;
        c = h0 >> 26; h1 += c; h0 -= c << 26;
        h[0] = (int) h0;
        h[1] = (int) h1;
        h[2] = (int) h2;
        h[3] = (int) h3;
        h[4] = (int) h4;
        h[5] = (int) h5;
        h[6] = (int) h6;
        h[7] = (int) h7;
        h[8] = (int) h8;
        h[9] = (int) h9;
    }

    static void sqr(int[] h, int[] f) {
        long f0 = f[0];
        long f1 = f[1];
        long f2 = f[2];
        long f3 = f[3];
        long f4 = f[4];
        long f5 = f[5];
        long f6 = f[6];
        long f7 = f[7];
        long f8 = f[8];
        long f9 = f[9];
        long f5_19 = 19 * f5;
        long f6_19 = 19 * f6;
        long f7_19 = 19 * f7;
        long f8_19 = 19 * f8;
        long f9_19 = 19 * f9;
        long f1_2 = 2 * f1;
        long f3_2 = 2 * f3;
        long f5_2 = 2 * f5;
        long f7_2 = 2 * f7;
        long f9_2 = 2 * f9;
        long f0_d = 2 * f0;
        long f1_d = 2 * f1;
        long f2_d = 2 * f2;
        long f3_d = 2 * f3;
        long f4_d = 2 * f4;
        long f5_d = 2 * f5;
        long f6_d = 2 * f6;
        long f7_d = 2 * f7;
        long f8_d = 2 * f8;
        long h0 = f0 * f0 + f1_d * f9_19 * 2 + f2_d * f8_19 + f3_d * f7_19 * 2 + f4_d * f6_19 + f5_2 * f5_19;
        long h1 = f0_d * f1 + f2_d * f9_19 + f3_d * f8_19 + f4_d * f7_19 + f5_d * f6_19;
        long h2 = f0_d * f2 + f1_2 * f1 + f3_d * f9_19 * 2 + f4_d * f8_19 + f5_d * f7_19 * 2 + f6 * f6_19;
        long h3 = f0_d * f3 + f1_d * f2 + f4_d * f9_19 + f5_d * f8_19 + f6_d * f7_19;
        long h4 = f0_d * f4 + f1_d * f3 * 2 + f2 * f2 + f5_d * f9_19 * 2 + f6_d * f8_19 + f7_2 * f7_19;
        long h5 = f0_d * f5 + f1_d * f4 + f2_d * f3 + f6_d * f9_19 + f7_d * f8_19;
        long h6 = f0_d * f6 + f1_d * f5 * 2 + f2_d * f4 + f3_2 * f3 + f7_d * f9_19 * 2 + f8 * f8_19;
        long h7 = f0_d * f7 + f1_d * f6 + f2_d * f5 + f3_d * f4 + f8_d * f9_19;
        long h8 = f0_d * f8 + f1_d * f7 * 2 + f2_d * f6 + f3_d * f5 * 2 + f4 * f4 + f9_2 * f9_19;
        long h9 = f0_d * f9 + f1_d * f8 + f2_d * f7 + f3_d * f6 + f4_d * f5;
        long c;
        c = h0 >> 26; h1 += c; h0 -= c << 26;
        c = h4 >> 26; h5 += c; h4 -= c << 26;
        c = h1 >> 25; h2 += c; h1 -= c << 25;
        c = h5 >> 25; h6 += c; h5 -= c << 25;
        c = h2 >> 26; h3 += c; h2 -= c << 26;
        c = h6 >> 26; h7 += c; h6 -= c << 26;
        c = h3 >> 25; h4 += c; h3 -= c << 25;
        c = h7 >> 25; h8 += c; h7 -= c << 25;
        c = h4 >> 26; h5 += c; h4 -= c << 26;
        c = h8 >> 26; h9 += c; h8 -= c << 26;
        c = h9 >> 25; h0 += c * 19; h9 -= c << 25;
        c = h0 >> 26; h1 += c; h0 -= c << 26;
        h[0] = (int) h0;
        h[1] = (int) h1;
        h[2] = (int) h2;
        h[3] = (int) h3;
        h[4] = (int) h4;
        h[5] = (int) h5;
        h[6] = (int) h6;
        h[7] = (int) h7;
        h[8] = (int) h8;
        h[9] = (int) h9;
    }

    static void sqrN(int[] h, int[] f, int n) {
        sqr(h, f);
        for (int i = 1; i < n; i++) {
            sqr(h, h);
        }
    }

    // z^(2^250 - 1), and z^11 in z11
    private static void pow250(int[] out, int[] z11, int[] z) {
        int[] t0 = fe();
        int[] t1 = fe();
        int[] t2 = fe();
        sqr(t0, z);
        sqrN(t1, t0, 2);
        mul(t1, z, t1);
        mul(z11, t0, t1);
        sqr(t0, z11);
        mul(t0, t1, t0);
        sqrN(t1, t0, 5);
        mul(t0, t1, t0);
        sqrN(t1, t0, 10);
        mul(t1, t1, t0);
        sqrN(t2, t1, 20);
        mul(t1, t2, t1);
        sqrN(t1, t1, 10);
        mul(t0, t1, t0);
        sqrN(t1, t0, 50);
        mul(t1, t1, t0);
        sqrN(t2, t1, 100);
        mul(t1, t2, t1);
        sqrN(t1, t1, 50);
        mul(out, t1, t0);
    }

    static void invert(int[] out, int[] z) {
        int[] t = fe();
        int[] z11 = fe();
        pow250(t, z11, z);
        sqrN(t, t, 5);
        mul(out, t, z11);
    }

    // z^((p - 5) / 8) = z^(2^252 - 3)
    static void pow22523(int[] out, int[] z) {
        int[] t = fe();
        int[] z11 = fe();
        pow250(t, z11, z);
        sqrN(t, t, 2);
        mul(out, t, z);
    }

    static void toBytes(byte[] s, int off, int[] f) {
        int[] h = fe();
        copy(h, f);
        carry(h);
        carry(h);
        for (int i = 0; i < 9; i++) {
            int bits = (i & 1) == 0 ? 26 : 25;
            int k = h[i] >> bits;
            h[i + 1] += k;
            h[i] -= k << bits;
        }
        // h is now below 2^255 + 2^26; add 19 and see whether 2^255 carries
        int[] t = fe();
        copy(t, h);
        t[0] += 19;
        int c = 0;
        for (int i = 0; i < 10; i++) {
            int bits = (i & 1) == 0 ? 26 : 25;
            t[i] += c;
            c = t[i] >> bits;
            t[i] -= c << bits;
        }
        // c is 1 exactly when h >= p, and t is then h - p
        cmov(h, t, c);
        long acc = 0;
        int accBits = 0;
        int o = off;
        for (int i = 0; i < 10; i++) {
            acc |= ((long) h[i]) << accBits;
            accBits += (i & 1) == 0 ? 26 : 25;
            while (accBits >= 8) {
                s[o++] = (byte) acc;
                acc >>>= 8;
                accBits -= 8;
            }
        }
        while (o < off + 32) {
            s[o++] = (byte) acc;
            acc >>>= 8;
        }
    }

    // the low 255 bits of 32 little-endian bytes; the top bit is ignored
    static void fromBytes(int[] h, byte[] s, int off) {
        long acc = 0;
        int accBits = 0;
        int o = off;
        for (int i = 0; i < 10; i++) {
            int bits = (i & 1) == 0 ? 26 : 25;
            while (accBits < bits) {
                int b = s[o] & 0xff;
                if (o == off + 31) {
                    b &= 0x7f;
                }
                o++;
                acc |= ((long) b) << accBits;
                accBits += 8;
            }
            h[i] = (int) (acc & ((1L << bits) - 1));
            acc >>>= bits;
            accBits -= bits;
        }
    }

    static boolean isNegative(int[] f) {
        byte[] s = new byte[32];
        toBytes(s, 0, f);
        return (s[0] & 1) != 0;
    }

    static boolean isZero(int[] f) {
        byte[] s = new byte[32];
        toBytes(s, 0, f);
        int r = 0;
        for (int i = 0; i < 32; i++) {
            r |= s[i];
        }
        return r == 0;
    }

    // ------------------------------------------------------------------
    // points: extended (X : Y : Z : T) with x = X/Z, y = Y/Z, xy = T/Z

    static final class P3 {
        final int[] x = fe();
        final int[] y = fe();
        final int[] z = fe();
        final int[] t = fe();

        void identity() {
            zero(x);
            one(y);
            one(z);
            zero(t);
        }
    }

    // a point ready to be added: (Y + X, Y - X, Z, 2 d T)
    static final class Cached {
        final int[] yPlusX = fe();
        final int[] yMinusX = fe();
        final int[] z = fe();
        final int[] t2d = fe();
    }

    // an affine point ready to be added: (y + x, y - x, 2 d x y)
    static final class Precomp {
        final int[] yPlusX = fe();
        final int[] yMinusX = fe();
        final int[] xy2d = fe();

        void identity() {
            one(yPlusX);
            one(yMinusX);
            zero(xy2d);
        }
    }

    // scratch for the formulas below
    static final class Ctx {
        final int[] a = fe();
        final int[] b = fe();
        final int[] c = fe();
        final int[] d = fe();
        final int[] e = fe();
        final int[] f = fe();
        final int[] g = fe();
        final int[] h = fe();
        // whether completed() computes T; cleared only around an addition
        // whose result goes straight into a doubling
        boolean needT = true;
    }

    static void toCached(Cached r, P3 p) {
        add(r.yPlusX, p.y, p.x);
        sub(r.yMinusX, p.y, p.x);
        carry(r.yPlusX);
        carry(r.yMinusX);
        copy(r.z, p.z);
        mul(r.t2d, p.t, D2);
    }

    // r = 2 p (dbl-2008-hwcd with a = -1)
    static void dbl(Ctx c, P3 r, P3 p) {
        dbl(c, r, p, true);
    }

    // without T when only another doubling reads the result
    static void dbl(Ctx c, P3 r, P3 p, boolean needT) {
        sqr(c.a, p.x);
        sqr(c.b, p.y);
        sqr(c.c, p.z);
        add(c.c, c.c, c.c);
        add(c.d, p.x, p.y);
        sqr(c.d, c.d);
        // E = (X + Y)^2 - A - B, G = B - A, F = G - C, H = -A - B
        neg(c.h, c.a);
        sub(c.h, c.h, c.b);
        sub(c.g, c.b, c.a);
        add(c.e, c.d, c.h);
        sub(c.f, c.g, c.c);
        carry(c.e);
        carry(c.f);
        mul(r.x, c.e, c.f);
        mul(r.y, c.g, c.h);
        mul(r.z, c.f, c.g);
        if (needT) {
            mul(r.t, c.e, c.h);
        }
    }

    // r = p + q (add-2008-hwcd-3)
    static void add(Ctx c, P3 r, P3 p, Cached q) {
        add(c.a, p.y, p.x);
        sub(c.b, p.y, p.x);
        mul(c.a, c.a, q.yPlusX);
        mul(c.b, c.b, q.yMinusX);
        mul(c.c, q.t2d, p.t);
        mul(c.d, p.z, q.z);
        add(c.d, c.d, c.d);
        completed(c, r);
    }

    // r = p - q
    static void sub(Ctx c, P3 r, P3 p, Cached q) {
        add(c.a, p.y, p.x);
        sub(c.b, p.y, p.x);
        mul(c.a, c.a, q.yMinusX);
        mul(c.b, c.b, q.yPlusX);
        mul(c.c, q.t2d, p.t);
        neg(c.c, c.c);
        mul(c.d, p.z, q.z);
        add(c.d, c.d, c.d);
        completed(c, r);
    }

    // r = p + q for an affine q
    static void madd(Ctx c, P3 r, P3 p, Precomp q) {
        add(c.a, p.y, p.x);
        sub(c.b, p.y, p.x);
        mul(c.a, c.a, q.yPlusX);
        mul(c.b, c.b, q.yMinusX);
        mul(c.c, q.xy2d, p.t);
        add(c.d, p.z, p.z);
        completed(c, r);
    }

    // r = p - q for an affine q
    static void msub(Ctx c, P3 r, P3 p, Precomp q) {
        add(c.a, p.y, p.x);
        sub(c.b, p.y, p.x);
        mul(c.a, c.a, q.yMinusX);
        mul(c.b, c.b, q.yPlusX);
        mul(c.c, q.xy2d, p.t);
        neg(c.c, c.c);
        add(c.d, p.z, p.z);
        completed(c, r);
    }

    // from A = (Y1+X1)(Y2+X2), B = (Y1-X1)(Y2-X2), C = 2d T1 T2, D = 2 Z1 Z2
    private static void completed(Ctx c, P3 r) {
        sub(c.e, c.a, c.b);
        add(c.h, c.a, c.b);
        add(c.g, c.d, c.c);
        sub(c.f, c.d, c.c);
        mul(r.x, c.e, c.f);
        mul(r.y, c.g, c.h);
        mul(r.z, c.f, c.g);
        if (c.needT) {
            mul(r.t, c.e, c.h);
        }
    }

    static void encode(byte[] out, int off, P3 p) {
        int[] zi = fe();
        int[] x = fe();
        int[] y = fe();
        invert(zi, p.z);
        mul(x, p.x, zi);
        mul(y, p.y, zi);
        toBytes(out, off, y);
        if (isNegative(x)) {
            out[off + 31] |= (byte) 0x80;
        }
    }

    // false when the bytes are not a canonical encoding of a curve point
    static boolean decode(P3 r, byte[] s, int off) {
        int[] u = fe();
        int[] v = fe();
        int[] v3 = fe();
        int[] chk = fe();
        fromBytes(r.y, s, off);
        byte[] back = new byte[32];
        toBytes(back, 0, r.y);
        for (int i = 0; i < 31; i++) {
            if (back[i] != s[off + i]) {
                return false;
            }
        }
        if (back[31] != (byte) (s[off + 31] & 0x7f)) {
            return false;
        }
        one(r.z);
        sqr(u, r.y);
        mul(v, u, D);
        sub(u, u, r.z);
        add(v, v, r.z);
        carry(u);
        carry(v);
        sqr(v3, v);
        mul(v3, v3, v);
        sqr(r.x, v3);
        mul(r.x, r.x, v);
        mul(r.x, r.x, u);
        pow22523(r.x, r.x);
        mul(r.x, r.x, v3);
        mul(r.x, r.x, u);
        sqr(chk, r.x);
        mul(chk, chk, v);
        sub(chk, chk, u);
        if (!isZero(chk)) {
            add(chk, chk, u);
            add(chk, chk, u);
            if (!isZero(chk)) {
                return false;
            }
            mul(r.x, r.x, SQRTM1);
        }
        boolean sign = (s[off + 31] & 0x80) != 0;
        if (isNegative(r.x) != sign) {
            if (isZero(r.x)) {
                return false;
            }
            neg(r.x, r.x);
        }
        mul(r.t, r.x, r.y);
        return true;
    }

    // ------------------------------------------------------------------
    // fixed-base multiplication: BASE[i][j] = (j + 1) 256^i B

    private static final class BaseTable {
        static final int[][] FLAT = flatten(build());

        // each row as one array: entry j's y + x, y - x and 2dxy at 30 j
        private static int[][] flatten(Precomp[][] t) {
            int[][] out = new int[t.length][];
            for (int i = 0; i < t.length; i++) {
                int[] row = new int[8 * 30];
                for (int j = 0; j < 8; j++) {
                    System.arraycopy(t[i][j].yPlusX, 0, row, 30 * j, 10);
                    System.arraycopy(t[i][j].yMinusX, 0, row, 30 * j + 10, 10);
                    System.arraycopy(t[i][j].xy2d, 0, row, 30 * j + 20, 10);
                }
                out[i] = row;
            }
            return out;
        }

        private static Precomp[][] build() {
            Ctx c = new Ctx();
            Precomp[][] t = new Precomp[32][8];
            P3 row = new P3();
            copy(row.x, BX);
            copy(row.y, BY);
            one(row.z);
            mul(row.t, row.x, row.y);
            P3[] pts = new P3[256];
            Cached cr = new Cached();
            for (int i = 0; i < 32; i++) {
                toCached(cr, row);
                P3 acc = row;
                for (int j = 0; j < 8; j++) {
                    pts[i * 8 + j] = acc;
                    if (j < 7) {
                        P3 next = new P3();
                        add(c, next, acc, cr);
                        acc = next;
                    }
                }
                P3 next = row;
                for (int k = 0; k < 8; k++) {
                    P3 d = new P3();
                    dbl(c, d, next);
                    next = d;
                }
                row = next;
            }
            Precomp[] flat = toPrecomp(pts);
            for (int i = 0; i < 32; i++) {
                for (int j = 0; j < 8; j++) {
                    t[i][j] = flat[i * 8 + j];
                }
            }
            return t;
        }
    }

    // affine precomputed forms of many points, with one inversion
    static Precomp[] toPrecomp(P3[] pts) {
        int n = pts.length;
        int[][] acc = new int[n][];
        int[] run = fe();
        one(run);
        for (int i = 0; i < n; i++) {
            acc[i] = fe();
            copy(acc[i], run);
            mul(run, run, pts[i].z);
        }
        int[] inv = fe();
        invert(inv, run);
        Precomp[] out = new Precomp[n];
        int[] zi = fe();
        int[] x = fe();
        int[] y = fe();
        for (int i = n - 1; i >= 0; i--) {
            mul(zi, inv, acc[i]);
            mul(inv, inv, pts[i].z);
            mul(x, pts[i].x, zi);
            mul(y, pts[i].y, zi);
            Precomp p = new Precomp();
            add(p.yPlusX, y, x);
            sub(p.yMinusX, y, x);
            carry(p.yPlusX);
            carry(p.yMinusX);
            mul(p.xy2d, x, y);
            mul(p.xy2d, p.xy2d, D2);
            out[i] = p;
        }
        return out;
    }

    // t = b * BASE[pos] for b in [-8, 8], reading all eight entries: each
    // word of the result is the OR of every entry's word under a mask that
    // is all ones for the wanted entry alone
    private static void select(Precomp t, int pos, int b) {
        int negative = b >>> 31;
        int babs = b - ((-negative & b) << 1);
        int[] row = BaseTable.FLAT[pos];
        int m0 = -(((babs ^ 1) - 1) >>> 31);
        int m1 = -(((babs ^ 2) - 1) >>> 31);
        int m2 = -(((babs ^ 3) - 1) >>> 31);
        int m3 = -(((babs ^ 4) - 1) >>> 31);
        int m4 = -(((babs ^ 5) - 1) >>> 31);
        int m5 = -(((babs ^ 6) - 1) >>> 31);
        int m6 = -(((babs ^ 7) - 1) >>> 31);
        int m7 = -(((babs ^ 8) - 1) >>> 31);
        int[] yp = t.yPlusX;
        int[] ym = t.yMinusX;
        int[] xy = t.xy2d;
        int neg = -negative;
        for (int k = 0; k < 10; k++) {
            int p = (row[k] & m0) | (row[30 + k] & m1) | (row[60 + k] & m2) | (row[90 + k] & m3)
                    | (row[120 + k] & m4) | (row[150 + k] & m5) | (row[180 + k] & m6) | (row[210 + k] & m7);
            int q = (row[k + 10] & m0) | (row[40 + k] & m1) | (row[70 + k] & m2) | (row[100 + k] & m3)
                    | (row[130 + k] & m4) | (row[160 + k] & m5) | (row[190 + k] & m6) | (row[220 + k] & m7);
            int x = (row[k + 20] & m0) | (row[50 + k] & m1) | (row[80 + k] & m2) | (row[110 + k] & m3)
                    | (row[140 + k] & m4) | (row[170 + k] & m5) | (row[200 + k] & m6) | (row[230 + k] & m7);
            // a negative digit swaps y + x with y - x and negates 2dxy
            yp[k] = p ^ ((p ^ q) & neg);
            ym[k] = q ^ ((p ^ q) & neg);
            xy[k] = x ^ ((x ^ -x) & neg);
        }
        // b = 0 selects nothing, which leaves zeros; the identity is (1, 1, 0)
        int none = (babs - 1) >>> 31;
        yp[0] |= none;
        ym[0] |= none;
    }

    // r = a B for a 32-byte little-endian scalar below 2^255; constant time
    static void mulBase(P3 r, byte[] a) {
        byte[] e = new byte[64];
        for (int i = 0; i < 32; i++) {
            e[2 * i] = (byte) (a[i] & 15);
            e[2 * i + 1] = (byte) ((a[i] >>> 4) & 15);
        }
        int carry = 0;
        for (int i = 0; i < 63; i++) {
            e[i] += carry;
            carry = (e[i] + 8) >> 4;
            e[i] -= carry << 4;
        }
        e[63] += carry;
        Ctx c = new Ctx();
        Precomp t = new Precomp();
        P3 s = new P3();
        r.identity();
        for (int i = 1; i < 64; i += 2) {
            select(t, i / 2, e[i]);
            madd(c, r, r, t);
        }
        dbl(c, s, r);
        dbl(c, r, s);
        dbl(c, s, r);
        dbl(c, r, s);
        for (int i = 0; i < 64; i += 2) {
            select(t, i / 2, e[i]);
            madd(c, r, r, t);
        }
        for (int i = 0; i < 64; i++) {
            e[i] = 0;
        }
    }

    // ------------------------------------------------------------------
    // variable-time double-scalar multiplication for verification

    private static final int WINDOW_B = 8;

    private static final class OddBase {
        // (2i + 1) B for i in [0, 2^(WINDOW_B - 2))
        static final Precomp[] ODD = build();

        private static Precomp[] build() {
            int n = 1 << (WINDOW_B - 2);
            Ctx c = new Ctx();
            P3 b = new P3();
            copy(b.x, BX);
            copy(b.y, BY);
            one(b.z);
            mul(b.t, b.x, b.y);
            P3 b2 = new P3();
            dbl(c, b2, b);
            Cached b2c = new Cached();
            toCached(b2c, b2);
            P3[] pts = new P3[n];
            pts[0] = b;
            for (int i = 1; i < n; i++) {
                pts[i] = new P3();
                add(c, pts[i], pts[i - 1], b2c);
            }
            return toPrecomp(pts);
        }
    }

    // width-w NAF of a 256-bit little-endian scalar: digits odd or zero,
    // |digit| < 2^(w-1)
    static byte[] wnaf(byte[] a, int w) {
        byte[] naf = new byte[257];
        int[] bits = new int[257];
        for (int i = 0; i < 256; i++) {
            bits[i] = (a[i >> 3] >> (i & 7)) & 1;
        }
        int width = 1 << w;
        int i = 0;
        int carry = 0;
        while (i < 257) {
            int bit = (i < 256 ? bits[i] : 0);
            if (bit == carry) {
                i++;
                continue;
            }
            int word = 0;
            for (int k = 0; k < w && i + k < 256; k++) {
                word |= bits[i + k] << k;
            }
            word += carry;
            int digit = word & (width - 1);
            if (digit >= (width >> 1)) {
                digit -= width;
                carry = 1;
            } else {
                carry = 0;
            }
            naf[i] = (byte) digit;
            i += w;
        }
        return naf;
    }

    // r = b B - h A, for public scalars; r.t is left unset
    static void doubleMulVar(P3 r, byte[] h, P3 a, byte[] b) {
        Ctx c = new Ctx();
        byte[] nh = wnaf(h, 5);
        byte[] nb = wnaf(b, WINDOW_B);
        Cached[] ta = new Cached[8];
        P3 a2 = new P3();
        dbl(c, a2, a);
        Cached a2c = new Cached();
        toCached(a2c, a2);
        P3 run = a;
        for (int i = 0; i < 8; i++) {
            ta[i] = new Cached();
            toCached(ta[i], run);
            if (i < 7) {
                P3 next = new P3();
                add(c, next, run, a2c);
                run = next;
            }
        }
        Precomp[] tb = OddBase.ODD;
        int top = 256;
        while (top >= 0 && nh[top] == 0 && nb[top] == 0) {
            top--;
        }
        r.identity();
        // every operation here reads its input completely before writing,
        // so they run in place; T is computed only when an addition reads it
        for (int i = top; i >= 0; i--) {
            int dh = nh[i];
            int db = nb[i];
            dbl(c, r, r, dh != 0 || db != 0);
            if (dh != 0) {
                c.needT = db != 0;
                if (dh > 0) {
                    sub(c, r, r, ta[dh >> 1]);
                } else {
                    add(c, r, r, ta[(-dh) >> 1]);
                }
            }
            if (db != 0) {
                c.needT = false;
                if (db > 0) {
                    madd(c, r, r, tb[db >> 1]);
                } else {
                    msub(c, r, r, tb[(-db) >> 1]);
                }
            }
            c.needT = true;
        }
        // r.t is stale when the last step skipped it; encode() reads X, Y, Z
    }

    // ------------------------------------------------------------------
    // scalars mod L = 2^252 + 27742317777372353535851937790883648493, in
    // signed 21-bit limbs; 2^252 = -C mod L

    private static final long[] C = {1430509, 1626855, 1442968, 997804, 1960495, 683900};
    private static final long[] L21 = toLimbs21(new byte[] {
        (byte) 0xed, (byte) 0xd3, (byte) 0xf5, (byte) 0x5c, (byte) 0x1a, (byte) 0x63, (byte) 0x12, (byte) 0x58,
        (byte) 0xd6, (byte) 0x9c, (byte) 0xf7, (byte) 0xa2, (byte) 0xde, (byte) 0xf9, (byte) 0xde, (byte) 0x14,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x10}, 13);

    private static long[] toLimbs21(byte[] s, int n) {
        long[] r = new long[n];
        for (int i = 0; i < n; i++) {
            int bit = 21 * i;
            long v = 0;
            for (int k = 0; k < 4 && (bit >> 3) + k < s.length; k++) {
                v |= ((long) (s[(bit >> 3) + k] & 0xff)) << (8 * k);
            }
            r[i] = (v >>> (bit & 7)) & 0x1fffff;
        }
        return r;
    }

    // s (n limbs) reduced mod L into 32 little-endian bytes
    private static void reduce(long[] s, int n, byte[] out, int off) {
        for (int top = n - 1; top >= 12; top--) {
            long v = s[top];
            s[top] = 0;
            for (int k = 0; k < 6; k++) {
                s[top - 12 + k] -= v * C[k];
            }
            for (int i = top - 12; i < top - 1; i++) {
                long carry = s[i] >> 21;
                s[i + 1] += carry;
                s[i] -= carry << 21;
            }
        }
        // the value now sits in twelve limbs, in (-2^253, 2^253); limb 12
        // takes the carries and the sign, and L has its 2^252 there
        s[12] = 0;
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 12; i++) {
                long carry = s[i] >> 21;
                s[i + 1] += carry;
                s[i] -= carry << 21;
            }
            if (s[12] < 0) {
                for (int i = 0; i < 13; i++) {
                    s[i] += L21[i];
                }
            }
        }
        long[] t = new long[13];
        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < 13; i++) {
                t[i] = s[i] - L21[i];
            }
            for (int i = 0; i < 12; i++) {
                long carry = t[i] >> 21;
                t[i + 1] += carry;
                t[i] -= carry << 21;
            }
            if (t[12] >= 0) {
                System.arraycopy(t, 0, s, 0, 13);
            }
        }
        for (int i = 0; i < 32; i++) {
            out[off + i] = 0;
        }
        for (int i = 0; i < 12; i++) {
            int bit = 21 * i;
            long v = s[i] << (bit & 7);
            for (int k = 0; k < 4 && (bit >> 3) + k < 32; k++) {
                out[off + (bit >> 3) + k] |= (byte) (v >>> (8 * k));
            }
        }
    }

    // a 64-byte little-endian value mod L
    static byte[] reduce64(byte[] h) {
        long[] s = toLimbs21(h, 25);
        byte[] out = new byte[32];
        reduce(s, 25, out, 0);
        return out;
    }

    // (a b + c) mod L
    static byte[] mulAdd(byte[] a, byte[] b, byte[] c) {
        long[] x = toLimbs21(a, 13);
        long[] y = toLimbs21(b, 13);
        long[] s = new long[26];
        long[] z = toLimbs21(c, 13);
        for (int i = 0; i < 13; i++) {
            s[i] = z[i];
        }
        for (int i = 0; i < 13; i++) {
            for (int j = 0; j < 13; j++) {
                s[i + j] += x[i] * y[j];
            }
        }
        for (int i = 0; i < 25; i++) {
            long carry = s[i] >> 21;
            s[i + 1] += carry;
            s[i] -= carry << 21;
        }
        byte[] out = new byte[32];
        reduce(s, 26, out, 0);
        return out;
    }

    // whether 32 little-endian bytes hold an integer below L
    static boolean isCanonicalScalar(byte[] s, int off) {
        byte[] l = {
            (byte) 0xed, (byte) 0xd3, (byte) 0xf5, (byte) 0x5c, (byte) 0x1a, (byte) 0x63, (byte) 0x12, (byte) 0x58,
            (byte) 0xd6, (byte) 0x9c, (byte) 0xf7, (byte) 0xa2, (byte) 0xde, (byte) 0xf9, (byte) 0xde, (byte) 0x14,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x10};
        for (int i = 31; i >= 0; i--) {
            int x = s[off + i] & 0xff;
            int y = l[i] & 0xff;
            if (x != y) {
                return x < y;
            }
        }
        return false;
    }
}
