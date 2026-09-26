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
/* Items 1a-1d: the object allocation fast path.
 *
 * Faithful to cn1BibopFastAlloc: per-class init flag (acquire), per-class
 * registration flag, per-thread current-page array, freeList/constantPool
 * guards, bump against slotCount, slot address by firstSlotOffset + index *
 * slotSize, body zero, class pointer, heapPosition, release store of the mark.
 *
 * Every arm allocates through a NOINLINE function called once per iteration,
 * because that is the shape in the VM -- one allocation inside one method call
 * -- and it is what stops the compiler hoisting the thread-local access out of
 * the loop. Measuring it in a tight inlined loop would report zero for item 1a
 * and the report would be wrong.
 */
#include "bench.h"
#include <stdatomic.h>

#define NCLASSES 23
#define SLOTS    2000

typedef struct Page {
    int classIndex;
    int slotSize;
    int slotCount;
    int firstSlotOffset;
    void* freeList;
    _Atomic int bumpIndex;
    char pad[24];
    char data[SLOTS * 64];
} Page;

typedef struct Clazz {
    _Atomic int initialized;
    int pad0;
    char pad[144];
    int registered;          /* CN1_CLAZZ_REGISTER flag, 152 bytes in */
} Clazz;

typedef struct Obj {
    struct Clazz* cls;
    int mark;
    int heapPosition;
    int f0, f1;
    long f2;
} Obj;                        /* 32 bytes, an ArrayList */

/* Arm A storage: thread-local array, exactly as bibopCurrent is declared. */
__thread Page* tlsCurrent[NCLASSES];

/* Arm B storage: the same array inside the thread state that is already
 * parameter one of every generated function. */
typedef struct TLD {
    long pad[17];
    Page* current[NCLASSES];
    int nextSlot;
    int slotsLeft;
} TLD;

static Clazz theClass;
static void* constantPoolObjects = (void*)1;
static Page thePage;
static TLD theTld;

static void resetPage(void) {
    thePage.classIndex = 0;
    thePage.slotSize = 32;
    thePage.slotCount = SLOTS;
    thePage.firstSlotOffset = (int)__builtin_offsetof(Page, data);
    thePage.freeList = 0;
    atomic_store_explicit(&thePage.bumpIndex, 0, memory_order_relaxed);
}

/* ---- A: today's shape -------------------------------------------------- */
__attribute__((noinline)) static Obj* allocTls(void) {
    if(__builtin_expect(!atomic_load_explicit(&theClass.initialized, memory_order_acquire), 0)) return 0;
    if(__builtin_expect(!theClass.registered, 0)) return 0;
    Page* p = tlsCurrent[0];
    if(__builtin_expect(p != 0 && p->freeList == 0 && constantPoolObjects != 0, 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < p->slotCount, 1)) {
            Obj* o = (Obj*)((char*)p + p->firstSlotOffset + (long)bi * p->slotSize);
            memset((char*)o + 16, 0, sizeof(Obj) - 16);
            o->cls = &theClass;
            o->heapPosition = -3;
            atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_relaxed);
            return o;
        }
    }
    return 0;
}

/* ---- B: 1a only, current-page array reached through the thread state ---- */
__attribute__((noinline)) static Obj* allocTld(TLD* t) {
    if(__builtin_expect(!atomic_load_explicit(&theClass.initialized, memory_order_acquire), 0)) return 0;
    if(__builtin_expect(!theClass.registered, 0)) return 0;
    Page* p = t->current[0];
    if(__builtin_expect(p != 0 && p->freeList == 0 && constantPoolObjects != 0, 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < p->slotCount, 1)) {
            Obj* o = (Obj*)((char*)p + p->firstSlotOffset + (long)bi * p->slotSize);
            memset((char*)o + 16, 0, sizeof(Obj) - 16);
            o->cls = &theClass;
            o->heapPosition = -3;
            atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_relaxed);
            return o;
        }
    }
    return 0;
}

/* ---- C: 1a + 1b (one per-class flag) + 1c (no startup test) ------------- */
__attribute__((noinline)) static Obj* allocFlags(TLD* t) {
    if(__builtin_expect(!atomic_load_explicit(&theClass.initialized, memory_order_acquire), 0)) return 0;
    Page* p = t->current[0];
    if(__builtin_expect(p != 0 && p->freeList == 0, 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < p->slotCount, 1)) {
            Obj* o = (Obj*)((char*)p + p->firstSlotOffset + (long)bi * p->slotSize);
            memset((char*)o + 16, 0, sizeof(Obj) - 16);
            o->cls = &theClass;
            o->heapPosition = -3;
            atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_relaxed);
            return o;
        }
    }
    return 0;
}

/* ---- C1: 1b only -- fold the two per-class flags, keep the startup test --- */
__attribute__((noinline)) static Obj* allocOneFlag(TLD* t) {
    if(__builtin_expect(!atomic_load_explicit(&theClass.initialized, memory_order_acquire), 0)) return 0;
    Page* p = t->current[0];
    if(__builtin_expect(p != 0 && p->freeList == 0 && constantPoolObjects != 0, 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < p->slotCount, 1)) {
            Obj* o = (Obj*)((char*)p + p->firstSlotOffset + (long)bi * p->slotSize);
            memset((char*)o + 16, 0, sizeof(Obj) - 16);
            o->cls = &theClass; o->heapPosition = -3;
            atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_relaxed);
            return o;
        }
    }
    return 0;
}

/* ---- C2: 1c only -- drop the VM-startup test, keep both class flags ------ */
__attribute__((noinline)) static Obj* allocNoStartup(TLD* t) {
    if(__builtin_expect(!atomic_load_explicit(&theClass.initialized, memory_order_acquire), 0)) return 0;
    if(__builtin_expect(!theClass.registered, 0)) return 0;
    Page* p = t->current[0];
    if(__builtin_expect(p != 0 && p->freeList == 0, 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < p->slotCount, 1)) {
            Obj* o = (Obj*)((char*)p + p->firstSlotOffset + (long)bi * p->slotSize);
            memset((char*)o + 16, 0, sizeof(Obj) - 16);
            o->cls = &theClass; o->heapPosition = -3;
            atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_relaxed);
            return o;
        }
    }
    return 0;
}

/* ---- D: C + 1d (no body zero: a never-used slot on a fresh page is zero) */
__attribute__((noinline)) static Obj* allocNoZero(TLD* t) {
    if(__builtin_expect(!atomic_load_explicit(&theClass.initialized, memory_order_acquire), 0)) return 0;
    Page* p = t->current[0];
    if(__builtin_expect(p != 0 && p->freeList == 0, 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < p->slotCount, 1)) {
            Obj* o = (Obj*)((char*)p + p->firstSlotOffset + (long)bi * p->slotSize);
            o->cls = &theClass;
            o->heapPosition = -3;
            atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_relaxed);
            return o;
        }
    }
    return 0;
}

typedef struct ObjBig { struct Clazz* cls; int mark; int heapPosition; char payload[80]; } ObjBig; /* 96B */

__attribute__((noinline)) static ObjBig* allocBigZero(TLD* t) {
    if(__builtin_expect(!atomic_load_explicit(&theClass.initialized, memory_order_acquire), 0)) return 0;
    Page* p = t->current[0];
    if(__builtin_expect(p != 0 && p->freeList == 0, 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < p->slotCount, 1)) {
            ObjBig* o = (ObjBig*)((char*)p + p->firstSlotOffset + (long)bi * 96);
            memset((char*)o + 16, 0, sizeof(ObjBig) - 16);
            o->cls = &theClass; o->heapPosition = -3;
            atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_relaxed);
            return o;
        }
    }
    return 0;
}
__attribute__((noinline)) static ObjBig* allocBigNoZero(TLD* t) {
    if(__builtin_expect(!atomic_load_explicit(&theClass.initialized, memory_order_acquire), 0)) return 0;
    Page* p = t->current[0];
    if(__builtin_expect(p != 0 && p->freeList == 0, 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < p->slotCount, 1)) {
            ObjBig* o = (ObjBig*)((char*)p + p->firstSlotOffset + (long)bi * 96);
            o->cls = &theClass; o->heapPosition = -3;
            atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_relaxed);
            return o;
        }
    }
    return 0;
}
/* G: 1d done properly -- the slot region is zeroed ONCE when the page is
 * formatted, so every bump slot is already zero and the fast path skips the body
 * write. This moves MORE bytes in total (whole slots, including headers that get
 * overwritten, and slots that are never used) but moves them in one bulk pass
 * instead of 680 scattered ones, so whether it wins is an empirical question. */
#define REAL_PAGE_BYTES (64 * 1024)
#define BIG_SLOTS       ((REAL_PAGE_BYTES - 64) / 96)      /* 680 */
static void resetPageZeroed(void) {
    resetPage();
    memset((char*)&thePage + __builtin_offsetof(Page, data), 0,
           (size_t)BIG_SLOTS * 96);
}
static uint64_t runBigPageZero(long n) {
    uint64_t a=0;
    for(long i=0;i<n;i++){ if(i % BIG_SLOTS == 0) resetPageZeroed(); a += (uintptr_t)allocBigNoZero(&theTld); }
    return a;
}
/* The two store-releases. Arm H is today: mark published release, then the bump
 * cursor published release -- the cursor release being the loop-carried edge for
 * the next allocation. Arm I relaxes the cursor (UNSOUND as it stands: the sweep
 * pairs a single acquire on the cursor with plain per-slot header loads, so the
 * release is what publishes parentCls/heapPosition/mark for every slot below it).
 * It is here only to price the ordering primitive before anyone does the work of
 * re-proving the sweep. Arm J relaxes both, as a floor. */
__attribute__((noinline)) static Obj* allocRelCursor(TLD* t) {
    Page* p = t->current[0];
    int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
    if(__builtin_expect(bi < p->slotCount, 1)) {
        Obj* o = (Obj*)((char*)p + p->firstSlotOffset + (long)bi * 32);
        o->cls = &theClass; o->heapPosition = -3;
        atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
        atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_release);
        return o;
    }
    return 0;
}
__attribute__((noinline)) static Obj* allocRlxCursor(TLD* t) {
    Page* p = t->current[0];
    int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
    if(__builtin_expect(bi < p->slotCount, 1)) {
        Obj* o = (Obj*)((char*)p + p->firstSlotOffset + (long)bi * 32);
        o->cls = &theClass; o->heapPosition = -3;
        atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
        atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_relaxed);
        return o;
    }
    return 0;
}
__attribute__((noinline)) static Obj* allocRlxBoth(TLD* t) {
    Page* p = t->current[0];
    int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
    if(__builtin_expect(bi < p->slotCount, 1)) {
        Obj* o = (Obj*)((char*)p + p->firstSlotOffset + (long)bi * 32);
        o->cls = &theClass; o->heapPosition = -3;
        atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_relaxed);
        atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_relaxed);
        return o;
    }
    return 0;
}
/* K: keep the release EXACTLY as it is -- it still orders the header stores
 * before it, so the sweep's "one acquire on the cursor publishes every slot
 * below it" contract is untouched -- but stop the fast path from READING the
 * page cursor. The next slot comes from a plain thread-local cursor instead, so
 * the stlr is no longer the loop-carried edge: nothing downstream loads it.
 *
 * This is the version that does not need the grace-window invariant re-proved,
 * because it changes what the ALLOCATOR depends on, not what the COLLECTOR sees. */
__attribute__((noinline)) static Obj* allocTldCursor(TLD* t) {
    int bi = t->nextSlot;
    if(__builtin_expect(bi < t->slotsLeft, 1)) {
        Page* p = t->current[0];
        Obj* o = (Obj*)((char*)p + p->firstSlotOffset + (long)bi * 32);
        o->cls = &theClass; o->heapPosition = -3;
        atomic_store_explicit((_Atomic int*)&o->mark, -1, memory_order_release);
        t->nextSlot = bi + 1;
        atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_release);
        return o;
    }
    return 0;
}
static uint64_t runTldCursor(long n){ uint64_t a=0; for(long i=0;i<n;i++){ if((i&(SLOTS-1))==0){ resetPage(); theTld.nextSlot=0; theTld.slotsLeft=SLOTS; } a+=(uintptr_t)allocTldCursor(&theTld);} return a; }

static uint64_t runRelCursor(long n){ uint64_t a=0; for(long i=0;i<n;i++){ if((i&(SLOTS-1))==0) resetPage(); a+=(uintptr_t)allocRelCursor(&theTld);} return a; }
static uint64_t runRlxCursor(long n){ uint64_t a=0; for(long i=0;i<n;i++){ if((i&(SLOTS-1))==0) resetPage(); a+=(uintptr_t)allocRlxCursor(&theTld);} return a; }
static uint64_t runRlxBoth(long n)  { uint64_t a=0; for(long i=0;i<n;i++){ if((i&(SLOTS-1))==0) resetPage(); a+=(uintptr_t)allocRlxBoth(&theTld);} return a; }

static uint64_t runBigZero(long n)   { uint64_t a=0; for(long i=0;i<n;i++){ if(i % BIG_SLOTS == 0) resetPage(); a += (uintptr_t)allocBigZero(&theTld); } return a; }
static uint64_t runBigNoZero(long n) { uint64_t a=0; for(long i=0;i<n;i++){ if(i % BIG_SLOTS == 0) resetPage(); a += (uintptr_t)allocBigNoZero(&theTld); } return a; }

static uint64_t runOneFlag(long n)   { uint64_t a=0; for(long i=0;i<n;i++){ if((i&(SLOTS-1))==0) resetPage(); a += (uintptr_t)allocOneFlag(&theTld); } return a; }
static uint64_t runNoStartup(long n) { uint64_t a=0; for(long i=0;i<n;i++){ if((i&(SLOTS-1))==0) resetPage(); a += (uintptr_t)allocNoStartup(&theTld); } return a; }
static uint64_t runTls(long n)     { uint64_t a=0; for(long i=0;i<n;i++){ if((i&(SLOTS-1))==0) resetPage(); a += (uintptr_t)allocTls(); } return a; }
static uint64_t runTld(long n)     { uint64_t a=0; for(long i=0;i<n;i++){ if((i&(SLOTS-1))==0) resetPage(); a += (uintptr_t)allocTld(&theTld); } return a; }
static uint64_t runFlags(long n)   { uint64_t a=0; for(long i=0;i<n;i++){ if((i&(SLOTS-1))==0) resetPage(); a += (uintptr_t)allocFlags(&theTld); } return a; }
static uint64_t runNoZero(long n)  { uint64_t a=0; for(long i=0;i<n;i++){ if((i&(SLOTS-1))==0) resetPage(); a += (uintptr_t)allocNoZero(&theTld); } return a; }

int main(void) {
    atomic_store(&theClass.initialized, 1);
    theClass.registered = 1;
    resetPage();
    tlsCurrent[0] = &thePage;
    theTld.current[0] = &thePage;

    cn1BenchArm arms[] = {
        { "A today (__thread bibopCurrent)", runTls, 0 },
        { "B 1a: current[] in ThreadLocalData", runTld, 0 },
        { "C1 B + 1b only (fold class flags)", runOneFlag, 0 },
        { "C2 B + 1c only (no startup test)", runNoStartup, 0 },
        { "C B + 1b + 1c", runFlags, 0 },
        { "D C + 1d no body zero", runNoZero, 0 },
    };
    cn1BenchRun("ALLOCATION FAST PATH (32-byte object)", arms, 6, 4000000, 9);

    cn1BenchArm big[] = {
        { "E 96B object, body zero (corpus avg)", runBigZero, 0 },
        { "F 96B: no zero at all (upper bound)", runBigNoZero, 0 },
        { "G 96B: 1d, page zeroed at format", runBigPageZero, 0 },
    };
    cn1BenchRun("ALLOCATION FAST PATH (96-byte object)", big, 3, 4000000, 9);
    cn1BenchArm ord[] = {
        { "H today: mark release + cursor release", runRelCursor, 0 },
        { "I cursor relaxed (unsound as-is)", runRlxCursor, 0 },
        { "J both relaxed (floor)", runRlxBoth, 0 },
        { "K release kept, cursor in TLD", runTldCursor, 0 },
    };
    cn1BenchRun("STORE-RELEASE COST IN THE BUMP PATH", ord, 4, 8000000, 9);

    if(cn1BenchSink == 12345) printf("unreachable\n");
    return 0;
}
