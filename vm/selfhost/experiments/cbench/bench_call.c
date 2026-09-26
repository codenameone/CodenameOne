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
/* Item 5: the stack-overflow guard on every frameless method entry, and item 6:
 * the class-initialization guard on every static call.
 *
 * CN1_FRAMELESS_SOE_GUARD as emitted:
 *     if (limit == 0) compute();
 *     addr = __builtin_frame_address(0);
 *     if (addr < limit && addr >= limit - 256KB) throw;
 *
 * The two-sided test is load bearing (a foreign stack must not trip it), so the
 * arms below keep it exactly. What varies is only whether it is there.
 */
#include "bench.h"
#include <stdatomic.h>

#define GUARD_BAND (256 * 1024)

typedef struct TLD {
    long pad[17];
    long nativeStackLimit;
    int callStackOffset;
} TLD;

typedef struct Clazz { _Atomic int initialized; int pad[3]; } Clazz;

static TLD theTld;
static Clazz theClass;
static volatile int throwCount;

__attribute__((noinline)) static void computeLimit(TLD* t) { t->nativeStackLimit = 1; }
__attribute__((noinline)) static void staticInit(void) { throwCount++; }

/* A: a frameless method as emitted today -- guard, then two field reads. */
__attribute__((noinline)) static int workGuarded(TLD* t, int* obj, int i) {
    if(__builtin_expect(t->nativeStackLimit == 0, 0)) { computeLimit(t); }
    long addr = (long)(intptr_t)__builtin_frame_address(0);
    if(__builtin_expect(addr < t->nativeStackLimit
                        && addr >= t->nativeStackLimit - (long)GUARD_BAND, 0)) {
        return 0;
    }
    return obj[i & 15] + i;
}

/* B: item 5 -- a leaf method cannot deepen the stack past its own frame. */
__attribute__((noinline)) static int workBare(TLD* t, int* obj, int i) {
    (void)t;
    return obj[i & 15] + i;
}

/* C: a static method as emitted today -- class-init guard, then the work. */
__attribute__((noinline)) static int workClinit(TLD* t, int* obj, int i) {
    if(__builtin_expect(!atomic_load_explicit(&theClass.initialized, memory_order_acquire), 0)) {
        staticInit();
    }
    if(__builtin_expect(t->nativeStackLimit == 0, 0)) { computeLimit(t); }
    long addr = (long)(intptr_t)__builtin_frame_address(0);
    if(__builtin_expect(addr < t->nativeStackLimit
                        && addr >= t->nativeStackLimit - (long)GUARD_BAND, 0)) {
        return 0;
    }
    return obj[i & 15] + i;
}

/* D: item 6 -- the class is provably initialized at this call site. */
__attribute__((noinline)) static int workNoClinit(TLD* t, int* obj, int i) {
    if(__builtin_expect(t->nativeStackLimit == 0, 0)) { computeLimit(t); }
    long addr = (long)(intptr_t)__builtin_frame_address(0);
    if(__builtin_expect(addr < t->nativeStackLimit
                        && addr >= t->nativeStackLimit - (long)GUARD_BAND, 0)) {
        return 0;
    }
    return obj[i & 15] + i;
}

static int theObj[16];

static uint64_t runGuarded(long n)   { uint64_t a=0; for(long i=0;i<n;i++) a += (uint64_t)workGuarded(&theTld, theObj, (int)i); return a; }
static uint64_t runBare(long n)      { uint64_t a=0; for(long i=0;i<n;i++) a += (uint64_t)workBare(&theTld, theObj, (int)i); return a; }
static uint64_t runClinit(long n)    { uint64_t a=0; for(long i=0;i<n;i++) a += (uint64_t)workClinit(&theTld, theObj, (int)i); return a; }
static uint64_t runNoClinit(long n)  { uint64_t a=0; for(long i=0;i<n;i++) a += (uint64_t)workNoClinit(&theTld, theObj, (int)i); return a; }

int main(void) {
    atomic_store(&theClass.initialized, 1);
    theTld.nativeStackLimit = 0x100;   /* far below any real frame address */
    for(int i=0;i<16;i++) theObj[i] = i;

    cn1BenchArm a1[] = {
        { "A frameless entry, SOE guard", runGuarded, 0 },
        { "B item 5: leaf, no guard", runBare, 0 },
    };
    cn1BenchRun("METHOD ENTRY: stack-overflow guard", a1, 2, 20000000, 9);

    cn1BenchArm a2[] = {
        { "C static entry, clinit + SOE", runClinit, 0 },
        { "D item 6: clinit proven, SOE only", runNoClinit, 0 },
    };
    cn1BenchRun("METHOD ENTRY: class-init guard", a2, 2, 20000000, 9);

    if(cn1BenchSink == 12345) printf("unreachable\n");
    return 0;
}
