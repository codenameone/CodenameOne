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
        if (KARATSUBA) {
            kmul(h, f, g);
        } else {
            mul10(h, f, g);
        }
    }

    // ref10's schoolbook product
    static void mul10(int[] h, int[] f, int[] g) {
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
        long g1By19 = 19 * g1;
        long g2By19 = 19 * g2;
        long g3By19 = 19 * g3;
        long g4By19 = 19 * g4;
        long g5By19 = 19 * g5;
        long g6By19 = 19 * g6;
        long g7By19 = 19 * g7;
        long g8By19 = 19 * g8;
        long g9By19 = 19 * g9;
        long f1By2 = 2 * f1;
        long f3By2 = 2 * f3;
        long f5By2 = 2 * f5;
        long f7By2 = 2 * f7;
        long f9By2 = 2 * f9;
        long h0 = f0 * g0 + f1By2 * g9By19 + f2 * g8By19 + f3By2 * g7By19 + f4 * g6By19 + f5By2 * g5By19 + f6 * g4By19 + f7By2 * g3By19 + f8 * g2By19 + f9By2 * g1By19;
        long h1 = f0 * g1 + f1 * g0 + f2 * g9By19 + f3 * g8By19 + f4 * g7By19 + f5 * g6By19 + f6 * g5By19 + f7 * g4By19 + f8 * g3By19 + f9 * g2By19;
        long h2 = f0 * g2 + f1By2 * g1 + f2 * g0 + f3By2 * g9By19 + f4 * g8By19 + f5By2 * g7By19 + f6 * g6By19 + f7By2 * g5By19 + f8 * g4By19 + f9By2 * g3By19;
        long h3 = f0 * g3 + f1 * g2 + f2 * g1 + f3 * g0 + f4 * g9By19 + f5 * g8By19 + f6 * g7By19 + f7 * g6By19 + f8 * g5By19 + f9 * g4By19;
        long h4 = f0 * g4 + f1By2 * g3 + f2 * g2 + f3By2 * g1 + f4 * g0 + f5By2 * g9By19 + f6 * g8By19 + f7By2 * g7By19 + f8 * g6By19 + f9By2 * g5By19;
        long h5 = f0 * g5 + f1 * g4 + f2 * g3 + f3 * g2 + f4 * g1 + f5 * g0 + f6 * g9By19 + f7 * g8By19 + f8 * g7By19 + f9 * g6By19;
        long h6 = f0 * g6 + f1By2 * g5 + f2 * g4 + f3By2 * g3 + f4 * g2 + f5By2 * g1 + f6 * g0 + f7By2 * g9By19 + f8 * g8By19 + f9By2 * g7By19;
        long h7 = f0 * g7 + f1 * g6 + f2 * g5 + f3 * g4 + f4 * g3 + f5 * g2 + f6 * g1 + f7 * g0 + f8 * g9By19 + f9 * g8By19;
        long h8 = f0 * g8 + f1By2 * g7 + f2 * g6 + f3By2 * g5 + f4 * g4 + f5By2 * g3 + f6 * g2 + f7By2 * g1 + f8 * g0 + f9By2 * g9By19;
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

    // On x86 HotSpot, ref10's 100 products (plus 9 by 19) are bound by the
    // one 64-bit multiplier and by register spills, while arm64 has the
    // registers to keep them all live; the C kernels choose the same way with
    // cpuid. kmul is Karatsuba over the two halves of the limbs: 75 products.
    private static final boolean KARATSUBA = isX86();

    private static boolean isX86() {
        String arch = System.getProperty("os.arch");
        return "amd64".equals(arch) || "x86_64".equals(arch) || "x86".equals(arch)
                || "i386".equals(arch) || "i686".equals(arch);
    }

    // The high five limbs sit 127 bits above the low five, at the low
    // limbs' weights once its even limbs are doubled (x5 is worth 2^128 =
    // 2^127 * 2), so both halves multiply with ref10's odd-by-odd doubling.
    // The cross term then lands half a bit off on its even columns and the
    // high-by-high term on all of them; each such column is a sum of products
    // with a doubled factor, hence even, and is halved exactly. 2^255 = 19.
    // With every input limb up to four times its carried bound the largest
    // intermediate stays below 2^62.5; callers pass sums of at most three.
    static void kmul(int[] h, int[] f, int[] g) {
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
        long fh0 = f5 << 1;
        long gh0 = g5 << 1;
        long fh1 = f6;
        long gh1 = g6;
        long fh2 = f7 << 1;
        long gh2 = g7 << 1;
        long fh3 = f8;
        long gh3 = g8;
        long fh4 = f9 << 1;
        long gh4 = g9 << 1;
        long fs0 = f0 + fh0;
        long gs0 = g0 + gh0;
        long fs1 = f1 + fh1;
        long gs1 = g1 + gh1;
        long fs2 = f2 + fh2;
        long gs2 = g2 + gh2;
        long fs3 = f3 + fh3;
        long gs3 = g3 + gh3;
        long fs4 = f4 + fh4;
        long gs4 = g4 + gh4;
        long l0 = f0 * g0;
        long l1 = f0 * g1 + f1 * g0;
        long l2 = f0 * g2 + f2 * g0 + ((f1 * g1) << 1);
        long l3 = f0 * g3 + f1 * g2 + f2 * g1 + f3 * g0;
        long l4 = f0 * g4 + f2 * g2 + f4 * g0 + ((f1 * g3 + f3 * g1) << 1);
        long l5 = f1 * g4 + f2 * g3 + f3 * g2 + f4 * g1;
        long l6 = f2 * g4 + f4 * g2 + ((f3 * g3) << 1);
        long l7 = f3 * g4 + f4 * g3;
        long l8 = f4 * g4;
        long u0 = fh0 * gh0;
        long u1 = fh0 * gh1 + fh1 * gh0;
        long u2 = fh0 * gh2 + fh2 * gh0 + ((fh1 * gh1) << 1);
        long u3 = fh0 * gh3 + fh1 * gh2 + fh2 * gh1 + fh3 * gh0;
        long u4 = fh0 * gh4 + fh2 * gh2 + fh4 * gh0 + ((fh1 * gh3 + fh3 * gh1) << 1);
        long u5 = fh1 * gh4 + fh2 * gh3 + fh3 * gh2 + fh4 * gh1;
        long u6 = fh2 * gh4 + fh4 * gh2 + ((fh3 * gh3) << 1);
        long u7 = fh3 * gh4 + fh4 * gh3;
        long u8 = fh4 * gh4;
        long m0 = fs0 * gs0;
        long m1 = fs0 * gs1 + fs1 * gs0;
        long m2 = fs0 * gs2 + fs2 * gs0 + ((fs1 * gs1) << 1);
        long m3 = fs0 * gs3 + fs1 * gs2 + fs2 * gs1 + fs3 * gs0;
        long m4 = fs0 * gs4 + fs2 * gs2 + fs4 * gs0 + ((fs1 * gs3 + fs3 * gs1) << 1);
        long m5 = fs1 * gs4 + fs2 * gs3 + fs3 * gs2 + fs4 * gs1;
        long m6 = fs2 * gs4 + fs4 * gs2 + ((fs3 * gs3) << 1);
        long m7 = fs3 * gs4 + fs4 * gs3;
        long m8 = fs4 * gs4;
        m0 = (m0 - l0 - u0) >> 1;
        m1 = (m1 - l1 - u1);
        m2 = (m2 - l2 - u2) >> 1;
        m3 = (m3 - l3 - u3);
        m4 = (m4 - l4 - u4) >> 1;
        m5 = (m5 - l5 - u5);
        m6 = (m6 - l6 - u6) >> 1;
        m7 = (m7 - l7 - u7);
        m8 = (m8 - l8 - u8) >> 1;
        u0 >>= 1;
        u1 >>= 1;
        u2 >>= 1;
        u3 >>= 1;
        u4 >>= 1;
        u5 >>= 1;
        u6 >>= 1;
        u7 >>= 1;
        u8 >>= 1;
        long h0 = l0 + 19 * (u0 + m5);
        long h1 = l1 + 19 * (u1 + m6);
        long h2 = l2 + 19 * (u2 + m7);
        long h3 = l3 + 19 * (u3 + m8);
        long h4 = l4 + 19 * (u4);
        long h5 = l5 + m0 + 19 * (u5);
        long h6 = l6 + m1 + 19 * (u6);
        long h7 = l7 + m2 + 19 * (u7);
        long h8 = l8 + m3 + 19 * (u8);
        long h9 = m4;

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
        long f5By19 = 19 * f5;
        long f6By19 = 19 * f6;
        long f7By19 = 19 * f7;
        long f8By19 = 19 * f8;
        long f9By19 = 19 * f9;
        long f1By2 = 2 * f1;
        long f3By2 = 2 * f3;
        long f5By2 = 2 * f5;
        long f7By2 = 2 * f7;
        long f9By2 = 2 * f9;
        long f0D = 2 * f0;
        long f1D = 2 * f1;
        long f2D = 2 * f2;
        long f3D = 2 * f3;
        long f4D = 2 * f4;
        long f5D = 2 * f5;
        long f6D = 2 * f6;
        long f7D = 2 * f7;
        long f8D = 2 * f8;
        long h0 = f0 * f0 + f1D * f9By19 * 2 + f2D * f8By19 + f3D * f7By19 * 2 + f4D * f6By19 + f5By2 * f5By19;
        long h1 = f0D * f1 + f2D * f9By19 + f3D * f8By19 + f4D * f7By19 + f5D * f6By19;
        long h2 = f0D * f2 + f1By2 * f1 + f3D * f9By19 * 2 + f4D * f8By19 + f5D * f7By19 * 2 + f6 * f6By19;
        long h3 = f0D * f3 + f1D * f2 + f4D * f9By19 + f5D * f8By19 + f6D * f7By19;
        long h4 = f0D * f4 + f1D * f3 * 2 + f2 * f2 + f5D * f9By19 * 2 + f6D * f8By19 + f7By2 * f7By19;
        long h5 = f0D * f5 + f1D * f4 + f2D * f3 + f6D * f9By19 + f7D * f8By19;
        long h6 = f0D * f6 + f1D * f5 * 2 + f2D * f4 + f3By2 * f3 + f7D * f9By19 * 2 + f8 * f8By19;
        long h7 = f0D * f7 + f1D * f6 + f2D * f5 + f3D * f4 + f8D * f9By19;
        long h8 = f0D * f8 + f1D * f7 * 2 + f2D * f6 + f3D * f5 * 2 + f4 * f4 + f9By2 * f9By19;
        long h9 = f0D * f9 + f1D * f8 + f2D * f7 + f3D * f6 + f4D * f5;
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
        invertDivsteps(out, z);
    }

    // z^(p - 2), by 254 squarings: the reference the divstep inverse is
    // tested against, and a quarter of what publicKey cost while it was the
    // inverse
    static void invertFermat(int[] out, int[] z) {
        int[] t = fe();
        int[] z11 = fe();
        pow250(t, z11, z);
        sqrN(t, t, 5);
        mul(out, t, z11);
    }

    // ------------------------------------------------------------------
    // constant-time inversion by divsteps (Bernstein and Yang, "Fast
    // constant-time gcd computation and modular inversion", 2019)
    //
    // Each step of
    //   delta > 0 and g odd: (delta, f, g) -> (1 - delta, g, (g - f) / 2)
    //   otherwise:           (delta, f, g) -> (1 + delta, f, (g + (g mod 2) f) / 2)
    // depends only on delta and the low bits of f and g, so 30 steps are run
    // on the low words alone, recording what they did to f and g as a matrix
    // [u v; q r] scaled by 2^30, and the matrix is then applied once to the
    // full values. Starting from f = p, g = z and d = 0, e = 1, the steps keep
    // f = d z and g = e z (mod p); after enough of them g is 0 and f is +1 or
    // -1, so f d is the inverse. delta starts at 1/2 rather than the paper's
    // 1, and is carried here as 2 delta; with that start 590 steps suffice for
    // inputs below 2^256 (the bound libsecp256k1 established by exhaustive
    // hull computation, doc/safegcd_implementation.md), so 20 rounds of 30
    // do. Every round runs the same instructions whatever z is.

    private static final int M30 = (1 << 30) - 1;
    private static final int DIVSTEP_ROUNDS = 20;
    // p = 2^255 - 19 in nine signed 30-bit limbs, and 1 / p mod 2^30
    private static final int[] P30 = {M30 - 18, M30, M30, M30, M30, M30, M30, M30, (1 << 15) - 1};
    private static final int P_INV30 = inverse30(P30[0]);

    private static int inverse30(int x) {
        // Newton: each step doubles the number of correct low bits
        int y = x;
        for (int i = 0; i < 5; i++) {
            y *= 2 - x * y;
        }
        return y & M30;
    }

    static void invertDivsteps(int[] out, int[] z) {
        byte[] zb = new byte[32];
        toBytes(zb, 0, z);
        int[] f = new int[9];
        int[] g = new int[9];
        int[] d = new int[9];
        int[] e = new int[9];
        int[] t = new int[9];
        System.arraycopy(P30, 0, f, 0, 9);
        long acc = 0;
        int accBits = 0;
        int w = 0;
        for (int i = 0; i < 32; i++) {
            acc |= ((long) (zb[i] & 0xff)) << accBits;
            accBits += 8;
            if (accBits >= 30) {
                g[w++] = (int) acc & M30;
                acc >>>= 30;
                accBits -= 30;
            }
        }
        g[8] = (int) acc;
        e[0] = 1;
        // 2 delta, for delta = 1/2
        int delta = 1;
        int[] m = new int[4];
        for (int round = 0; round < DIVSTEP_ROUNDS; round++) {
            delta = divsteps30(delta, f[0], g[0], m);
            applyToFg(f, g, t, m);
            applyToDe(d, e, t, m);
        }
        // f is +1 or -1; the inverse is f d, which lies within 21 p of zero
        // because d started at 0 and e at 1 and each round adds less than p:
        // a different start needs the 32 p margin below rechecked
        int neg = f[8] >> 31;
        long c = 0;
        for (int i = 0; i < 9; i++) {
            // (d xor neg) - neg negates d when f = -1; then add 32 p
            c += (long) ((d[i] ^ neg) - neg) + 32L * P30[i];
            t[i] = (int) c & M30;
            c >>= 30;
        }
        t[8] += (int) c << 30;
        // t < 2^261: fold the bits at and above 2^255 back in as 19 each, twice
        for (int pass = 0; pass < 2; pass++) {
            int hi = t[8] >>> 15;
            t[8] &= (1 << 15) - 1;
            c = 19L * hi;
            for (int i = 0; i < 9; i++) {
                c += t[i];
                t[i] = (int) c & M30;
                c >>= 30;
            }
            t[8] += (int) c << 30;
        }
        for (int i = 0; i < 32; i++) {
            int bit = 8 * i;
            int wi = bit / 30;
            int sh = bit % 30;
            int v = t[wi] >>> sh;
            if (sh > 22 && wi < 8) {
                v |= t[wi + 1] << (30 - sh);
            }
            zb[i] = (byte) v;
        }
        fromBytes(out, zb, 0);
        for (int i = 0; i < 32; i++) {
            zb[i] = 0;
        }
    }

    // 30 divsteps on the low words of f and g, with delta given and returned
    // doubled; m receives u, v, q, r
    private static int divsteps30(int delta, int f, int g, int[] m) {
        int u = 1;
        int v = 0;
        int q = 0;
        int r = 1;
        for (int i = 0; i < 30; i++) {
            // all ones when delta > 0 and g is odd
            int swap = (-delta >> 31) & -(g & 1);
            // on a swap: delta = -delta, (f, g) = (g, -f), (u, v, q, r) = (q, r, -u, -v)
            int x = (f ^ g) & swap;
            f ^= x;
            g ^= x;
            g = (g ^ swap) - swap;
            x = (u ^ q) & swap;
            u ^= x;
            q ^= x;
            q = (q ^ swap) - swap;
            x = (v ^ r) & swap;
            v ^= x;
            r ^= x;
            r = (r ^ swap) - swap;
            delta = ((delta ^ swap) - swap) + 2;
            // g is now even plus, when odd, f; f is always odd
            int odd = -(g & 1);
            g += f & odd;
            q += u & odd;
            r += v & odd;
            g >>= 1;
            u <<= 1;
            v <<= 1;
        }
        m[0] = u;
        m[1] = v;
        m[2] = q;
        m[3] = r;
        return delta;
    }

    // (f, g) = ([u v; q r] (f, g)) / 2^30, exactly
    private static void applyToFg(int[] f, int[] g, int[] t, int[] m) {
        long u = m[0];
        long v = m[1];
        long q = m[2];
        long r = m[3];
        long cf = u * f[0] + v * g[0];
        long cg = q * f[0] + r * g[0];
        cf >>= 30;
        cg >>= 30;
        for (int i = 1; i < 9; i++) {
            cf += u * f[i] + v * g[i];
            cg += q * f[i] + r * g[i];
            t[i - 1] = (int) cf & M30;
            g[i - 1] = (int) cg & M30;
            cf >>= 30;
            cg >>= 30;
        }
        t[8] = (int) cf;
        g[8] = (int) cg;
        System.arraycopy(t, 0, f, 0, 9);
    }

    // (d, e) = ([u v; q r] (d, e) + (md, me) p) / 2^30, with md and me making
    // the sums divisible by 2^30; each round grows the values by at most p
    private static void applyToDe(int[] d, int[] e, int[] t, int[] m) {
        long u = m[0];
        long v = m[1];
        long q = m[2];
        long r = m[3];
        long cd = u * d[0] + v * e[0];
        long ce = q * d[0] + r * e[0];
        long md = (-((int) cd * P_INV30)) & M30;
        long me = (-((int) ce * P_INV30)) & M30;
        cd += md * P30[0];
        ce += me * P30[0];
        cd >>= 30;
        ce >>= 30;
        for (int i = 1; i < 9; i++) {
            cd += u * d[i] + v * e[i] + md * P30[i];
            ce += q * d[i] + r * e[i] + me * P30[i];
            t[i - 1] = (int) cd & M30;
            e[i - 1] = (int) ce & M30;
            cd >>= 30;
            ce >>= 30;
        }
        t[8] = (int) cd;
        e[8] = (int) ce;
        System.arraycopy(t, 0, d, 0, 9);
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
    // fixed-base multiplication: BASE[i][j] = (j + 1) 16^i B. One row per
    // signed radix-16 digit, so a multiplication is 64 table additions and no
    // doublings, where ref10's 32 rows need two passes with 4 doublings
    // between them. That trade costs a 61 KB table, built once.

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
            Precomp[][] t = new Precomp[64][8];
            P3 row = new P3();
            copy(row.x, BX);
            copy(row.y, BY);
            one(row.z);
            mul(row.t, row.x, row.y);
            P3[] pts = new P3[512];
            Cached cr = new Cached();
            for (int i = 0; i < 64; i++) {
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
                for (int k = 0; k < 4; k++) {
                    P3 d = new P3();
                    dbl(c, d, next);
                    next = d;
                }
                row = next;
            }
            Precomp[] flat = toPrecomp(pts);
            for (int i = 0; i < 64; i++) {
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
            e[i] = (byte) (e[i] + carry);
            carry = (e[i] + 8) >> 4;
            e[i] = (byte) (e[i] - (carry << 4));
        }
        e[63] = (byte) (e[63] + carry);
        Ctx c = new Ctx();
        Precomp t = new Precomp();
        r.identity();
        for (int i = 0; i < 64; i++) {
            select(t, i, e[i]);
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

    // ------------------------------------------------------------------
    // verification with half-length scalars
    //
    // [s]B = R + [k]A is checked as [u]B - [v1]R - [v0]A = O, where v0 = v1 k
    // (mod 8L) and u = v1 s (mod L), with v1 odd and both v0 and v1 near 2^127.
    // Both sides of the original equation are points of the full group of order
    // 8L, so multiplying it by v1 is exact when v1 is odd and not a multiple of
    // L, and taking v0 modulo 8L rather than L keeps [v1 k]A = [v0]A for a key
    // with a small-order component too: the answer is the single verify's for
    // every input, torsion included. With all four scalars about half length,
    // the loop does about half the doublings. The short (v0, v1) is found by a
    // binary Euclid on the lattice {(a, b) : a = b k mod 8L}.

    private static final class HighBase {
        // (2i + 1) 2^128 B for i in [0, 2^(WINDOW_B - 2))
        static final Precomp[] ODD = build();

        private static Precomp[] build() {
            int n = 1 << (WINDOW_B - 2);
            Ctx c = new Ctx();
            P3 b = new P3();
            copy(b.x, BX);
            copy(b.y, BY);
            one(b.z);
            mul(b.t, b.x, b.y);
            for (int i = 0; i < 128; i++) {
                dbl(c, b, b);
            }
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

    // 8L in 32-bit little-endian words
    private static final int[] EIGHT_L = {
        0x5cf5d3ed << 3, (0x5812631a << 3) | (0x5cf5d3ed >>> 29),
        (0xa2f79cd6 << 3) | (0x5812631a >>> 29), (0x14def9de << 3) | (0xa2f79cd6 >>> 29),
        0x14def9de >>> 29, 0, 0, 0x10000000 << 3};

    private static int bitLength(int[] x) {
        for (int i = x.length - 1; i >= 0; i--) {
            if (x[i] != 0) {
                return 32 * i + 32 - Integer.numberOfLeadingZeros(x[i]);
            }
        }
        return 0;
    }

    // word j of b << sh
    private static long shiftedWord(int[] b, int j, int words, int bits) {
        int k = j - words;
        long hi = k >= 0 && k < b.length ? b[k] & 0xffffffffL : 0;
        if (bits == 0) {
            return hi;
        }
        long lo = k - 1 >= 0 && k - 1 < b.length ? b[k - 1] & 0xffffffffL : 0;
        return ((hi << bits) | (lo >>> (32 - bits))) & 0xffffffffL;
    }

    // out = a - (b << sh); true when that is negative, and out is then garbage
    private static boolean subShifted(int[] out, int[] a, int[] b, int sh) {
        int words = sh >>> 5;
        int bits = sh & 31;
        long borrow = 0;
        for (int j = 0; j < a.length; j++) {
            long d = (a[j] & 0xffffffffL) - shiftedWord(b, j, words, bits) - borrow;
            out[j] = (int) d;
            borrow = (d >>> 63);
        }
        return borrow != 0;
    }

    // a += b << sh
    private static void addShifted(int[] a, int[] b, int sh) {
        int words = sh >>> 5;
        int bits = sh & 31;
        long carry = 0;
        for (int j = 0; j < a.length; j++) {
            long d = (a[j] & 0xffffffffL) + shiftedWord(b, j, words, bits) + carry;
            a[j] = (int) d;
            carry = d >>> 32;
        }
    }

    private static int compare(int[] a, int[] b) {
        for (int i = a.length - 1; i >= 0; i--) {
            if (a[i] != b[i]) {
                return (a[i] & 0xffffffffL) < (b[i] & 0xffffffffL) ? -1 : 1;
            }
        }
        return 0;
    }

    private static void toWords(int[] out, byte[] s) {
        for (int i = 0; i < 8; i++) {
            out[i] = (s[4 * i] & 0xff) | ((s[4 * i + 1] & 0xff) << 8)
                    | ((s[4 * i + 2] & 0xff) << 16) | ((s[4 * i + 3] & 0xff) << 24);
        }
    }

    private static void wordsToBytes(byte[] out, int[] w) {
        for (int i = 0; i < 8; i++) {
            out[4 * i] = (byte) w[i];
            out[4 * i + 1] = (byte) (w[i] >>> 8);
            out[4 * i + 2] = (byte) (w[i] >>> 16);
            out[4 * i + 3] = (byte) (w[i] >>> 24);
        }
    }

    // finds v0 = v1 k (mod 8L) with v1 odd and positive and both about 2^127;
    // v0 goes to v0 (its magnitude) and its sign is the answer, true for negative
    static boolean reduceBasis(byte[] k, byte[] v0, byte[] v1) {
        // rows (r0, t0) and (r1, t1) with r = t k (mod 8L), r0 >= r1, t0 and t1
        // of opposite signs (t1's is neg1) and r0 |t1| + r1 |t0| = 8L
        int[] r0 = new int[8];
        int[] r1 = new int[8];
        int[] t0 = new int[8];
        int[] t1 = new int[8];
        int[] tmp = new int[8];
        System.arraycopy(EIGHT_L, 0, r0, 0, 8);
        toWords(r1, k);
        t1[0] = 1;
        boolean neg1 = false;
        while (true) {
            if (compare(r0, r1) < 0) {
                int[] x = r0;
                r0 = r1;
                r1 = x;
                x = t0;
                t0 = t1;
                t1 = x;
                neg1 = !neg1;
            }
            if (bitLength(r1) <= 127) {
                break;
            }
            int sh = bitLength(r0) - bitLength(r1);
            if (subShifted(tmp, r0, r1, sh)) {
                sh--;
                subShifted(tmp, r0, r1, sh);
            }
            int[] x = r0;
            r0 = tmp;
            tmp = x;
            addShifted(t0, t1, sh);
        }
        // a lattice row with an odd t: (r1, t1) when t1 is odd, otherwise the
        // shorter of (r0, t0) and their sum (r0 + r1, t1 + t0), whose t is
        // |t1| - |t0| with t1's sign; t0 is odd whenever t1 is even, because the
        // rows' determinant is 8L and (k, 1) is in the lattice
        boolean tNeg;
        int[] r;
        int[] t;
        if ((t1[0] & 1) != 0) {
            r = r1;
            t = t1;
            tNeg = neg1;
        } else {
            int[] sr = new int[8];
            System.arraycopy(r0, 0, sr, 0, 8);
            addShifted(sr, r1, 0);
            int[] st = new int[8];
            boolean stNeg;
            if (compare(t1, t0) >= 0) {
                subShifted(st, t1, t0, 0);
                stNeg = neg1;
            } else {
                subShifted(st, t0, t1, 0);
                stNeg = !neg1;
            }
            if (Math.max(bitLength(sr), bitLength(st)) < Math.max(bitLength(r0), bitLength(t0))) {
                r = sr;
                t = st;
                tNeg = stNeg;
            } else {
                r = r0;
                t = t0;
                tNeg = !neg1;
            }
        }
        // v1 is made positive by negating the row
        boolean vNeg = tNeg;
        // a k whose lattice has a very short even row, such as L - 1 with its
        // (8, 8), has no short odd one; (k, 1) is always a row, and only costs
        // the full length. Any row of 252 bits or fewer is still faster, and
        // its v1 is below L, so never a multiple of it.
        if (Math.max(bitLength(r), bitLength(t)) > 252) {
            toWords(r, k);
            for (int i = 0; i < 8; i++) {
                t[i] = 0;
            }
            t[0] = 1;
            vNeg = false;
        }
        wordsToBytes(v0, r);
        wordsToBytes(v1, t);
        return vNeg;
    }

    // whether [s]B = R + [k]A, for public inputs, with a and r decoded
    static boolean verifyHalf(byte[] k, byte[] s, P3 a, P3 rp) {
        byte[] v0 = new byte[32];
        byte[] v1 = new byte[32];
        boolean v0Neg = reduceBasis(k, v0, v1);
        byte[] u = mulAdd(v1, s, new byte[32]);
        byte[] uLo = new byte[32];
        byte[] uHi = new byte[32];
        System.arraycopy(u, 0, uLo, 0, 16);
        System.arraycopy(u, 16, uHi, 0, 16);
        byte[] nA = wnaf(v0, 5);
        byte[] nR = wnaf(v1, 5);
        byte[] nLo = wnaf(uLo, WINDOW_B);
        byte[] nHi = wnaf(uHi, WINDOW_B);
        Ctx c = new Ctx();
        Cached[] ta = oddMultiples(c, a);
        Cached[] tr = oddMultiples(c, rp);
        Precomp[] tLo = OddBase.ODD;
        Precomp[] tHi = HighBase.ODD;
        int top = 256;
        while (top >= 0 && nA[top] == 0 && nR[top] == 0 && nLo[top] == 0 && nHi[top] == 0) {
            top--;
        }
        P3 q = new P3();
        q.identity();
        // the sum is u B - v1 R - v0 A: the A digits are subtracted when v0 is
        // positive, the R digits always
        int signA = v0Neg ? 1 : -1;
        for (int i = top; i >= 0; i--) {
            int dA = nA[i] * signA;
            int dR = -nR[i];
            int dLo = nLo[i];
            int dHi = nHi[i];
            int left = (dA != 0 ? 1 : 0) + (dR != 0 ? 1 : 0) + (dLo != 0 ? 1 : 0) + (dHi != 0 ? 1 : 0);
            dbl(c, q, q, left != 0);
            if (dA != 0) {
                c.needT = --left != 0;
                addCached(c, q, ta, dA);
            }
            if (dR != 0) {
                c.needT = --left != 0;
                addCached(c, q, tr, dR);
            }
            if (dLo != 0) {
                c.needT = --left != 0;
                addPrecomp(c, q, tLo, dLo);
            }
            if (dHi != 0) {
                c.needT = --left != 0;
                addPrecomp(c, q, tHi, dHi);
            }
            c.needT = true;
        }
        int[] yz = fe();
        sub(yz, q.y, q.z);
        return isZero(q.x) && isZero(yz);
    }

    // P, 3P, ..., 15P
    private static Cached[] oddMultiples(Ctx c, P3 p) {
        Cached[] t = new Cached[8];
        P3 p2 = new P3();
        dbl(c, p2, p);
        Cached p2c = new Cached();
        toCached(p2c, p2);
        P3 run = p;
        for (int i = 0; i < 8; i++) {
            t[i] = new Cached();
            toCached(t[i], run);
            if (i < 7) {
                P3 next = new P3();
                add(c, next, run, p2c);
                run = next;
            }
        }
        return t;
    }

    private static void addCached(Ctx c, P3 q, Cached[] t, int d) {
        if (d > 0) {
            add(c, q, q, t[d >> 1]);
        } else {
            sub(c, q, q, t[(-d) >> 1]);
        }
    }

    private static void addPrecomp(Ctx c, P3 q, Precomp[] t, int d) {
        if (d > 0) {
            madd(c, q, q, t[d >> 1]);
        } else {
            msub(c, q, q, t[(-d) >> 1]);
        }
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
