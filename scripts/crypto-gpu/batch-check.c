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
 * The GPU batches against the CPU batches, result byte for result byte, with the time of each.
 * Every verify batch mixes valid and corrupted signatures; the hashes are packed batches. Each
 * shape runs several times, so the later runs are shared between the GPU and the CPU in the ratio
 * the earlier ones measured, which is how an application meets them.
 *
 *   scripts/crypto-gpu/batch-check.sh [entries] [--require-gpu]
 *
 * With --require-gpu it fails when no GPU became ready, which is what CI asks of lavapipe
 * (CN1_CRYPTO_GPU_ALLOW_CPU_DEVICE=1); without it a machine with no GPU checks the CPU path only.
 */
#include "cn1_globals.h"
#include "cn1_crypto_kernels.c"
#include <stdio.h>
#include <unistd.h>

static unsigned long long rs = 88172645463325252ULL;
static unsigned char rnd8(void) { rs ^= rs << 13; rs ^= rs >> 7; rs ^= rs << 17; return (unsigned char) rs; }
static void rnd(unsigned char *p, size_t n) { while (n--) *p++ = rnd8(); }

static const char *const NAMES[] = { "sha256", "keccak256", "ecdsa", "schnorr", "ed25519" };

/* the fastest round of each kernel, CPU alone and shared */
static double bestCpu[5], bestShared[5];

static int check(int alg, size_t n, unsigned char *msgs, size_t mlen, unsigned char *sigs, unsigned char *keys,
                 size_t klen, int expectValid) {
    int hash = alg >= CN1K_BATCH_SHA256, kernel = cn1g_kernel_of(alg), bad = 0;
    size_t resBytes = hash ? 32 * n : n, work = hash ? n * (mlen / 64 + 1) : n * 400, i, valid = 0;
    unsigned char *want = (unsigned char*) malloc(resBytes), *got = (unsigned char*) malloc(resBytes);
    cn1k_batch b;
    uint64_t t0, t1, t2;
    memset(&b, 0, sizeof(b));
    b.alg = alg; b.msgs = msgs; b.msgStride = mlen; b.sigs = sigs; b.keys = keys; b.keyStride = klen; b.count = n;
    memset(want, CN1K_BATCH_TODO, resBytes);
    memset(got, CN1K_BATCH_TODO, resBytes);
    b.results = want;
    t0 = cn1g_micros();
    cn1k_batch_run(&b, work);
    t1 = cn1g_micros();
    b.results = got;
    cn1g_batch(&b, work);
    t2 = cn1g_micros();
    for (i = 0; i < resBytes; i++) {
        bad += want[i] != got[i];
    }
    if (!hash) {
        for (i = 0; i < n; i++) {
            valid += want[i] == 1;
        }
    }
    if (bestCpu[kernel] == 0 || (t1 - t0) / 1000.0 < bestCpu[kernel]) {
        bestCpu[kernel] = (t1 - t0) / 1000.0;
    }
    if (bestShared[kernel] == 0 || (t2 - t1) / 1000.0 < bestShared[kernel]) {
        bestShared[kernel] = (t2 - t1) / 1000.0;
    }
    printf("%-9s %7zu  cpu %8.2f ms  shared %8.2f ms  rates cpu %9u gpu %9u  %s\n", NAMES[kernel], n,
           (t1 - t0) / 1000.0, (t2 - t1) / 1000.0, cn1g_cpu_rate[kernel], cn1g_gpu_rate[kernel],
           bad ? "MISMATCH" : "ok");
    if (!hash && (int) valid != expectValid) {
        printf("  %zu valid, expected %d: the corpus is wrong, not the kernels\n", valid, expectValid);
        bad++;
    }
    free(want);
    free(got);
    return bad == 0;
}

int main(int argc, char **argv) {
    size_t n = argc > 1 && argv[1][0] != '-' ? (size_t) atol(argv[1]) : 8192, i;
    int requireGpu = 0, fails = 0, w, a, valid = 0;
    secp256k1_context *ctx = secp256k1_context_create(SECP256K1_CONTEXT_NONE);
    unsigned char *hash = malloc(32 * n), *sig = malloc(64 * n), *pub = malloc(33 * n), *x = malloc(32 * n);
    unsigned char *msg = malloc(64 * n), *ssig = malloc(64 * n), *esig = malloc(64 * n), *epub = malloc(32 * n);
    const cn1g_backend *g;
    for (a = 1; a < argc; a++) {
        requireGpu |= strcmp(argv[a], "--require-gpu") == 0;
    }
    /* the slowest kernel to build is the last one ready */
    for (w = 0; w < 6000 && (cn1g_pick(CN1G_KERNEL_SCHNORR) == NULL || cn1g_pick(CN1G_KERNEL_ECDSA) == NULL
                             || cn1g_pick(CN1G_KERNEL_ED25519) == NULL); w++) {
#if !defined(__APPLE__)
        if (CN1VK_LOAD_STATE() == 3) {
            /* no usable device: nothing will become ready */
            break;
        }
#endif
        usleep(100000);
    }
    g = cn1g_pick(CN1G_KERNEL_SCHNORR);
    printf("GPU: %s after %.1f s\n", g == NULL ? "none" : g->name, w / 10.0);
    if (g == NULL && requireGpu) {
        printf("FAIL: no GPU became ready\n");
        return 1;
    }
    for (i = 0; i < n; i++) {
        unsigned char sk[32];
        secp256k1_pubkey pk;
        secp256k1_ecdsa_signature es;
        secp256k1_keypair kp;
        secp256k1_xonly_pubkey xp;
        size_t pl = 33;
        int broken = i % 7 == 3;
        do {
            rnd(sk, 32);
        } while (!secp256k1_ec_pubkey_create(ctx, &pk, sk));
        rnd(hash + 32 * i, 32);
        rnd(msg + 64 * i, 64);
        secp256k1_ec_pubkey_serialize(ctx, pub + 33 * i, &pl, &pk, SECP256K1_EC_COMPRESSED);
        secp256k1_ecdsa_sign(ctx, &es, hash + 32 * i, sk, NULL, NULL);
        secp256k1_ecdsa_signature_serialize_compact(ctx, sig + 64 * i, &es);
        secp256k1_keypair_create(ctx, &kp, sk);
        secp256k1_keypair_xonly_pub(ctx, &xp, NULL, &kp);
        secp256k1_xonly_pubkey_serialize(ctx, x + 32 * i, &xp);
        secp256k1_schnorrsig_sign_custom(ctx, ssig + 64 * i, msg + 64 * i, 64, &kp, NULL);
        ed25519_publickey_cn1(sk, epub + 32 * i);
        ed25519_sign_cn1(msg + 64 * i, 64, sk, epub + 32 * i, esig + 64 * i);
        if (broken) {
            /* one bit anywhere in the signature: r, s, or past the order */
            sig[64 * i + (rnd8() & 63)] ^= (unsigned char) (1 << (rnd8() & 7));
            ssig[64 * i + (rnd8() & 63)] ^= (unsigned char) (1 << (rnd8() & 7));
            esig[64 * i + (rnd8() & 63)] ^= (unsigned char) (1 << (rnd8() & 7));
        } else {
            valid++;
        }
    }
    for (w = 0; w < 5; w++) {
        fails += !check(CN1K_BATCH_ECDSA, n, hash, 32, sig, pub, 33, valid);
    }
    for (w = 0; w < 5; w++) {
        fails += !check(CN1K_BATCH_SCHNORR, n, msg, 64, ssig, x, 32, valid);
    }
    for (w = 0; w < 5; w++) {
        fails += !check(CN1K_BATCH_ED25519, n, msg, 64, esig, epub, 32, valid);
    }
    {
        size_t hn = 1u << 16, lens[2] = { 64, 200 }, l;
        unsigned char *hm = malloc(hn * 200);
        rnd(hm, hn * 200);
        for (l = 0; l < 2; l++) {
            for (w = 0; w < 4; w++) {
                fails += !check(CN1K_BATCH_SHA256, hn, hm, lens[l], NULL, NULL, 0, 0);
                fails += !check(CN1K_BATCH_KECCAK256, hn, hm, lens[l], NULL, NULL, 0, 0);
            }
        }
        free(hm);
    }
    /*
     * One report row per kernel: the GPU's measured throughput against all the CPU's cores, the
     * best shared round against the best CPU-only one, and the smallest batch cn1g_batch would
     * hand the GPU a share of at these rates (the share must reach its minimum).
     */
    for (a = 0; a < 5; a++) {
        double cr = cn1g_cpu_rate[a], gr = cn1g_gpu_rate[a];
        double minShare = a <= CN1G_KERNEL_KECCAK256 ? CN1G_MIN_GPU_HASH_BYTES : CN1G_MIN_GPU_VERIFY;
        if (gr > 0 && cr > 0) {
            printf("gpu-row %-9s gpu/cpu %5.2f  speedup %5.2f  crossover %.0f %s\n", NAMES[a], gr / cr,
                   bestCpu[a] / bestShared[a], minShare * (gr + cr) / gr, a <= CN1G_KERNEL_KECCAK256 ? "bytes" : "entries");
        } else {
            printf("gpu-row %-9s no GPU measurement: no device, or no batch reached its minimum\n", NAMES[a]);
        }
    }
    printf(fails ? "FAIL: %d mismatched batches\n" : "PASS\n", fails);
    return fails != 0;
}
