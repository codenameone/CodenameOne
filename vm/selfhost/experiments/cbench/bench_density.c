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
/* Does object SIZE cost the mutator time, independently of the collector?
 *
 * The same Java program allocates the same NUMBER of objects on both VMs; ours
 * are ~1.56x larger (16-byte header + 8-byte references against 12 + 4). If that
 * matters to the mutator it has to be through cache density: the same object
 * graph spans 1.56x the cache lines, so the same traversal takes more misses.
 *
 * This walks a randomly permuted cycle of N nodes -- pointer chasing, so the
 * prefetcher cannot help -- reading two fields per node, at two node sizes with
 * the SAME node count. Sweeping N takes the working set from inside L1 to well
 * past L2, which is where a density effect has to appear if it exists at all.
 *
 * 40 bytes models 12-byte header + one 4-byte ref + 24 bytes of fields.
 * 64 bytes models 16-byte header + one 8-byte ref + 40 bytes of the same fields
 * after 8-byte references widen them, rounded to the 64-byte size class.
 */
#include "bench.h"

typedef struct N40 { struct N40* next; int a, b; char pad[24]; } N40;
typedef struct N64 { struct N64* next; int a, b; char pad[48]; } N64;

static N40* heap40;
static N64* heap64;
static long gN;

static unsigned long rngState = 88172645463325252UL;
static unsigned long xorshift(void) {
    rngState ^= rngState << 13; rngState ^= rngState >> 7; rngState ^= rngState << 17;
    return rngState;
}

/* A single cycle through all N nodes in random order: every step is a dependent
 * load, so the measurement is load-to-use latency, not bandwidth. */
static void buildCycle(long n) {
    long* perm = (long*)malloc(sizeof(long) * n);
    for(long i = 0; i < n; i++) perm[i] = i;
    for(long i = n - 1; i > 0; i--) { long j = (long)(xorshift() % (unsigned long)(i + 1)); long t = perm[i]; perm[i] = perm[j]; perm[j] = t; }
    for(long i = 0; i < n; i++) {
        long cur = perm[i], nxt = perm[(i + 1) % n];
        heap40[cur].next = &heap40[nxt]; heap40[cur].a = (int)cur; heap40[cur].b = (int)i;
        heap64[cur].next = &heap64[nxt]; heap64[cur].a = (int)cur; heap64[cur].b = (int)i;
    }
    free(perm);
}

static uint64_t walk40(long iters) {
    N40* p = &heap40[0]; uint64_t acc = 0;
    for(long i = 0; i < iters; i++) { acc += (uint64_t)(p->a + p->b); p = p->next; }
    return acc;
}
static uint64_t walk64(long iters) {
    N64* p = &heap64[0]; uint64_t acc = 0;
    for(long i = 0; i < iters; i++) { acc += (uint64_t)(p->a + p->b); p = p->next; }
    return acc;
}

int main(void) {
    const long counts[] = { 512, 4096, 32768, 262144, 1048576, 4194304 };
    heap40 = (N40*)malloc(sizeof(N40) * 4194304);
    heap64 = (N64*)malloc(sizeof(N64) * 4194304);
    if(!heap40 || !heap64) { printf("alloc failed\n"); return 1; }
    printf("pointer-chase over the same NUMBER of nodes at two node sizes\n");
    printf("%10s %12s %12s %12s %10s %10s\n",
           "nodes", "40B set", "64B set", "40B ns/node", "64B ns/node", "ratio");
    for(unsigned k = 0; k < sizeof(counts)/sizeof(counts[0]); k++) {
        long n = counts[k];
        gN = n; buildCycle(n);
        cn1BenchArm arms[] = { { "40B", walk40, 0 }, { "64B", walk64, 0 } };
        /* run enough steps that the cycle is traversed many times */
        long iters = n < 100000 ? 20000000 : 8000000;
        for(int a = 0; a < 2; a++) arms[a].bestNs = 1e30;
        for(int a = 0; a < 2; a++) cn1BenchSink += arms[a].fn(iters / 20 + 1);
        for(int r = 0; r < 7; r++) {
            for(int a = 0; a < 2; a++) {
                double t0 = cn1NowNs(); uint64_t v = arms[a].fn(iters); double t1 = cn1NowNs();
                cn1BenchSink += v;
                double per = (t1 - t0) / (double)iters;
                if(per < arms[a].bestNs) arms[a].bestNs = per;
            }
        }
        printf("%10ld %10.1fKB %10.1fKB %12.3f %10.3f %9.2fx\n",
               n, n * 40 / 1024.0, n * 64 / 1024.0,
               arms[0].bestNs, arms[1].bestNs, arms[1].bestNs / arms[0].bestNs);
    }
    if(cn1BenchSink == 12345) printf("unreachable\n");
    return 0;
}
