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
 * The CPU half of the GPU batches, included by cn1_crypto_kernels.c after the amalgamation (it uses
 * libsecp256k1's and ed25519-donna's internals, which are static there).
 *
 * A verify is split where the work changes shape. Everything that branches per entry -- parsing,
 * decompressing a key, the range checks, the inversion of s, the challenge hash -- runs here, on
 * every core, and either decides the entry or writes a fixed-size record of field elements and
 * scalars. The GPU then does what is the same for every entry and is most of the cost: the two
 * scalar multiplications and the final comparison. Each prep function below mirrors its library's
 * own verify step by step, so an entry decided here and an entry checked there give the answer the
 * single verify gives.
 */

/* ------------------------------------------------------------------ the kernels, compiled as C */

#if defined(_MSC_VER) && !defined(__clang__)
#  define CN1K_UNUSED_FN
#  define CN1G_CAS(p, expected, desired) \
       (_InterlockedCompareExchangePointer((void* volatile*) (p), (desired), (expected)) == (expected))
#else
#  define CN1K_UNUSED_FN __attribute__((unused))
#  define CN1G_CAS(p, expected, desired) \
       __atomic_compare_exchange_n((p), &(expected), (desired), 0, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)
#endif

#define uint uint32_t
#define U(x) ((uint32_t) (x))
#define MULHI(a, b) ((uint32_t) (((uint64_t) (a) * (uint64_t) (b)) >> 32))
#define FN static CN1K_UNUSED_FN
#define STRUCT(n) typedef struct n n; struct n
#define CARR(t, n, c) static const t n[c] = {
#define CARR_END };
#define TBL_PARAM , const uint32_t *tbl
#define TBL_ARG , tbl
#define TBL_W(i) tbl[i]
typedef struct { uint32_t x, y; } cn1g_u2;
#define U2 cn1g_u2
static CN1K_UNUSED_FN cn1g_u2 u2(uint32_t a, uint32_t b) {
    cn1g_u2 r;
    r.x = a;
    r.y = b;
    return r;
}
#include "cn1_crypto_gpu_kernels.h"
#undef uint
#undef U
#undef MULHI
#undef FN
#undef STRUCT
#undef CARR
#undef CARR_END
#undef TBL_PARAM
#undef TBL_ARG
#undef TBL_W
#undef U2

#define CN1G_KERNEL_SHA256 0
#define CN1G_KERNEL_KECCAK256 1
#define CN1G_KERNEL_ECDSA 2
#define CN1G_KERNEL_SCHNORR 3
#define CN1G_KERNEL_ED25519 4

/* record sizes in words, as the kernels read them */
#define CN1G_REC_ECDSA 49
#define CN1G_REC_SCHNORR 40
#define CN1G_REC_ED25519 40

/* the fixed-base tables, 64 windows of 16 entries */
#define CN1G_TABLE_G_WORDS (64 * 16 * 16)
#define CN1G_TABLE_B_WORDS (64 * 16 * 24)

/* a prep answer that means "the GPU decides this one" */
#define CN1G_PENDING 2

static void cn1g_words_be(uint32_t *w, const unsigned char *be32) {
    int i;
    for (i = 0; i < 8; i++) {
        const unsigned char *p = be32 + 28 - 4 * i;
        w[i] = ((uint32_t) p[0] << 24) | ((uint32_t) p[1] << 16) | ((uint32_t) p[2] << 8) | p[3];
    }
}

static void cn1g_words_le(uint32_t *w, const unsigned char *le32) {
    int i;
    for (i = 0; i < 8; i++) {
        const unsigned char *p = le32 + 4 * i;
        w[i] = ((uint32_t) p[3] << 24) | ((uint32_t) p[2] << 16) | ((uint32_t) p[1] << 8) | p[0];
    }
}

static void cn1g_scalar_words(uint32_t *w, const secp256k1_scalar *s) {
    unsigned char b[32];
    secp256k1_scalar_get_b32(b, s);
    cn1g_words_be(w, b);
}

static void cn1g_fe_words(uint32_t *w, const secp256k1_fe *f) {
    unsigned char b[32];
    secp256k1_fe t = *f;
    secp256k1_fe_normalize_var(&t);
    secp256k1_fe_get_b32(b, &t);
    cn1g_words_be(w, b);
}

/* secp256k1_ecdsa_verify, up to the multiplication */
static int cn1g_prep_ecdsa(const unsigned char *hash, const unsigned char *sig64, const unsigned char *pub,
                           size_t pubLen, uint32_t *rec) {
    const secp256k1_context *ctx = secp256k1_context_static;
    secp256k1_pubkey pk;
    secp256k1_ecdsa_signature sig;
    secp256k1_scalar r, s, m, sn, u1, u2;
    secp256k1_ge q;
    secp256k1_fe xr;
    if (!secp256k1_ec_pubkey_parse(ctx, &pk, pub, pubLen)) {
        return 0xff;
    }
    if (!secp256k1_ecdsa_signature_parse_compact(ctx, &sig, sig64)) {
        return 0;
    }
    secp256k1_ecdsa_signature_load(ctx, &r, &s, &sig);
    if (secp256k1_scalar_is_high(&s) || !secp256k1_pubkey_load(ctx, &q, &pk)) {
        return 0;
    }
    if (secp256k1_scalar_is_zero(&r) || secp256k1_scalar_is_zero(&s)) {
        return 0;
    }
    secp256k1_scalar_set_b32(&m, hash, NULL);
    secp256k1_scalar_inverse_var(&sn, &s);
    secp256k1_scalar_mul(&u1, &sn, &m);
    secp256k1_scalar_mul(&u2, &sn, &r);
    cn1g_scalar_words(rec, &u1);
    cn1g_scalar_words(rec + 8, &u2);
    cn1g_fe_words(rec + 16, &q.x);
    cn1g_fe_words(rec + 24, &q.y);
    {
        unsigned char c[32];
        secp256k1_scalar_get_b32(c, &r);
        (void) secp256k1_fe_set_b32_limit(&xr, c);
    }
    cn1g_fe_words(rec + 32, &xr);
    if (secp256k1_fe_cmp_var(&xr, &secp256k1_ecdsa_const_p_minus_order) >= 0) {
        memset(rec + 40, 0, 8 * sizeof(uint32_t));
        rec[48] = 0;
    } else {
        secp256k1_fe_add(&xr, &secp256k1_ecdsa_const_order_as_fe);
        cn1g_fe_words(rec + 40, &xr);
        rec[48] = 1;
    }
    return CN1G_PENDING;
}

/* secp256k1_schnorrsig_verify, up to the multiplication */
static int cn1g_prep_schnorr(const unsigned char *msg, size_t msgLen, const unsigned char *sig64,
                             const unsigned char *xonly, uint32_t *rec) {
    const secp256k1_context *ctx = secp256k1_context_static;
    secp256k1_xonly_pubkey xpk;
    secp256k1_scalar s, e;
    secp256k1_fe rx;
    secp256k1_ge pk;
    unsigned char buf[32];
    int overflow;
    if (!secp256k1_xonly_pubkey_parse(ctx, &xpk, xonly)) {
        return 0;
    }
    if (!secp256k1_fe_set_b32_limit(&rx, sig64)) {
        return 0;
    }
    secp256k1_scalar_set_b32(&s, sig64 + 32, &overflow);
    if (overflow || !secp256k1_xonly_pubkey_load(ctx, &pk, &xpk)) {
        return 0;
    }
    secp256k1_fe_get_b32(buf, &pk.x);
    secp256k1_schnorrsig_challenge(&ctx->hash_ctx, &e, sig64, msg, msgLen, buf);
    secp256k1_scalar_negate(&e, &e);
    cn1g_scalar_words(rec, &s);
    cn1g_scalar_words(rec + 8, &e);
    cn1g_fe_words(rec + 16, &pk.x);
    cn1g_fe_words(rec + 24, &pk.y);
    cn1g_fe_words(rec + 32, &rx);
    return CN1G_PENDING;
}

/* ed25519-donna's ed25519_sign_open, up to the multiplication */
static int cn1g_prep_ed25519(const unsigned char *msg, size_t msgLen, const unsigned char *pub,
                             const unsigned char *sig64, uint32_t *rec) {
    ge25519 ALIGN(16) a;
    hash_512bits hash;
    bignum256modm k, s;
    unsigned char b[32];
    if ((sig64[63] & 224) || !ge25519_unpack_negative_vartime(&a, pub)) {
        return 0;
    }
    expand256_modm(s, sig64 + 32, 32);
    contract256_modm(b, s);
    cn1g_words_le(rec, b);
    ed25519_hram(hash, sig64, pub, msg, msgLen);
    expand256_modm(k, hash, 64);
    contract256_modm(b, k);
    cn1g_words_le(rec + 8, b);
    curve25519_contract(b, a.x);
    cn1g_words_le(rec + 16, b);
    curve25519_contract(b, a.y);
    cn1g_words_le(rec + 24, b);
    cn1g_words_le(rec + 32, sig64);
    return CN1G_PENDING;
}

static FE cn1g_fe_of(const uint32_t *w) {
    FE r;
    memcpy(r.v, w, sizeof(r.v));
    return r;
}

/* the generator's 16^w * n multiples, affine, as kj_double_mul reads them */
static uint32_t *cn1g_build_table_g(void) {
    static const uint32_t gx[8] = { 0x16F81798u, 0x59F2815Bu, 0x2DCE28D9u, 0x029BFCDBu,
                                    0xCE870B07u, 0x55A06295u, 0xF9DCBBACu, 0x79BE667Eu };
    static const uint32_t gy[8] = { 0xFB10D4B8u, 0x9C47D08Fu, 0xA6855419u, 0xFD17B448u,
                                    0x0E1108A8u, 0x5DA4FBFCu, 0x26A3C465u, 0x483ADA77u };
    uint32_t *t = (uint32_t*) calloc(CN1G_TABLE_G_WORDS, sizeof(uint32_t));
    KJ base;
    int w, n;
    if (t == NULL) {
        return NULL;
    }
    base = kj_affine(cn1g_fe_of(gx), cn1g_fe_of(gy));
    for (w = 0; w < 64; w++) {
        KJ acc = base;
        for (n = 1; n < 16; n++) {
            FE zi = k_inv(acc.z);
            FE x = k_canon(k_mul(acc.x, zi)), y = k_canon(k_mul(acc.y, zi));
            uint32_t *e = t + ((w * 16) + n) * 16;
            memcpy(e, x.v, 32);
            memcpy(e + 8, y.v, 32);
            acc = kj_add(acc, base);
        }
        base = kj_dbl(kj_dbl(kj_dbl(kj_dbl(base))));
    }
    return t;
}

/* the ed25519 base point's multiples as (y - x, y + x, 2d*x*y), for ep_double_mul */
static uint32_t *cn1g_build_table_b(void) {
    static const uint32_t bx[8] = { 0x8F25D51Au, 0xC9562D60u, 0x9525A7B2u, 0x692CC760u,
                                    0xFDD6DC5Cu, 0xC0A4E231u, 0xCD6E53FEu, 0x216936D3u };
    static const uint32_t by[8] = { 0x66666658u, 0x66666666u, 0x66666666u, 0x66666666u,
                                    0x66666666u, 0x66666666u, 0x66666666u, 0x66666666u };
    uint32_t *t = (uint32_t*) calloc(CN1G_TABLE_B_WORDS, sizeof(uint32_t));
    EP base;
    int w, n;
    if (t == NULL) {
        return NULL;
    }
    base.x = cn1g_fe_of(bx);
    base.y = cn1g_fe_of(by);
    base.z = fe_small(1u);
    base.t = e_mul(base.x, base.y);
    for (w = 0; w < 64; w++) {
        EP acc = base;
        for (n = 1; n < 16; n++) {
            FE zi = e_inv(acc.z);
            FE x = e_mul(acc.x, zi), y = e_mul(acc.y, zi);
            FE ym = e_canon(e_sub(y, x)), yp = e_canon(e_add(y, x)), t2d = e_canon(e_mul(e_mul(x, y), e_d2()));
            uint32_t *e = t + ((w * 16) + n) * 24;
            memcpy(e, ym.v, 32);
            memcpy(e + 8, yp.v, 32);
            memcpy(e + 16, t2d.v, 32);
            acc = ep_add(acc, ep_cache(base));
        }
        base = ep_dbl(ep_dbl(ep_dbl(ep_dbl(base))));
    }
    return t;
}

/* built once; two threads that race here both build it and one copy is freed */
static uint32_t *cn1g_table_g_ptr = NULL;
static uint32_t *cn1g_table_b_ptr = NULL;

static const uint32_t *cn1g_table(int ed) {
    uint32_t **slot = ed ? &cn1g_table_b_ptr : &cn1g_table_g_ptr;
    uint32_t *t = CN1K_PLOAD(slot, uint32_t*);
    if (t == NULL) {
        uint32_t *built = ed ? cn1g_build_table_b() : cn1g_build_table_g();
        uint32_t *expected = NULL;
        if (built == NULL) {
            return NULL;
        }
        if (CN1G_CAS(slot, expected, built)) {
            t = built;
        } else {
            free(built);
            t = CN1K_PLOAD(slot, uint32_t*);
        }
    }
    return t;
}

/* the C build of a kernel, one record: what the GPU computes, for tests and as a reference */
static uint32_t cn1g_check_on_cpu(int kernel, const uint32_t *rec) {
    const uint32_t *tbl = cn1g_table(kernel == CN1G_KERNEL_ED25519);
    if (tbl == NULL) {
        return 0;
    }
    switch (kernel) {
        case CN1G_KERNEL_ECDSA:
            return ecdsa_check(cn1g_fe_of(rec), cn1g_fe_of(rec + 8), cn1g_fe_of(rec + 16), cn1g_fe_of(rec + 24),
                               cn1g_fe_of(rec + 32), cn1g_fe_of(rec + 40), rec[48], tbl);
        case CN1G_KERNEL_SCHNORR:
            return schnorr_check(cn1g_fe_of(rec), cn1g_fe_of(rec + 8), cn1g_fe_of(rec + 16), cn1g_fe_of(rec + 24),
                                 cn1g_fe_of(rec + 32), tbl);
        default:
            return ed25519_check(cn1g_fe_of(rec), cn1g_fe_of(rec + 8), cn1g_fe_of(rec + 16), cn1g_fe_of(rec + 24),
                                 cn1g_fe_of(rec + 32), tbl);
    }
}
