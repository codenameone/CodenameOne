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

/*
 * The GPU crypto kernels, in ONE source that compiles as Metal Shading Language, as GLSL (to
 * SPIR-V, for Vulkan) and as C. Each dialect supplies a short prelude of macros
 * (scripts/crypto-gpu/gen.py writes the MSL and GLSL ones; cn1_crypto_kernels.c has the C one),
 * and the code below keeps to what all three share: 32-bit unsigned arithmetic, structs passed and
 * returned by value, no pointers, no recursion. The C build is what makes the GPU arithmetic
 * testable against libsecp256k1 and ed25519-donna on any machine, and it builds the fixed-base
 * tables the kernels read.
 *
 * Everything here works on PUBLIC data -- signature verification and hashing. Nothing is constant
 * time, and nothing secret may ever be handed to it.
 *
 * Field elements are eight 32-bit limbs, least significant first, kept loosely reduced in
 * [0, 2^256): a field is described by C = 2^256 mod p as two limbs (c0, c1), which is all the
 * folding needs -- secp256k1 has C = 2^32 + 977, ed25519 has C = 38. Only a comparison or an
 * encoding reduces to the canonical value.
 *
 * Prelude names: uint, U(x) (to uint), MULHI(a, b), FN, STRUCT(name), CARR(type, name, n) ...
 * CARR_END, KERNEL(name), IN_W(i), OUT_W(i, v), TBL_W(i), TBL_PARAM / TBL_ARG, P_LEN, P_COUNT
 * (one past the last item of this dispatch), P_BASE (its first item), GID, and U2 / u2(a, b) /
 * .x .y for a pair of words.
 */

#define CN1G_ACC(x) { uint acc_ = (x); lo += acc_; hi += U(lo < acc_); }

STRUCT(FE) { uint v[8]; };
STRUCT(W16) { uint v[16]; };
STRUCT(FEC) { FE f; uint c; };

FN FE fe_small(uint a) {
    FE r;
    for (int i = 0; i < 8; i++) {
        r.v[i] = 0u;
    }
    r.v[0] = a;
    return r;
}

/* a + k*C, k a small multiplier (0..3); the carry out of 2^256 comes back in .c */
FN FEC fe_add_c(FE a, uint k, uint c0, uint c1) {
    FEC r;
    uint lo = 0u, hi = 0u;
    CN1G_ACC(a.v[0]) CN1G_ACC(k * c0)
    r.f.v[0] = lo; lo = hi; hi = 0u;
    CN1G_ACC(a.v[1]) CN1G_ACC(MULHI(k, c0)) CN1G_ACC(k * c1)
    r.f.v[1] = lo; lo = hi; hi = 0u;
    for (int i = 2; i < 8; i++) {
        CN1G_ACC(a.v[i])
        r.f.v[i] = lo; lo = hi; hi = 0u;
    }
    r.c = lo;
    return r;
}

/* a - k*C, the borrow comes back in .c */
FN FEC fe_sub_c(FE a, uint k, uint c0, uint c1) {
    FEC r;
    uint s0 = k * c0, s1 = MULHI(k, c0) + k * c1, bw = 0u;
    for (int i = 0; i < 8; i++) {
        uint s = i == 0 ? s0 : (i == 1 ? s1 : 0u);
        uint d = a.v[i] - s;
        uint b1 = U(a.v[i] < s);
        uint d2 = d - bw;
        uint b2 = U(d < bw);
        r.f.v[i] = d2;
        bw = b1 + b2;
    }
    r.c = bw;
    return r;
}

FN FE f_add(FE a, FE b, uint c0, uint c1) {
    FE r;
    uint c = 0u;
    for (int i = 0; i < 8; i++) {
        uint s = a.v[i] + b.v[i];
        uint k1 = U(s < a.v[i]);
        uint s2 = s + c;
        uint k2 = U(s2 < c);
        r.v[i] = s2;
        c = k1 + k2;
    }
    /* the true value is r + c*2^256 = r + c*C (mod p); that may carry once more */
    FEC t = fe_add_c(r, c, c0, c1);
    FEC t2 = fe_add_c(t.f, t.c, c0, c1);
    return t2.f;
}

FN FE f_sub(FE a, FE b, uint c0, uint c1) {
    FE r;
    uint bw = 0u;
    for (int i = 0; i < 8; i++) {
        uint d = a.v[i] - b.v[i];
        uint b1 = U(a.v[i] < b.v[i]);
        uint d2 = d - bw;
        uint b2 = U(d < bw);
        r.v[i] = d2;
        bw = b1 + b2;
    }
    /* wrapped by 2^256, which is C (mod p) too much */
    FEC t = fe_sub_c(r, bw, c0, c1);
    FEC t2 = fe_sub_c(t.f, t.c, c0, c1);
    return t2.f;
}

/* t mod p for a 512-bit t, loosely */
FN FE f_reduce(W16 t, uint c0, uint c1) {
    FE r;
    uint lo = 0u, hi = 0u, u8 = 0u, u9 = 0u;
    for (int k = 0; k < 10; k++) {
        if (k < 8) {
            CN1G_ACC(t.v[k])
            CN1G_ACC(t.v[8 + k] * c0)
        }
        if (k >= 1 && k <= 8) {
            CN1G_ACC(MULHI(t.v[7 + k], c0))
            CN1G_ACC(t.v[7 + k] * c1)
        }
        if (k < 8) {
            r.v[k] = lo;
        } else if (k == 8) {
            u8 = lo;
        } else {
            u9 = lo;
        }
        lo = hi;
        hi = 0u;
    }
    /* r + (u8 + u9*2^32) * C */
    lo = 0u; hi = 0u;
    CN1G_ACC(r.v[0]) CN1G_ACC(u8 * c0)
    r.v[0] = lo; lo = hi; hi = 0u;
    CN1G_ACC(r.v[1]) CN1G_ACC(MULHI(u8, c0)) CN1G_ACC(u9 * c0) CN1G_ACC(u8 * c1)
    r.v[1] = lo; lo = hi; hi = 0u;
    CN1G_ACC(r.v[2]) CN1G_ACC(MULHI(u9, c0)) CN1G_ACC(u9 * c1)
    r.v[2] = lo; lo = hi; hi = 0u;
    for (int k = 3; k < 8; k++) {
        CN1G_ACC(r.v[k])
        r.v[k] = lo; lo = hi; hi = 0u;
    }
    FEC f = fe_add_c(r, lo, c0, c1);
    return f.f;
}

FN FE f_mul(FE a, FE b, uint c0, uint c1) {
    W16 t;
    for (int k = 0; k < 16; k++) {
        t.v[k] = 0u;
    }
    for (int i = 0; i < 8; i++) {
        uint carry = 0u;
        for (int j = 0; j < 8; j++) {
            uint lo = a.v[i] * b.v[j];
            uint hi = MULHI(a.v[i], b.v[j]);
            uint s = t.v[i + j] + lo;
            hi += U(s < lo);
            uint s2 = s + carry;
            hi += U(s2 < carry);
            t.v[i + j] = s2;
            carry = hi;
        }
        t.v[i + 8] = carry;
    }
    return f_reduce(t, c0, c1);
}

/* squaring: each cross product once, doubled, then the squares */
FN FE f_sqr(FE a, uint c0, uint c1) {
    W16 t;
    for (int k = 0; k < 16; k++) {
        t.v[k] = 0u;
    }
    for (int i = 0; i < 7; i++) {
        uint carry = 0u;
        for (int j = i + 1; j < 8; j++) {
            uint lo = a.v[i] * a.v[j];
            uint hi = MULHI(a.v[i], a.v[j]);
            uint s = t.v[i + j] + lo;
            hi += U(s < lo);
            uint s2 = s + carry;
            hi += U(s2 < carry);
            t.v[i + j] = s2;
            carry = hi;
        }
        t.v[i + 8] = carry;
    }
    uint top = 0u;
    for (int k = 0; k < 16; k++) {
        uint v = t.v[k];
        t.v[k] = (v << 1) | top;
        top = v >> 31;
    }
    uint carry = 0u;
    for (int i = 0; i < 8; i++) {
        uint lo = a.v[i] * a.v[i];
        uint hi = MULHI(a.v[i], a.v[i]);
        uint s = t.v[2 * i] + lo;
        uint k1 = U(s < lo);
        uint s2 = s + carry;
        uint k2 = U(s2 < carry);
        t.v[2 * i] = s2;
        uint h = hi + k1 + k2;
        uint s3 = t.v[2 * i + 1] + h;
        carry = U(s3 < h);
        t.v[2 * i + 1] = s3;
    }
    return f_reduce(t, c0, c1);
}

FN FE f_mul_small(FE a, uint k, uint c0, uint c1) {
    W16 t;
    uint carry = 0u;
    for (int i = 0; i < 8; i++) {
        uint lo = a.v[i] * k;
        uint hi = MULHI(a.v[i], k);
        uint s = lo + carry;
        hi += U(s < carry);
        t.v[i] = s;
        carry = hi;
    }
    t.v[8] = carry;
    for (int i = 9; i < 16; i++) {
        t.v[i] = 0u;
    }
    return f_reduce(t, c0, c1);
}

/* the canonical value, given p; a loose value is below 2^256 < 3p for both fields */
FN FE f_canon(FE a, FE p) {
    for (int rnd = 0; rnd < 2; rnd++) {
        FE d;
        uint bw = 0u;
        for (int i = 0; i < 8; i++) {
            uint x = a.v[i] - p.v[i];
            uint b1 = U(a.v[i] < p.v[i]);
            uint x2 = x - bw;
            uint b2 = U(x < bw);
            d.v[i] = x2;
            bw = b1 + b2;
        }
        if (bw == 0u) {
            a = d;
        }
    }
    return a;
}

FN uint f_eq(FE a, FE b, FE p) {
    FE x = f_canon(a, p), y = f_canon(b, p);
    uint diff = 0u;
    for (int i = 0; i < 8; i++) {
        diff |= x.v[i] ^ y.v[i];
    }
    return U(diff == 0u);
}

FN uint f_is_zero(FE a, FE p) {
    FE x = f_canon(a, p);
    uint acc = 0u;
    for (int i = 0; i < 8; i++) {
        acc |= x.v[i];
    }
    return U(acc == 0u);
}

/* a^e by square and multiply, most significant bit first */
FN FE f_pow(FE a, FE e, uint c0, uint c1) {
    FE r = fe_small(1u);
    for (int i = 255; i >= 0; i--) {
        r = f_sqr(r, c0, c1);
        if (((e.v[i >> 5] >> U(i & 31)) & 1u) != 0u) {
            r = f_mul(r, a, c0, c1);
        }
    }
    return r;
}

/* ------------------------------------------------------------------ secp256k1 */

#define K_C0 977u
#define K_C1 1u

FN FE k_p(void) {
    FE p;
    p.v[0] = 0xFFFFFC2Fu; p.v[1] = 0xFFFFFFFEu;
    for (int i = 2; i < 8; i++) {
        p.v[i] = 0xFFFFFFFFu;
    }
    return p;
}

FN FE k_add(FE a, FE b) { return f_add(a, b, K_C0, K_C1); }
FN FE k_sub(FE a, FE b) { return f_sub(a, b, K_C0, K_C1); }
FN FE k_mul(FE a, FE b) { return f_mul(a, b, K_C0, K_C1); }
FN FE k_sqr(FE a) { return f_sqr(a, K_C0, K_C1); }
FN FE k_mul_small(FE a, uint k) { return f_mul_small(a, k, K_C0, K_C1); }
FN uint k_eq(FE a, FE b) { return f_eq(a, b, k_p()); }
FN uint k_is_zero(FE a) { return f_is_zero(a, k_p()); }
FN FE k_canon(FE a) { return f_canon(a, k_p()); }

FN FE k_inv(FE a) {
    FE e = k_p();
    e.v[0] = 0xFFFFFC2Du;
    return f_pow(a, e, K_C0, K_C1);
}

/*
 * Homogeneous projective: (X/Z, Y/Z), infinity is (0 : 1 : 0). Projective rather than Jacobian
 * because it has a COMPLETE addition (Renes, Costello and Batina 2016): one formula for every pair
 * of inputs, doubling and infinity included, so a scalar multiplication needs one addition site
 * and one doubling site and nothing else.
 *
 * That count is the point of the whole layout. A GPU compiler inlines every call, and the field
 * multiply is the bulk of the code: with the earlier Jacobian formulas, special-cased and called
 * from nine places, the driver's compile of the ECDSA pipeline took 214 seconds on Mesa's
 * lavapipe and its LLVM backend. The addition costs 12 multiplications against 11, which the
 * single-site layout pays back many times over the first time a device compiles it.
 */
STRUCT(KJ) { FE x; FE y; FE z; };

/* 3 * b, b = 7 */
#define K_B3 21u

FN KJ kj_inf(void) {
    KJ r;
    r.x = fe_small(0u);
    r.y = fe_small(1u);
    r.z = fe_small(0u);
    return r;
}

FN KJ kj_affine(FE x, FE y) {
    KJ r;
    r.x = x;
    r.y = y;
    r.z = fe_small(1u);
    return r;
}

/* RCB algorithm 7, a = 0: complete */
FN KJ kj_add(KJ p, KJ q) {
    FE t0 = k_mul(p.x, q.x);
    FE t1 = k_mul(p.y, q.y);
    FE t2 = k_mul(p.z, q.z);
    FE t3 = k_sub(k_mul(k_add(p.x, p.y), k_add(q.x, q.y)), k_add(t0, t1));
    FE t4 = k_sub(k_mul(k_add(p.y, p.z), k_add(q.y, q.z)), k_add(t1, t2));
    FE y3 = k_sub(k_mul(k_add(p.x, p.z), k_add(q.x, q.z)), k_add(t0, t2));
    t0 = k_mul_small(t0, 3u);
    t2 = k_mul_small(t2, K_B3);
    FE z3 = k_add(t1, t2);
    t1 = k_sub(t1, t2);
    y3 = k_mul_small(y3, K_B3);
    KJ r;
    r.x = k_sub(k_mul(t3, t1), k_mul(t4, y3));
    r.y = k_add(k_mul(t1, z3), k_mul(y3, t0));
    r.z = k_add(k_mul(z3, t4), k_mul(t0, t3));
    return r;
}

/* RCB algorithm 9, a = 0: complete */
FN KJ kj_dbl(KJ p) {
    FE t0 = k_sqr(p.y);
    FE z3 = k_mul_small(t0, 8u);
    FE t1 = k_mul(p.y, p.z);
    FE t2 = k_mul_small(k_sqr(p.z), K_B3);
    FE x3 = k_mul(t2, z3);
    FE y3 = k_add(t0, t2);
    KJ r;
    r.z = k_mul(t1, z3);
    t0 = k_sub(t0, k_mul_small(t2, 3u));
    r.y = k_add(x3, k_mul(t0, y3));
    x3 = k_mul(t0, k_mul(p.x, p.y));
    r.x = k_add(x3, x3);
    return r;
}

/* the 4-bit window w of a scalar */
FN uint scalar_nibble(FE s, int w) {
    return (s.v[w >> 3] >> U((w & 7) * 4)) & 15u;
}

/*
 * a*G + b*Q for an affine Q, as one loop of point operations so that each formula is inlined once.
 * Steps 0..13 fill the table of Q's multiples; 14..333 are the variable-base windows, four
 * doublings and an addition each, most significant first; 334..397 add the fixed-base entries,
 * n * 16^w * G from the table in affine form (16 words, x then y, at ((w * 16) + n) * 16); the
 * last step adds the two halves. A zero window is a step skipped, not an addition of infinity.
 */
FN KJ kj_double_mul(FE a, FE b, FE qx, FE qy TBL_PARAM) {
    KJ t[16];
    t[0] = kj_inf();
    t[1] = kj_affine(qx, qy);
    KJ r = kj_inf();
    KJ g = kj_inf();
    for (int s = 0; s < 399; s++) {
        KJ p = r;
        KJ q = t[1];
        uint dbl = 0u;
        if (s < 14) {
            p = t[s + 1];
        } else if (s < 334) {
            int k = s - 14;
            if ((k % 5) < 4) {
                dbl = 1u;
            } else {
                uint n = scalar_nibble(b, 63 - k / 5);
                if (n == 0u) {
                    continue;
                }
                q = t[n];
            }
        } else if (s < 398) {
            int w = s - 334;
            uint n = scalar_nibble(a, w);
            if (n == 0u) {
                continue;
            }
            uint base = (U(w) * 16u + n) * 16u;
            for (int i = 0; i < 8; i++) {
                q.x.v[i] = TBL_W(base + U(i));
                q.y.v[i] = TBL_W(base + 8u + U(i));
            }
            q.z = fe_small(1u);
            p = g;
        } else {
            q = g;
        }
        KJ o;
        if (dbl != 0u) {
            o = kj_dbl(p);
        } else {
            o = kj_add(p, q);
        }
        if (s < 14) {
            t[s + 2] = o;
        } else if (s >= 334 && s < 398) {
            g = o;
        } else {
            r = o;
        }
    }
    return r;
}

/*
 * ECDSA, with every scalar computed on the CPU: u1 = z/s and uu2 = r/s. Valid when R = u1*G + uu2*Q
 * is not infinity and its x is r (or r + n, when that is below p): checked as X == r * Z without
 * an inversion, which is libsecp256k1's own test.
 */
FN uint ecdsa_check(FE u1, FE uu2, FE qx, FE qy, FE xr, FE xr2, uint twice TBL_PARAM) {
    KJ r = kj_double_mul(u1, uu2, qx, qy TBL_ARG);
    if (k_is_zero(r.z) != 0u) {
        return 0u;
    }
    if (k_eq(r.x, k_mul(xr, r.z)) != 0u) {
        return 1u;
    }
    if (twice != 0u && k_eq(r.x, k_mul(xr2, r.z)) != 0u) {
        return 1u;
    }
    return 0u;
}

/* BIP-340, with e (negated) computed on the CPU: R = s*G - e*P must have an even y and x == r */
FN uint schnorr_check(FE s, FE ne, FE px, FE py, FE rx TBL_PARAM) {
    KJ r = kj_double_mul(s, ne, px, py TBL_ARG);
    if (k_is_zero(r.z) != 0u) {
        return 0u;
    }
    FE zi = k_inv(r.z);
    FE y = k_canon(k_mul(r.y, zi));
    if ((y.v[0] & 1u) != 0u) {
        return 0u;
    }
    return k_eq(k_mul(r.x, zi), rx);
}

/* ------------------------------------------------------------------ ed25519 */

#define E_C0 38u
#define E_C1 0u

FN FE e_p(void) {
    FE p;
    p.v[0] = 0xFFFFFFEDu;
    for (int i = 1; i < 7; i++) {
        p.v[i] = 0xFFFFFFFFu;
    }
    p.v[7] = 0x7FFFFFFFu;
    return p;
}

FN FE e_d2(void) {
    FE d;
    d.v[0] = 0x26B2F159u; d.v[1] = 0xEBD69B94u; d.v[2] = 0x8283B156u; d.v[3] = 0x00E0149Au;
    d.v[4] = 0xEEF3D130u; d.v[5] = 0x198E80F2u; d.v[6] = 0x56DFFCE7u; d.v[7] = 0x2406D9DCu;
    return d;
}

FN FE e_add(FE a, FE b) { return f_add(a, b, E_C0, E_C1); }
FN FE e_sub(FE a, FE b) { return f_sub(a, b, E_C0, E_C1); }
FN FE e_mul(FE a, FE b) { return f_mul(a, b, E_C0, E_C1); }
FN FE e_sqr(FE a) { return f_sqr(a, E_C0, E_C1); }
FN FE e_canon(FE a) { return f_canon(a, e_p()); }

FN FE e_inv(FE a) {
    FE e = e_p();
    e.v[0] = 0xFFFFFFEBu;
    return f_pow(a, e, E_C0, E_C1);
}

/* extended twisted Edwards, a = -1: x = X/Z, y = Y/Z, T = XY/Z */
STRUCT(EP) { FE x; FE y; FE z; FE t; };

/* the addend form: (Y - X, Y + X, 2d*T, Z) */
STRUCT(EC) { FE ym; FE yp; FE t2d; FE z; };

FN EP ep_identity(void) {
    EP r;
    r.x = fe_small(0u);
    r.y = fe_small(1u);
    r.z = fe_small(1u);
    r.t = fe_small(0u);
    return r;
}

FN EC ep_cache(EP p) {
    EC r;
    r.ym = e_sub(p.y, p.x);
    r.yp = e_add(p.y, p.x);
    r.t2d = e_mul(p.t, e_d2());
    r.z = p.z;
    return r;
}

/* add-2008-hwcd-3: complete, no special cases */
FN EP ep_add(EP p, EC q) {
    FE a = e_mul(e_sub(p.y, p.x), q.ym);
    FE b = e_mul(e_add(p.y, p.x), q.yp);
    FE c = e_mul(p.t, q.t2d);
    FE zz = e_mul(p.z, q.z);
    FE d = e_add(zz, zz);
    FE e = e_sub(b, a), f = e_sub(d, c), g = e_add(d, c), h = e_add(b, a);
    EP r;
    r.x = e_mul(e, f);
    r.y = e_mul(g, h);
    r.t = e_mul(e, h);
    r.z = e_mul(f, g);
    return r;
}

/* dbl-2008-hwcd, a = -1 */
FN EP ep_dbl(EP p) {
    FE a = e_sqr(p.x);
    FE b = e_sqr(p.y);
    FE zz = e_sqr(p.z);
    FE c = e_add(zz, zz);
    FE e = e_sub(e_sub(e_sqr(e_add(p.x, p.y)), a), b);
    FE g = e_sub(b, a);
    FE f = e_sub(g, c);
    FE h = e_sub(e_sub(fe_small(0u), a), b);
    EP r;
    r.x = e_mul(e, f);
    r.y = e_mul(g, h);
    r.t = e_mul(e, h);
    r.z = e_mul(f, g);
    return r;
}

/*
 * a*B + b*A for an affine A, laid out as kj_double_mul is and for the same reason: one addition
 * site, one doubling site. The fixed-base entries are (y - x, y + x, 2d*x*y), 24 words at
 * ((w * 16) + n) * 24, with Z = 1.
 */
FN EP ep_double_mul(FE a, FE b, FE ax, FE ay TBL_PARAM) {
    EC t[16];
    EP one;
    one.x = ax;
    one.y = ay;
    one.z = fe_small(1u);
    one.t = e_mul(ax, ay);
    t[0] = ep_cache(ep_identity());
    t[1] = ep_cache(one);
    EP last = one;
    EP r = ep_identity();
    EP g = ep_identity();
    for (int s = 0; s < 399; s++) {
        EP p = r;
        EC q = t[1];
        uint dbl = 0u;
        if (s < 14) {
            p = last;
        } else if (s < 334) {
            int k = s - 14;
            if ((k % 5) < 4) {
                dbl = 1u;
            } else {
                uint n = scalar_nibble(b, 63 - k / 5);
                if (n == 0u) {
                    continue;
                }
                q = t[n];
            }
        } else if (s < 398) {
            int w = s - 334;
            uint n = scalar_nibble(a, w);
            if (n == 0u) {
                continue;
            }
            uint base = (U(w) * 16u + n) * 24u;
            for (int i = 0; i < 8; i++) {
                q.ym.v[i] = TBL_W(base + U(i));
                q.yp.v[i] = TBL_W(base + 8u + U(i));
                q.t2d.v[i] = TBL_W(base + 16u + U(i));
            }
            q.z = fe_small(1u);
            p = g;
        } else {
            q = ep_cache(g);
        }
        EP o;
        if (dbl != 0u) {
            o = ep_dbl(p);
        } else {
            o = ep_add(p, q);
        }
        if (s < 14) {
            last = o;
            t[s + 2] = ep_cache(o);
        } else if (s >= 334 && s < 398) {
            g = o;
        } else {
            r = o;
        }
    }
    return r;
}

/* the 32-byte encoding of a point, as eight little-endian words */
FN FE ep_pack(EP p) {
    FE zi = e_inv(p.z);
    FE x = e_canon(e_mul(p.x, zi));
    FE y = e_canon(e_mul(p.y, zi));
    y.v[7] |= (x.v[0] & 1u) << 31;
    return y;
}

/*
 * RFC 8032 as ed25519-donna checks it: encode([s]B + [k](-A)) must be the bytes of R. The CPU
 * has decompressed and negated A, computed k = H(R || A || M) mod L and rejected a non-canonical s.
 */
FN uint ed25519_check(FE s, FE k, FE nax, FE nay, FE rbytes TBL_PARAM) {
    FE enc = ep_pack(ep_double_mul(s, k, nax, nay TBL_ARG));
    uint diff = 0u;
    for (int i = 0; i < 8; i++) {
        diff |= enc.v[i] ^ rbytes.v[i];
    }
    return U(diff == 0u);
}

/* ------------------------------------------------------------------ hashes */

FN uint g_bswap(uint x) {
    return (x >> 24) | ((x >> 8) & 0xFF00u) | ((x << 8) & 0xFF0000u) | (x << 24);
}

FN uint g_ror(uint x, uint n) {
    return (x >> n) | (x << (32u - n));
}

CARR(uint, SHA256_K, 64)
    0x428a2f98u, 0x71374491u, 0xb5c0fbcfu, 0xe9b5dba5u, 0x3956c25bu, 0x59f111f1u, 0x923f82a4u, 0xab1c5ed5u,
    0xd807aa98u, 0x12835b01u, 0x243185beu, 0x550c7dc3u, 0x72be5d74u, 0x80deb1feu, 0x9bdc06a7u, 0xc19bf174u,
    0xe49b69c1u, 0xefbe4786u, 0x0fc19dc6u, 0x240ca1ccu, 0x2de92c6fu, 0x4a7484aau, 0x5cb0a9dcu, 0x76f988dau,
    0x983e5152u, 0xa831c66du, 0xb00327c8u, 0xbf597fc7u, 0xc6e00bf3u, 0xd5a79147u, 0x06ca6351u, 0x14292967u,
    0x27b70a85u, 0x2e1b2138u, 0x4d2c6dfcu, 0x53380d13u, 0x650a7354u, 0x766a0abbu, 0x81c2c92eu, 0x92722c85u,
    0xa2bfe8a1u, 0xa81a664bu, 0xc24b8b70u, 0xc76c51a3u, 0xd192e819u, 0xd6990624u, 0xf40e3585u, 0x106aa070u,
    0x19a4c116u, 0x1e376c08u, 0x2748774cu, 0x34b0bcb5u, 0x391c0cb3u, 0x4ed8aa4au, 0x5b9cca4fu, 0x682e6ff3u,
    0x748f82eeu, 0x78a5636fu, 0x84c87814u, 0x8cc70208u, 0x90befffau, 0xa4506cebu, 0xbef9a3f7u, 0xc67178f2u
CARR_END

STRUCT(H8) { uint v[8]; };

FN H8 sha256_compress(H8 st, W16 w) {
    uint a = st.v[0], b = st.v[1], c = st.v[2], d = st.v[3];
    uint e = st.v[4], f = st.v[5], g = st.v[6], h = st.v[7];
    for (int i = 0; i < 64; i++) {
        uint wi;
        if (i < 16) {
            wi = w.v[i];
        } else {
            uint w15 = w.v[(i - 15) & 15], w2 = w.v[(i - 2) & 15];
            uint s0 = g_ror(w15, 7u) ^ g_ror(w15, 18u) ^ (w15 >> 3);
            uint s1 = g_ror(w2, 17u) ^ g_ror(w2, 19u) ^ (w2 >> 10);
            wi = w.v[i & 15] + s0 + w.v[(i - 7) & 15] + s1;
            w.v[i & 15] = wi;
        }
        uint t1 = h + (g_ror(e, 6u) ^ g_ror(e, 11u) ^ g_ror(e, 25u)) + ((e & f) ^ (~e & g)) + SHA256_K[i] + wi;
        uint t2 = (g_ror(a, 2u) ^ g_ror(a, 13u) ^ g_ror(a, 22u)) + ((a & b) ^ (a & c) ^ (b & c));
        h = g; g = f; f = e; e = d + t1;
        d = c; c = b; b = a; a = t1 + t2;
    }
    st.v[0] += a; st.v[1] += b; st.v[2] += c; st.v[3] += d;
    st.v[4] += e; st.v[5] += f; st.v[6] += g; st.v[7] += h;
    return st;
}

CARR(uint, KECCAK_RC, 48)
    0x00000001u, 0x00000000u, 0x00008082u, 0x00000000u, 0x0000808au, 0x80000000u, 0x80008000u, 0x80000000u,
    0x0000808bu, 0x00000000u, 0x80000001u, 0x00000000u, 0x80008081u, 0x80000000u, 0x00008009u, 0x80000000u,
    0x0000008au, 0x00000000u, 0x00000088u, 0x00000000u, 0x80008009u, 0x00000000u, 0x8000000au, 0x00000000u,
    0x8000808bu, 0x00000000u, 0x0000008bu, 0x80000000u, 0x00008089u, 0x80000000u, 0x00008003u, 0x80000000u,
    0x00008002u, 0x80000000u, 0x00000080u, 0x80000000u, 0x0000800au, 0x00000000u, 0x8000000au, 0x80000000u,
    0x80008081u, 0x80000000u, 0x00008080u, 0x80000000u, 0x80000001u, 0x00000000u, 0x80008008u, 0x80000000u
CARR_END

/* rho offsets and pi destinations, in the order of the classic lane walk */
CARR(uint, KECCAK_ROTC, 24)
    1u, 3u, 6u, 10u, 15u, 21u, 28u, 36u, 45u, 55u, 2u, 14u, 27u, 41u, 56u, 8u, 25u, 43u, 62u, 18u, 39u, 61u, 20u, 44u
CARR_END

CARR(int, KECCAK_PILN, 24)
    10, 7, 11, 17, 18, 3, 5, 16, 8, 21, 24, 4, 15, 23, 19, 13, 12, 2, 20, 14, 22, 9, 6, 1
CARR_END

FN U2 g_rol(U2 v, uint n) {
    /* v.x is the low word */
    if (n == 0u) {
        return v;
    }
    if (n >= 32u) {
        v = u2(v.y, v.x);
        n -= 32u;
        if (n == 0u) {
            return v;
        }
    }
    return u2((v.x << n) | (v.y >> (32u - n)), (v.y << n) | (v.x >> (32u - n)));
}

STRUCT(KS) { U2 a[25]; };

FN KS keccak_f(KS s) {
    for (int rnd = 0; rnd < 24; rnd++) {
        U2 bc[5];
        for (int i = 0; i < 5; i++) {
            bc[i] = u2(s.a[i].x ^ s.a[i + 5].x ^ s.a[i + 10].x ^ s.a[i + 15].x ^ s.a[i + 20].x,
                       s.a[i].y ^ s.a[i + 5].y ^ s.a[i + 10].y ^ s.a[i + 15].y ^ s.a[i + 20].y);
        }
        for (int i = 0; i < 5; i++) {
            U2 r = g_rol(bc[(i + 1) % 5], 1u);
            U2 t = u2(bc[(i + 4) % 5].x ^ r.x, bc[(i + 4) % 5].y ^ r.y);
            for (int j = 0; j < 25; j += 5) {
                s.a[j + i] = u2(s.a[j + i].x ^ t.x, s.a[j + i].y ^ t.y);
            }
        }
        U2 cur = s.a[1];
        for (int i = 0; i < 24; i++) {
            int j = KECCAK_PILN[i];
            U2 nxt = s.a[j];
            s.a[j] = g_rol(cur, KECCAK_ROTC[i]);
            cur = nxt;
        }
        for (int j = 0; j < 25; j += 5) {
            for (int i = 0; i < 5; i++) {
                bc[i] = s.a[j + i];
            }
            for (int i = 0; i < 5; i++) {
                U2 n1 = bc[(i + 1) % 5], n2 = bc[(i + 2) % 5];
                s.a[j + i] = u2(s.a[j + i].x ^ (~n1.x & n2.x), s.a[j + i].y ^ (~n1.y & n2.y));
            }
        }
        s.a[0] = u2(s.a[0].x ^ KECCAK_RC[2 * rnd], s.a[0].y ^ KECCAK_RC[2 * rnd + 1]);
    }
    return s;
}

/* ------------------------------------------------------------------ the kernels */
#ifdef CN1G_KERNELS

/* byte `at` of the input buffer */
#define CN1G_BYTE(at) ((IN_W((at) >> 2u) >> (((at) & 3u) * 8u)) & 0xFFu)

/* one thread per input: P_LEN bytes at GID * P_LEN, digest at GID * 8 words */
#if CN1G_ALL || defined(CN1G_K_SHA256)
KERNEL(cn1g_sha256) {
    uint id = GID + P_BASE;
    if (id >= P_COUNT) {
        return;
    }
    uint len = P_LEN, base = id * len;
    uint blocks = (len + 9u + 63u) / 64u;
    H8 st;
    st.v[0] = 0x6a09e667u; st.v[1] = 0xbb67ae85u; st.v[2] = 0x3c6ef372u; st.v[3] = 0xa54ff53au;
    st.v[4] = 0x510e527fu; st.v[5] = 0x9b05688cu; st.v[6] = 0x1f83d9abu; st.v[7] = 0x5be0cd19u;
    for (uint blk = 0u; blk < blocks; blk++) {
        W16 w;
        for (int i = 0; i < 16; i++) {
            uint q = blk * 64u + U(i) * 4u;
            uint word;
            if (q + 4u <= len && ((base + q) & 3u) == 0u) {
                word = g_bswap(IN_W((base + q) >> 2u));
            } else {
                word = 0u;
                for (uint t = 0u; t < 4u; t++) {
                    uint at = q + t, by = 0u;
                    if (at < len) {
                        by = CN1G_BYTE(base + at);
                    } else if (at == len) {
                        by = 0x80u;
                    } else if (at >= blocks * 64u - 4u) {
                        /* the bit length's low word: a batch entry is far below 512MB */
                        by = ((len * 8u) >> ((blocks * 64u - 1u - at) * 8u)) & 0xFFu;
                    }
                    word |= by << ((3u - t) * 8u);
                }
            }
            w.v[i] = word;
        }
        st = sha256_compress(st, w);
    }
    for (int i = 0; i < 8; i++) {
        OUT_W(id * 8u + U(i), g_bswap(st.v[i]));
    }
}
#endif

#if CN1G_ALL || defined(CN1G_K_KECCAK256)
KERNEL(cn1g_keccak256) {
    uint id = GID + P_BASE;
    if (id >= P_COUNT) {
        return;
    }
    uint len = P_LEN, base = id * len;
    KS s;
    for (int i = 0; i < 25; i++) {
        s.a[i] = u2(0u, 0u);
    }
    uint at = 0u;
    uint done = 0u;
    while (done == 0u) {
        /* one 136-byte block, the last one padded */
        uint last = U(len - at < 136u);
        for (int lane = 0; lane < 17; lane++) {
            uint words[2];
            for (int h = 0; h < 2; h++) {
                uint q = at + U(lane) * 8u + U(h) * 4u;
                uint word;
                if (last == 0u && ((base + q) & 3u) == 0u) {
                    word = IN_W((base + q) >> 2u);
                } else {
                    word = 0u;
                    for (uint t = 0u; t < 4u; t++) {
                        uint p = q + t, by = 0u;
                        if (p < len) {
                            by = CN1G_BYTE(base + p);
                        }
                        if (last != 0u && p == len) {
                            by |= 0x01u;
                        }
                        if (last != 0u && p - at == 135u) {
                            by |= 0x80u;
                        }
                        word |= by << (t * 8u);
                    }
                }
                words[h] = word;
            }
            s.a[lane] = u2(s.a[lane].x ^ words[0], s.a[lane].y ^ words[1]);
        }
        s = keccak_f(s);
        if (last != 0u) {
            done = 1u;
        } else {
            at += 136u;
        }
    }
    for (int i = 0; i < 4; i++) {
        OUT_W(id * 8u + U(i) * 2u, s.a[i].x);
        OUT_W(id * 8u + U(i) * 2u + 1u, s.a[i].y);
    }
}
#endif

/* the verify records, each one result word; a macro because only a kernel can see its buffers */
#define CN1G_LOAD(name, at) FE name; for (int i_ = 0; i_ < 8; i_++) { name.v[i_] = IN_W((at) + U(i_)); }

#if CN1G_ALL || defined(CN1G_K_ECDSA)
KERNEL(cn1g_ecdsa) {
    uint id = GID + P_BASE;
    if (id >= P_COUNT) {
        return;
    }
    uint at = id * 49u;
    CN1G_LOAD(u1, at) CN1G_LOAD(uu2, at + 8u) CN1G_LOAD(qx, at + 16u) CN1G_LOAD(qy, at + 24u)
    CN1G_LOAD(xr, at + 32u) CN1G_LOAD(xr2, at + 40u)
    OUT_W(id, ecdsa_check(u1, uu2, qx, qy, xr, xr2, IN_W(at + 48u) TBL_ARG));
}
#endif

#if CN1G_ALL || defined(CN1G_K_SCHNORR)
KERNEL(cn1g_schnorr) {
    uint id = GID + P_BASE;
    if (id >= P_COUNT) {
        return;
    }
    uint at = id * 40u;
    CN1G_LOAD(s, at) CN1G_LOAD(ne, at + 8u) CN1G_LOAD(px, at + 16u) CN1G_LOAD(py, at + 24u) CN1G_LOAD(rx, at + 32u)
    OUT_W(id, schnorr_check(s, ne, px, py, rx TBL_ARG));
}
#endif

#if CN1G_ALL || defined(CN1G_K_ED25519)
KERNEL(cn1g_ed25519) {
    uint id = GID + P_BASE;
    if (id >= P_COUNT) {
        return;
    }
    uint at = id * 40u;
    CN1G_LOAD(s, at) CN1G_LOAD(k, at + 8u) CN1G_LOAD(nax, at + 16u) CN1G_LOAD(nay, at + 24u) CN1G_LOAD(rb, at + 32u)
    OUT_W(id, ed25519_check(s, k, nax, nay, rb TBL_ARG));
}
#endif

#endif
