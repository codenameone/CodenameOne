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

/* Shared by the C and C++ benchmark workloads: the same measurements the
 * Codename One demo prints, in the same format. */
#ifndef CN1_BENCH_COMMON_H
#define CN1_BENCH_COMMON_H
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>

static long long bench_now_ms(void) {
    struct timeval tv;
    gettimeofday(&tv, 0);
    return (long long) tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

static long bench_cpu_ticks(void) {
    char buf[1024];
    FILE* f = fopen("/proc/self/stat", "r");
    size_t n;
    char* p;
    long ut = 0, st = 0;
    int i;
    if (!f) return 0;
    n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    buf[n] = 0;
    p = strrchr(buf, ')');
    if (!p) return 0;
    p += 2;
    for (i = 0; i < 11 && p; i++) { p = strchr(p, ' '); if (p) p++; }
    /* strtol, not sscanf: glibc 2.38 headers redirect the scanf family to
     * __isoc23_* symbols an older target libc lacks, failing the link. */
    if (p) { char* e; ut = strtol(p, &e, 10); st = strtol(e, 0, 10); }
    return ut + st;
}

static long bench_rss_kb(void) {
    long size = 0, rss = 0;
    FILE* f = fopen("/proc/self/statm", "r");
    char line[128];
    if (f) {
        if (fgets(line, sizeof(line), f)) { char* e; size = strtol(line, &e, 10); rss = strtol(e, 0, 10); }
        fclose(f);
    }
    (void) size;
    return rss * (sysconf(_SC_PAGESIZE) / 1024);
}

static int bench_seconds(void) {
    const char* s = getenv("CN1_BENCH_SECONDS");
    return s ? atoi(s) : 10;
}

static void bench_first_frame(void) {
    printf("CN1BENCH first_frame_epoch_ms=%lld\n", bench_now_ms());
    fflush(stdout);
}

static void bench_report(long frames, long long elapsedMs, long cpuTicks) {
    if (elapsedMs < 1) elapsedMs = 1;
    printf("CN1BENCH fps=%lld frames=%ld rss_kb=%ld cpu_pct=%lld\n", frames * 1000LL / elapsedMs,
           frames, bench_rss_kb(), cpuTicks * 1000LL / elapsedMs);
    fflush(stdout);
}
#endif
