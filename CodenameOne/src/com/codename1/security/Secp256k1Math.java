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

/// The arithmetic behind [Secp256k1]: the field modulo
/// `p = 2^256 - 2^32 - 977`, the curve group over it, and a constant-time
/// modular inverse that serves both the field and the group order.
///
/// #### Field elements
///
/// A field element is an `int[10]` of 26-bit limbs, little endian, the top
/// limb 22 bits wide. Additions do not carry: an element's *magnitude* is how
/// many reduced values were summed into it, and every limb of a magnitude-`m`
/// element is at most `2 * m * (2^26 - 1)`. [#mul] and [#sqr] accept inputs
/// whose magnitudes multiply to at most 48 (a column of ten 58-bit products
/// then stays below 2^63) and return magnitude 1. Each caller tracks the
/// magnitudes in comments; [#weak] brings any element back to magnitude 1.
///
/// #### Points
///
/// Public-data code (verification, recovery, parsing) uses Jacobian
/// coordinates with an explicit infinity flag and formulas that branch.
/// Secret-data code (key derivation, signing) uses [#mulGen], a signed-digit
/// comb over a fixed table whose 64 additions use the complete projective
/// formula of Renes, Costello and Batina and read every table entry, so the
/// sequence of operations and the memory touched do not depend on the
/// scalar.
///
/// #### Inversion
///
/// [#modInverse] is the constant-time "safegcd" of Bernstein and Yang
/// (2019): twenty batches of thirty division steps on 30-bit signed limbs,
/// each batch applied as a 2x2 matrix. It has no data-dependent branch and is
/// several times faster than inverting by Fermat's little theorem.
@com.codename1.impl.SharedWithBackend
final class Secp256k1Math {
    static final int M26 = 0x3FFFFFF;
    static final int M22 = 0x3FFFFF;
    private static final int M30 = 0x3FFFFFFF;

    private static final int[] P26 = {
        0x3FFFC2F, 0x3FFFFBF, M26, M26, M26, M26, M26, M26, M26, M22
    };

    /// `p` in the 30-bit limbs [#modInverse] takes.
    static final int[] P30 = {
        0x3FFFFC2F, 0x3FFFFFFB, M30, M30, M30, M30, M30, M30, 0xFFFF
    };
    /// `p^-1 mod 2^30`
    static final int P_INV30 = 0x2DDACACF;

    /// The group order `n` in 30-bit limbs.
    static final int[] N30 = {
        0x10364141, 0x3F497A33, 0x348A03BB, 0x2BB739AB, 0x3FFFFEBA, M30, M30, M30, 0xFFFF
    };
    /// `n^-1 mod 2^30`
    static final int N_INV30 = 0x2A774EC1;

    /// A cube root of unity in the field: `(beta * x, y)` is `lambda * (x, y)`.
    static final int[] BETA = {
        0x19501EE, 0x25B0A1C, 0x995C13, 0x1D44BD6, 0x19CF049, 0x30D0D3A, 0x24479EA, 0x1C41B9, 0x22B657C, 0x1EBA5A
    };

    private Secp256k1Math() {
    }

    // ----------------------------------------------------------------
    // field

    static int[] fe() {
        return new int[10];
    }

    static void set(int[] r, int[] a) {
        System.arraycopy(a, 0, r, 0, 10);
    }

    static void setInt(int[] r, int v) {
        r[0] = v;
        for (int i = 1; i < 10; i++) {
            r[i] = 0;
        }
    }

    // On x86 HotSpot the schoolbook product's 100 multiplications are bound by
    // the one 64-bit multiplier and by register spills, while arm64 keeps them
    // all live; Ed25519Math chooses its field product the same way.
    private static final boolean KARATSUBA = isX86();

    private static boolean isX86() {
        String arch = System.getProperty("os.arch");
        return "amd64".equals(arch) || "x86_64".equals(arch) || "x86".equals(arch)
                || "i386".equals(arch) || "i686".equals(arch);
    }

    /// r = a * b, magnitude 1. r may alias a or b.
    ///
    /// The 19 product columns (each below 2^63) reduce in place: column
    /// 10 + j sits at bit 260 + 26j, and 2^260 = 16 * 2^256 == 16 *
    /// 0x1000003D1 = 0x400 * 2^26 + 0x3D10 (mod p), so once the high columns
    /// are 26 bits each they fold onto columns j and j + 1. The bits left at
    /// or above 2^256 fold once more by 2^256 == 0x1000003D1. [#sqr] reduces
    /// the same way; the code is repeated rather than called because a call
    /// with nineteen long arguments cost as much as the multiplication.
    static void mul(int[] r, int[] a, int[] b) {
        long a0 = a[0];
        long a1 = a[1];
        long a2 = a[2];
        long a3 = a[3];
        long a4 = a[4];
        long a5 = a[5];
        long a6 = a[6];
        long a7 = a[7];
        long a8 = a[8];
        long a9 = a[9];
        long b0 = b[0];
        long b1 = b[1];
        long b2 = b[2];
        long b3 = b[3];
        long b4 = b[4];
        long b5 = b[5];
        long b6 = b[6];
        long b7 = b[7];
        long b8 = b[8];
        long b9 = b[9];
        long c0;
        long c1;
        long c2;
        long c3;
        long c4;
        long c5;
        long c6;
        long c7;
        long c8;
        long c9;
        long c10;
        long c11;
        long c12;
        long c13;
        long c14;
        long c15;
        long c16;
        long c17;
        long c18;
        if (KARATSUBA) {
            // 2^130 splits the limbs into halves of five, so a * b = L + (L + U - D) 2^130 + U 2^260
            // with D = (a_lo - a_hi)(b_lo - b_hi): 75 products for 100. Limbs are never negative,
            // so a difference of two is no larger than the larger of them and every product keeps
            // the schoolbook bound. Each column's true value is the schoolbook column's, below
            // 2^63; L + U - D may wrap on the way there and comes back exactly, as two's
            // complement arithmetic does.
            long da0 = a0 - a5;
            long db0 = b0 - b5;
            long da1 = a1 - a6;
            long db1 = b1 - b6;
            long da2 = a2 - a7;
            long db2 = b2 - b7;
            long da3 = a3 - a8;
            long db3 = b3 - b8;
            long da4 = a4 - a9;
            long db4 = b4 - b9;
            long l0 = a0 * b0;
            long l1 = a0 * b1 + a1 * b0;
            long l2 = a0 * b2 + a1 * b1 + a2 * b0;
            long l3 = a0 * b3 + a1 * b2 + a2 * b1 + a3 * b0;
            long l4 = a0 * b4 + a1 * b3 + a2 * b2 + a3 * b1 + a4 * b0;
            long l5 = a1 * b4 + a2 * b3 + a3 * b2 + a4 * b1;
            long l6 = a2 * b4 + a3 * b3 + a4 * b2;
            long l7 = a3 * b4 + a4 * b3;
            long l8 = a4 * b4;
            long u0 = a5 * b5;
            long u1 = a5 * b6 + a6 * b5;
            long u2 = a5 * b7 + a6 * b6 + a7 * b5;
            long u3 = a5 * b8 + a6 * b7 + a7 * b6 + a8 * b5;
            long u4 = a5 * b9 + a6 * b8 + a7 * b7 + a8 * b6 + a9 * b5;
            long u5 = a6 * b9 + a7 * b8 + a8 * b7 + a9 * b6;
            long u6 = a7 * b9 + a8 * b8 + a9 * b7;
            long u7 = a8 * b9 + a9 * b8;
            long u8 = a9 * b9;
            long d0 = da0 * db0;
            long d1 = da0 * db1 + da1 * db0;
            long d2 = da0 * db2 + da1 * db1 + da2 * db0;
            long d3 = da0 * db3 + da1 * db2 + da2 * db1 + da3 * db0;
            long d4 = da0 * db4 + da1 * db3 + da2 * db2 + da3 * db1 + da4 * db0;
            long d5 = da1 * db4 + da2 * db3 + da3 * db2 + da4 * db1;
            long d6 = da2 * db4 + da3 * db3 + da4 * db2;
            long d7 = da3 * db4 + da4 * db3;
            long d8 = da4 * db4;
            c0 = l0;
            c1 = l1;
            c2 = l2;
            c3 = l3;
            c4 = l4;
            c5 = l5 + l0 + u0 - d0;
            c6 = l6 + l1 + u1 - d1;
            c7 = l7 + l2 + u2 - d2;
            c8 = l8 + l3 + u3 - d3;
            c9 = l4 + u4 - d4;
            c10 = l5 + u5 - d5 + u0;
            c11 = l6 + u6 - d6 + u1;
            c12 = l7 + u7 - d7 + u2;
            c13 = l8 + u8 - d8 + u3;
            c14 = u4;
            c15 = u5;
            c16 = u6;
            c17 = u7;
            c18 = u8;
        } else {
            c0 = a0 * b0;
            c1 = a0 * b1 + a1 * b0;
            c2 = a0 * b2 + a1 * b1 + a2 * b0;
            c3 = a0 * b3 + a1 * b2 + a2 * b1 + a3 * b0;
            c4 = a0 * b4 + a1 * b3 + a2 * b2 + a3 * b1 + a4 * b0;
            c5 = a0 * b5 + a1 * b4 + a2 * b3 + a3 * b2 + a4 * b1 + a5 * b0;
            c6 = a0 * b6 + a1 * b5 + a2 * b4 + a3 * b3 + a4 * b2 + a5 * b1 + a6 * b0;
            c7 = a0 * b7 + a1 * b6 + a2 * b5 + a3 * b4 + a4 * b3 + a5 * b2 + a6 * b1 + a7 * b0;
            c8 = a0 * b8 + a1 * b7 + a2 * b6 + a3 * b5 + a4 * b4 + a5 * b3 + a6 * b2 + a7 * b1 + a8 * b0;
            c9 = a0 * b9 + a1 * b8 + a2 * b7 + a3 * b6 + a4 * b5 + a5 * b4 + a6 * b3 + a7 * b2 + a8 * b1 + a9 * b0;
            c10 = a1 * b9 + a2 * b8 + a3 * b7 + a4 * b6 + a5 * b5 + a6 * b4 + a7 * b3 + a8 * b2 + a9 * b1;
            c11 = a2 * b9 + a3 * b8 + a4 * b7 + a5 * b6 + a6 * b5 + a7 * b4 + a8 * b3 + a9 * b2;
            c12 = a3 * b9 + a4 * b8 + a5 * b7 + a6 * b6 + a7 * b5 + a8 * b4 + a9 * b3;
            c13 = a4 * b9 + a5 * b8 + a6 * b7 + a7 * b6 + a8 * b5 + a9 * b4;
            c14 = a5 * b9 + a6 * b8 + a7 * b7 + a8 * b6 + a9 * b5;
            c15 = a6 * b9 + a7 * b8 + a8 * b7 + a9 * b6;
            c16 = a7 * b9 + a8 * b8 + a9 * b7;
            c17 = a8 * b9 + a9 * b8;
            c18 = a9 * b9;
        }
        c11 += c10 >> 26;
        c10 &= M26;
        c12 += c11 >> 26;
        c11 &= M26;
        c13 += c12 >> 26;
        c12 &= M26;
        c14 += c13 >> 26;
        c13 &= M26;
        c15 += c14 >> 26;
        c14 &= M26;
        c16 += c15 >> 26;
        c15 &= M26;
        c17 += c16 >> 26;
        c16 &= M26;
        c18 += c17 >> 26;
        c17 &= M26;
        long c19 = c18 >> 26;
        c18 &= M26;

        c0 += c10 * 0x3D10L;
        c1 += c10 * 0x400L + c11 * 0x3D10L;
        c2 += c11 * 0x400L + c12 * 0x3D10L;
        c3 += c12 * 0x400L + c13 * 0x3D10L;
        c4 += c13 * 0x400L + c14 * 0x3D10L;
        c5 += c14 * 0x400L + c15 * 0x3D10L;
        c6 += c15 * 0x400L + c16 * 0x3D10L;
        c7 += c16 * 0x400L + c17 * 0x3D10L;
        c8 += c17 * 0x400L + c18 * 0x3D10L;
        c9 += c18 * 0x400L + c19 * 0x3D10L;
        long hi = c19 * 0x400L;

        c1 += c0 >> 26;
        c0 &= M26;
        c2 += c1 >> 26;
        c1 &= M26;
        c3 += c2 >> 26;
        c2 &= M26;
        c4 += c3 >> 26;
        c3 &= M26;
        c5 += c4 >> 26;
        c4 &= M26;
        c6 += c5 >> 26;
        c5 &= M26;
        c7 += c6 >> 26;
        c6 &= M26;
        c8 += c7 >> 26;
        c7 &= M26;
        c9 += c8 >> 26;
        c8 &= M26;
        long top = (c9 >> 22) + (hi << 4);
        c9 &= M22;

        c0 += top * 0x3D1L;
        c1 += top << 6;
        c1 += c0 >> 26;
        r[0] = (int) (c0 & M26);
        c2 += c1 >> 26;
        r[1] = (int) (c1 & M26);
        c3 += c2 >> 26;
        r[2] = (int) (c2 & M26);
        c4 += c3 >> 26;
        r[3] = (int) (c3 & M26);
        c5 += c4 >> 26;
        r[4] = (int) (c4 & M26);
        c6 += c5 >> 26;
        r[5] = (int) (c5 & M26);
        c7 += c6 >> 26;
        r[6] = (int) (c6 & M26);
        c8 += c7 >> 26;
        r[7] = (int) (c7 & M26);
        c9 += c8 >> 26;
        r[8] = (int) (c8 & M26);
        r[9] = (int) c9;
    }

    /// r = a * a, magnitude 1. r may alias a.
    static void sqr(int[] r, int[] a) {
        long a0 = a[0];
        long a1 = a[1];
        long a2 = a[2];
        long a3 = a[3];
        long a4 = a[4];
        long a5 = a[5];
        long a6 = a[6];
        long a7 = a[7];
        long a8 = a[8];
        long a9 = a[9];
        long d0 = a0 << 1;
        long d1 = a1 << 1;
        long d2 = a2 << 1;
        long d3 = a3 << 1;
        long d4 = a4 << 1;
        long d5 = a5 << 1;
        long d6 = a6 << 1;
        long d7 = a7 << 1;
        long d8 = a8 << 1;
        long c0 = a0 * a0;
        long c1 = d0 * a1;
        long c2 = d0 * a2 + a1 * a1;
        long c3 = d0 * a3 + d1 * a2;
        long c4 = d0 * a4 + d1 * a3 + a2 * a2;
        long c5 = d0 * a5 + d1 * a4 + d2 * a3;
        long c6 = d0 * a6 + d1 * a5 + d2 * a4 + a3 * a3;
        long c7 = d0 * a7 + d1 * a6 + d2 * a5 + d3 * a4;
        long c8 = d0 * a8 + d1 * a7 + d2 * a6 + d3 * a5 + a4 * a4;
        long c9 = d0 * a9 + d1 * a8 + d2 * a7 + d3 * a6 + d4 * a5;
        long c10 = d1 * a9 + d2 * a8 + d3 * a7 + d4 * a6 + a5 * a5;
        long c11 = d2 * a9 + d3 * a8 + d4 * a7 + d5 * a6;
        long c12 = d3 * a9 + d4 * a8 + d5 * a7 + a6 * a6;
        long c13 = d4 * a9 + d5 * a8 + d6 * a7;
        long c14 = d5 * a9 + d6 * a8 + a7 * a7;
        long c15 = d6 * a9 + d7 * a8;
        long c16 = d7 * a9 + a8 * a8;
        long c17 = d8 * a9;
        long c18 = a9 * a9;
        c11 += c10 >> 26;
        c10 &= M26;
        c12 += c11 >> 26;
        c11 &= M26;
        c13 += c12 >> 26;
        c12 &= M26;
        c14 += c13 >> 26;
        c13 &= M26;
        c15 += c14 >> 26;
        c14 &= M26;
        c16 += c15 >> 26;
        c15 &= M26;
        c17 += c16 >> 26;
        c16 &= M26;
        c18 += c17 >> 26;
        c17 &= M26;
        long c19 = c18 >> 26;
        c18 &= M26;

        c0 += c10 * 0x3D10L;
        c1 += c10 * 0x400L + c11 * 0x3D10L;
        c2 += c11 * 0x400L + c12 * 0x3D10L;
        c3 += c12 * 0x400L + c13 * 0x3D10L;
        c4 += c13 * 0x400L + c14 * 0x3D10L;
        c5 += c14 * 0x400L + c15 * 0x3D10L;
        c6 += c15 * 0x400L + c16 * 0x3D10L;
        c7 += c16 * 0x400L + c17 * 0x3D10L;
        c8 += c17 * 0x400L + c18 * 0x3D10L;
        c9 += c18 * 0x400L + c19 * 0x3D10L;
        long hi = c19 * 0x400L;

        c1 += c0 >> 26;
        c0 &= M26;
        c2 += c1 >> 26;
        c1 &= M26;
        c3 += c2 >> 26;
        c2 &= M26;
        c4 += c3 >> 26;
        c3 &= M26;
        c5 += c4 >> 26;
        c4 &= M26;
        c6 += c5 >> 26;
        c5 &= M26;
        c7 += c6 >> 26;
        c6 &= M26;
        c8 += c7 >> 26;
        c7 &= M26;
        c9 += c8 >> 26;
        c8 &= M26;
        long top = (c9 >> 22) + (hi << 4);
        c9 &= M22;

        c0 += top * 0x3D1L;
        c1 += top << 6;
        c1 += c0 >> 26;
        r[0] = (int) (c0 & M26);
        c2 += c1 >> 26;
        r[1] = (int) (c1 & M26);
        c3 += c2 >> 26;
        r[2] = (int) (c2 & M26);
        c4 += c3 >> 26;
        r[3] = (int) (c3 & M26);
        c5 += c4 >> 26;
        r[4] = (int) (c4 & M26);
        c6 += c5 >> 26;
        r[5] = (int) (c5 & M26);
        c7 += c6 >> 26;
        r[6] = (int) (c6 & M26);
        c8 += c7 >> 26;
        r[7] = (int) (c7 & M26);
        c9 += c8 >> 26;
        r[8] = (int) (c8 & M26);
        r[9] = (int) c9;
    }


    /// r += a; the magnitudes add.
    static void add(int[] r, int[] a) {
        for (int i = 0; i < 10; i++) {
            r[i] += a[i];
        }
    }

    /// r *= k; the magnitude is multiplied by k.
    static void mulInt(int[] r, int k) {
        for (int i = 0; i < 10; i++) {
            r[i] *= k;
        }
    }

    /// r = -a for an `a` of magnitude at most m; the result has magnitude
    /// m + 1. r may alias a.
    static void neg(int[] r, int[] a, int m) {
        int f = 2 * (m + 1);
        for (int i = 0; i < 10; i++) {
            r[i] = P26[i] * f - a[i];
        }
    }

    /// Brings r to magnitude 1 without fully reducing it.
    static void weak(int[] r) {
        long t0 = r[0];
        long t1 = r[1];
        long t2 = r[2];
        long t3 = r[3];
        long t4 = r[4];
        long t5 = r[5];
        long t6 = r[6];
        long t7 = r[7];
        long t8 = r[8];
        long t9 = r[9];
        t1 += t0 >> 26;
        t0 &= M26;
        t2 += t1 >> 26;
        t1 &= M26;
        t3 += t2 >> 26;
        t2 &= M26;
        t4 += t3 >> 26;
        t3 &= M26;
        t5 += t4 >> 26;
        t4 &= M26;
        t6 += t5 >> 26;
        t5 &= M26;
        t7 += t6 >> 26;
        t6 &= M26;
        t8 += t7 >> 26;
        t7 &= M26;
        t9 += t8 >> 26;
        t8 &= M26;
        long x = t9 >> 22;
        t9 &= M22;
        t0 += x * 0x3D1L;
        t1 += x << 6;
        t1 += t0 >> 26;
        r[0] = (int) (t0 & M26);
        t2 += t1 >> 26;
        r[1] = (int) (t1 & M26);
        t3 += t2 >> 26;
        r[2] = (int) (t2 & M26);
        t4 += t3 >> 26;
        r[3] = (int) (t3 & M26);
        t5 += t4 >> 26;
        r[4] = (int) (t4 & M26);
        t6 += t5 >> 26;
        r[5] = (int) (t5 & M26);
        t7 += t6 >> 26;
        r[6] = (int) (t6 & M26);
        t8 += t7 >> 26;
        r[7] = (int) (t7 & M26);
        t9 += t8 >> 26;
        r[8] = (int) (t8 & M26);
        r[9] = (int) t9;
    }

    /// Reduces r to its canonical value in `[0, p)`. Constant time.
    static void normalize(int[] r) {
        weak(r);
        // weak leaves at most one bit at 2^256; fold it, which cannot carry
        // out again
        int x = r[9] >>> 22;
        r[9] &= M22;
        long t = r[0] + x * 0x3D1L;
        r[0] = (int) (t & M26);
        t = r[1] + (x << 6) + (t >> 26);
        r[1] = (int) (t & M26);
        for (int i = 2; i < 10; i++) {
            t = r[i] + (t >> 26);
            r[i] = (int) (t & M26);
        }
        // now r < 2^256 < 2p: subtract p when r + (2^256 - p) carries out
        int[] u = new int[10];
        t = r[0] + 0x3D1L;
        u[0] = (int) (t & M26);
        t = r[1] + 0x40L + (t >> 26);
        u[1] = (int) (t & M26);
        for (int i = 2; i < 9; i++) {
            t = r[i] + (t >> 26);
            u[i] = (int) (t & M26);
        }
        t = r[9] + (t >> 26);
        u[9] = (int) (t & M22);
        int mask = -(int) (t >> 22);
        for (int i = 0; i < 10; i++) {
            r[i] = (u[i] & mask) | (r[i] & ~mask);
        }
    }

    /// True when a is zero modulo p. Leaves a untouched. Variable time.
    static boolean isZero(int[] a) {
        // after a weak reduction the value is below 2^256 + 2^33, so it is
        // zero modulo p only as 0 or p itself
        long t0 = a[0];
        long t1 = a[1];
        long t2 = a[2];
        long t3 = a[3];
        long t4 = a[4];
        long t5 = a[5];
        long t6 = a[6];
        long t7 = a[7];
        long t8 = a[8];
        long t9 = a[9];
        t1 += t0 >> 26;
        t0 &= M26;
        t2 += t1 >> 26;
        t1 &= M26;
        t3 += t2 >> 26;
        t2 &= M26;
        t4 += t3 >> 26;
        t3 &= M26;
        t5 += t4 >> 26;
        t4 &= M26;
        t6 += t5 >> 26;
        t5 &= M26;
        t7 += t6 >> 26;
        t6 &= M26;
        t8 += t7 >> 26;
        t7 &= M26;
        t9 += t8 >> 26;
        t8 &= M26;
        long x = t9 >> 22;
        t9 &= M22;
        t0 += x * 0x3D1L;
        t1 += x << 6;
        t1 += t0 >> 26;
        t0 &= M26;
        t2 += t1 >> 26;
        t1 &= M26;
        t3 += t2 >> 26;
        t2 &= M26;
        t4 += t3 >> 26;
        t3 &= M26;
        t5 += t4 >> 26;
        t4 &= M26;
        t6 += t5 >> 26;
        t5 &= M26;
        t7 += t6 >> 26;
        t6 &= M26;
        t8 += t7 >> 26;
        t7 &= M26;
        t9 += t8 >> 26;
        t8 &= M26;
        long z = t0 | t1 | t2 | t3 | t4 | t5 | t6 | t7 | t8 | t9;
        return z == 0 || ((t0 ^ 0x3FFFC2FL) | (t1 ^ 0x3FFFFBFL) | (t2 ^ M26) | (t3 ^ M26) | (t4 ^ M26)
                | (t5 ^ M26) | (t6 ^ M26) | (t7 ^ M26) | (t8 ^ M26) | (t9 ^ M22)) == 0;
    }

    /// True when a and b are equal modulo p. Leaves both untouched.
    static boolean equal(int[] a, int[] b) {
        int[] t = new int[10];
        int[] u = new int[10];
        set(t, a);
        set(u, b);
        normalize(t);
        normalize(u);
        int acc = 0;
        for (int i = 0; i < 10; i++) {
            acc |= t[i] ^ u[i];
        }
        return acc == 0;
    }

    /// Reads 32 big-endian bytes, without reducing them.
    static void fromBytes(int[] r, byte[] b, int off) {
        for (int i = 0; i < 10; i++) {
            r[i] = 0;
        }
        for (int i = 0; i < 32; i++) {
            int bit = (31 - i) * 8;
            int v = b[off + i] & 0xff;
            int limb = bit / 26;
            int shift = bit % 26;
            r[limb] |= (v << shift) & M26;
            if (shift > 18 && limb < 9) {
                r[limb + 1] |= v >>> (26 - shift);
            }
        }
    }

    /// True when 32 big-endian bytes hold a value below p.
    static boolean isCanonical(byte[] b, int off) {
        int[] t = new int[10];
        fromBytes(t, b, off);
        int[] u = new int[10];
        set(u, t);
        normalize(u);
        int acc = 0;
        for (int i = 0; i < 10; i++) {
            acc |= t[i] ^ u[i];
        }
        return acc == 0;
    }

    /// Writes a normalized element as 32 big-endian bytes.
    static void toBytes(int[] a, byte[] out, int off) {
        for (int i = 0; i < 32; i++) {
            int bit = (31 - i) * 8;
            int limb = bit / 26;
            int shift = bit % 26;
            int v = a[limb] >>> shift;
            if (shift > 18 && limb < 9) {
                v |= a[limb + 1] << (26 - shift);
            }
            out[off + i] = (byte) v;
        }
    }

    /// True when the normalized element is odd.
    static boolean isOdd(int[] a) {
        return (a[0] & 1) != 0;
    }

    /// r = 1 / a mod p (0 maps to 0). Constant time. r may alias a.
    static void inv(int[] r, int[] a) {
        int[] t = new int[10];
        set(t, a);
        normalize(t);
        int[] s = feToS30(t);
        int[] q = modInverse(s, P30, P_INV30);
        s30ToFe(q, r);
    }

    // packs a normalized 26-bit-limb element into nine 30-bit limbs
    private static int[] feToS30(int[] a) {
        int[] out = new int[9];
        long acc = 0;
        int bits = 0;
        int o = 0;
        for (int i = 0; i < 10; i++) {
            acc |= ((long) a[i]) << bits;
            bits += 26;
            while (bits >= 30 && o < 9) {
                out[o++] = (int) (acc & M30);
                acc >>>= 30;
                bits -= 30;
            }
        }
        if (o < 9) {
            out[o] = (int) acc;
        }
        return out;
    }

    private static void s30ToFe(int[] s, int[] r) {
        long acc = 0;
        int bits = 0;
        int o = 0;
        for (int i = 0; i < 9; i++) {
            acc |= ((long) s[i]) << bits;
            bits += 30;
            while (bits >= 26 && o < 10) {
                r[o++] = (int) (acc & M26);
                acc >>>= 26;
                bits -= 26;
            }
        }
        if (o < 10) {
            r[o] = (int) acc;
        }
    }

    /// r = sqrt(a) when a is a square; returns false (with r undefined)
    /// otherwise. Since p == 3 mod 4 the root is a^((p+1)/4), computed by an
    /// addition chain of 253 squarings and 13 multiplications. r must not
    /// alias a.
    static boolean sqrt(int[] r, int[] a) {
        int[] x2 = fe();
        int[] x3 = fe();
        int[] x6 = fe();
        int[] x9 = fe();
        int[] x11 = fe();
        int[] x22 = fe();
        int[] x44 = fe();
        int[] x88 = fe();
        int[] x176 = fe();
        int[] x220 = fe();
        int[] x223 = fe();
        int[] t = fe();
        sqr(x2, a);
        mul(x2, x2, a);
        sqr(x3, x2);
        mul(x3, x3, a);
        sqrN(x6, x3, 3);
        mul(x6, x6, x3);
        sqrN(x9, x6, 3);
        mul(x9, x9, x3);
        sqrN(x11, x9, 2);
        mul(x11, x11, x2);
        sqrN(x22, x11, 11);
        mul(x22, x22, x11);
        sqrN(x44, x22, 22);
        mul(x44, x44, x22);
        sqrN(x88, x44, 44);
        mul(x88, x88, x44);
        sqrN(x176, x88, 88);
        mul(x176, x176, x88);
        sqrN(x220, x176, 44);
        mul(x220, x220, x44);
        sqrN(x223, x220, 3);
        mul(x223, x223, x3);
        sqrN(t, x223, 23);
        mul(t, t, x22);
        sqrN(t, t, 6);
        mul(t, t, x2);
        sqrN(r, t, 2);
        sqr(t, r);
        return equal(t, a);
    }

    private static void sqrN(int[] r, int[] a, int n) {
        sqr(r, a);
        for (int i = 1; i < n; i++) {
            sqr(r, r);
        }
    }

    // ----------------------------------------------------------------
    // safegcd modular inverse

    /// The inverse of x modulo an odd modulus, both as nine 30-bit limbs
    /// (x reduced, the modulus below 2^256), with `modInv30` the modulus's
    /// inverse modulo 2^30. Constant time; 0 maps to 0.
    static int[] modInverse(int[] x, int[] modulus, int modInv30) {
        int[] d = new int[9];
        int[] e = new int[9];
        e[0] = 1;
        int[] f = new int[9];
        System.arraycopy(modulus, 0, f, 0, 9);
        int[] g = new int[9];
        System.arraycopy(x, 0, g, 0, 9);
        int[] t = new int[4];
        int zeta = -1;
        // 20 * 30 = 600 > 590, the bound on divsteps for 256-bit inputs
        for (int i = 0; i < 20; i++) {
            zeta = divsteps30(zeta, f[0], g[0], t);
            updateDe(d, e, t, modulus, modInv30);
            updateFg(f, g, t);
        }
        normalize30(d, f[8], modulus);
        return d;
    }

    // Thirty division steps on the low bits of f and g. zeta is
    // -(delta + 1/2) of the paper; the returned matrix t = {u, v, q, r} is
    // scaled by 2^30, so that [f', g'] = t [f, g] / 2^30.
    private static int divsteps30(int zeta, int f0, int g0, int[] t) {
        int u = 1;
        int v = 0;
        int q = 0;
        int r = 1;
        int f = f0;
        int g = g0;
        for (int i = 0; i < 30; i++) {
            // c1: zeta < 0, so delta > 0 and the step may swap; c2: g odd
            int c1 = zeta >> 31;
            int c2 = -(g & 1);
            int x = (f ^ c1) - c1;
            int y = (u ^ c1) - c1;
            int z = (v ^ c1) - c1;
            g += x & c2;
            q += y & c2;
            r += z & c2;
            c1 &= c2;
            zeta = (zeta ^ c1) - 1;
            f += g & c1;
            u += q & c1;
            v += r & c1;
            g >>>= 1;
            u <<= 1;
            v <<= 1;
        }
        t[0] = u;
        t[1] = v;
        t[2] = q;
        t[3] = r;
        return zeta;
    }

    // [d, e] = t [d, e] / 2^30 modulo the modulus, keeping both in
    // (-2 modulus, modulus)
    private static void updateDe(int[] d, int[] e, int[] t, int[] p, int pInv30) {
        int u = t[0];
        int v = t[1];
        int q = t[2];
        int r = t[3];
        int sd = d[8] >> 31;
        int se = e[8] >> 31;
        int md = (u & sd) + (v & se);
        int me = (q & sd) + (r & se);
        long cd = (long) u * d[0] + (long) v * e[0];
        long ce = (long) q * d[0] + (long) r * e[0];
        md -= (pInv30 * (int) cd + md) & M30;
        me -= (pInv30 * (int) ce + me) & M30;
        cd += (long) p[0] * md;
        ce += (long) p[0] * me;
        cd >>= 30;
        ce >>= 30;
        for (int i = 1; i < 9; i++) {
            cd += (long) u * d[i] + (long) v * e[i] + (long) p[i] * md;
            ce += (long) q * d[i] + (long) r * e[i] + (long) p[i] * me;
            d[i - 1] = (int) cd & M30;
            cd >>= 30;
            e[i - 1] = (int) ce & M30;
            ce >>= 30;
        }
        d[8] = (int) cd;
        e[8] = (int) ce;
    }

    private static void updateFg(int[] f, int[] g, int[] t) {
        int u = t[0];
        int v = t[1];
        int q = t[2];
        int r = t[3];
        long cf = (long) u * f[0] + (long) v * g[0];
        long cg = (long) q * f[0] + (long) r * g[0];
        cf >>= 30;
        cg >>= 30;
        for (int i = 1; i < 9; i++) {
            cf += (long) u * f[i] + (long) v * g[i];
            cg += (long) q * f[i] + (long) r * g[i];
            f[i - 1] = (int) cf & M30;
            cf >>= 30;
            g[i - 1] = (int) cg & M30;
            cg >>= 30;
        }
        f[8] = (int) cf;
        g[8] = (int) cg;
    }

    // brings d from (-2 modulus, modulus) to [0, modulus), negating it when
    // the final f (the sign of which is passed) is -1
    private static void normalize30(int[] r, int sign, int[] p) {
        int add = r[8] >> 31;
        for (int i = 0; i < 9; i++) {
            r[i] += p[i] & add;
        }
        int negate = sign >> 31;
        for (int i = 0; i < 9; i++) {
            r[i] = (r[i] ^ negate) - negate;
        }
        carry30(r);
        add = r[8] >> 31;
        for (int i = 0; i < 9; i++) {
            r[i] += p[i] & add;
        }
        carry30(r);
    }

    private static void carry30(int[] r) {
        for (int i = 0; i < 8; i++) {
            r[i + 1] += r[i] >> 30;
            r[i] &= M30;
        }
    }

    // ----------------------------------------------------------------
    // points

    /// Scratch space for one operation, so the inner loops allocate nothing.
    static final class Ctx {
        final int[] t0 = new int[10];
        final int[] t1 = new int[10];
        final int[] t2 = new int[10];
        final int[] t3 = new int[10];
        final int[] t4 = new int[10];
        final int[] t5 = new int[10];
        final int[] t6 = new int[10];
        final int[] t7 = new int[10];
        final int[] t8 = new int[10];
        final int[] t9 = new int[10];
    }

    /// A point in Jacobian coordinates, `(x / z^2, y / z^3)`, or infinity.
    static final class Gej {
        final int[] x = new int[10];
        final int[] y = new int[10];
        final int[] z = new int[10];
        boolean inf;

        static Gej infinity() {
            Gej p = new Gej();
            p.inf = true;
            return p;
        }

        static Gej affine(int[] x, int[] y) {
            Gej p = new Gej();
            Secp256k1Math.set(p.x, x);
            Secp256k1Math.set(p.y, y);
            p.z[0] = 1;
            return p;
        }

        void set(Gej o) {
            Secp256k1Math.set(x, o.x);
            Secp256k1Math.set(y, o.y);
            Secp256k1Math.set(z, o.z);
            inf = o.inf;
        }
    }

    /// r = 2a. Inputs of magnitude at most (x 6, y 4, z 2), which every
    /// point operation here returns; the result is (6, 4, 2). r may be a.
    static void dbl(Ctx c, Gej r, Gej a) {
        if (a.inf) {
            r.inf = true;
            return;
        }
        r.inf = false;
        int[] t1 = c.t0;
        int[] t2 = c.t1;
        int[] t3 = c.t2;
        int[] t4 = c.t3;
        mul(r.z, a.z, a.y);
        mulInt(r.z, 2);         // 2yz (2)
        sqr(t1, a.x);
        mulInt(t1, 3);          // 3x^2 (3)
        sqr(t2, t1);            // 9x^4 (1)
        sqr(t3, a.y);
        mulInt(t3, 2);          // 2y^2 (2)
        sqr(t4, t3);
        mulInt(t4, 2);          // 8y^4 (2)
        mul(t3, t3, a.x);       // 2xy^2 (1)
        set(r.x, t3);
        mulInt(r.x, 4);
        neg(r.x, r.x, 4);
        add(r.x, t2);           // 9x^4 - 8xy^2 (6)
        neg(t2, t2, 1);
        mulInt(t3, 6);
        add(t3, t2);            // 12xy^2 - 9x^4 (8)
        mul(r.y, t1, t3);       // (1)
        neg(t2, t4, 2);
        add(r.y, t2);           // 36x^3y^2 - 27x^6 - 8y^4 (4)
    }

    /// r = a + (bx, by), variable time, for public data only. `by` may have
    /// magnitude 2; the result is (5, 3, 1). r may be a.
    static void addGe(Ctx c, Gej r, Gej a, int[] bx, int[] by) {
        if (a.inf) {
            set(r.x, bx);
            set(r.y, by);
            weak(r.y);
            setInt(r.z, 1);
            r.inf = false;
            return;
        }
        int[] z12 = c.t4;
        int[] u1 = c.t5;
        int[] u2 = c.t6;
        int[] s1 = c.t7;
        int[] s2 = c.t8;
        int[] h = c.t9;
        int[] i = c.t0;
        sqr(z12, a.z);
        set(u1, a.x);
        weak(u1);
        mul(u2, bx, z12);
        set(s1, a.y);
        weak(s1);
        mul(s2, by, z12);
        mul(s2, s2, a.z);
        neg(h, u1, 1);
        add(h, u2);             // (3)
        neg(i, s1, 1);
        add(i, s2);             // (3)
        if (isZero(h)) {
            if (isZero(i)) {
                dbl(c, r, a);
            } else {
                r.inf = true;
            }
            return;
        }
        int[] i2 = c.t1;
        int[] h2 = c.t2;
        int[] h3 = c.t3;
        sqr(i2, i);
        sqr(h2, h);
        mul(h3, h, h2);
        mul(r.z, a.z, h);       // (1)
        int[] t = c.t4;
        mul(t, u1, h2);
        set(r.x, t);
        mulInt(r.x, 2);
        add(r.x, h3);
        neg(r.x, r.x, 3);
        add(r.x, i2);           // (5)
        neg(r.y, r.x, 5);
        add(r.y, t);            // (7)
        mul(r.y, r.y, i);
        mul(h3, h3, s1);
        neg(h3, h3, 1);
        add(r.y, h3);           // (3)
        r.inf = false;
    }

    /// r = a + b, variable time, for public data only. The result is
    /// (5, 3, 1). r may be a or b.
    static void addGej(Ctx c, Gej r, Gej a, Gej b) {
        if (a.inf) {
            r.set(b);
            return;
        }
        if (b.inf) {
            r.set(a);
            return;
        }
        int[] z22 = c.t4;
        int[] z12 = c.t5;
        int[] u1 = c.t6;
        int[] u2 = c.t7;
        int[] s1 = c.t8;
        int[] s2 = c.t9;
        sqr(z22, b.z);
        sqr(z12, a.z);
        mul(u1, a.x, z22);
        mul(u2, b.x, z12);
        mul(s1, a.y, z22);
        mul(s1, s1, b.z);
        mul(s2, b.y, z12);
        mul(s2, s2, a.z);
        int[] h = c.t4;
        int[] i = c.t5;
        neg(h, u1, 1);
        add(h, u2);             // (3)
        neg(i, s1, 1);
        add(i, s2);             // (3)
        if (isZero(h)) {
            if (isZero(i)) {
                dbl(c, r, a);
            } else {
                r.inf = true;
            }
            return;
        }
        int[] i2 = c.t1;
        int[] h2 = c.t2;
        int[] h3 = c.t3;
        sqr(i2, i);
        sqr(h2, h);
        mul(h3, h, h2);
        mul(h, h, b.z);
        mul(r.z, a.z, h);       // (1)
        int[] t = c.t7;
        mul(t, u1, h2);
        set(r.x, t);
        mulInt(r.x, 2);
        add(r.x, h3);
        neg(r.x, r.x, 3);
        add(r.x, i2);           // (5)
        neg(r.y, r.x, 5);
        add(r.y, t);            // (7)
        mul(r.y, r.y, i);
        mul(h3, h3, s1);
        neg(h3, h3, 1);
        add(r.y, h3);           // (3)
        r.inf = false;
    }

    /// The affine coordinates of a, normalized; false for infinity.
    static boolean toAffine(Ctx c, Gej a, int[] x, int[] y) {
        if (a.inf) {
            return false;
        }
        int[] zi = c.t0;
        int[] zi2 = c.t1;
        inv(zi, a.z);
        sqr(zi2, zi);
        mul(x, a.x, zi2);
        mul(zi2, zi2, zi);
        mul(y, a.y, zi2);
        normalize(x);
        normalize(y);
        return true;
    }

    /// Converts points (none at infinity) to affine with one inversion.
    static void batchToAffine(Ctx c, Gej[] in, int count, int[][] xs, int[][] ys, int offset) {
        int[][] acc = new int[count][];
        acc[0] = new int[10];
        set(acc[0], in[0].z);
        for (int i = 1; i < count; i++) {
            acc[i] = new int[10];
            mul(acc[i], acc[i - 1], in[i].z);
        }
        int[] inv = new int[10];
        inv(inv, acc[count - 1]);
        int[] zi = c.t2;
        int[] zi2 = c.t3;
        for (int i = count - 1; i >= 0; i--) {
            if (i > 0) {
                mul(zi, inv, acc[i - 1]);
                mul(inv, inv, in[i].z);
            } else {
                set(zi, inv);
            }
            int[] x = new int[10];
            int[] y = new int[10];
            sqr(zi2, zi);
            mul(x, in[i].x, zi2);
            mul(zi2, zi2, zi);
            mul(y, in[i].y, zi2);
            normalize(x);
            normalize(y);
            xs[offset + i] = x;
            ys[offset + i] = y;
        }
    }

    /// The point with abscissa x and the requested parity of y, or null
    /// when x is not on the curve. x must be normalized.
    static int[] liftY(int[] x, boolean odd) {
        int[] rhs = new int[10];
        sqr(rhs, x);
        mul(rhs, rhs, x);
        int[] seven = new int[10];
        seven[0] = 7;
        add(rhs, seven);
        int[] y = new int[10];
        if (!sqrt(y, rhs)) {
            return null;
        }
        normalize(y);
        if (isOdd(y) != odd) {
            neg(y, y, 1);
            normalize(y);
        }
        return y;
    }

    /// True when (x, y) satisfies y^2 = x^3 + 7.
    static boolean onCurve(int[] x, int[] y) {
        int[] rhs = new int[10];
        sqr(rhs, x);
        mul(rhs, rhs, x);
        int[] seven = new int[10];
        seven[0] = 7;
        add(rhs, seven);
        int[] lhs = new int[10];
        sqr(lhs, y);
        return equal(lhs, rhs);
    }

    // ----------------------------------------------------------------
    // constant-time multiplication by the generator

    /// A point in homogeneous projective coordinates `(x / z, y / z)`; the
    /// point at infinity is `(0 : 1 : 0)`.
    static final class Proj {
        final int[] x = new int[10];
        final int[] y = new int[10];
        final int[] z = new int[10];
    }

    // r = r + (x2, y2) by the complete mixed addition of Renes, Costello and
    // Batina (2016, algorithm 8, a = 0, b3 = 21): correct for every r,
    // including infinity and r == +-(x2, y2), with no branch. Inputs of
    // magnitude 1, result magnitude 1.
    private static void addComplete(Ctx c, Proj r, int[] x2, int[] y2) {
        int[] t0 = c.t0;
        int[] t1 = c.t1;
        int[] t2 = c.t2;
        int[] t3 = c.t3;
        int[] t4 = c.t4;
        int[] x3 = c.t5;
        int[] y3 = c.t6;
        int[] z3 = c.t7;
        mul(t0, r.x, x2);
        mul(t1, r.y, y2);
        set(t3, x2);
        add(t3, y2);
        set(t4, r.x);
        add(t4, r.y);
        mul(t3, t3, t4);
        set(t4, t0);
        add(t4, t1);
        neg(t4, t4, 2);
        add(t3, t4);            // x1 y2 + x2 y1 (4)
        mul(t4, y2, r.z);
        add(t4, r.y);           // y1 + y2 z1 (2)
        mul(y3, x2, r.z);
        add(y3, r.x);           // x1 + x2 z1 (2)
        set(x3, t0);
        add(x3, t0);
        add(t0, x3);            // 3 x1 x2 (3)
        set(t2, r.z);
        mulInt(t2, 21);
        weak(t2);               // b3 z1 (1)
        set(z3, t1);
        add(z3, t2);            // (2)
        neg(t2, t2, 1);
        add(t1, t2);            // (3)
        weak(y3);               // (1): 21 times magnitude 2 would overflow an int
        mulInt(y3, 21);
        weak(y3);               // (1)
        mul(x3, t4, y3);
        mul(t2, t3, t1);
        neg(x3, x3, 1);
        add(x3, t2);            // (3)
        mul(y3, y3, t0);
        mul(t1, t1, z3);
        add(y3, t1);            // (2)
        mul(t0, t0, t3);
        mul(z3, z3, t4);
        add(z3, t0);            // (2)
        weak(x3);
        weak(y3);
        weak(z3);
        set(r.x, x3);
        set(r.y, y3);
        set(r.z, z3);
    }

    // Comb width for mulGen: each of GEN_WINDOWS windows of GEN_BITS bits
    // adds one of GEN_ENTRIES odd multiples. Wider windows mean fewer
    // additions and longer constant-time table scans; 6 bits is where the two
    // balance (measured against 4 and 6), at 52 windows of 16 points.
    private static final int GEN_BITS = 5;
    private static final int GEN_ENTRIES = 1 << (GEN_BITS - 1);
    private static final int GEN_WINDOWS = (256 + GEN_BITS - 1) / GEN_BITS;

    // the odd multiples (2j + 1) 2^(GEN_BITS i) G, then 2^(GEN_BITS GEN_WINDOWS) G
    private static final class GenTable {
        static final int[][] X = new int[GEN_WINDOWS * GEN_ENTRIES + 1][];
        static final int[][] Y = new int[GEN_WINDOWS * GEN_ENTRIES + 1][];

        static {
            Ctx c = new Ctx();
            Gej[] pts = new Gej[GEN_WINDOWS * GEN_ENTRIES + 1];
            Gej base = Gej.affine(G_X, G_Y);
            for (int i = 0; i < GEN_WINDOWS; i++) {
                Gej twice = new Gej();
                dbl(c, twice, base);
                Gej acc = new Gej();
                acc.set(base);
                for (int j = 0; j < GEN_ENTRIES; j++) {
                    pts[i * GEN_ENTRIES + j] = new Gej();
                    pts[i * GEN_ENTRIES + j].set(acc);
                    addGej(c, acc, acc, twice);
                }
                for (int k = 0; k < GEN_BITS; k++) {
                    dbl(c, base, base);
                }
            }
            pts[GEN_WINDOWS * GEN_ENTRIES] = base;
            batchToAffine(c, pts, pts.length, X, Y, 0);
        }
    }

    /// The generator's coordinates.
    static final int[] G_X = new int[10];
    static final int[] G_Y = new int[10];

    static {
        byte[] gx = Hash.fromHex("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798");
        byte[] gy = Hash.fromHex("483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8");
        fromBytes(G_X, gx, 0);
        fromBytes(G_Y, gy, 0);
    }

    /// k G for a scalar `0 < k < n` given as eight little-endian 32-bit
    /// words, written to x and y normalized. Constant time.
    ///
    /// The scalar is made odd first (an even k is replaced by n - k and the
    /// result negated), and an odd k has signed odd digits in base
    /// `2^w` (w = GEN_BITS): with `b_i` the (w + 1)-bit window of k starting at
    /// bit `w i`, `k = sum ((b_i | 1) - 2^w) 2^(w i) + 2^(w W)` over the W
    /// windows. Every digit is odd, so every table entry added is a real
    /// point and the comb has no zero case to leak.
    static void mulGen(int[] k, int[] x, int[] y) {
        Ctx c = new Ctx();
        int[] kk = new int[10];
        int even = (k[0] & 1) ^ 1;
        int mask = -even;
        // kk = even ? n - k : k
        int[] nk = sub32(N_WORDS, k);
        for (int i = 0; i < 8; i++) {
            kk[i] = (nk[i] & mask) | (k[i] & ~mask);
        }
        Proj r = new Proj();
        set(r.x, GenTable.X[GEN_WINDOWS * GEN_ENTRIES]);
        set(r.y, GenTable.Y[GEN_WINDOWS * GEN_ENTRIES]);
        r.z[0] = 1;
        int[] px = c.t8;
        int[] py = c.t9;
        int[] ny = new int[10];
        int digitMask = (1 << (GEN_BITS + 1)) - 1;
        for (int i = 0; i < GEN_WINDOWS; i++) {
            int bit = GEN_BITS * i;
            int w = bit >>> 5;
            int b = bit & 31;
            long v = (kk[w] & 0xffffffffL) >>> b;
            if (b + GEN_BITS + 1 > 32) {
                v |= (kk[w + 1] & 0xffffffffL) << (32 - b);
            }
            int d = (((int) v & digitMask) | 1) - (1 << GEN_BITS);
            int s = d >> 31;
            int idx = (((d ^ s) - s) - 1) >> 1;
            for (int l = 0; l < 10; l++) {
                px[l] = 0;
                py[l] = 0;
            }
            int base = i * GEN_ENTRIES;
            for (int j = 0; j < GEN_ENTRIES; j++) {
                int m = ((j ^ idx) - 1) >> 31;
                int[] ex = GenTable.X[base + j];
                int[] ey = GenTable.Y[base + j];
                for (int l = 0; l < 10; l++) {
                    px[l] |= ex[l] & m;
                    py[l] |= ey[l] & m;
                }
            }
            neg(ny, py, 1);
            weak(ny);
            for (int l = 0; l < 10; l++) {
                py[l] = (ny[l] & s) | (py[l] & ~s);
            }
            addComplete(c, r, px, py);
        }
        int[] zi = new int[10];
        inv(zi, r.z);
        mul(x, r.x, zi);
        mul(y, r.y, zi);
        normalize(x);
        normalize(y);
        int[] yn = new int[10];
        neg(yn, y, 1);
        normalize(yn);
        for (int l = 0; l < 10; l++) {
            y[l] = (yn[l] & mask) | (y[l] & ~mask);
        }
        for (int i = 0; i < kk.length; i++) {
            kk[i] = 0;
        }
        for (int i = 0; i < nk.length; i++) {
            nk[i] = 0;
        }
        for (int l = 0; l < 10; l++) {
            px[l] = 0;
            py[l] = 0;
        }
    }

    /// n as eight little-endian 32-bit words.
    static final int[] N_WORDS = {
        0xD0364141, 0xBFD25E8C, 0xAF48A03B, 0xBAAEDCE6, 0xFFFFFFFE, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF
    };

    private static int[] sub32(int[] a, int[] b) {
        int[] out = new int[8];
        long borrow = 0;
        for (int i = 0; i < 8; i++) {
            long d = (a[i] & 0xffffffffL) - (b[i] & 0xffffffffL) - borrow;
            out[i] = (int) d;
            borrow = (d >> 63) & 1;
        }
        return out;
    }

    // ----------------------------------------------------------------
    // variable-time double multiplication for verification

    private static final int WINDOW_A = 5;
    private static final int WINDOW_G = 12;

    // the odd multiples (2j + 1) G, j < 2^(WINDOW_G - 2), and the same
    // points' images under the endomorphism (beta x, y)
    private static final class VerifyTable {
        static final int SIZE = 1 << (WINDOW_G - 2);
        static final int[][] X = new int[SIZE][];
        static final int[][] Y = new int[SIZE][];
        static final int[][] XB = new int[SIZE][];

        static {
            Ctx c = new Ctx();
            Gej g = Gej.affine(G_X, G_Y);
            Gej twice = new Gej();
            dbl(c, twice, g);
            Gej[] pts = new Gej[SIZE];
            Gej acc = new Gej();
            acc.set(g);
            for (int j = 0; j < SIZE; j++) {
                pts[j] = new Gej();
                pts[j].set(acc);
                addGej(c, acc, acc, twice);
            }
            batchToAffine(c, pts, SIZE, X, Y, 0);
            for (int j = 0; j < SIZE; j++) {
                XB[j] = new int[10];
                mul(XB[j], X[j], BETA);
                normalize(XB[j]);
            }
        }
    }

    /// The width-w non-adjacent form of a non-negative scalar below 2^129,
    /// given as 32-bit little-endian words: digits odd and below 2^(w-1) in
    /// absolute value, at most one nonzero in any w consecutive positions.
    static int[] wnaf(int[] s, int w) {
        int len = 130;
        int[] out = new int[len];
        int bit = 0;
        int carry = 0;
        while (bit < len) {
            if (getBits(s, bit, 1) == carry) {
                bit++;
                continue;
            }
            int now = w;
            if (now > len - bit) {
                now = len - bit;
            }
            int word = getBits(s, bit, now) + carry;
            carry = (word >> (w - 1)) & 1;
            word -= carry << w;
            out[bit] = word;
            bit += now;
        }
        return out;
    }

    private static int getBits(int[] s, int off, int count) {
        int w = off >>> 5;
        if (w >= s.length) {
            return 0;
        }
        int b = off & 31;
        long v = (s[w] & 0xffffffffL) >>> b;
        if (b + count > 32 && w + 1 < s.length) {
            v |= (s[w + 1] & 0xffffffffL) << (32 - b);
        }
        return (int) (v & ((1L << count) - 1));
    }

    /// r = (a1 + a2 lambda) A + (g1 + g2 lambda) G, where each half-scalar
    /// is below 2^129 and its sign is passed separately (true = negative).
    /// Strauss-Shamir over four wNAF expansions sharing one chain of
    /// doublings. Variable time; public data only.
    static void ecmult(Gej r, int[] ax, int[] ay, int[] a1, boolean negA1, int[] a2, boolean negA2,
            int[] g1, boolean negG1, int[] g2, boolean negG2) {
        Ctx c = new Ctx();
        int sizeA = 1 << (WINDOW_A - 2);
        Gej[] pre = new Gej[sizeA];
        Gej a = Gej.affine(ax, ay);
        Gej twice = new Gej();
        dbl(c, twice, a);
        Gej acc = new Gej();
        acc.set(a);
        for (int j = 0; j < sizeA; j++) {
            pre[j] = new Gej();
            pre[j].set(acc);
            if (j + 1 < sizeA) {
                addGej(c, acc, acc, twice);
            }
        }
        int[][] tx = new int[sizeA][];
        int[][] ty = new int[sizeA][];
        batchToAffine(c, pre, sizeA, tx, ty, 0);
        int[][] txb = new int[sizeA][];
        for (int j = 0; j < sizeA; j++) {
            txb[j] = new int[10];
            mul(txb[j], tx[j], BETA);
        }
        int[] wa1 = wnaf(a1, WINDOW_A);
        int[] wa2 = wnaf(a2, WINDOW_A);
        int[] wg1 = wnaf(g1, WINDOW_G);
        int[] wg2 = wnaf(g2, WINDOW_G);
        int top = wa1.length - 1;
        while (top >= 0 && wa1[top] == 0 && wa2[top] == 0 && wg1[top] == 0 && wg2[top] == 0) {
            top--;
        }
        int[][] gx = VerifyTable.X;
        int[][] gy = VerifyTable.Y;
        int[][] gxb = VerifyTable.XB;
        int[] ny = new int[10];
        r.inf = true;
        for (int i = top; i >= 0; i--) {
            dbl(c, r, r);
            int d = wa1[i];
            if (d != 0) {
                addEntry(c, r, tx, ty, d, negA1, ny);
            }
            d = wa2[i];
            if (d != 0) {
                addEntry(c, r, txb, ty, d, negA2, ny);
            }
            d = wg1[i];
            if (d != 0) {
                addEntry(c, r, gx, gy, d, negG1, ny);
            }
            d = wg2[i];
            if (d != 0) {
                addEntry(c, r, gxb, gy, d, negG2, ny);
            }
        }
    }

    private static void addEntry(Ctx c, Gej r, int[][] xs, int[][] ys, int d, boolean negate, int[] ny) {
        int idx = ((d < 0 ? -d : d) - 1) >> 1;
        int[] y = ys[idx];
        if ((d < 0) != negate) {
            neg(ny, y, 1);
            y = ny;
        }
        addGe(c, r, r, xs[idx], y);
    }
}
