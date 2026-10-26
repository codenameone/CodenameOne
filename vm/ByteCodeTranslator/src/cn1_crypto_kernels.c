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
 * Native kernels behind com.codename1.security.NativeCryptoKernels: secp256k1 ECDSA and BIP-340
 * Schnorr (libsecp256k1), Ed25519 (ed25519-donna), Keccak-256, on every ParparVM C target -- iOS,
 * macOS, native Windows and Linux, and the backend.
 *
 * This file is always emitted, so the native entry points always link. The engines themselves
 * arrive in cn1_crypto_kernels_amalgamation.h, which the translator emits only for an
 * application that uses the blockchain cryptography (cn1.cryptoKernels, set by the builders from
 * a scan of the application's classes). Without it every entry point is a stub and available()
 * answers false, so the Java side keeps its own implementation and nothing else changes.
 */
#include "cn1_globals.h"
#include <string.h>
#include <stdint.h>
#include <stddef.h>

#if defined(__has_include)
#  if __has_include("cn1_crypto_kernels_amalgamation.h")
#    define CN1_CRYPTO_KERNELS 1
#  endif
#endif

#define CN1K_BYTES(a) ((unsigned char*) CN1_ARRAY_DATA(a))
#define CN1K_LEN(a) (((JAVA_ARRAY)(a))->length)

#ifdef CN1_CRYPTO_KERNELS

/* ------------------------------------------------------------------ SHA-512, for Ed25519 */

static const uint64_t cn1k_sha512_k[80] = {
    0x428a2f98d728ae22ULL, 0x7137449123ef65cdULL, 0xb5c0fbcfec4d3b2fULL, 0xe9b5dba58189dbbcULL,
    0x3956c25bf348b538ULL, 0x59f111f1b605d019ULL, 0x923f82a4af194f9bULL, 0xab1c5ed5da6d8118ULL,
    0xd807aa98a3030242ULL, 0x12835b0145706fbeULL, 0x243185be4ee4b28cULL, 0x550c7dc3d5ffb4e2ULL,
    0x72be5d74f27b896fULL, 0x80deb1fe3b1696b1ULL, 0x9bdc06a725c71235ULL, 0xc19bf174cf692694ULL,
    0xe49b69c19ef14ad2ULL, 0xefbe4786384f25e3ULL, 0x0fc19dc68b8cd5b5ULL, 0x240ca1cc77ac9c65ULL,
    0x2de92c6f592b0275ULL, 0x4a7484aa6ea6e483ULL, 0x5cb0a9dcbd41fbd4ULL, 0x76f988da831153b5ULL,
    0x983e5152ee66dfabULL, 0xa831c66d2db43210ULL, 0xb00327c898fb213fULL, 0xbf597fc7beef0ee4ULL,
    0xc6e00bf33da88fc2ULL, 0xd5a79147930aa725ULL, 0x06ca6351e003826fULL, 0x142929670a0e6e70ULL,
    0x27b70a8546d22ffcULL, 0x2e1b21385c26c926ULL, 0x4d2c6dfc5ac42aedULL, 0x53380d139d95b3dfULL,
    0x650a73548baf63deULL, 0x766a0abb3c77b2a8ULL, 0x81c2c92e47edaee6ULL, 0x92722c851482353bULL,
    0xa2bfe8a14cf10364ULL, 0xa81a664bbc423001ULL, 0xc24b8b70d0f89791ULL, 0xc76c51a30654be30ULL,
    0xd192e819d6ef5218ULL, 0xd69906245565a910ULL, 0xf40e35855771202aULL, 0x106aa07032bbd1b8ULL,
    0x19a4c116b8d2d0c8ULL, 0x1e376c085141ab53ULL, 0x2748774cdf8eeb99ULL, 0x34b0bcb5e19b48a8ULL,
    0x391c0cb3c5c95a63ULL, 0x4ed8aa4ae3418acbULL, 0x5b9cca4f7763e373ULL, 0x682e6ff3d6b2b8a3ULL,
    0x748f82ee5defb2fcULL, 0x78a5636f43172f60ULL, 0x84c87814a1f0ab72ULL, 0x8cc702081a6439ecULL,
    0x90befffa23631e28ULL, 0xa4506cebde82bde9ULL, 0xbef9a3f7b2c67915ULL, 0xc67178f2e372532bULL,
    0xca273eceea26619cULL, 0xd186b8c721c0c207ULL, 0xeada7dd6cde0eb1eULL, 0xf57d4f7fee6ed178ULL,
    0x06f067aa72176fbaULL, 0x0a637dc5a2c898a6ULL, 0x113f9804bef90daeULL, 0x1b710b35131c471bULL,
    0x28db77f523047d84ULL, 0x32caab7b40c72493ULL, 0x3c9ebe0a15c9bebcULL, 0x431d67c49c100d4cULL,
    0x4cc5d4becb3e42b6ULL, 0x597f299cfc657e2aULL, 0x5fcb6fab3ad6faecULL, 0x6c44198c4a475817ULL
};

typedef struct {
    uint64_t h[8];
    uint64_t bytes;
    unsigned char buf[128];
    size_t used;
} ed25519_hash_context;

#define CN1K_ROR64(x, n) (((x) >> (n)) | ((x) << (64 - (n))))

static void cn1k_sha512_block(uint64_t *h, const unsigned char *p) {
    uint64_t w[80];
    uint64_t a, b, c, d, e, f, g, k;
    int i;
    for (i = 0; i < 16; i++) {
        w[i] = ((uint64_t) p[8 * i] << 56) | ((uint64_t) p[8 * i + 1] << 48) | ((uint64_t) p[8 * i + 2] << 40)
             | ((uint64_t) p[8 * i + 3] << 32) | ((uint64_t) p[8 * i + 4] << 24) | ((uint64_t) p[8 * i + 5] << 16)
             | ((uint64_t) p[8 * i + 6] << 8) | (uint64_t) p[8 * i + 7];
    }
    for (i = 16; i < 80; i++) {
        uint64_t s0 = CN1K_ROR64(w[i - 15], 1) ^ CN1K_ROR64(w[i - 15], 8) ^ (w[i - 15] >> 7);
        uint64_t s1 = CN1K_ROR64(w[i - 2], 19) ^ CN1K_ROR64(w[i - 2], 61) ^ (w[i - 2] >> 6);
        w[i] = w[i - 16] + s0 + w[i - 7] + s1;
    }
    a = h[0]; b = h[1]; c = h[2]; d = h[3]; e = h[4]; f = h[5]; g = h[6]; k = h[7];
    for (i = 0; i < 80; i++) {
        uint64_t t1 = k + (CN1K_ROR64(e, 14) ^ CN1K_ROR64(e, 18) ^ CN1K_ROR64(e, 41))
                    + ((e & f) ^ (~e & g)) + cn1k_sha512_k[i] + w[i];
        uint64_t t2 = (CN1K_ROR64(a, 28) ^ CN1K_ROR64(a, 34) ^ CN1K_ROR64(a, 39))
                    + ((a & b) ^ (a & c) ^ (b & c));
        k = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2;
    }
    h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += k;
}

static void ed25519_hash_init(ed25519_hash_context *ctx) {
    static const uint64_t iv[8] = {
        0x6a09e667f3bcc908ULL, 0xbb67ae8584caa73bULL, 0x3c6ef372fe94f82bULL, 0xa54ff53a5f1d36f1ULL,
        0x510e527fade682d1ULL, 0x9b05688c2b3e6c1fULL, 0x1f83d9abfb41bd6bULL, 0x5be0cd19137e2179ULL
    };
    memcpy(ctx->h, iv, sizeof(iv));
    ctx->bytes = 0;
    ctx->used = 0;
}

static void ed25519_hash_update(ed25519_hash_context *ctx, const unsigned char *in, size_t len) {
    ctx->bytes += len;
    if (ctx->used) {
        size_t take = 128 - ctx->used;
        if (take > len) {
            take = len;
        }
        memcpy(ctx->buf + ctx->used, in, take);
        ctx->used += take;
        in += take;
        len -= take;
        if (ctx->used < 128) {
            return;
        }
        cn1k_sha512_block(ctx->h, ctx->buf);
        ctx->used = 0;
    }
    while (len >= 128) {
        cn1k_sha512_block(ctx->h, in);
        in += 128;
        len -= 128;
    }
    memcpy(ctx->buf, in, len);
    ctx->used = len;
}

static void ed25519_hash_final(ed25519_hash_context *ctx, unsigned char *out) {
    uint64_t bits = ctx->bytes << 3;
    int i;
    ctx->buf[ctx->used++] = 0x80;
    if (ctx->used > 112) {
        memset(ctx->buf + ctx->used, 0, 128 - ctx->used);
        cn1k_sha512_block(ctx->h, ctx->buf);
        ctx->used = 0;
    }
    memset(ctx->buf + ctx->used, 0, 120 - ctx->used);
    for (i = 0; i < 8; i++) {
        ctx->buf[120 + i] = (unsigned char) (bits >> (56 - 8 * i));
    }
    cn1k_sha512_block(ctx->h, ctx->buf);
    for (i = 0; i < 64; i++) {
        out[i] = (unsigned char) (ctx->h[i >> 3] >> (56 - 8 * (i & 7)));
    }
    memset(ctx, 0, sizeof(*ctx));
}

static void ed25519_hash(unsigned char *out, const unsigned char *in, size_t len) {
    ed25519_hash_context ctx;
    ed25519_hash_init(&ctx);
    ed25519_hash_update(&ctx, in, len);
    ed25519_hash_final(&ctx, out);
}

/* ed25519-donna wants a random source only for batch verification, which nothing here calls. */
void ed25519_randombytes_unsafe_cn1(void *p, size_t len);

#include "cn1_crypto_kernels_amalgamation.h"

void ed25519_randombytes_unsafe_cn1(void *p, size_t len) {
    /* unreachable: only ed25519_sign_open_batch calls it */
    memset(p, 0, len);
}

/* ------------------------------------------------------------------ Keccak-256 */

static const uint64_t cn1k_keccak_rc[24] = {
    0x0000000000000001ULL, 0x0000000000008082ULL, 0x800000000000808aULL, 0x8000000080008000ULL,
    0x000000000000808bULL, 0x0000000080000001ULL, 0x8000000080008081ULL, 0x8000000000008009ULL,
    0x000000000000008aULL, 0x0000000000000088ULL, 0x0000000080008009ULL, 0x000000008000000aULL,
    0x000000008000808bULL, 0x800000000000008bULL, 0x8000000000008089ULL, 0x8000000000008003ULL,
    0x8000000000008002ULL, 0x8000000000000080ULL, 0x000000000000800aULL, 0x800000008000000aULL,
    0x8000000080008081ULL, 0x8000000000008080ULL, 0x0000000080000001ULL, 0x8000000080008008ULL
};
static const int cn1k_keccak_rot[25] = {
    0, 1, 62, 28, 27, 36, 44, 6, 55, 20, 3, 10, 43, 25, 39, 41, 45, 15, 21, 8, 18, 2, 61, 56, 14
};

#define CN1K_ROL64(x, n) ((n) == 0 ? (x) : (((x) << (n)) | ((x) >> (64 - (n)))))

static void cn1k_keccak_f(uint64_t *s) {
    uint64_t b[25], c[5], d;
    int round, x, y;
    for (round = 0; round < 24; round++) {
        for (x = 0; x < 5; x++) {
            c[x] = s[x] ^ s[x + 5] ^ s[x + 10] ^ s[x + 15] ^ s[x + 20];
        }
        for (x = 0; x < 5; x++) {
            d = c[(x + 4) % 5] ^ CN1K_ROL64(c[(x + 1) % 5], 1);
            for (y = 0; y < 25; y += 5) {
                s[y + x] ^= d;
            }
        }
        /* rho and pi: lane (x, y) moves to (y, 2x + 3y) */
        for (x = 0; x < 5; x++) {
            for (y = 0; y < 5; y++) {
                int from = x + 5 * y;
                b[y + 5 * ((2 * x + 3 * y) % 5)] = CN1K_ROL64(s[from], cn1k_keccak_rot[from]);
            }
        }
        for (y = 0; y < 25; y += 5) {
            for (x = 0; x < 5; x++) {
                s[y + x] = b[y + x] ^ (~b[y + (x + 1) % 5] & b[y + (x + 2) % 5]);
            }
        }
        s[0] ^= cn1k_keccak_rc[round];
    }
}

static uint64_t cn1k_le64(const unsigned char *p) {
    return (uint64_t) p[0] | ((uint64_t) p[1] << 8) | ((uint64_t) p[2] << 16) | ((uint64_t) p[3] << 24)
         | ((uint64_t) p[4] << 32) | ((uint64_t) p[5] << 40) | ((uint64_t) p[6] << 48) | ((uint64_t) p[7] << 56);
}

static void cn1k_keccak256(const unsigned char *in, size_t len, unsigned char *out) {
    uint64_t s[25];
    unsigned char last[136];
    size_t i;
    memset(s, 0, sizeof(s));
    while (len >= 136) {
        for (i = 0; i < 17; i++) {
            s[i] ^= cn1k_le64(in + 8 * i);
        }
        cn1k_keccak_f(s);
        in += 136;
        len -= 136;
    }
    memset(last, 0, sizeof(last));
    memcpy(last, in, len);
    last[len] = 0x01;
    last[135] |= 0x80;
    for (i = 0; i < 17; i++) {
        s[i] ^= cn1k_le64(last + 8 * i);
    }
    cn1k_keccak_f(s);
    for (i = 0; i < 32; i++) {
        out[i] = (unsigned char) (s[i >> 3] >> (8 * (i & 7)));
    }
}

/* ------------------------------------------------------------------ the secp256k1 context */

#if defined(__APPLE__)
#  include <stdlib.h>
static void cn1k_random(unsigned char *out, size_t len) { arc4random_buf(out, len); }
#elif defined(_WIN32)
#  define _CRT_RAND_S
#  include <stdlib.h>
static void cn1k_random(unsigned char *out, size_t len) {
    size_t i;
    for (i = 0; i < len; i++) {
        unsigned int v = 0;
        rand_s(&v);
        out[i] = (unsigned char) v;
    }
}
#else
#  include <stdio.h>
static void cn1k_random(unsigned char *out, size_t len) {
    FILE *f = fopen("/dev/urandom", "rb");
    if (f == NULL || fread(out, 1, len, f) != len) {
        memset(out, 0, len);
    }
    if (f != NULL) {
        fclose(f);
    }
}
#endif

#if defined(_MSC_VER) && !defined(__clang__)
#  include <intrin.h>
#  define CN1K_CAS(p, expected, desired) \
       (_InterlockedCompareExchangePointer((void* volatile*) (p), (desired), (expected)) == (expected))
#  define CN1K_LOAD(p) ((secp256k1_context*) _InterlockedCompareExchangePointer((void* volatile*) (p), NULL, NULL))
#else
#  define CN1K_CAS(p, expected, desired) \
       __atomic_compare_exchange_n((p), &(expected), (desired), 0, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)
#  define CN1K_LOAD(p) __atomic_load_n((p), __ATOMIC_ACQUIRE)
#endif

/*
 * One context for the process, made on first use. libsecp256k1 allows any number of threads to
 * use a context at once for everything but re-randomizing it, which happens here exactly once,
 * before the context is published. Two threads that race to make it each build one and the loser
 * frees its own.
 */
static secp256k1_context *cn1k_secp_ctx_ptr = NULL;

static secp256k1_context *cn1k_secp(void) {
    secp256k1_context *ctx = CN1K_LOAD(&cn1k_secp_ctx_ptr);
    if (ctx == NULL) {
        unsigned char seed[32];
        secp256k1_context *fresh = secp256k1_context_create(SECP256K1_CONTEXT_NONE);
        secp256k1_context *expected = NULL;
        cn1k_random(seed, sizeof(seed));
        /* blinding against timing and power analysis of the signing multiplication */
        (void) secp256k1_context_randomize(fresh, seed);
        memset(seed, 0, sizeof(seed));
        if (CN1K_CAS(&cn1k_secp_ctx_ptr, expected, fresh)) {
            ctx = fresh;
        } else {
            secp256k1_context_destroy(fresh);
            ctx = CN1K_LOAD(&cn1k_secp_ctx_ptr);
        }
    }
    return ctx;
}

static int cn1k_serialize(secp256k1_context *ctx, const secp256k1_pubkey *pk, unsigned char *out, size_t outLen) {
    size_t len = outLen;
    return secp256k1_ec_pubkey_serialize(ctx, out, &len, pk,
            outLen == 33 ? SECP256K1_EC_COMPRESSED : SECP256K1_EC_UNCOMPRESSED) && len == outLen;
}

#endif /* CN1_CRYPTO_KERNELS */

/* ------------------------------------------------------------------ entry points
 *
 * The Java side (NativeCryptoKernels) validates every length before it calls, so the natives
 * read and write exactly the documented sizes. An output buffer's length chooses the form where
 * there is a choice: 33 bytes for a compressed public key, 65 for an uncompressed one.
 */

JAVA_BOOLEAN com_codename1_security_NativeCryptoKernels_available___R_boolean(CODENAME_ONE_THREAD_STATE) {
#ifdef CN1_CRYPTO_KERNELS
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_security_NativeCryptoKernels_secpPublicKey___byte_1ARRAY_byte_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT priv, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    secp256k1_context *ctx = cn1k_secp();
    secp256k1_pubkey pk;
    if (!secp256k1_ec_pubkey_create(ctx, &pk, CN1K_BYTES(priv))) {
        return JAVA_FALSE;
    }
    return cn1k_serialize(ctx, &pk, CN1K_BYTES(out), (size_t) CN1K_LEN(out)) ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

/* out: r (32) || s (32) || recovery id (1), s normalized to the lower half */
JAVA_BOOLEAN com_codename1_security_NativeCryptoKernels_secpSign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT hash, JAVA_OBJECT priv, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    secp256k1_context *ctx = cn1k_secp();
    secp256k1_ecdsa_recoverable_signature sig;
    int recid = 0;
    unsigned char *o = CN1K_BYTES(out);
    if (!secp256k1_ecdsa_sign_recoverable(ctx, &sig, CN1K_BYTES(hash), CN1K_BYTES(priv), NULL, NULL)) {
        return JAVA_FALSE;
    }
    secp256k1_ecdsa_recoverable_signature_serialize_compact(ctx, o, &recid, &sig);
    o[64] = (unsigned char) recid;
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

/* 1 valid, 0 invalid, -1 when the public key does not parse */
JAVA_INT com_codename1_security_NativeCryptoKernels_secpVerify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT hash, JAVA_OBJECT sig64, JAVA_OBJECT pub) {
#ifdef CN1_CRYPTO_KERNELS
    const secp256k1_context *ctx = secp256k1_context_static;
    secp256k1_pubkey pk;
    secp256k1_ecdsa_signature sig;
    if (!secp256k1_ec_pubkey_parse(ctx, &pk, CN1K_BYTES(pub), (size_t) CN1K_LEN(pub))) {
        return -1;
    }
    if (!secp256k1_ecdsa_signature_parse_compact(ctx, &sig, CN1K_BYTES(sig64))) {
        return 0;
    }
    return secp256k1_ecdsa_verify(ctx, &sig, CN1K_BYTES(hash), &pk) ? 1 : 0;
#else
    return -1;
#endif
}

JAVA_BOOLEAN com_codename1_security_NativeCryptoKernels_secpRecover___byte_1ARRAY_byte_1ARRAY_int_byte_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT hash, JAVA_OBJECT sig64, JAVA_INT recId, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    const secp256k1_context *ctx = secp256k1_context_static;
    secp256k1_ecdsa_recoverable_signature sig;
    secp256k1_pubkey pk;
    if (!secp256k1_ecdsa_recoverable_signature_parse_compact(ctx, &sig, CN1K_BYTES(sig64), recId)) {
        return JAVA_FALSE;
    }
    if (!secp256k1_ecdsa_recover(ctx, &pk, &sig, CN1K_BYTES(hash))) {
        return JAVA_FALSE;
    }
    return cn1k_serialize((secp256k1_context*) ctx, &pk, CN1K_BYTES(out), (size_t) CN1K_LEN(out))
            ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_security_NativeCryptoKernels_secpPrivateTweakAdd___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT priv, JAVA_OBJECT tweak, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    unsigned char *o = CN1K_BYTES(out);
    memcpy(o, CN1K_BYTES(priv), 32);
    if (!secp256k1_ec_seckey_tweak_add(secp256k1_context_static, o, CN1K_BYTES(tweak))) {
        memset(o, 0, 32);
        return JAVA_FALSE;
    }
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_security_NativeCryptoKernels_secpPublicTweakAdd___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pub, JAVA_OBJECT tweak, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    const secp256k1_context *ctx = secp256k1_context_static;
    secp256k1_pubkey pk;
    if (!secp256k1_ec_pubkey_parse(ctx, &pk, CN1K_BYTES(pub), (size_t) CN1K_LEN(pub))) {
        return JAVA_FALSE;
    }
    if (!secp256k1_ec_pubkey_tweak_add(ctx, &pk, CN1K_BYTES(tweak))) {
        return JAVA_FALSE;
    }
    return cn1k_serialize((secp256k1_context*) ctx, &pk, CN1K_BYTES(out), (size_t) CN1K_LEN(out))
            ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_security_NativeCryptoKernels_xOnlyPublicKey___byte_1ARRAY_byte_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT priv, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    secp256k1_context *ctx = cn1k_secp();
    secp256k1_keypair kp;
    secp256k1_xonly_pubkey xpk;
    int ok = secp256k1_keypair_create(ctx, &kp, CN1K_BYTES(priv))
            && secp256k1_keypair_xonly_pub(ctx, &xpk, NULL, &kp)
            && secp256k1_xonly_pubkey_serialize(ctx, CN1K_BYTES(out), &xpk);
    memset(&kp, 0, sizeof(kp));
    return ok ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

/* aux may be null, meaning 32 zero bytes, as in the Java implementation */
JAVA_BOOLEAN com_codename1_security_NativeCryptoKernels_schnorrSign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT msg, JAVA_OBJECT priv, JAVA_OBJECT aux, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    secp256k1_context *ctx = cn1k_secp();
    secp256k1_keypair kp;
    secp256k1_schnorrsig_extraparams extra = SECP256K1_SCHNORRSIG_EXTRAPARAMS_INIT;
    unsigned char zero[32];
    int ok;
    memset(zero, 0, sizeof(zero));
    extra.ndata = aux == JAVA_NULL ? zero : CN1K_BYTES(aux);
    ok = secp256k1_keypair_create(ctx, &kp, CN1K_BYTES(priv))
            && secp256k1_schnorrsig_sign_custom(ctx, CN1K_BYTES(out), CN1K_BYTES(msg),
                    (size_t) CN1K_LEN(msg), &kp, &extra);
    memset(&kp, 0, sizeof(kp));
    return ok ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_INT com_codename1_security_NativeCryptoKernels_schnorrVerify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT msg, JAVA_OBJECT sig64, JAVA_OBJECT xonly) {
#ifdef CN1_CRYPTO_KERNELS
    const secp256k1_context *ctx = secp256k1_context_static;
    secp256k1_xonly_pubkey xpk;
    if (!secp256k1_xonly_pubkey_parse(ctx, &xpk, CN1K_BYTES(xonly))) {
        return 0;
    }
    return secp256k1_schnorrsig_verify(ctx, CN1K_BYTES(sig64), CN1K_BYTES(msg), (size_t) CN1K_LEN(msg), &xpk)
            ? 1 : 0;
#else
    return -1;
#endif
}

JAVA_VOID com_codename1_security_NativeCryptoKernels_ed25519PublicKey___byte_1ARRAY_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT seed, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    ed25519_publickey_cn1(CN1K_BYTES(seed), CN1K_BYTES(out));
#endif
}

JAVA_VOID com_codename1_security_NativeCryptoKernels_ed25519Sign___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT seed, JAVA_OBJECT pub, JAVA_OBJECT msg, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    ed25519_sign_cn1(CN1K_BYTES(msg), (size_t) CN1K_LEN(msg), CN1K_BYTES(seed), CN1K_BYTES(pub), CN1K_BYTES(out));
#endif
}

/* 1 valid, 0 invalid; the Java side has already refused a non-canonical S or key */
JAVA_INT com_codename1_security_NativeCryptoKernels_ed25519Verify___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT msg, JAVA_OBJECT sig, JAVA_OBJECT pub) {
#ifdef CN1_CRYPTO_KERNELS
    return ed25519_sign_open_cn1(CN1K_BYTES(msg), (size_t) CN1K_LEN(msg), CN1K_BYTES(pub), CN1K_BYTES(sig)) == 0
            ? 1 : 0;
#else
    return -1;
#endif
}

JAVA_VOID com_codename1_security_NativeCryptoKernels_keccak256___byte_1ARRAY_int_int_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data, JAVA_INT off, JAVA_INT len, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    cn1k_keccak256(CN1K_BYTES(data) + off, (size_t) len, CN1K_BYTES(out));
#endif
}
