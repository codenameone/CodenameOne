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
 * WHERE SHOULD THE BODY ZERO HAPPEN?
 *
 * objectAllocation measures 3.71x HotSpot (run-benchmark.sh, 5 rounds). The
 * inline bump in cn1BibopFastAlloc is already on by default, so the gap is what
 * the bump DOES per object that a TLAB bump does not:
 *
 *   - memset the body, per object
 *   - two store-releases (the mark, and the bump cursor)
 *   - a class-registry flag test and three guard loads
 *
 * HotSpot does none of those per object: its TLAB is bulk-zeroed at refill and
 * its top pointer is a plain store. This isolates the zeroing half, which is the
 * part with an obvious alternative -- the same bytes, zeroed once per 64KB page
 * instead of once per object.
 *
 * Arms:
 *   perObject  what the VM does today
 *   bulkPage   one memset per page at format time, none per object
 *   noZero     the floor, to bound what bulk can possibly win
 *
 * The consumer touches every object's body so no arm can skip the writes it is
 * supposed to have made, and the accumulator is printed so nothing folds away.
 */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <time.h>

#define PAGE  (64*1024)
#define HDR   32
#define PAGES 256

static double now(void) {
    struct timespec t; clock_gettime(CLOCK_MONOTONIC, &t);
    return t.tv_sec + t.tv_nsec / 1e9;
}

static uint64_t run(char* arena, int slotSize, int mode, int rounds) {
    uint64_t acc = 0;
    int slots = (PAGE - HDR) / slotSize;
    for (int r = 0; r < rounds; r++) {
        for (int pg = 0; pg < PAGES; pg++) {
            char* p = arena + (size_t)pg * PAGE;
            if (mode == 1) {               /* bulkPage: one memset per page */
                memset(p + HDR, 0, (size_t)PAGE - HDR);
            }
            for (int s = 0; s < slots; s++) {
                char* o = p + HDR + (size_t)s * slotSize;
                if (mode == 0) {           /* perObject: what the VM does today */
                    memset(o + 16, 0, (size_t)slotSize - 16);
                }
                *(uint64_t*)o = (uint64_t)(uintptr_t)p;   /* class ptr */
                *(int*)(o + 8) = -3;                       /* heapPosition */
                acc += (uint64_t)(unsigned char)o[slotSize - 1];
            }
        }
    }
    return acc;
}

int main(int argc, char** argv) {
    (void)argv;
    char* arena = (char*)malloc((size_t)PAGE * PAGES);
    if (!arena) return 1;
    memset(arena, argc, (size_t)PAGE * PAGES);   /* dirty it: no lazy-zero advantage */
    const char* names[3] = {"perObject", "bulkPage", "noZero"};
    int sizes[3] = {32, 48, 96};
    printf("%-10s %10s %10s %10s\n", "slot bytes", names[0], names[1], names[2]);
    for (int si = 0; si < 3; si++) {
        double best[3] = {1e9, 1e9, 1e9};
        uint64_t sink = 0;
        for (int rep = 0; rep < 5; rep++) {
            for (int m = 0; m < 3; m++) {        /* interleaved, not arm-at-a-time */
                double t0 = now();
                sink += run(arena, sizes[si], m, 2);
                double dt = now() - t0;
                if (dt < best[m]) best[m] = dt;
            }
        }
        printf("%-10d %9.2fms %9.2fms %9.2fms   (bulk %.2fx of perObject)  sink=%llu\n",
               sizes[si], best[0]*1e3, best[1]*1e3, best[2]*1e3,
               best[1] / best[0], (unsigned long long)(sink & 0xffff));
    }
    return 0;
}
