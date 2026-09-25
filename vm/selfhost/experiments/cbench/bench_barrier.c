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
/* Item 3: the SATB write barrier around a reference store.
 *
 * As written the barrier is one predicted-not-taken branch:
 *     if (__builtin_expect(gcSatbActive, 0)) { if (v && !TAGGED(v)) enqueue(v); }
 *
 * In ArrayList.add's real assembly it is not. It is two loads of gcSatbActive
 * (insertion barrier then deletion barrier, not CSE'd because the cold
 * cn1SatbEnqueue could write the global) plus a tag test clang HOISTED out of
 * the cold branch and pays unconditionally:
 *     ldr w8,[x23] / cbnz ... / ldr w8,[x23] / cmp / and x8,x20,#7 / ccmp / ccmp / b.eq
 *
 * Arms: today's shape, one shared read of the flag, and the tag test kept cold.
 */
#include "bench.h"
#include <stdatomic.h>

int gcSatbActive = 0;
#define CN1_IS_TAGGED(o) (((uintptr_t)(o) & 7) != 0)

__attribute__((noinline, cold)) static void satbEnqueue(void* o) {
    cn1BenchSink += (uintptr_t)o;
    gcSatbActive = gcSatbActive;   /* opaque: may write the flag */
}

static void* slots[64];

/* A: today -- insertion barrier then deletion barrier, each reading the flag. */
__attribute__((noinline)) static void storeToday(void** field, void* v) {
    if(__builtin_expect(gcSatbActive, 0)) {
        void* nv = v;
        if(nv != 0 && !CN1_IS_TAGGED(nv)) satbEnqueue(nv);
    }
    if(__builtin_expect(gcSatbActive, 0)) {
        void* old = *(void* volatile*)field;
        if(old != 0 && !CN1_IS_TAGGED(old)) satbEnqueue(old);
    }
    *field = v;
}

/* B: one read of the flag covers both halves. */
__attribute__((noinline)) static void storeOneRead(void** field, void* v) {
    if(__builtin_expect(gcSatbActive, 0)) {
        void* nv = v;
        if(nv != 0 && !CN1_IS_TAGGED(nv)) satbEnqueue(nv);
        void* old = *(void* volatile*)field;
        if(old != 0 && !CN1_IS_TAGGED(old)) satbEnqueue(old);
    }
    *field = v;
}

/* C: one read, and the whole test body pushed out of line so clang cannot
 * hoist the tag arithmetic into the fall-through path. */
__attribute__((noinline, cold)) static void satbBoth(void** field, void* v) {
    if(v != 0 && !CN1_IS_TAGGED(v)) satbEnqueue(v);
    void* old = *(void* volatile*)field;
    if(old != 0 && !CN1_IS_TAGGED(old)) satbEnqueue(old);
}
__attribute__((noinline)) static void storeColdTest(void** field, void* v) {
    if(__builtin_expect(gcSatbActive, 0)) satbBoth(field, v);
    *field = v;
}

static uint64_t runToday(long n)    { for(long i=0;i<n;i++) storeToday(&slots[i & 63], (void*)(uintptr_t)((i & ~7L) + 8)); return (uintptr_t)slots[0]; }
static uint64_t runOneRead(long n)  { for(long i=0;i<n;i++) storeOneRead(&slots[i & 63], (void*)(uintptr_t)((i & ~7L) + 8)); return (uintptr_t)slots[0]; }
static uint64_t runColdTest(long n) { for(long i=0;i<n;i++) storeColdTest(&slots[i & 63], (void*)(uintptr_t)((i & ~7L) + 8)); return (uintptr_t)slots[0]; }

int main(void) {
    cn1BenchArm arms[] = {
        { "A today: two flag reads, hot tag test", runToday, 0 },
        { "B one flag read", runOneRead, 0 },
        { "C one flag read, tag test kept cold", runColdTest, 0 },
    };
    cn1BenchRun("SATB REFERENCE STORE BARRIER", arms, 3, 20000000, 9);
    if(cn1BenchSink == 12345) printf("unreachable\n");
    return 0;
}
