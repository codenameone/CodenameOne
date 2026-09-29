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
/* Minimal interleaved A/B harness for the generated-C cost items.
 *
 * Whole-program timing on this host cannot resolve anything under about 5%, so
 * every claim in GENERATED-C-REPORT.md has to be settled here instead: the
 * mechanism in isolation, run enough times that the per-operation cost is well
 * above the clock's resolution, with the arms INTERLEAVED inside one process so
 * thermal drift cannot separate them.
 *
 * Rules the harness enforces:
 *  - arms alternate every rep, never all-A-then-all-B;
 *  - the reported figure is the MINIMUM over reps, which is the least noisy
 *    estimator of a deterministic cost;
 *  - every arm returns a value that is accumulated into a volatile sink, so the
 *    optimizer cannot delete the work it is supposed to be measuring.
 */
#ifndef CN1_CBENCH_H
#define CN1_CBENCH_H
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>

static volatile uint64_t cn1BenchSink;

static double cn1NowNs(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (double)ts.tv_sec * 1e9 + (double)ts.tv_nsec;
}

typedef uint64_t (*cn1BenchFn)(long iters);

typedef struct {
    const char* name;
    cn1BenchFn fn;
    double bestNs;
} cn1BenchArm;

static void cn1BenchRun(const char* title, cn1BenchArm* arms, int narms,
                        long iters, int reps) {
    for(int a = 0; a < narms; a++) {
        arms[a].bestNs = 1e30;
    }
    /* one untimed pass so page faults and i-cache misses are not in the sample */
    for(int a = 0; a < narms; a++) {
        cn1BenchSink += arms[a].fn(iters / 10 + 1);
    }
    for(int r = 0; r < reps; r++) {
        for(int a = 0; a < narms; a++) {
            double t0 = cn1NowNs();
            uint64_t v = arms[a].fn(iters);
            double t1 = cn1NowNs();
            cn1BenchSink += v;
            double per = (t1 - t0) / (double)iters;
            if(per < arms[a].bestNs) arms[a].bestNs = per;
        }
    }
    printf("%s  (%ld iters x %d reps, min ns/op)\n", title, iters, reps);
    double base = arms[0].bestNs;
    for(int a = 0; a < narms; a++) {
        printf("    %-34s %8.3f ns/op", arms[a].name, arms[a].bestNs);
        if(a > 0) {
            printf("   %+6.1f%%  (%.3f ns saved)",
                   100.0 * (arms[a].bestNs - base) / base, base - arms[a].bestNs);
        }
        printf("\n");
    }
    printf("\n");
}
#endif
