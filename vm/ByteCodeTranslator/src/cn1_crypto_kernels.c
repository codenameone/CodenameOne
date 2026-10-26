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
#include <stdlib.h>

#if defined(__has_include)
#  if __has_include("cn1_crypto_kernels_amalgamation.h")
#    define CN1_CRYPTO_KERNELS 1
#  endif
#endif

#define CN1K_BYTES(a) ((unsigned char*) CN1_ARRAY_DATA(a))
#define CN1K_LEN(a) (((JAVA_ARRAY)(a))->length)

#ifdef CN1_CRYPTO_KERNELS

/* ------------------------------------------------------------------ SHA-512 constants */

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

/* ------------------------------------------------------------------ SHA-256, SHA-512, RIPEMD-160,
 * HMAC and PBKDF2. Portable C everywhere, and the CPU's own SHA instructions where it has them:
 * ARMv8 SHA-256 (every 64-bit Apple and Android device), ARMv8.2 SHA-512 (Apple A13 / M1 and
 * later, and the Android cores that report it), and x86 SHA-NI. Each hardware path is chosen once
 * per process from what the CPU reports, so one binary is right on every device of its ABI. */

#if defined(__aarch64__) && (defined(__clang__) || defined(__GNUC__))
#  include <arm_neon.h>
#  define CN1K_ARM64 1
#  if defined(__APPLE__)
#    include <sys/sysctl.h>
#  elif defined(__linux__)
#    include <sys/auxv.h>
#  endif
/* clang names the extensions bare, GCC with a leading plus */
#  if defined(__ARM_FEATURE_SHA2)
#    define CN1K_ATTR_SHA2
#  elif defined(__clang__)
#    define CN1K_ATTR_SHA2 __attribute__((target("sha2")))
#  else
#    define CN1K_ATTR_SHA2 __attribute__((target("+sha2")))
#  endif
#  if defined(__ARM_FEATURE_SHA512)
#    define CN1K_ATTR_SHA512
#  elif defined(__clang__)
#    define CN1K_ATTR_SHA512 __attribute__((target("sha3")))
#  else
#    define CN1K_ATTR_SHA512 __attribute__((target("+sha3")))
#  endif
#elif (defined(__x86_64__) || defined(__i386__)) && (defined(__clang__) || defined(__GNUC__))
#  include <immintrin.h>
#  include <cpuid.h>
#  define CN1K_X86 1
#endif

static const uint32_t cn1k_sha256_k[64] = {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
};

static const uint32_t cn1k_sha256_iv[8] = {
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
};

static const uint64_t cn1k_sha512_iv[8] = {
    0x6a09e667f3bcc908ULL, 0xbb67ae8584caa73bULL, 0x3c6ef372fe94f82bULL, 0xa54ff53a5f1d36f1ULL,
    0x510e527fade682d1ULL, 0x9b05688c2b3e6c1fULL, 0x1f83d9abfb41bd6bULL, 0x5be0cd19137e2179ULL
};

#define CN1K_ROR32(x, n) (((x) >> (n)) | ((x) << (32 - (n))))

static uint32_t cn1k_be32(const unsigned char *p) {
    return ((uint32_t) p[0] << 24) | ((uint32_t) p[1] << 16) | ((uint32_t) p[2] << 8) | (uint32_t) p[3];
}

static void cn1k_put_be32(unsigned char *p, uint32_t v) {
    p[0] = (unsigned char) (v >> 24); p[1] = (unsigned char) (v >> 16);
    p[2] = (unsigned char) (v >> 8); p[3] = (unsigned char) v;
}

static void cn1k_put_be64(unsigned char *p, uint64_t v) {
    cn1k_put_be32(p, (uint32_t) (v >> 32));
    cn1k_put_be32(p + 4, (uint32_t) v);
}

/* n consecutive blocks, unrolled on rotating names with a 16-word rolling schedule */
#define CN1K_W256(w, i) (w[i] += (CN1K_ROR32(w[(i + 14) & 15], 17) ^ CN1K_ROR32(w[(i + 14) & 15], 19) ^ (w[(i + 14) & 15] >> 10))\
        + w[(i + 9) & 15] + (CN1K_ROR32(w[(i + 1) & 15], 7) ^ CN1K_ROR32(w[(i + 1) & 15], 18) ^ (w[(i + 1) & 15] >> 3)))
#define CN1K_R256(a, b, c, d, e, f, g, h, wv, k) do { \
        uint32_t t1_ = h + (CN1K_ROR32(e, 6) ^ CN1K_ROR32(e, 11) ^ CN1K_ROR32(e, 25)) + (g ^ (e & (f ^ g))) + (k) + (wv); \
        d += t1_; \
        h = t1_ + (CN1K_ROR32(a, 2) ^ CN1K_ROR32(a, 13) ^ CN1K_ROR32(a, 22)) + ((a & b) | (c & (a | b))); } while (0)
#define CN1K_W512(w, i) (w[i] += (CN1K_ROR64(w[(i + 14) & 15], 19) ^ CN1K_ROR64(w[(i + 14) & 15], 61) ^ (w[(i + 14) & 15] >> 6))\
        + w[(i + 9) & 15] + (CN1K_ROR64(w[(i + 1) & 15], 1) ^ CN1K_ROR64(w[(i + 1) & 15], 8) ^ (w[(i + 1) & 15] >> 7)))
#define CN1K_R512(a, b, c, d, e, f, g, h, wv, k) do { \
        uint64_t t1_ = h + (CN1K_ROR64(e, 14) ^ CN1K_ROR64(e, 18) ^ CN1K_ROR64(e, 41)) + (g ^ (e & (f ^ g))) + (k) + (wv); \
        d += t1_; \
        h = t1_ + (CN1K_ROR64(a, 28) ^ CN1K_ROR64(a, 34) ^ CN1K_ROR64(a, 39)) + ((a & b) | (c & (a | b))); } while (0)

static void cn1k_sha256_blocks_c(uint32_t *h, const unsigned char *p, size_t n) {
    while (n-- > 0) {
        uint32_t w[16], a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7];
        int i;
        for (i = 0; i < 16; i++) {
            w[i] = cn1k_be32(p + 4 * i);
        }
        CN1K_R256(a, b, c, d, e, f, g, hh, w[0], cn1k_sha256_k[0]);
        CN1K_R256(hh, a, b, c, d, e, f, g, w[1], cn1k_sha256_k[1]);
        CN1K_R256(g, hh, a, b, c, d, e, f, w[2], cn1k_sha256_k[2]);
        CN1K_R256(f, g, hh, a, b, c, d, e, w[3], cn1k_sha256_k[3]);
        CN1K_R256(e, f, g, hh, a, b, c, d, w[4], cn1k_sha256_k[4]);
        CN1K_R256(d, e, f, g, hh, a, b, c, w[5], cn1k_sha256_k[5]);
        CN1K_R256(c, d, e, f, g, hh, a, b, w[6], cn1k_sha256_k[6]);
        CN1K_R256(b, c, d, e, f, g, hh, a, w[7], cn1k_sha256_k[7]);
        CN1K_R256(a, b, c, d, e, f, g, hh, w[8], cn1k_sha256_k[8]);
        CN1K_R256(hh, a, b, c, d, e, f, g, w[9], cn1k_sha256_k[9]);
        CN1K_R256(g, hh, a, b, c, d, e, f, w[10], cn1k_sha256_k[10]);
        CN1K_R256(f, g, hh, a, b, c, d, e, w[11], cn1k_sha256_k[11]);
        CN1K_R256(e, f, g, hh, a, b, c, d, w[12], cn1k_sha256_k[12]);
        CN1K_R256(d, e, f, g, hh, a, b, c, w[13], cn1k_sha256_k[13]);
        CN1K_R256(c, d, e, f, g, hh, a, b, w[14], cn1k_sha256_k[14]);
        CN1K_R256(b, c, d, e, f, g, hh, a, w[15], cn1k_sha256_k[15]);
        CN1K_R256(a, b, c, d, e, f, g, hh, CN1K_W256(w, 0), cn1k_sha256_k[16]);
        CN1K_R256(hh, a, b, c, d, e, f, g, CN1K_W256(w, 1), cn1k_sha256_k[17]);
        CN1K_R256(g, hh, a, b, c, d, e, f, CN1K_W256(w, 2), cn1k_sha256_k[18]);
        CN1K_R256(f, g, hh, a, b, c, d, e, CN1K_W256(w, 3), cn1k_sha256_k[19]);
        CN1K_R256(e, f, g, hh, a, b, c, d, CN1K_W256(w, 4), cn1k_sha256_k[20]);
        CN1K_R256(d, e, f, g, hh, a, b, c, CN1K_W256(w, 5), cn1k_sha256_k[21]);
        CN1K_R256(c, d, e, f, g, hh, a, b, CN1K_W256(w, 6), cn1k_sha256_k[22]);
        CN1K_R256(b, c, d, e, f, g, hh, a, CN1K_W256(w, 7), cn1k_sha256_k[23]);
        CN1K_R256(a, b, c, d, e, f, g, hh, CN1K_W256(w, 8), cn1k_sha256_k[24]);
        CN1K_R256(hh, a, b, c, d, e, f, g, CN1K_W256(w, 9), cn1k_sha256_k[25]);
        CN1K_R256(g, hh, a, b, c, d, e, f, CN1K_W256(w, 10), cn1k_sha256_k[26]);
        CN1K_R256(f, g, hh, a, b, c, d, e, CN1K_W256(w, 11), cn1k_sha256_k[27]);
        CN1K_R256(e, f, g, hh, a, b, c, d, CN1K_W256(w, 12), cn1k_sha256_k[28]);
        CN1K_R256(d, e, f, g, hh, a, b, c, CN1K_W256(w, 13), cn1k_sha256_k[29]);
        CN1K_R256(c, d, e, f, g, hh, a, b, CN1K_W256(w, 14), cn1k_sha256_k[30]);
        CN1K_R256(b, c, d, e, f, g, hh, a, CN1K_W256(w, 15), cn1k_sha256_k[31]);
        CN1K_R256(a, b, c, d, e, f, g, hh, CN1K_W256(w, 0), cn1k_sha256_k[32]);
        CN1K_R256(hh, a, b, c, d, e, f, g, CN1K_W256(w, 1), cn1k_sha256_k[33]);
        CN1K_R256(g, hh, a, b, c, d, e, f, CN1K_W256(w, 2), cn1k_sha256_k[34]);
        CN1K_R256(f, g, hh, a, b, c, d, e, CN1K_W256(w, 3), cn1k_sha256_k[35]);
        CN1K_R256(e, f, g, hh, a, b, c, d, CN1K_W256(w, 4), cn1k_sha256_k[36]);
        CN1K_R256(d, e, f, g, hh, a, b, c, CN1K_W256(w, 5), cn1k_sha256_k[37]);
        CN1K_R256(c, d, e, f, g, hh, a, b, CN1K_W256(w, 6), cn1k_sha256_k[38]);
        CN1K_R256(b, c, d, e, f, g, hh, a, CN1K_W256(w, 7), cn1k_sha256_k[39]);
        CN1K_R256(a, b, c, d, e, f, g, hh, CN1K_W256(w, 8), cn1k_sha256_k[40]);
        CN1K_R256(hh, a, b, c, d, e, f, g, CN1K_W256(w, 9), cn1k_sha256_k[41]);
        CN1K_R256(g, hh, a, b, c, d, e, f, CN1K_W256(w, 10), cn1k_sha256_k[42]);
        CN1K_R256(f, g, hh, a, b, c, d, e, CN1K_W256(w, 11), cn1k_sha256_k[43]);
        CN1K_R256(e, f, g, hh, a, b, c, d, CN1K_W256(w, 12), cn1k_sha256_k[44]);
        CN1K_R256(d, e, f, g, hh, a, b, c, CN1K_W256(w, 13), cn1k_sha256_k[45]);
        CN1K_R256(c, d, e, f, g, hh, a, b, CN1K_W256(w, 14), cn1k_sha256_k[46]);
        CN1K_R256(b, c, d, e, f, g, hh, a, CN1K_W256(w, 15), cn1k_sha256_k[47]);
        CN1K_R256(a, b, c, d, e, f, g, hh, CN1K_W256(w, 0), cn1k_sha256_k[48]);
        CN1K_R256(hh, a, b, c, d, e, f, g, CN1K_W256(w, 1), cn1k_sha256_k[49]);
        CN1K_R256(g, hh, a, b, c, d, e, f, CN1K_W256(w, 2), cn1k_sha256_k[50]);
        CN1K_R256(f, g, hh, a, b, c, d, e, CN1K_W256(w, 3), cn1k_sha256_k[51]);
        CN1K_R256(e, f, g, hh, a, b, c, d, CN1K_W256(w, 4), cn1k_sha256_k[52]);
        CN1K_R256(d, e, f, g, hh, a, b, c, CN1K_W256(w, 5), cn1k_sha256_k[53]);
        CN1K_R256(c, d, e, f, g, hh, a, b, CN1K_W256(w, 6), cn1k_sha256_k[54]);
        CN1K_R256(b, c, d, e, f, g, hh, a, CN1K_W256(w, 7), cn1k_sha256_k[55]);
        CN1K_R256(a, b, c, d, e, f, g, hh, CN1K_W256(w, 8), cn1k_sha256_k[56]);
        CN1K_R256(hh, a, b, c, d, e, f, g, CN1K_W256(w, 9), cn1k_sha256_k[57]);
        CN1K_R256(g, hh, a, b, c, d, e, f, CN1K_W256(w, 10), cn1k_sha256_k[58]);
        CN1K_R256(f, g, hh, a, b, c, d, e, CN1K_W256(w, 11), cn1k_sha256_k[59]);
        CN1K_R256(e, f, g, hh, a, b, c, d, CN1K_W256(w, 12), cn1k_sha256_k[60]);
        CN1K_R256(d, e, f, g, hh, a, b, c, CN1K_W256(w, 13), cn1k_sha256_k[61]);
        CN1K_R256(c, d, e, f, g, hh, a, b, CN1K_W256(w, 14), cn1k_sha256_k[62]);
        CN1K_R256(b, c, d, e, f, g, hh, a, CN1K_W256(w, 15), cn1k_sha256_k[63]);
        h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh;
        p += 64;
    }
}

static void cn1k_sha512_blocks_c(uint64_t *h, const unsigned char *p, size_t n) {
    while (n-- > 0) {
        uint64_t w[16], a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7];
        int i;
        for (i = 0; i < 16; i++) {
            w[i] = ((uint64_t) cn1k_be32(p + 8 * i) << 32) | cn1k_be32(p + 8 * i + 4);
        }
        CN1K_R512(a, b, c, d, e, f, g, hh, w[0], cn1k_sha512_k[0]);
        CN1K_R512(hh, a, b, c, d, e, f, g, w[1], cn1k_sha512_k[1]);
        CN1K_R512(g, hh, a, b, c, d, e, f, w[2], cn1k_sha512_k[2]);
        CN1K_R512(f, g, hh, a, b, c, d, e, w[3], cn1k_sha512_k[3]);
        CN1K_R512(e, f, g, hh, a, b, c, d, w[4], cn1k_sha512_k[4]);
        CN1K_R512(d, e, f, g, hh, a, b, c, w[5], cn1k_sha512_k[5]);
        CN1K_R512(c, d, e, f, g, hh, a, b, w[6], cn1k_sha512_k[6]);
        CN1K_R512(b, c, d, e, f, g, hh, a, w[7], cn1k_sha512_k[7]);
        CN1K_R512(a, b, c, d, e, f, g, hh, w[8], cn1k_sha512_k[8]);
        CN1K_R512(hh, a, b, c, d, e, f, g, w[9], cn1k_sha512_k[9]);
        CN1K_R512(g, hh, a, b, c, d, e, f, w[10], cn1k_sha512_k[10]);
        CN1K_R512(f, g, hh, a, b, c, d, e, w[11], cn1k_sha512_k[11]);
        CN1K_R512(e, f, g, hh, a, b, c, d, w[12], cn1k_sha512_k[12]);
        CN1K_R512(d, e, f, g, hh, a, b, c, w[13], cn1k_sha512_k[13]);
        CN1K_R512(c, d, e, f, g, hh, a, b, w[14], cn1k_sha512_k[14]);
        CN1K_R512(b, c, d, e, f, g, hh, a, w[15], cn1k_sha512_k[15]);
        CN1K_R512(a, b, c, d, e, f, g, hh, CN1K_W512(w, 0), cn1k_sha512_k[16]);
        CN1K_R512(hh, a, b, c, d, e, f, g, CN1K_W512(w, 1), cn1k_sha512_k[17]);
        CN1K_R512(g, hh, a, b, c, d, e, f, CN1K_W512(w, 2), cn1k_sha512_k[18]);
        CN1K_R512(f, g, hh, a, b, c, d, e, CN1K_W512(w, 3), cn1k_sha512_k[19]);
        CN1K_R512(e, f, g, hh, a, b, c, d, CN1K_W512(w, 4), cn1k_sha512_k[20]);
        CN1K_R512(d, e, f, g, hh, a, b, c, CN1K_W512(w, 5), cn1k_sha512_k[21]);
        CN1K_R512(c, d, e, f, g, hh, a, b, CN1K_W512(w, 6), cn1k_sha512_k[22]);
        CN1K_R512(b, c, d, e, f, g, hh, a, CN1K_W512(w, 7), cn1k_sha512_k[23]);
        CN1K_R512(a, b, c, d, e, f, g, hh, CN1K_W512(w, 8), cn1k_sha512_k[24]);
        CN1K_R512(hh, a, b, c, d, e, f, g, CN1K_W512(w, 9), cn1k_sha512_k[25]);
        CN1K_R512(g, hh, a, b, c, d, e, f, CN1K_W512(w, 10), cn1k_sha512_k[26]);
        CN1K_R512(f, g, hh, a, b, c, d, e, CN1K_W512(w, 11), cn1k_sha512_k[27]);
        CN1K_R512(e, f, g, hh, a, b, c, d, CN1K_W512(w, 12), cn1k_sha512_k[28]);
        CN1K_R512(d, e, f, g, hh, a, b, c, CN1K_W512(w, 13), cn1k_sha512_k[29]);
        CN1K_R512(c, d, e, f, g, hh, a, b, CN1K_W512(w, 14), cn1k_sha512_k[30]);
        CN1K_R512(b, c, d, e, f, g, hh, a, CN1K_W512(w, 15), cn1k_sha512_k[31]);
        CN1K_R512(a, b, c, d, e, f, g, hh, CN1K_W512(w, 0), cn1k_sha512_k[32]);
        CN1K_R512(hh, a, b, c, d, e, f, g, CN1K_W512(w, 1), cn1k_sha512_k[33]);
        CN1K_R512(g, hh, a, b, c, d, e, f, CN1K_W512(w, 2), cn1k_sha512_k[34]);
        CN1K_R512(f, g, hh, a, b, c, d, e, CN1K_W512(w, 3), cn1k_sha512_k[35]);
        CN1K_R512(e, f, g, hh, a, b, c, d, CN1K_W512(w, 4), cn1k_sha512_k[36]);
        CN1K_R512(d, e, f, g, hh, a, b, c, CN1K_W512(w, 5), cn1k_sha512_k[37]);
        CN1K_R512(c, d, e, f, g, hh, a, b, CN1K_W512(w, 6), cn1k_sha512_k[38]);
        CN1K_R512(b, c, d, e, f, g, hh, a, CN1K_W512(w, 7), cn1k_sha512_k[39]);
        CN1K_R512(a, b, c, d, e, f, g, hh, CN1K_W512(w, 8), cn1k_sha512_k[40]);
        CN1K_R512(hh, a, b, c, d, e, f, g, CN1K_W512(w, 9), cn1k_sha512_k[41]);
        CN1K_R512(g, hh, a, b, c, d, e, f, CN1K_W512(w, 10), cn1k_sha512_k[42]);
        CN1K_R512(f, g, hh, a, b, c, d, e, CN1K_W512(w, 11), cn1k_sha512_k[43]);
        CN1K_R512(e, f, g, hh, a, b, c, d, CN1K_W512(w, 12), cn1k_sha512_k[44]);
        CN1K_R512(d, e, f, g, hh, a, b, c, CN1K_W512(w, 13), cn1k_sha512_k[45]);
        CN1K_R512(c, d, e, f, g, hh, a, b, CN1K_W512(w, 14), cn1k_sha512_k[46]);
        CN1K_R512(b, c, d, e, f, g, hh, a, CN1K_W512(w, 15), cn1k_sha512_k[47]);
        CN1K_R512(a, b, c, d, e, f, g, hh, CN1K_W512(w, 0), cn1k_sha512_k[48]);
        CN1K_R512(hh, a, b, c, d, e, f, g, CN1K_W512(w, 1), cn1k_sha512_k[49]);
        CN1K_R512(g, hh, a, b, c, d, e, f, CN1K_W512(w, 2), cn1k_sha512_k[50]);
        CN1K_R512(f, g, hh, a, b, c, d, e, CN1K_W512(w, 3), cn1k_sha512_k[51]);
        CN1K_R512(e, f, g, hh, a, b, c, d, CN1K_W512(w, 4), cn1k_sha512_k[52]);
        CN1K_R512(d, e, f, g, hh, a, b, c, CN1K_W512(w, 5), cn1k_sha512_k[53]);
        CN1K_R512(c, d, e, f, g, hh, a, b, CN1K_W512(w, 6), cn1k_sha512_k[54]);
        CN1K_R512(b, c, d, e, f, g, hh, a, CN1K_W512(w, 7), cn1k_sha512_k[55]);
        CN1K_R512(a, b, c, d, e, f, g, hh, CN1K_W512(w, 8), cn1k_sha512_k[56]);
        CN1K_R512(hh, a, b, c, d, e, f, g, CN1K_W512(w, 9), cn1k_sha512_k[57]);
        CN1K_R512(g, hh, a, b, c, d, e, f, CN1K_W512(w, 10), cn1k_sha512_k[58]);
        CN1K_R512(f, g, hh, a, b, c, d, e, CN1K_W512(w, 11), cn1k_sha512_k[59]);
        CN1K_R512(e, f, g, hh, a, b, c, d, CN1K_W512(w, 12), cn1k_sha512_k[60]);
        CN1K_R512(d, e, f, g, hh, a, b, c, CN1K_W512(w, 13), cn1k_sha512_k[61]);
        CN1K_R512(c, d, e, f, g, hh, a, b, CN1K_W512(w, 14), cn1k_sha512_k[62]);
        CN1K_R512(b, c, d, e, f, g, hh, a, CN1K_W512(w, 15), cn1k_sha512_k[63]);
        CN1K_R512(a, b, c, d, e, f, g, hh, CN1K_W512(w, 0), cn1k_sha512_k[64]);
        CN1K_R512(hh, a, b, c, d, e, f, g, CN1K_W512(w, 1), cn1k_sha512_k[65]);
        CN1K_R512(g, hh, a, b, c, d, e, f, CN1K_W512(w, 2), cn1k_sha512_k[66]);
        CN1K_R512(f, g, hh, a, b, c, d, e, CN1K_W512(w, 3), cn1k_sha512_k[67]);
        CN1K_R512(e, f, g, hh, a, b, c, d, CN1K_W512(w, 4), cn1k_sha512_k[68]);
        CN1K_R512(d, e, f, g, hh, a, b, c, CN1K_W512(w, 5), cn1k_sha512_k[69]);
        CN1K_R512(c, d, e, f, g, hh, a, b, CN1K_W512(w, 6), cn1k_sha512_k[70]);
        CN1K_R512(b, c, d, e, f, g, hh, a, CN1K_W512(w, 7), cn1k_sha512_k[71]);
        CN1K_R512(a, b, c, d, e, f, g, hh, CN1K_W512(w, 8), cn1k_sha512_k[72]);
        CN1K_R512(hh, a, b, c, d, e, f, g, CN1K_W512(w, 9), cn1k_sha512_k[73]);
        CN1K_R512(g, hh, a, b, c, d, e, f, CN1K_W512(w, 10), cn1k_sha512_k[74]);
        CN1K_R512(f, g, hh, a, b, c, d, e, CN1K_W512(w, 11), cn1k_sha512_k[75]);
        CN1K_R512(e, f, g, hh, a, b, c, d, CN1K_W512(w, 12), cn1k_sha512_k[76]);
        CN1K_R512(d, e, f, g, hh, a, b, c, CN1K_W512(w, 13), cn1k_sha512_k[77]);
        CN1K_R512(c, d, e, f, g, hh, a, b, CN1K_W512(w, 14), cn1k_sha512_k[78]);
        CN1K_R512(b, c, d, e, f, g, hh, a, CN1K_W512(w, 15), cn1k_sha512_k[79]);
        h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh;
        p += 128;
    }
}

#ifdef CN1K_ARM64
CN1K_ATTR_SHA2
static void cn1k_sha256_blocks_arm(uint32_t *h, const unsigned char *p, size_t n) {
    uint32x4_t s0 = vld1q_u32(h), s1 = vld1q_u32(h + 4);
    while (n-- > 0) {
        uint32x4_t a0 = s0, a1 = s1, t, w[4];
        int i;
        for (i = 0; i < 4; i++) {
            w[i] = vreinterpretq_u32_u8(vrev32q_u8(vld1q_u8(p + 16 * i)));
        }
        for (i = 0; i < 16; i++) {
            uint32x4_t wk = vaddq_u32(w[i & 3], vld1q_u32(cn1k_sha256_k + 4 * i));
            uint32x4_t prev = s0;
            if (i < 12) {
                w[i & 3] = vsha256su1q_u32(vsha256su0q_u32(w[i & 3], w[(i + 1) & 3]),
                                           w[(i + 2) & 3], w[(i + 3) & 3]);
            }
            s0 = vsha256hq_u32(s0, s1, wk);
            s1 = vsha256h2q_u32(s1, prev, wk);
        }
        s0 = vaddq_u32(s0, a0);
        s1 = vaddq_u32(s1, a1);
        (void) t;
        p += 64;
    }
    vst1q_u32(h, s0);
    vst1q_u32(h + 4, s1);
}

CN1K_ATTR_SHA512
static void cn1k_sha512_blocks_arm(uint64_t *h, const unsigned char *p, size_t n) {
    uint64x2_t ab = vld1q_u64(h), cd = vld1q_u64(h + 2), ef = vld1q_u64(h + 4), gh = vld1q_u64(h + 6);
    while (n-- > 0) {
        uint64x2_t sab = ab, scd = cd, sef = ef, sgh = gh, w[8];
        int i;
        for (i = 0; i < 8; i++) {
            w[i] = vreinterpretq_u64_u8(vrev64q_u8(vld1q_u8(p + 16 * i)));
        }
        /* the four state registers rotate one place every two rounds */
        for (i = 0; i < 40; i++) {
            uint64x2_t kw = vaddq_u64(w[i & 7], vld1q_u64(cn1k_sha512_k + 2 * i));
            uint64x2_t x, y, t;
            switch (i & 3) {
                case 0: x = ab; y = cd; break;
                case 1: x = gh; y = ab; break;
                case 2: x = ef; y = gh; break;
                default: x = cd; y = ef; break;
            }
            (void) x; (void) y;
            if ((i & 3) == 0) {
                kw = vextq_u64(kw, kw, 1);
                t = vaddq_u64(kw, gh);
                t = vsha512hq_u64(t, vextq_u64(ef, gh, 1), vextq_u64(cd, ef, 1));
                gh = vsha512h2q_u64(t, cd, ab);
                cd = vaddq_u64(cd, t);
            } else if ((i & 3) == 1) {
                kw = vextq_u64(kw, kw, 1);
                t = vaddq_u64(kw, ef);
                t = vsha512hq_u64(t, vextq_u64(cd, ef, 1), vextq_u64(ab, cd, 1));
                ef = vsha512h2q_u64(t, ab, gh);
                ab = vaddq_u64(ab, t);
            } else if ((i & 3) == 2) {
                kw = vextq_u64(kw, kw, 1);
                t = vaddq_u64(kw, cd);
                t = vsha512hq_u64(t, vextq_u64(ab, cd, 1), vextq_u64(gh, ab, 1));
                cd = vsha512h2q_u64(t, gh, ef);
                gh = vaddq_u64(gh, t);
            } else {
                kw = vextq_u64(kw, kw, 1);
                t = vaddq_u64(kw, ab);
                t = vsha512hq_u64(t, vextq_u64(gh, ab, 1), vextq_u64(ef, gh, 1));
                ab = vsha512h2q_u64(t, ef, cd);
                ef = vaddq_u64(ef, t);
            }
            if (i < 32) {
                w[i & 7] = vsha512su1q_u64(vsha512su0q_u64(w[i & 7], w[(i + 1) & 7]),
                                           w[(i + 7) & 7], vextq_u64(w[(i + 4) & 7], w[(i + 5) & 7], 1));
            }
        }
        ab = vaddq_u64(ab, sab); cd = vaddq_u64(cd, scd); ef = vaddq_u64(ef, sef); gh = vaddq_u64(gh, sgh);
        p += 128;
    }
    vst1q_u64(h, ab); vst1q_u64(h + 2, cd); vst1q_u64(h + 4, ef); vst1q_u64(h + 6, gh);
}

static int cn1k_arm_has(const char *appleName, unsigned long linuxBit) {
#  if defined(__APPLE__)
    int v = 0;
    size_t sz = sizeof(v);
    (void) linuxBit;
    return sysctlbyname(appleName, &v, &sz, NULL, 0) == 0 && v != 0;
#  elif defined(__linux__)
    (void) appleName;
    return (getauxval(AT_HWCAP) & linuxBit) != 0;
#  else
    (void) appleName; (void) linuxBit;
    return 0;
#  endif
}
#endif

#ifdef CN1K_X86
__attribute__((target("sha,sse4.1")))
static void cn1k_sha256_blocks_x86(uint32_t *h, const unsigned char *p, size_t n) {
    const __m128i mask = _mm_set_epi64x(0x0c0d0e0f08090a0bULL, 0x0405060700010203ULL);
    __m128i tmp = _mm_loadu_si128((const __m128i*) h);
    __m128i s1 = _mm_loadu_si128((const __m128i*) (h + 4));
    __m128i s0;
    tmp = _mm_shuffle_epi32(tmp, 0xB1);          /* CDAB */
    s1 = _mm_shuffle_epi32(s1, 0x1B);            /* EFGH */
    s0 = _mm_alignr_epi8(tmp, s1, 8);            /* ABEF */
    s1 = _mm_blend_epi16(s1, tmp, 0xF0);         /* CDGH */
    while (n-- > 0) {
        __m128i a0 = s0, a1 = s1, m[4], msg;
        int i;
        for (i = 0; i < 4; i++) {
            m[i] = _mm_shuffle_epi8(_mm_loadu_si128((const __m128i*) (p + 16 * i)), mask);
        }
        for (i = 0; i < 16; i++) {
            msg = _mm_add_epi32(m[i & 3], _mm_loadu_si128((const __m128i*) (cn1k_sha256_k + 4 * i)));
            s1 = _mm_sha256rnds2_epu32(s1, s0, msg);
            if (i >= 3 && i < 15) {
                __m128i t = _mm_alignr_epi8(m[i & 3], m[(i + 3) & 3], 4);
                m[(i + 1) & 3] = _mm_sha256msg2_epu32(_mm_add_epi32(m[(i + 1) & 3], t), m[i & 3]);
            }
            msg = _mm_shuffle_epi32(msg, 0x0E);
            s0 = _mm_sha256rnds2_epu32(s0, s1, msg);
            if (i >= 1 && i < 13) {
                m[(i + 3) & 3] = _mm_sha256msg1_epu32(m[(i + 3) & 3], m[i & 3]);
            }
        }
        s0 = _mm_add_epi32(s0, a0);
        s1 = _mm_add_epi32(s1, a1);
        p += 64;
    }
    tmp = _mm_shuffle_epi32(s0, 0x1B);           /* FEBA */
    s1 = _mm_shuffle_epi32(s1, 0xB1);            /* DCHG */
    s0 = _mm_blend_epi16(tmp, s1, 0xF0);         /* DCBA */
    s1 = _mm_alignr_epi8(s1, tmp, 8);            /* ABEF */
    _mm_storeu_si128((__m128i*) h, s0);
    _mm_storeu_si128((__m128i*) (h + 4), s1);
}

static int cn1k_x86_has_sha(void) {
    unsigned int a, b, c, d;
    if (!__get_cpuid_count(7, 0, &a, &b, &c, &d)) {
        return 0;
    }
    if ((b & (1u << 29)) == 0) {
        return 0;
    }
    __get_cpuid(1, &a, &b, &c, &d);
    return (c & (1u << 19)) != 0;   /* SSE4.1 */
}
#endif

#if defined(_MSC_VER) && !defined(__clang__)
#  include <intrin.h>
#  define CN1K_PSTORE(p, v) _InterlockedExchangePointer((void* volatile*) (p), (void*) (v))
#  define CN1K_PLOAD(p, t) ((t) _InterlockedCompareExchangePointer((void* volatile*) (p), NULL, NULL))
#else
#  define CN1K_PSTORE(p, v) __atomic_store_n((p), (v), __ATOMIC_RELEASE)
#  define CN1K_PLOAD(p, t) __atomic_load_n((p), __ATOMIC_ACQUIRE)
#endif

typedef void (*cn1k_blocks256_fn)(uint32_t *h, const unsigned char *p, size_t n);
typedef void (*cn1k_blocks512_fn)(uint64_t *h, const unsigned char *p, size_t n);

/* chosen on first use; a race only stores the same answer twice */
static cn1k_blocks256_fn cn1k_sha256_impl = NULL;
static cn1k_blocks512_fn cn1k_sha512_impl = NULL;

static void cn1k_hash_select(void) {
    cn1k_blocks256_fn f256 = cn1k_sha256_blocks_c;
    cn1k_blocks512_fn f512 = cn1k_sha512_blocks_c;
#ifdef CN1K_ARM64
#  if defined(__ARM_FEATURE_SHA2)
    f256 = cn1k_sha256_blocks_arm;
#  else
    if (cn1k_arm_has("hw.optional.arm.FEAT_SHA256", 1ul << 6)) {
        f256 = cn1k_sha256_blocks_arm;
    }
#  endif
    if (cn1k_arm_has("hw.optional.arm.FEAT_SHA512", 1ul << 21) || cn1k_arm_has("hw.optional.armv8_2_sha512", 0)) {
        f512 = cn1k_sha512_blocks_arm;
    }
#endif
#ifdef CN1K_X86
    if (cn1k_x86_has_sha()) {
        f256 = cn1k_sha256_blocks_x86;
    }
#endif
    CN1K_PSTORE(&cn1k_sha512_impl, f512);
    CN1K_PSTORE(&cn1k_sha256_impl, f256);
}

static cn1k_blocks256_fn cn1k_sha256_blocks(void) {
    cn1k_blocks256_fn f = CN1K_PLOAD(&cn1k_sha256_impl, cn1k_blocks256_fn);
    if (f == NULL) {
        cn1k_hash_select();
        f = CN1K_PLOAD(&cn1k_sha256_impl, cn1k_blocks256_fn);
    }
    return f;
}

static cn1k_blocks512_fn cn1k_sha512_blocks(void) {
    cn1k_blocks512_fn f = CN1K_PLOAD(&cn1k_sha512_impl, cn1k_blocks512_fn);
    if (f == NULL) {
        cn1k_hash_select();
        f = CN1K_PLOAD(&cn1k_sha512_impl, cn1k_blocks512_fn);
    }
    return f;
}

/* A whole message, the state already holding `prefix` bytes of blocks (an HMAC pad). */
static void cn1k_sha256_run(uint32_t *h, uint64_t prefix, const unsigned char *in, size_t len, unsigned char *out) {
    cn1k_blocks256_fn blocks = cn1k_sha256_blocks();
    unsigned char tail[128];
    size_t whole = len / 64, rest = len % 64, padded;
    uint64_t bits = (prefix + (uint64_t) len) * 8;
    int i;
    blocks(h, in, whole);
    memcpy(tail, in + whole * 64, rest);
    tail[rest] = 0x80;
    padded = rest < 56 ? 64 : 128;
    memset(tail + rest + 1, 0, padded - rest - 9);
    cn1k_put_be64(tail + padded - 8, bits);
    blocks(h, tail, padded / 64);
    for (i = 0; i < 8; i++) {
        cn1k_put_be32(out + 4 * i, h[i]);
    }
}

static void cn1k_sha512_run(uint64_t *h, uint64_t prefix, const unsigned char *in, size_t len, unsigned char *out) {
    cn1k_blocks512_fn blocks = cn1k_sha512_blocks();
    unsigned char tail[256];
    size_t whole = len / 128, rest = len % 128, padded;
    uint64_t bits = (prefix + (uint64_t) len) * 8;
    int i;
    blocks(h, in, whole);
    memcpy(tail, in + whole * 128, rest);
    tail[rest] = 0x80;
    padded = rest < 112 ? 128 : 256;
    memset(tail + rest + 1, 0, padded - rest - 17);
    /* the length field is 128 bits; the upper 64 are zero for anything that fits in memory */
    cn1k_put_be64(tail + padded - 16, (prefix + (uint64_t) len) >> 61);
    cn1k_put_be64(tail + padded - 8, bits);
    blocks(h, tail, padded / 128);
    for (i = 0; i < 8; i++) {
        cn1k_put_be64(out + 8 * i, h[i]);
    }
}

static void cn1k_sha256(const unsigned char *in, size_t len, unsigned char *out) {
    uint32_t h[8];
    memcpy(h, cn1k_sha256_iv, sizeof(h));
    cn1k_sha256_run(h, 0, in, len, out);
}

static void cn1k_sha512(const unsigned char *in, size_t len, unsigned char *out) {
    uint64_t h[8];
    memcpy(h, cn1k_sha512_iv, sizeof(h));
    cn1k_sha512_run(h, 0, in, len, out);
}

/* ------------------------------------------------------------------ RIPEMD-160 */

#define CN1K_ROL32(x, n) (((x) << (n)) | ((x) >> (32 - (n))))

static uint32_t cn1k_le32(const unsigned char *p) {
    return (uint32_t) p[0] | ((uint32_t) p[1] << 8) | ((uint32_t) p[2] << 16) | ((uint32_t) p[3] << 24);
}

#define CN1K_F1(x, y, z) ((x) ^ (y) ^ (z))
#define CN1K_F2(x, y, z) (((x) & (y)) | (~(x) & (z)))
#define CN1K_F3(x, y, z) (((x) | ~(y)) ^ (z))
#define CN1K_F4(x, y, z) (((x) & (z)) | ((y) & ~(z)))
#define CN1K_F5(x, y, z) ((x) ^ ((y) | ~(z)))
/* one step on rotating names: a takes the new word, c is rotated by 10 */
#define CN1K_RMD(f, a, b, c, d, e, xw, k, s) \
    do { a = CN1K_ROL32(a + f(b, c, d) + (xw) + (k), s) + e; c = CN1K_ROL32(c, 10); } while (0)

/* both lines unrolled, so every message index and rotation is a constant */
static void cn1k_rmd_block(uint32_t *h, const unsigned char *p) {
    uint32_t x[16], al = h[0], bl = h[1], cl = h[2], dl = h[3], el = h[4];
    uint32_t ar = al, br = bl, cr = cl, dr = dl, er = el, t;
    int j;
    for (j = 0; j < 16; j++) {
        x[j] = cn1k_le32(p + 4 * j);
    }
    CN1K_RMD(CN1K_F1, al, bl, cl, dl, el, x[0], 0x00000000u, 11);
    CN1K_RMD(CN1K_F5, ar, br, cr, dr, er, x[5], 0x50a28be6u, 8);
    CN1K_RMD(CN1K_F1, el, al, bl, cl, dl, x[1], 0x00000000u, 14);
    CN1K_RMD(CN1K_F5, er, ar, br, cr, dr, x[14], 0x50a28be6u, 9);
    CN1K_RMD(CN1K_F1, dl, el, al, bl, cl, x[2], 0x00000000u, 15);
    CN1K_RMD(CN1K_F5, dr, er, ar, br, cr, x[7], 0x50a28be6u, 9);
    CN1K_RMD(CN1K_F1, cl, dl, el, al, bl, x[3], 0x00000000u, 12);
    CN1K_RMD(CN1K_F5, cr, dr, er, ar, br, x[0], 0x50a28be6u, 11);
    CN1K_RMD(CN1K_F1, bl, cl, dl, el, al, x[4], 0x00000000u, 5);
    CN1K_RMD(CN1K_F5, br, cr, dr, er, ar, x[9], 0x50a28be6u, 13);
    CN1K_RMD(CN1K_F1, al, bl, cl, dl, el, x[5], 0x00000000u, 8);
    CN1K_RMD(CN1K_F5, ar, br, cr, dr, er, x[2], 0x50a28be6u, 15);
    CN1K_RMD(CN1K_F1, el, al, bl, cl, dl, x[6], 0x00000000u, 7);
    CN1K_RMD(CN1K_F5, er, ar, br, cr, dr, x[11], 0x50a28be6u, 15);
    CN1K_RMD(CN1K_F1, dl, el, al, bl, cl, x[7], 0x00000000u, 9);
    CN1K_RMD(CN1K_F5, dr, er, ar, br, cr, x[4], 0x50a28be6u, 5);
    CN1K_RMD(CN1K_F1, cl, dl, el, al, bl, x[8], 0x00000000u, 11);
    CN1K_RMD(CN1K_F5, cr, dr, er, ar, br, x[13], 0x50a28be6u, 7);
    CN1K_RMD(CN1K_F1, bl, cl, dl, el, al, x[9], 0x00000000u, 13);
    CN1K_RMD(CN1K_F5, br, cr, dr, er, ar, x[6], 0x50a28be6u, 7);
    CN1K_RMD(CN1K_F1, al, bl, cl, dl, el, x[10], 0x00000000u, 14);
    CN1K_RMD(CN1K_F5, ar, br, cr, dr, er, x[15], 0x50a28be6u, 8);
    CN1K_RMD(CN1K_F1, el, al, bl, cl, dl, x[11], 0x00000000u, 15);
    CN1K_RMD(CN1K_F5, er, ar, br, cr, dr, x[8], 0x50a28be6u, 11);
    CN1K_RMD(CN1K_F1, dl, el, al, bl, cl, x[12], 0x00000000u, 6);
    CN1K_RMD(CN1K_F5, dr, er, ar, br, cr, x[1], 0x50a28be6u, 14);
    CN1K_RMD(CN1K_F1, cl, dl, el, al, bl, x[13], 0x00000000u, 7);
    CN1K_RMD(CN1K_F5, cr, dr, er, ar, br, x[10], 0x50a28be6u, 14);
    CN1K_RMD(CN1K_F1, bl, cl, dl, el, al, x[14], 0x00000000u, 9);
    CN1K_RMD(CN1K_F5, br, cr, dr, er, ar, x[3], 0x50a28be6u, 12);
    CN1K_RMD(CN1K_F1, al, bl, cl, dl, el, x[15], 0x00000000u, 8);
    CN1K_RMD(CN1K_F5, ar, br, cr, dr, er, x[12], 0x50a28be6u, 6);
    CN1K_RMD(CN1K_F2, el, al, bl, cl, dl, x[7], 0x5a827999u, 7);
    CN1K_RMD(CN1K_F4, er, ar, br, cr, dr, x[6], 0x5c4dd124u, 9);
    CN1K_RMD(CN1K_F2, dl, el, al, bl, cl, x[4], 0x5a827999u, 6);
    CN1K_RMD(CN1K_F4, dr, er, ar, br, cr, x[11], 0x5c4dd124u, 13);
    CN1K_RMD(CN1K_F2, cl, dl, el, al, bl, x[13], 0x5a827999u, 8);
    CN1K_RMD(CN1K_F4, cr, dr, er, ar, br, x[3], 0x5c4dd124u, 15);
    CN1K_RMD(CN1K_F2, bl, cl, dl, el, al, x[1], 0x5a827999u, 13);
    CN1K_RMD(CN1K_F4, br, cr, dr, er, ar, x[7], 0x5c4dd124u, 7);
    CN1K_RMD(CN1K_F2, al, bl, cl, dl, el, x[10], 0x5a827999u, 11);
    CN1K_RMD(CN1K_F4, ar, br, cr, dr, er, x[0], 0x5c4dd124u, 12);
    CN1K_RMD(CN1K_F2, el, al, bl, cl, dl, x[6], 0x5a827999u, 9);
    CN1K_RMD(CN1K_F4, er, ar, br, cr, dr, x[13], 0x5c4dd124u, 8);
    CN1K_RMD(CN1K_F2, dl, el, al, bl, cl, x[15], 0x5a827999u, 7);
    CN1K_RMD(CN1K_F4, dr, er, ar, br, cr, x[5], 0x5c4dd124u, 9);
    CN1K_RMD(CN1K_F2, cl, dl, el, al, bl, x[3], 0x5a827999u, 15);
    CN1K_RMD(CN1K_F4, cr, dr, er, ar, br, x[10], 0x5c4dd124u, 11);
    CN1K_RMD(CN1K_F2, bl, cl, dl, el, al, x[12], 0x5a827999u, 7);
    CN1K_RMD(CN1K_F4, br, cr, dr, er, ar, x[14], 0x5c4dd124u, 7);
    CN1K_RMD(CN1K_F2, al, bl, cl, dl, el, x[0], 0x5a827999u, 12);
    CN1K_RMD(CN1K_F4, ar, br, cr, dr, er, x[15], 0x5c4dd124u, 7);
    CN1K_RMD(CN1K_F2, el, al, bl, cl, dl, x[9], 0x5a827999u, 15);
    CN1K_RMD(CN1K_F4, er, ar, br, cr, dr, x[8], 0x5c4dd124u, 12);
    CN1K_RMD(CN1K_F2, dl, el, al, bl, cl, x[5], 0x5a827999u, 9);
    CN1K_RMD(CN1K_F4, dr, er, ar, br, cr, x[12], 0x5c4dd124u, 7);
    CN1K_RMD(CN1K_F2, cl, dl, el, al, bl, x[2], 0x5a827999u, 11);
    CN1K_RMD(CN1K_F4, cr, dr, er, ar, br, x[4], 0x5c4dd124u, 6);
    CN1K_RMD(CN1K_F2, bl, cl, dl, el, al, x[14], 0x5a827999u, 7);
    CN1K_RMD(CN1K_F4, br, cr, dr, er, ar, x[9], 0x5c4dd124u, 15);
    CN1K_RMD(CN1K_F2, al, bl, cl, dl, el, x[11], 0x5a827999u, 13);
    CN1K_RMD(CN1K_F4, ar, br, cr, dr, er, x[1], 0x5c4dd124u, 13);
    CN1K_RMD(CN1K_F2, el, al, bl, cl, dl, x[8], 0x5a827999u, 12);
    CN1K_RMD(CN1K_F4, er, ar, br, cr, dr, x[2], 0x5c4dd124u, 11);
    CN1K_RMD(CN1K_F3, dl, el, al, bl, cl, x[3], 0x6ed9eba1u, 11);
    CN1K_RMD(CN1K_F3, dr, er, ar, br, cr, x[15], 0x6d703ef3u, 9);
    CN1K_RMD(CN1K_F3, cl, dl, el, al, bl, x[10], 0x6ed9eba1u, 13);
    CN1K_RMD(CN1K_F3, cr, dr, er, ar, br, x[5], 0x6d703ef3u, 7);
    CN1K_RMD(CN1K_F3, bl, cl, dl, el, al, x[14], 0x6ed9eba1u, 6);
    CN1K_RMD(CN1K_F3, br, cr, dr, er, ar, x[1], 0x6d703ef3u, 15);
    CN1K_RMD(CN1K_F3, al, bl, cl, dl, el, x[4], 0x6ed9eba1u, 7);
    CN1K_RMD(CN1K_F3, ar, br, cr, dr, er, x[3], 0x6d703ef3u, 11);
    CN1K_RMD(CN1K_F3, el, al, bl, cl, dl, x[9], 0x6ed9eba1u, 14);
    CN1K_RMD(CN1K_F3, er, ar, br, cr, dr, x[7], 0x6d703ef3u, 8);
    CN1K_RMD(CN1K_F3, dl, el, al, bl, cl, x[15], 0x6ed9eba1u, 9);
    CN1K_RMD(CN1K_F3, dr, er, ar, br, cr, x[14], 0x6d703ef3u, 6);
    CN1K_RMD(CN1K_F3, cl, dl, el, al, bl, x[8], 0x6ed9eba1u, 13);
    CN1K_RMD(CN1K_F3, cr, dr, er, ar, br, x[6], 0x6d703ef3u, 6);
    CN1K_RMD(CN1K_F3, bl, cl, dl, el, al, x[1], 0x6ed9eba1u, 15);
    CN1K_RMD(CN1K_F3, br, cr, dr, er, ar, x[9], 0x6d703ef3u, 14);
    CN1K_RMD(CN1K_F3, al, bl, cl, dl, el, x[2], 0x6ed9eba1u, 14);
    CN1K_RMD(CN1K_F3, ar, br, cr, dr, er, x[11], 0x6d703ef3u, 12);
    CN1K_RMD(CN1K_F3, el, al, bl, cl, dl, x[7], 0x6ed9eba1u, 8);
    CN1K_RMD(CN1K_F3, er, ar, br, cr, dr, x[8], 0x6d703ef3u, 13);
    CN1K_RMD(CN1K_F3, dl, el, al, bl, cl, x[0], 0x6ed9eba1u, 13);
    CN1K_RMD(CN1K_F3, dr, er, ar, br, cr, x[12], 0x6d703ef3u, 5);
    CN1K_RMD(CN1K_F3, cl, dl, el, al, bl, x[6], 0x6ed9eba1u, 6);
    CN1K_RMD(CN1K_F3, cr, dr, er, ar, br, x[2], 0x6d703ef3u, 14);
    CN1K_RMD(CN1K_F3, bl, cl, dl, el, al, x[13], 0x6ed9eba1u, 5);
    CN1K_RMD(CN1K_F3, br, cr, dr, er, ar, x[10], 0x6d703ef3u, 13);
    CN1K_RMD(CN1K_F3, al, bl, cl, dl, el, x[11], 0x6ed9eba1u, 12);
    CN1K_RMD(CN1K_F3, ar, br, cr, dr, er, x[0], 0x6d703ef3u, 13);
    CN1K_RMD(CN1K_F3, el, al, bl, cl, dl, x[5], 0x6ed9eba1u, 7);
    CN1K_RMD(CN1K_F3, er, ar, br, cr, dr, x[4], 0x6d703ef3u, 7);
    CN1K_RMD(CN1K_F3, dl, el, al, bl, cl, x[12], 0x6ed9eba1u, 5);
    CN1K_RMD(CN1K_F3, dr, er, ar, br, cr, x[13], 0x6d703ef3u, 5);
    CN1K_RMD(CN1K_F4, cl, dl, el, al, bl, x[1], 0x8f1bbcdcu, 11);
    CN1K_RMD(CN1K_F2, cr, dr, er, ar, br, x[8], 0x7a6d76e9u, 15);
    CN1K_RMD(CN1K_F4, bl, cl, dl, el, al, x[9], 0x8f1bbcdcu, 12);
    CN1K_RMD(CN1K_F2, br, cr, dr, er, ar, x[6], 0x7a6d76e9u, 5);
    CN1K_RMD(CN1K_F4, al, bl, cl, dl, el, x[11], 0x8f1bbcdcu, 14);
    CN1K_RMD(CN1K_F2, ar, br, cr, dr, er, x[4], 0x7a6d76e9u, 8);
    CN1K_RMD(CN1K_F4, el, al, bl, cl, dl, x[10], 0x8f1bbcdcu, 15);
    CN1K_RMD(CN1K_F2, er, ar, br, cr, dr, x[1], 0x7a6d76e9u, 11);
    CN1K_RMD(CN1K_F4, dl, el, al, bl, cl, x[0], 0x8f1bbcdcu, 14);
    CN1K_RMD(CN1K_F2, dr, er, ar, br, cr, x[3], 0x7a6d76e9u, 14);
    CN1K_RMD(CN1K_F4, cl, dl, el, al, bl, x[8], 0x8f1bbcdcu, 15);
    CN1K_RMD(CN1K_F2, cr, dr, er, ar, br, x[11], 0x7a6d76e9u, 14);
    CN1K_RMD(CN1K_F4, bl, cl, dl, el, al, x[12], 0x8f1bbcdcu, 9);
    CN1K_RMD(CN1K_F2, br, cr, dr, er, ar, x[15], 0x7a6d76e9u, 6);
    CN1K_RMD(CN1K_F4, al, bl, cl, dl, el, x[4], 0x8f1bbcdcu, 8);
    CN1K_RMD(CN1K_F2, ar, br, cr, dr, er, x[0], 0x7a6d76e9u, 14);
    CN1K_RMD(CN1K_F4, el, al, bl, cl, dl, x[13], 0x8f1bbcdcu, 9);
    CN1K_RMD(CN1K_F2, er, ar, br, cr, dr, x[5], 0x7a6d76e9u, 6);
    CN1K_RMD(CN1K_F4, dl, el, al, bl, cl, x[3], 0x8f1bbcdcu, 14);
    CN1K_RMD(CN1K_F2, dr, er, ar, br, cr, x[12], 0x7a6d76e9u, 9);
    CN1K_RMD(CN1K_F4, cl, dl, el, al, bl, x[7], 0x8f1bbcdcu, 5);
    CN1K_RMD(CN1K_F2, cr, dr, er, ar, br, x[2], 0x7a6d76e9u, 12);
    CN1K_RMD(CN1K_F4, bl, cl, dl, el, al, x[15], 0x8f1bbcdcu, 6);
    CN1K_RMD(CN1K_F2, br, cr, dr, er, ar, x[13], 0x7a6d76e9u, 9);
    CN1K_RMD(CN1K_F4, al, bl, cl, dl, el, x[14], 0x8f1bbcdcu, 8);
    CN1K_RMD(CN1K_F2, ar, br, cr, dr, er, x[9], 0x7a6d76e9u, 12);
    CN1K_RMD(CN1K_F4, el, al, bl, cl, dl, x[5], 0x8f1bbcdcu, 6);
    CN1K_RMD(CN1K_F2, er, ar, br, cr, dr, x[7], 0x7a6d76e9u, 5);
    CN1K_RMD(CN1K_F4, dl, el, al, bl, cl, x[6], 0x8f1bbcdcu, 5);
    CN1K_RMD(CN1K_F2, dr, er, ar, br, cr, x[10], 0x7a6d76e9u, 15);
    CN1K_RMD(CN1K_F4, cl, dl, el, al, bl, x[2], 0x8f1bbcdcu, 12);
    CN1K_RMD(CN1K_F2, cr, dr, er, ar, br, x[14], 0x7a6d76e9u, 8);
    CN1K_RMD(CN1K_F5, bl, cl, dl, el, al, x[4], 0xa953fd4eu, 9);
    CN1K_RMD(CN1K_F1, br, cr, dr, er, ar, x[12], 0x00000000u, 8);
    CN1K_RMD(CN1K_F5, al, bl, cl, dl, el, x[0], 0xa953fd4eu, 15);
    CN1K_RMD(CN1K_F1, ar, br, cr, dr, er, x[15], 0x00000000u, 5);
    CN1K_RMD(CN1K_F5, el, al, bl, cl, dl, x[5], 0xa953fd4eu, 5);
    CN1K_RMD(CN1K_F1, er, ar, br, cr, dr, x[10], 0x00000000u, 12);
    CN1K_RMD(CN1K_F5, dl, el, al, bl, cl, x[9], 0xa953fd4eu, 11);
    CN1K_RMD(CN1K_F1, dr, er, ar, br, cr, x[4], 0x00000000u, 9);
    CN1K_RMD(CN1K_F5, cl, dl, el, al, bl, x[7], 0xa953fd4eu, 6);
    CN1K_RMD(CN1K_F1, cr, dr, er, ar, br, x[1], 0x00000000u, 12);
    CN1K_RMD(CN1K_F5, bl, cl, dl, el, al, x[12], 0xa953fd4eu, 8);
    CN1K_RMD(CN1K_F1, br, cr, dr, er, ar, x[5], 0x00000000u, 5);
    CN1K_RMD(CN1K_F5, al, bl, cl, dl, el, x[2], 0xa953fd4eu, 13);
    CN1K_RMD(CN1K_F1, ar, br, cr, dr, er, x[8], 0x00000000u, 14);
    CN1K_RMD(CN1K_F5, el, al, bl, cl, dl, x[10], 0xa953fd4eu, 12);
    CN1K_RMD(CN1K_F1, er, ar, br, cr, dr, x[7], 0x00000000u, 6);
    CN1K_RMD(CN1K_F5, dl, el, al, bl, cl, x[14], 0xa953fd4eu, 5);
    CN1K_RMD(CN1K_F1, dr, er, ar, br, cr, x[6], 0x00000000u, 8);
    CN1K_RMD(CN1K_F5, cl, dl, el, al, bl, x[1], 0xa953fd4eu, 12);
    CN1K_RMD(CN1K_F1, cr, dr, er, ar, br, x[2], 0x00000000u, 13);
    CN1K_RMD(CN1K_F5, bl, cl, dl, el, al, x[3], 0xa953fd4eu, 13);
    CN1K_RMD(CN1K_F1, br, cr, dr, er, ar, x[13], 0x00000000u, 6);
    CN1K_RMD(CN1K_F5, al, bl, cl, dl, el, x[8], 0xa953fd4eu, 14);
    CN1K_RMD(CN1K_F1, ar, br, cr, dr, er, x[14], 0x00000000u, 5);
    CN1K_RMD(CN1K_F5, el, al, bl, cl, dl, x[11], 0xa953fd4eu, 11);
    CN1K_RMD(CN1K_F1, er, ar, br, cr, dr, x[0], 0x00000000u, 15);
    CN1K_RMD(CN1K_F5, dl, el, al, bl, cl, x[6], 0xa953fd4eu, 8);
    CN1K_RMD(CN1K_F1, dr, er, ar, br, cr, x[3], 0x00000000u, 13);
    CN1K_RMD(CN1K_F5, cl, dl, el, al, bl, x[15], 0xa953fd4eu, 5);
    CN1K_RMD(CN1K_F1, cr, dr, er, ar, br, x[9], 0x00000000u, 11);
    CN1K_RMD(CN1K_F5, bl, cl, dl, el, al, x[13], 0xa953fd4eu, 6);
    CN1K_RMD(CN1K_F1, br, cr, dr, er, ar, x[11], 0x00000000u, 11);
    t = h[1] + cl + dr;
    h[1] = h[2] + dl + er;
    h[2] = h[3] + el + ar;
    h[3] = h[4] + al + br;
    h[4] = h[0] + bl + cr;
    h[0] = t;
}

static void cn1k_ripemd160(const unsigned char *in, size_t len, unsigned char *out) {
    uint32_t h[5] = { 0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476, 0xc3d2e1f0 };
    unsigned char tail[128];
    size_t rest = len % 64, padded, i;
    uint64_t bits = (uint64_t) len * 8;
    for (i = 0; i + 64 <= len; i += 64) {
        cn1k_rmd_block(h, in + i);
    }
    memcpy(tail, in + (len - rest), rest);
    tail[rest] = 0x80;
    padded = rest < 56 ? 64 : 128;
    memset(tail + rest + 1, 0, padded - rest - 9);
    for (i = 0; i < 8; i++) {
        tail[padded - 8 + i] = (unsigned char) (bits >> (8 * i));
    }
    cn1k_rmd_block(h, tail);
    if (padded == 128) {
        cn1k_rmd_block(h, tail + 64);
    }
    for (i = 0; i < 5; i++) {
        out[4 * i] = (unsigned char) h[i]; out[4 * i + 1] = (unsigned char) (h[i] >> 8);
        out[4 * i + 2] = (unsigned char) (h[i] >> 16); out[4 * i + 3] = (unsigned char) (h[i] >> 24);
    }
}

/* ------------------------------------------------------------------ HMAC and PBKDF2 */

/* The states after the inner and outer pad blocks, so every later MAC with the key starts there. */
static void cn1k_hmac512_pads(const unsigned char *key, size_t keyLen, uint64_t *inner, uint64_t *outer) {
    unsigned char k[128], pad[128];
    int i;
    memset(k, 0, sizeof(k));
    if (keyLen > 128) {
        cn1k_sha512(key, keyLen, k);
    } else {
        memcpy(k, key, keyLen);
    }
    memcpy(inner, cn1k_sha512_iv, 64);
    memcpy(outer, cn1k_sha512_iv, 64);
    for (i = 0; i < 128; i++) pad[i] = (unsigned char) (k[i] ^ 0x36);
    cn1k_sha512_blocks()(inner, pad, 1);
    for (i = 0; i < 128; i++) pad[i] = (unsigned char) (k[i] ^ 0x5c);
    cn1k_sha512_blocks()(outer, pad, 1);
    memset(k, 0, sizeof(k));
    memset(pad, 0, sizeof(pad));
}

static void cn1k_hmac256_pads(const unsigned char *key, size_t keyLen, uint32_t *inner, uint32_t *outer) {
    unsigned char k[64], pad[64];
    int i;
    memset(k, 0, sizeof(k));
    if (keyLen > 64) {
        cn1k_sha256(key, keyLen, k);
    } else {
        memcpy(k, key, keyLen);
    }
    memcpy(inner, cn1k_sha256_iv, 32);
    memcpy(outer, cn1k_sha256_iv, 32);
    for (i = 0; i < 64; i++) pad[i] = (unsigned char) (k[i] ^ 0x36);
    cn1k_sha256_blocks()(inner, pad, 1);
    for (i = 0; i < 64; i++) pad[i] = (unsigned char) (k[i] ^ 0x5c);
    cn1k_sha256_blocks()(outer, pad, 1);
    memset(k, 0, sizeof(k));
    memset(pad, 0, sizeof(pad));
}

static void cn1k_hmac_sha512(const unsigned char *key, size_t keyLen, const unsigned char *in, size_t len,
                             unsigned char *out) {
    uint64_t inner[8], outer[8];
    unsigned char mid[64];
    cn1k_hmac512_pads(key, keyLen, inner, outer);
    cn1k_sha512_run(inner, 128, in, len, mid);
    cn1k_sha512_run(outer, 128, mid, 64, out);
    memset(mid, 0, sizeof(mid));
}

static void cn1k_hmac_sha256(const unsigned char *key, size_t keyLen, const unsigned char *in, size_t len,
                             unsigned char *out) {
    uint32_t inner[8], outer[8];
    unsigned char mid[32];
    cn1k_hmac256_pads(key, keyLen, inner, outer);
    cn1k_sha256_run(inner, 64, in, len, mid);
    cn1k_sha256_run(outer, 64, mid, 32, out);
    memset(mid, 0, sizeof(mid));
}

/*
 * PBKDF2-HMAC-SHA-512. After the first round every MAC input is the 64-byte previous output, so
 * each iteration is exactly two compressions on blocks whose padding never changes: the block is
 * laid out once and only its first 64 bytes are rewritten.
 */
static void cn1k_pbkdf2_sha512(const unsigned char *pw, size_t pwLen, const unsigned char *salt, size_t saltLen,
                               uint32_t iterations, unsigned char *out, size_t outLen) {
    cn1k_blocks512_fn blocks = cn1k_sha512_blocks();
    uint64_t inner[8], outer[8], h[8], acc[8];
    unsigned char block[128], u[64], *msg;
    uint32_t index;
    size_t done = 0;
    int i;
    cn1k_hmac512_pads(pw, pwLen, inner, outer);
    msg = (unsigned char*) malloc(saltLen + 4);
    if (msg == NULL) {
        memset(out, 0, outLen);
        return;
    }
    memcpy(msg, salt, saltLen);
    memset(block, 0, sizeof(block));
    block[64] = 0x80;
    cn1k_put_be64(block + 120, (128 + 64) * 8);
    for (index = 1; done < outLen; index++) {
        uint32_t it;
        size_t take;
        cn1k_put_be32(msg + saltLen, index);
        memcpy(h, inner, sizeof(h));
        cn1k_sha512_run(h, 128, msg, saltLen + 4, u);
        memcpy(h, outer, sizeof(h));
        cn1k_sha512_run(h, 128, u, 64, u);
        for (i = 0; i < 8; i++) {
            acc[i] = h[i];
        }
        for (it = 1; it < iterations; it++) {
            for (i = 0; i < 8; i++) cn1k_put_be64(block + 8 * i, h[i]);
            memcpy(h, inner, sizeof(h));
            blocks(h, block, 1);
            for (i = 0; i < 8; i++) cn1k_put_be64(block + 8 * i, h[i]);
            memcpy(h, outer, sizeof(h));
            blocks(h, block, 1);
            for (i = 0; i < 8; i++) acc[i] ^= h[i];
        }
        for (i = 0; i < 8; i++) cn1k_put_be64(u + 8 * i, acc[i]);
        take = outLen - done < 64 ? outLen - done : 64;
        memcpy(out + done, u, take);
        done += take;
    }
    memset(msg, 0, saltLen + 4);
    free(msg);
    memset(block, 0, sizeof(block));
    memset(u, 0, sizeof(u));
    memset(acc, 0, sizeof(acc));
    memset(h, 0, sizeof(h));
    memset(inner, 0, sizeof(inner));
    memset(outer, 0, sizeof(outer));
}

static void cn1k_pbkdf2_sha256(const unsigned char *pw, size_t pwLen, const unsigned char *salt, size_t saltLen,
                               uint32_t iterations, unsigned char *out, size_t outLen) {
    cn1k_blocks256_fn blocks = cn1k_sha256_blocks();
    uint32_t inner[8], outer[8], h[8], acc[8];
    unsigned char block[64], u[32], *msg;
    uint32_t index;
    size_t done = 0;
    int i;
    cn1k_hmac256_pads(pw, pwLen, inner, outer);
    msg = (unsigned char*) malloc(saltLen + 4);
    if (msg == NULL) {
        memset(out, 0, outLen);
        return;
    }
    memcpy(msg, salt, saltLen);
    memset(block, 0, sizeof(block));
    block[32] = 0x80;
    cn1k_put_be64(block + 56, (64 + 32) * 8);
    for (index = 1; done < outLen; index++) {
        uint32_t it;
        size_t take;
        cn1k_put_be32(msg + saltLen, index);
        memcpy(h, inner, sizeof(h));
        cn1k_sha256_run(h, 64, msg, saltLen + 4, u);
        memcpy(h, outer, sizeof(h));
        cn1k_sha256_run(h, 64, u, 32, u);
        for (i = 0; i < 8; i++) {
            acc[i] = h[i];
        }
        for (it = 1; it < iterations; it++) {
            for (i = 0; i < 8; i++) cn1k_put_be32(block + 4 * i, h[i]);
            memcpy(h, inner, sizeof(h));
            blocks(h, block, 1);
            for (i = 0; i < 8; i++) cn1k_put_be32(block + 4 * i, h[i]);
            memcpy(h, outer, sizeof(h));
            blocks(h, block, 1);
            for (i = 0; i < 8; i++) acc[i] ^= h[i];
        }
        for (i = 0; i < 8; i++) cn1k_put_be32(u + 4 * i, acc[i]);
        take = outLen - done < 32 ? outLen - done : 32;
        memcpy(out + done, u, take);
        done += take;
    }
    memset(msg, 0, saltLen + 4);
    free(msg);
    memset(block, 0, sizeof(block));
    memset(u, 0, sizeof(u));
    memset(acc, 0, sizeof(acc));
    memset(h, 0, sizeof(h));
    memset(inner, 0, sizeof(inner));
    memset(outer, 0, sizeof(outer));
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
        cn1k_sha512_blocks()(ctx->h, ctx->buf, 1);
        ctx->used = 0;
    }
    if (len >= 128) {
        cn1k_sha512_blocks()(ctx->h, in, len / 128);
        in += len & ~(size_t) 127;
        len &= 127;
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
        cn1k_sha512_blocks()(ctx->h, ctx->buf, 1);
        ctx->used = 0;
    }
    memset(ctx->buf + ctx->used, 0, 120 - ctx->used);
    for (i = 0; i < 8; i++) {
        ctx->buf[120 + i] = (unsigned char) (bits >> (56 - 8 * i));
    }
    cn1k_sha512_blocks()(ctx->h, ctx->buf, 1);
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

/*
 * Every loop inside a round has a constant trip count and is unrolled outright: an iOS Release
 * build compiles for size, and -Os otherwise keeps them rolled, six times slower than OpenSSL.
 */
#if defined(__clang__)
#  define CN1K_UNROLL _Pragma("clang loop unroll(full)")
#elif defined(__GNUC__)
#  define CN1K_UNROLL _Pragma("GCC unroll 25")
#else
#  define CN1K_UNROLL
#endif

static void cn1k_keccak_f(uint64_t *s) {
    uint64_t b[25], c[5], d;
    int round, x, y;
    for (round = 0; round < 24; round++) {
        CN1K_UNROLL
        for (x = 0; x < 5; x++) {
            c[x] = s[x] ^ s[x + 5] ^ s[x + 10] ^ s[x + 15] ^ s[x + 20];
        }
        CN1K_UNROLL
        for (x = 0; x < 5; x++) {
            d = c[(x + 4) % 5] ^ CN1K_ROL64(c[(x + 1) % 5], 1);
            CN1K_UNROLL
            for (y = 0; y < 25; y += 5) {
                s[y + x] ^= d;
            }
        }
        /* rho and pi: lane (x, y) moves to (y, 2x + 3y) */
        CN1K_UNROLL
        for (x = 0; x < 5; x++) {
            CN1K_UNROLL
            for (y = 0; y < 5; y++) {
                int from = x + 5 * y;
                b[y + 5 * ((2 * x + 3 * y) % 5)] = CN1K_ROL64(s[from], cn1k_keccak_rot[from]);
            }
        }
        CN1K_UNROLL
        for (y = 0; y < 25; y += 5) {
            CN1K_UNROLL
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

/* The hashes take an offset and length into data; NativeCryptoEngine has bounds-checked both. */
JAVA_VOID com_codename1_security_NativeCryptoKernels_sha256___byte_1ARRAY_int_int_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data, JAVA_INT off, JAVA_INT len, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    cn1k_sha256(CN1K_BYTES(data) + off, (size_t) len, CN1K_BYTES(out));
#endif
}

JAVA_VOID com_codename1_security_NativeCryptoKernels_sha512___byte_1ARRAY_int_int_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data, JAVA_INT off, JAVA_INT len, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    cn1k_sha512(CN1K_BYTES(data) + off, (size_t) len, CN1K_BYTES(out));
#endif
}

JAVA_VOID com_codename1_security_NativeCryptoKernels_ripemd160___byte_1ARRAY_int_int_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data, JAVA_INT off, JAVA_INT len, JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    cn1k_ripemd160(CN1K_BYTES(data) + off, (size_t) len, CN1K_BYTES(out));
#endif
}

/* HMAC with SHA-256 (bits 256) or SHA-512 (bits 512) */
JAVA_VOID com_codename1_security_NativeCryptoKernels_hmac___int_byte_1ARRAY_byte_1ARRAY_int_int_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_INT bits, JAVA_OBJECT key, JAVA_OBJECT data, JAVA_INT off, JAVA_INT len,
        JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    if (bits == 512) {
        cn1k_hmac_sha512(CN1K_BYTES(key), (size_t) CN1K_LEN(key), CN1K_BYTES(data) + off, (size_t) len,
                         CN1K_BYTES(out));
    } else {
        cn1k_hmac_sha256(CN1K_BYTES(key), (size_t) CN1K_LEN(key), CN1K_BYTES(data) + off, (size_t) len,
                         CN1K_BYTES(out));
    }
#endif
}

/* PBKDF2 with HMAC-SHA-256 or -512, filling all of out */
JAVA_VOID com_codename1_security_NativeCryptoKernels_pbkdf2___int_byte_1ARRAY_byte_1ARRAY_int_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_INT bits, JAVA_OBJECT password, JAVA_OBJECT salt, JAVA_INT iterations,
        JAVA_OBJECT out) {
#ifdef CN1_CRYPTO_KERNELS
    if (bits == 512) {
        cn1k_pbkdf2_sha512(CN1K_BYTES(password), (size_t) CN1K_LEN(password), CN1K_BYTES(salt),
                           (size_t) CN1K_LEN(salt), (uint32_t) iterations, CN1K_BYTES(out), (size_t) CN1K_LEN(out));
    } else {
        cn1k_pbkdf2_sha256(CN1K_BYTES(password), (size_t) CN1K_LEN(password), CN1K_BYTES(salt),
                           (size_t) CN1K_LEN(salt), (uint32_t) iterations, CN1K_BYTES(out), (size_t) CN1K_LEN(out));
    }
#endif
}
