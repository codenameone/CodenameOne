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
 * Shares a batch between the GPU and the CPU cores; included by cn1_crypto_kernels.c after the CPU
 * batch code. The caller never chooses a device. When a GPU is ready the batch is split in the
 * ratio of the two sides' measured throughput and both halves run at once -- the GPU's on a thread
 * that only waits, the CPU's on every core -- so the batch takes the time of the faster mix rather
 * than of either side. With no GPU, or a GPU that refuses, everything runs on the CPU, and the
 * results are byte for byte the same either way.
 */
#include "cn1_crypto_gpu.h"

const cn1g_backend *cn1g_metal_backend = NULL;

#if !defined(__APPLE__)
#  include "cn1_crypto_gpu_vulkan.h"
#endif
#if defined(_WIN32)
/* windows.h is already in */
#else
#  include <time.h>
#endif

#if defined(_MSC_VER) && !defined(__clang__)
#  define CN1G_LOAD_RATE(p) ((uint32_t) InterlockedCompareExchange((volatile LONG*) (p), 0, 0))
#  define CN1G_STORE_RATE(p, v) InterlockedExchange((volatile LONG*) (p), (LONG) (v))
#else
#  define CN1G_LOAD_RATE(p) __atomic_load_n((p), __ATOMIC_RELAXED)
#  define CN1G_STORE_RATE(p, v) __atomic_store_n((p), (v), __ATOMIC_RELAXED)
#endif

/* microseconds on a monotonic clock */
static uint64_t cn1g_micros(void) {
#if defined(_WIN32)
    LARGE_INTEGER f, c;
    QueryPerformanceFrequency(&f);
    QueryPerformanceCounter(&c);
    return (uint64_t) (c.QuadPart / (f.QuadPart / 1000000 > 0 ? f.QuadPart / 1000000 : 1));
#else
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    return (uint64_t) t.tv_sec * 1000000u + (uint64_t) t.tv_nsec / 1000u;
#endif
}

/*
 * Throughput per kernel, in work units per millisecond (entries for a verify, bytes for a hash),
 * learned from every batch; 0 until measured. A rate races benignly: it is an estimate, and a lost
 * update only delays how fast the split adapts.
 */
static uint32_t cn1g_cpu_rate[5], cn1g_gpu_rate[5];

static void cn1g_learn(uint32_t *rate, uint64_t units, uint64_t micros) {
    uint64_t r = units * 1000u / (micros ? micros : 1), old = CN1G_LOAD_RATE(rate);
    if (r < 1) {
        r = 1;
    }
    if (r > 0x7fffffffu) {
        r = 0x7fffffffu;
    }
    CN1G_STORE_RATE(rate, (uint32_t) (old == 0 ? r : (old + r) / 2));
}

/* a GPU share below this is not worth a dispatch, and a batch the CPU finishes this fast keeps it */
#define CN1G_MIN_GPU_VERIFY 256
#define CN1G_MIN_GPU_HASH_BYTES (1u << 20)
#define CN1G_MIN_CPU_MICROS 2000u

/* 0 not started, 1 warming, 2 warm: the backend is used only once its tables are on the GPU */
static uint32_t cn1g_warm_state = 0;

/*
 * Builds both fixed-base tables and hands each to the GPU with an empty dispatch, off every batch:
 * done inline, the first batch to reach the GPU would wait ~100 ms for it.
 */
static void cn1g_warm(void *arg) {
    const cn1g_backend *g = (const cn1g_backend*) arg;
    uint32_t dummy = 0;
    const uint32_t *tg = cn1g_table(0), *tb = cn1g_table(1);
    if (tg != NULL && tb != NULL) {
        /* a kernel that is not ready yet refuses, and uploads its table on its first real batch */
        g->run(CN1G_KERNEL_ECDSA, &dummy, 4, &dummy, 4, tg, CN1G_TABLE_G_WORDS * sizeof(uint32_t), 0, 0);
        g->run(CN1G_KERNEL_ED25519, &dummy, 4, &dummy, 4, tb, CN1G_TABLE_B_WORDS * sizeof(uint32_t), 0, 0);
    }
    CN1G_STORE_RATE(&cn1g_warm_state, 2u);
}

#if defined(_WIN32)
static DWORD WINAPI cn1g_warm_thread(LPVOID arg) {
    cn1g_warm(arg);
    return 0;
}
#elif !defined(__APPLE__)
static void *cn1g_warm_thread(void *arg) {
    cn1g_warm(arg);
    return NULL;
}
#endif

static const cn1g_backend *cn1g_pick(int kernel) {
    const cn1g_backend *g = cn1g_metal_backend;
#if !defined(__APPLE__)
    if (g == NULL) {
        g = cn1g_vulkan_get();
    }
#endif
    if (g == NULL || !g->ready(kernel)) {
        return NULL;
    }
    if (kernel <= CN1G_KERNEL_KECCAK256 || CN1G_LOAD_RATE(&cn1g_warm_state) == 2u) {
        return g;
    }
    if (CN1G_LOAD_RATE(&cn1g_warm_state) == 0u) {
        CN1G_STORE_RATE(&cn1g_warm_state, 1u);
#if defined(__APPLE__)
        dispatch_async_f(dispatch_get_global_queue(QOS_CLASS_UTILITY, 0), (void*) g, cn1g_warm);
#elif defined(_WIN32)
        {
            HANDLE t = CreateThread(NULL, 0, cn1g_warm_thread, (LPVOID) g, 0, NULL);
            if (t != NULL) {
                CloseHandle(t);
            }
        }
#else
        {
            pthread_t t;
            if (pthread_create(&t, NULL, cn1g_warm_thread, (void*) g) == 0) {
                pthread_detach(t);
            }
        }
#endif
    }
    return NULL;
}

static int cn1g_kernel_of(int alg) {
    switch (alg) {
        case CN1K_BATCH_ECDSA: return CN1G_KERNEL_ECDSA;
        case CN1K_BATCH_SCHNORR: return CN1G_KERNEL_SCHNORR;
        case CN1K_BATCH_ED25519: return CN1G_KERNEL_ED25519;
        case CN1K_BATCH_SHA256: return CN1G_KERNEL_SHA256;
        default: return CN1G_KERNEL_KECCAK256;
    }
}

typedef struct {
    cn1k_batch *b;
    size_t recWords;
    uint32_t *recs;
    unsigned char *pending;
} cn1g_prep_job;

static void cn1g_prep_chunk(void *arg, size_t c) {
    cn1g_prep_job *j = (cn1g_prep_job*) arg;
    cn1k_batch *b = j->b;
    size_t from = c * b->count / b->chunks, to = (c + 1) * b->count / b->chunks, i;
    for (i = from; i < to; i++) {
        const unsigned char *m = b->msgOff ? b->msgs + b->msgOff[i] : b->msgs + i * b->msgStride;
        size_t mlen = b->msgOff ? (size_t) (b->msgOff[i + 1] - b->msgOff[i]) : b->msgStride;
        uint32_t *rec = j->recs + i * j->recWords;
        int r;
        j->pending[i] = 0;
        if (b->results[i] != CN1K_BATCH_TODO) {
            continue;
        }
        switch (b->alg) {
            case CN1K_BATCH_ECDSA:
                r = cn1g_prep_ecdsa(m, b->sigs + 64 * i,
                                    b->keyOff ? b->keys + b->keyOff[i] : b->keys + i * b->keyStride,
                                    b->keyOff ? (size_t) (b->keyOff[i + 1] - b->keyOff[i]) : b->keyStride, rec);
                break;
            case CN1K_BATCH_SCHNORR:
                r = cn1g_prep_schnorr(m, mlen, b->sigs + 64 * i, b->keys + 32 * i, rec);
                break;
            default:
                r = cn1g_prep_ed25519(m, mlen, b->keys + 32 * i, b->sigs + 64 * i, rec);
                break;
        }
        if (r == CN1G_PENDING) {
            j->pending[i] = 1;
        } else {
            b->results[i] = (unsigned char) r;
        }
    }
}

/* the GPU's share of one batch: prepared up front, run on the second of two parallel chunks */
typedef struct {
    const cn1g_backend *g;
    int kernel;
    cn1k_batch gpu;        /* entries 0..split-1 of the batch */
    cn1k_batch cpu;        /* the rest */
    size_t cpuWork;
    cn1g_prep_job job;
    uint32_t *out;
    size_t n;              /* pending records, packed to the front of job.recs */
    int ok;
    uint64_t gpuMicros, cpuMicros;
} cn1g_split;

static void cn1g_split_chunk(void *arg, size_t c) {
    cn1g_split *s = (cn1g_split*) arg;
    uint64_t t0 = cn1g_micros();
    if (c == 0) {
        if (s->cpu.count > 0) {
            cn1k_batch_run(&s->cpu, s->cpuWork);
        }
        s->cpuMicros = cn1g_micros() - t0;
        return;
    }
    if (s->kernel <= CN1G_KERNEL_KECCAK256) {
        s->ok = s->g->run(s->kernel, s->gpu.msgs, s->gpu.count * s->gpu.msgStride, s->gpu.results,
                          32 * s->gpu.count, NULL, 0, (uint32_t) s->gpu.msgStride, (uint32_t) s->gpu.count);
    } else {
        s->ok = s->n == 0 || s->g->run(s->kernel, s->job.recs, s->n * s->job.recWords * sizeof(uint32_t), s->out,
                                       s->n * sizeof(uint32_t), cn1g_table(s->kernel == CN1G_KERNEL_ED25519),
                                       (s->kernel == CN1G_KERNEL_ED25519 ? CN1G_TABLE_B_WORDS : CN1G_TABLE_G_WORDS)
                                       * sizeof(uint32_t), 0, (uint32_t) s->n);
    }
    s->gpuMicros += cn1g_micros() - t0;
}

/* entries from..from+count-1 of b as a batch of their own */
static void cn1g_slice(const cn1k_batch *b, size_t from, size_t count, cn1k_batch *out) {
    int hash = b->alg == CN1K_BATCH_SHA256 || b->alg == CN1K_BATCH_KECCAK256;
    *out = *b;
    out->count = count;
    out->results = b->results + (hash ? 32 : 1) * from;
    if (b->msgOff != NULL) {
        out->msgOff = b->msgOff + from;
    } else {
        out->msgs = b->msgs + from * b->msgStride;
    }
    if (b->sigs != NULL) {
        out->sigs = b->sigs + 64 * from;
    }
    if (b->keyOff != NULL) {
        out->keyOff = b->keyOff + from;
    } else if (b->keys != NULL) {
        out->keys = b->keys + from * (b->alg == CN1K_BATCH_ECDSA ? b->keyStride : 32);
    }
}

/* prepares the GPU share; 0 when there is no memory for it, with nothing left allocated */
static int cn1g_prepare(cn1g_split *s) {
    cn1k_batch *b = &s->gpu;
    size_t i, n = 0;
    s->job.b = b;
    s->job.recWords = s->kernel == CN1G_KERNEL_ECDSA ? CN1G_REC_ECDSA
            : s->kernel == CN1G_KERNEL_SCHNORR ? CN1G_REC_SCHNORR : CN1G_REC_ED25519;
    s->job.recs = (uint32_t*) malloc(b->count * s->job.recWords * sizeof(uint32_t));
    s->job.pending = (unsigned char*) malloc(b->count);
    s->out = (uint32_t*) malloc(b->count * sizeof(uint32_t));
    if (cn1g_table(s->kernel == CN1G_KERNEL_ED25519) == NULL || s->job.recs == NULL || s->job.pending == NULL
            || s->out == NULL) {
        free(s->job.recs);
        free(s->job.pending);
        free(s->out);
        return 0;
    }
    b->chunks = cn1k_cores();
    if (b->chunks > CN1K_MAX_CHUNKS) {
        b->chunks = CN1K_MAX_CHUNKS;
    }
    if (b->chunks > b->count) {
        b->chunks = b->count;
    }
    cn1k_parallel(b->chunks, &s->job, cn1g_prep_chunk);
    for (i = 0; i < b->count; i++) {
        if (s->job.pending[i]) {
            if (n != i) {
                memcpy(s->job.recs + n * s->job.recWords, s->job.recs + i * s->job.recWords,
                       s->job.recWords * sizeof(uint32_t));
            }
            n++;
        }
    }
    s->n = n;
    return 1;
}

/*
 * Runs a batch -- the entry point of both batch natives. `work` is the CPU's cost estimate, as
 * cn1k_batch_run takes it.
 */
static void cn1g_batch(cn1k_batch *b, size_t work) {
    int hash = b->alg == CN1K_BATCH_SHA256 || b->alg == CN1K_BATCH_KECCAK256;
    int kernel = cn1g_kernel_of(b->alg);
    /* a hash goes to the GPU only packed, which it reads as it is */
    uint64_t units = hash ? (uint64_t) b->count * b->msgStride : (uint64_t) b->count;
    uint32_t cr = CN1G_LOAD_RATE(&cn1g_cpu_rate[kernel]), gr = CN1G_LOAD_RATE(&cn1g_gpu_rate[kernel]);
    const cn1g_backend *g = NULL;
    cn1g_split s;
    uint64_t share, t0;
    size_t split, i;
    if ((!hash || b->msgOff == NULL) && b->count <= 0xFFFFFFFFu && units >= (hash ? CN1G_MIN_GPU_HASH_BYTES : CN1G_MIN_GPU_VERIFY)
            && (cr == 0 || units * 1000u / cr >= CN1G_MIN_CPU_MICROS)) {
        g = cn1g_pick(kernel);
    }
    if (g == NULL || cr == 0) {
        /* no GPU yet, or the CPU's rate still unknown: the CPU takes it all and is measured */
        t0 = cn1g_micros();
        cn1k_batch_run(b, work);
        cn1g_learn(&cn1g_cpu_rate[kernel], units, cn1g_micros() - t0);
        return;
    }
    /* the GPU gets the fraction of the batch its throughput is of the two together; half until
     * it has been measured, and never all of it, so the CPU's rate stays current */
    share = gr == 0 ? units / 2 : units * gr / ((uint64_t) gr + cr);
    split = (size_t) (hash ? share / b->msgStride : share);
    if (split > b->count - b->count / 16) {
        split = b->count - b->count / 16;
    }
    if ((hash ? (uint64_t) split * b->msgStride : (uint64_t) split) < (hash ? CN1G_MIN_GPU_HASH_BYTES : CN1G_MIN_GPU_VERIFY)) {
        t0 = cn1g_micros();
        cn1k_batch_run(b, work);
        cn1g_learn(&cn1g_cpu_rate[kernel], units, cn1g_micros() - t0);
        return;
    }
    memset(&s, 0, sizeof(s));
    s.g = g;
    s.kernel = kernel;
    cn1g_slice(b, 0, split, &s.gpu);
    cn1g_slice(b, split, b->count - split, &s.cpu);
    s.cpuWork = (size_t) ((uint64_t) work * (b->count - split) / b->count);
    t0 = cn1g_micros();
    if (!hash && !cn1g_prepare(&s)) {
        cn1k_batch_run(b, work);
        return;
    }
    s.gpuMicros = cn1g_micros() - t0;
    cn1k_parallel(2, &s, cn1g_split_chunk);
    if (!hash && s.ok) {
        size_t n = 0;
        for (i = 0; i < s.gpu.count; i++) {
            if (s.job.pending[i]) {
                s.gpu.results[i] = (unsigned char) (s.out[n++] != 0);
            }
        }
    }
    if (!hash) {
        free(s.job.recs);
        free(s.job.pending);
        free(s.out);
    }
    if (s.ok) {
        cn1g_learn(&cn1g_gpu_rate[kernel], hash ? (uint64_t) split * b->msgStride : split, s.gpuMicros);
        if (s.cpu.count > 0) {
            cn1g_learn(&cn1g_cpu_rate[kernel], units - (hash ? (uint64_t) split * b->msgStride : split), s.cpuMicros);
        }
    } else {
        /* the GPU refused: its share, still holding TODO where undecided, goes to the CPU */
        cn1k_batch_run(&s.gpu, (size_t) ((uint64_t) work * split / b->count));
    }
}
