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
 * Writes the GPU prep vectors: for each verify kernel, signatures (one in seven corrupted, plus
 * entries the prep itself rejects) with the record the C prep makes of each and the answer the C
 * build of the kernel gives that record. The Java prep the JavaScript port uses is held to these
 * records word for word, and the WebGPU kernels to these answers.
 *
 *   scripts/crypto-gpu/vectors.sh [entries] > vectors.txt
 *   scripts/crypto-gpu/vectors.sh tables > tables.txt
 *
 * The second form prints the two fixed-base tables the kernels read, one line each
 * ("g" for secp256k1, "b" for Ed25519) followed by their words in hex, and
 *
 *   scripts/crypto-gpu/vectors.sh hashes > hashes.txt
 *
 * prints, for a few entry lengths, "length messages sha256s keccak256s", each one run of hex.
 *
 * One line per entry: kernel message signature key prep record answer, all hex, where prep is
 * the prep's own verdict (2 means the record is the GPU's to decide) and record is empty when it
 * is not 2.
 */
#include "cn1_globals.h"
#include "cn1_crypto_kernels.c"
#include <stdio.h>

static unsigned long long rs = 0x9E3779B97F4A7C15ULL;
static unsigned char rnd8(void) { rs ^= rs << 13; rs ^= rs >> 7; rs ^= rs << 17; return (unsigned char) rs; }
static void rnd(unsigned char *p, size_t n) { while (n--) *p++ = rnd8(); }

static void hex(const unsigned char *p, size_t n) {
    size_t i;
    putchar(' ');
    if (n == 0) {
        putchar('-');
    }
    for (i = 0; i < n; i++) {
        printf("%02x", p[i]);
    }
}

static void line(int kernel, const unsigned char *msg, size_t msgLen, const unsigned char *sig,
                 const unsigned char *key, size_t keyLen) {
    uint32_t rec[64];
    int prep, i, words = kernel == CN1G_KERNEL_ECDSA ? CN1G_REC_ECDSA : CN1G_REC_SCHNORR;
    memset(rec, 0, sizeof(rec));
    prep = kernel == CN1G_KERNEL_ECDSA ? cn1g_prep_ecdsa(msg, sig, key, keyLen, rec)
            : kernel == CN1G_KERNEL_SCHNORR ? cn1g_prep_schnorr(msg, msgLen, sig, key, rec)
            : cn1g_prep_ed25519(msg, msgLen, key, sig, rec);
    printf("%d", kernel);
    hex(msg, msgLen);
    hex(sig, 64);
    hex(key, keyLen);
    printf(" %d ", prep);
    if (prep == CN1G_PENDING) {
        for (i = 0; i < words; i++) {
            printf("%08x", rec[i]);
        }
        printf(" %u\n", cn1g_check_on_cpu(kernel, rec) != 0);
    } else {
        printf("- -\n");
    }
}

static void table(const char *name, int ed, size_t words) {
    const uint32_t *t = cn1g_table(ed);
    size_t i;
    printf("%s ", name);
    for (i = 0; i < words; i++) {
        printf("%08x", t[i]);
    }
    putchar('\n');
}

int main(int argc, char **argv) {
    int n, i;
    secp256k1_context *ctx;
    if (argc > 1 && strcmp(argv[1], "tables") == 0) {
        table("g", 0, CN1G_TABLE_G_WORDS);
        table("b", 1, CN1G_TABLE_B_WORDS);
        return 0;
    }
    if (argc > 1 && strcmp(argv[1], "hashes") == 0) {
        /* lengths around both kernels' block edges, and one that is not a multiple of four */
        static const int lens[] = {0, 1, 31, 32, 55, 56, 64, 135, 136, 137, 200, 271};
        unsigned char m[16 * 271], d[32];
        size_t k;
        for (k = 0; k < sizeof(lens) / sizeof(lens[0]); k++) {
            int len = lens[k], e, entries = 16;
            rnd(m, sizeof(m));
            printf("%d", len);
            hex(m, (size_t) (len * entries));
            putchar(' ');
            for (e = 0; e < entries; e++) {
                cn1k_sha256(m + e * len, (size_t) len, d);
                for (i = 0; i < 32; i++) {
                    printf("%02x", d[i]);
                }
            }
            putchar(' ');
            for (e = 0; e < entries; e++) {
                cn1k_keccak256_c(m + e * len, (size_t) len, d);
                for (i = 0; i < 32; i++) {
                    printf("%02x", d[i]);
                }
            }
            putchar('\n');
        }
        return 0;
    }
    n = argc > 1 ? atoi(argv[1]) : 48;
    ctx = secp256k1_context_create(SECP256K1_CONTEXT_NONE);
    for (i = 0; i < n; i++) {
        unsigned char sk[32], hash[32], msg[64], sig[64], ssig[64], esig[64], pub[65], x[32], epub[32];
        secp256k1_pubkey pk;
        secp256k1_ecdsa_signature es;
        secp256k1_keypair kp;
        secp256k1_xonly_pubkey xp;
        size_t pl = i % 5 == 4 ? 65 : 33;
        int broken = i % 7 == 3, garbage = i % 11 == 7;
        do {
            rnd(sk, 32);
        } while (!secp256k1_ec_pubkey_create(ctx, &pk, sk));
        rnd(hash, 32);
        rnd(msg, 64);
        secp256k1_ec_pubkey_serialize(ctx, pub, &pl, &pk,
                                      pl == 65 ? SECP256K1_EC_UNCOMPRESSED : SECP256K1_EC_COMPRESSED);
        secp256k1_ecdsa_sign(ctx, &es, hash, sk, NULL, NULL);
        secp256k1_ecdsa_signature_serialize_compact(ctx, sig, &es);
        if (!secp256k1_keypair_create(ctx, &kp, sk) || !secp256k1_keypair_xonly_pub(ctx, &xp, NULL, &kp)) {
            return 1;
        }
        secp256k1_xonly_pubkey_serialize(ctx, x, &xp);
        if (!secp256k1_schnorrsig_sign_custom(ctx, ssig, msg, 32 + (i % 3) * 16, &kp, NULL)) {
            return 1;
        }
        ed25519_publickey_cn1(sk, epub);
        ed25519_sign_cn1(msg, 64, sk, epub, esig);
        if (broken) {
            sig[rnd8() & 63] ^= (unsigned char) (1 << (rnd8() & 7));
            ssig[rnd8() & 63] ^= (unsigned char) (1 << (rnd8() & 7));
            esig[rnd8() & 63] ^= (unsigned char) (1 << (rnd8() & 7));
        }
        if (garbage) {
            /* entries the prep has to reject on its own: keys off the curve, s past the order */
            pub[1] ^= 0x55;
            memset(ssig + 32, 0xff, 32);
            memset(esig + 32, 0xff, 32);
            x[3] ^= 0x55;
        }
        line(CN1G_KERNEL_ECDSA, hash, 32, sig, pub, pl);
        line(CN1G_KERNEL_SCHNORR, msg, 32 + (i % 3) * 16, ssig, x, 32);
        line(CN1G_KERNEL_ED25519, msg, 64, esig, epub, 32);
    }
    return 0;
}
