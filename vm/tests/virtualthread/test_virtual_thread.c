/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

/* Correctness first, cost second. A fast switch that corrupts a register or
 * loses a stack is not a foundation for a scheduler. */
/* This exercises the BACKEND virtual-thread runtime, which is gated off
 * everywhere else, so the test turns it on for itself rather than depending on
 * whatever flags a caller happens to pass. */
#ifndef CN1_VIRTUAL_THREADS
#define CN1_VIRTUAL_THREADS 1
#endif

#include "cn1_virtual_thread.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

static int failures = 0;
static void check(const char* what, int ok) {
    if(!ok) { printf("FAIL %s\n", what); failures++; }
}

/* ---- 1. a virtual thread runs, yields, resumes, and finishes ---- */
static int steps = 0;
static void counter(void* arg) {
    int* out = (int*)arg;
    for(int i = 0; i < 5; i++) { steps++; *out = i; cn1VirtualThreadYield(); }
}

/* ---- 2. callee-saved registers survive a switch ---- */
static long regsSeen[8];
static void regUser(void* arg) {
    (void)arg;
    /* Give the compiler reason to keep values in callee-saved registers across
     * the yield: they are live before and after. */
    volatile long a=0x1111, b=0x2222, c=0x3333, d=0x4444;
    volatile long e=0x5555, f=0x6666, g=0x7777, h=0x8888;
    cn1VirtualThreadYield();
    regsSeen[0]=a; regsSeen[1]=b; regsSeen[2]=c; regsSeen[3]=d;
    regsSeen[4]=e; regsSeen[5]=f; regsSeen[6]=g; regsSeen[7]=h;
}

/* ---- 3. deep recursion on a small stack, values intact across a yield ---- */
static long deepSum = 0;
static long recurse(int depth) {
    volatile long marker = depth;
    if(depth == 0) { cn1VirtualThreadYield(); return 0; }
    long r = recurse(depth - 1);
    return r + marker;      /* marker must survive the yield made below us */
}
static void deep(void* arg) { (void)arg; deepSum = recurse(200); }

/* ---- 4. many virtual threads interleave without treading on each other ---- */
#define MANY 64
static int slot[MANY];
static void many(void* arg) {
    long id = (long)arg;
    for(int i = 0; i < 10; i++) { slot[id] = (int)(id * 1000 + i); cn1VirtualThreadYield(); }
}

static long long nowNs(void){ struct timespec t; clock_gettime(CLOCK_MONOTONIC,&t);
    return (long long)t.tv_sec*1000000000LL+t.tv_nsec; }

/* ---- 6. the collector must be able to SEE a reference a parked virtual thread
 *         holds only in a C local. This is the property the whole GC integration
 *         rests on: if the range handed to the scan does not cover it, the object
 *         is freed while a parked request still means to use it, and the crash
 *         lands nowhere near the cause. ---- */
static volatile void* hiddenRef = 0;
static void holder(void* arg) {
    /* `mine` exists only here, in a C local, on this virtual thread's stack */
    void* volatile mine = arg;
    cn1VirtualThreadYield();
    /* still ours after the park */
    hiddenRef = mine;
}

static int rangeContains(struct cn1VirtualThread* vt, void* needle) {
    void *lo, *hi; char** w;
    cn1VirtualThreadStackBounds(vt, &lo, &hi);
    if(lo == 0 || hi == 0) return 0;
    for(w = (char**)lo ; (void*)w < hi ; w++) {
        if(*w == (char*)needle) return 1;
    }
    return 0;
}

/* ---- 7. the registry must enumerate every live virtual thread ---- */
static int registrySeen = 0;
static void countOne(struct cn1VirtualThread* vt, void* ctx) {
    (void)vt; (void)ctx; registrySeen++;
}

int main(void) {
    /* 1 */
    int seen = -1;
    struct cn1VirtualThread* co = cn1VirtualThreadCreate(counter, &seen, 64*1024);
    check("create", co != 0);
    for(int i = 0; i < 5; i++) {
        cn1VirtualThreadResume(co);
        check("yield value", seen == i);
    }
    cn1VirtualThreadResume(co);
    check("finishes", cn1VirtualThreadFinished(co));
    check("ran every step", steps == 5);
    cn1VirtualThreadResume(co);                 /* resuming a finished one is a no-op */
    check("resume after finish is safe", cn1VirtualThreadFinished(co));
    cn1VirtualThreadFree(co);

    /* 2 */
    memset(regsSeen, 0, sizeof(regsSeen));
    co = cn1VirtualThreadCreate(regUser, 0, 64*1024);
    cn1VirtualThreadResume(co);                 /* runs to the yield */
    { volatile long clobber[8];             /* stomp the registers in between */
      for(int i=0;i<8;i++) clobber[i]=0xDEAD0000L+i;
      (void)clobber; }
    cn1VirtualThreadResume(co);                 /* must still see its own values */
    check("callee-saved registers survive",
          regsSeen[0]==0x1111 && regsSeen[1]==0x2222 && regsSeen[2]==0x3333 &&
          regsSeen[3]==0x4444 && regsSeen[4]==0x5555 && regsSeen[5]==0x6666 &&
          regsSeen[6]==0x7777 && regsSeen[7]==0x8888);
    cn1VirtualThreadFree(co);

    /* 3 */
    co = cn1VirtualThreadCreate(deep, 0, 256*1024);
    cn1VirtualThreadResume(co);
    cn1VirtualThreadResume(co);
    check("deep stack intact across yield", deepSum == 200L*201L/2);
    cn1VirtualThreadFree(co);

    /* 4 */
    struct cn1VirtualThread* cs[MANY];
    for(long i = 0; i < MANY; i++) cs[i] = cn1VirtualThreadCreate(many, (void*)i, 32*1024);
    for(int round = 0; round < 10; round++)
        for(int i = 0; i < MANY; i++) cn1VirtualThreadResume(cs[i]);
    int ok = 1;
    for(long i = 0; i < MANY; i++) if(slot[i] != (int)(i*1000+9)) ok = 0;
    check("64 virtual threads stayed independent", ok);

    /* 5 stack bounds must be inside the virtual thread's own stack */
    { void *lo, *hi; cn1VirtualThreadStackBounds(cs[0], &lo, &hi);
      check("stack bounds sane", lo != 0 && hi != 0 && lo < hi); }
    for(long i = 0; i < MANY; i++) { cn1VirtualThreadResume(cs[i]); cn1VirtualThreadFree(cs[i]); }

    /* cost */
    struct cn1VirtualThread* fast = cn1VirtualThreadCreate(counter, &seen, 64*1024);
    const long N = 500000;
    long long t0 = nowNs();
    for(long i = 0; i < N; i++) cn1VirtualThreadResume(fast);
    long long t1 = nowNs();
    printf("switch cost %.1f ns (round trip, %ld resumes)\n", (double)(t1-t0)/N, N);
    cn1VirtualThreadFree(fast);

    /* 6: a parked virtual thread's C local must be inside the scanned range */
    { int marker;
      struct cn1VirtualThread* h = cn1VirtualThreadCreate(holder, &marker, 64*1024);
      cn1VirtualThreadResume(h);            /* runs to the yield, now parked */
      check("parked stack range covers a C local",
            rangeContains(h, &marker));
      check("parked virtual thread is not running", !cn1VirtualThreadIsRunning(h));
      cn1VirtualThreadResume(h);
      check("resumed and kept its value", hiddenRef == (void*)&marker);
      cn1VirtualThreadFree(h); }

    /* 7: registry membership tracks create and free */
    { struct cn1VirtualThread* a = cn1VirtualThreadCreate(counter, &seen, 32*1024);
      struct cn1VirtualThread* b = cn1VirtualThreadCreate(counter, &seen, 32*1024);
      registrySeen = 0; cn1VirtualThreadForEach(countOne, 0);
      check("registry sees both", registrySeen == 2);
      cn1VirtualThreadFree(a);
      registrySeen = 0; cn1VirtualThreadForEach(countOne, 0);
      check("registry sees one after free", registrySeen == 1);
      cn1VirtualThreadFree(b);
      registrySeen = 0; cn1VirtualThreadForEach(countOne, 0);
      check("registry empty after both freed", registrySeen == 0); }

    /* 8: a free that lands during a GC scan defers the release
     *
     * The collector copies raw virtual-thread pointers into a snapshot and reads
     * through them while other threads are still running and still freeing. So a
     * free between Begin and End must leave the memory readable -- if it unmaps,
     * the read below faults and this test dies rather than reporting, which is
     * exactly the production failure. It is left OUT of the registry immediately
     * either way, so no later snapshot picks it up. */
    { struct cn1VirtualThread* v = cn1VirtualThreadCreate(counter, &seen, 32*1024);
      void* lo; void* hi;
      volatile unsigned char* probe;
      cn1VirtualThreadResume(v);                  /* start it so sp is set */
      cn1VirtualThreadStackBounds(v, &lo, &hi);
      probe = (volatile unsigned char*)lo;
      cn1VirtualThreadGcScanBegin();
      cn1VirtualThreadFree(v);
      registrySeen = 0; cn1VirtualThreadForEach(countOne, 0);
      check("freed during a scan leaves the registry at once", registrySeen == 0);
      check("freed during a scan is still readable", (*probe | 1) != 0);
      cn1VirtualThreadGcScanEnd();
      /* released for real now; nothing may read it again */ }

    printf(failures ? "FAILURES: %d\n" : "ALL VIRTUAL THREAD TESTS PASSED\n", failures);
    return failures ? 1 : 0;
}
