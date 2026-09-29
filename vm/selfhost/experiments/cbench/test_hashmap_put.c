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
 * WHAT DOES A HashMap.put COST WHEN THE KEY AND VALUE ARE TAGGED IMMEDIATES?
 *
 * hashMapChurn uses Integer keys and values. Every Integer in this VM is a
 * TAGGED IMMEDIATE (CN1_TAG_INT shifts a full 32-bit value), so the benchmark
 * allocates nothing -- yet we measure 1.55-1.74x HotSpot. The profile puts 54.9%
 * in put itself and 43.1% in the probe, with no allocation and no dispatch.
 *
 * Reading java_util_HashMap_put, one put with a tagged key AND tagged value
 * still executes:
 *   - THREE CN1_KEEP_NATIVE_OWNER clobbers (asm volatile ... : "memory"),
 *   - CN1_WRITE_BARRIER on the value (SATB insertion),
 *   - CN1_SATB_DELETE on the slot (a VOLATILE load of the old value, then a tag
 *     test that always says "tagged, nothing to do"),
 *   - and, inside cn1HmFindSlot, cn1IsStringClass(CN1_CLASS_OF(key)) -- a tag
 *     resolve per call purely to ask whether the key is a String.
 *
 * None of it can do anything for an immediate: a tagged value is not a heap
 * object, is never collected, and can never dangle. This splits the cost so the
 * fix targets whichever part actually pays.
 *
 * Arms are separate functions with compile-time flags -- passing the mode as a
 * parameter puts branches in the hot loop and inverts the result.
 */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <time.h>
#include <stdatomic.h>

#define CAP   (1<<18)
#define ITERS 3000000
#define TAG(v)      ((void*)((((uintptr_t)(v))<<3)|1))
#define IS_TAGGED(o) ((((uintptr_t)(o))&7)!=0)

static int*   META;
static void** KEYS;
static void** VALS;
static volatile int gcSatbActive = 0;   /* off-mark, as in a normal run */
static int   modCount = 0;

static double now(void){ struct timespec t; clock_gettime(CLOCK_MONOTONIC,&t); return t.tv_sec+t.tv_nsec/1e9; }
static void ownerFence(void** o){ __asm__ __volatile__("" : : "r"(*o) : "memory"); }
static int  classOfIsString(void* k){ /* tag resolve, then a class compare */
    if (IS_TAGGED(k)) return ((uintptr_t)k & 7) == 7;
    return ((uintptr_t)k & 0x100) != 0;
}

#define PUT(NAME, KEEPALIVE, BARRIERS, STRCHK)                                   \
static void* NAME(void* key, void* value, int marker) {                          \
    void* owner0 = META; void* owner1 = key; void* owner2 = value;               \
    if (KEEPALIVE) { ownerFence(&owner0); ownerFence(&owner1); ownerFence(&owner2); } \
    int mask = CAP-1, i = marker & mask;                                         \
    uint32_t perturb = (uint32_t)marker;                                         \
    int stringKey = STRCHK ? classOfIsString(key) : 0;                           \
    (void)stringKey;                                                             \
    for(;;){                                                                     \
        int m = META[i];                                                         \
        if (m == 0) {                                                            \
            META[i] = marker;                                                    \
            if (BARRIERS && gcSatbActive && !IS_TAGGED(key))   { asm volatile("":::"memory"); } \
            KEYS[i] = key;                                                       \
            if (BARRIERS && gcSatbActive && !IS_TAGGED(value)) { asm volatile("":::"memory"); } \
            VALS[i] = value;                                                     \
            modCount++;                                                          \
            return 0;                                                            \
        }                                                                        \
        if (m == marker && KEYS[i] == key) {                                     \
            void* old = VALS[i];                                                 \
            if (BARRIERS) {                                                      \
                if (gcSatbActive && !IS_TAGGED(value)) { asm volatile("":::"memory"); } \
                void* prev = *(void* volatile*)&VALS[i];   /* CN1_SATB_DELETE */ \
                if (gcSatbActive && prev && !IS_TAGGED(prev)) { asm volatile("":::"memory"); } \
            }                                                                    \
            VALS[i] = value;                                                     \
            return old;                                                          \
        }                                                                        \
        perturb >>= 5;                                                           \
        i = (i*5 + 1 + (int)perturb) & mask;                                     \
    }                                                                            \
}
PUT(p_full,    1,1,1)   /* exactly what java_util_HashMap_put does today */
PUT(p_nokeep,  0,1,1)   /* without the three "memory" clobbers          */
PUT(p_nobar,   0,0,1)   /* ... and without the SATB barriers            */
PUT(p_bare,    0,0,0)   /* ... and without the per-call String test     */

/* THE REMAINING STRUCTURAL DIFFERENCE: we keep meta[], keys[] and vals[] as three
 * separate blocks, so one operation touches three independent cache lines. HotSpot
 * chains Node objects holding hash+key+value+next together, so a hit touches one.
 * This arm is the same open-addressed probe over ONE interleaved array. */
struct entry { int meta; int pad; void* key; void* val; };
static struct entry* TBL;
static void* p_interleaved(void* key, void* value, int marker) {
    int mask = CAP-1, i = marker & mask;
    uint32_t perturb=(uint32_t)marker;
    for(;;){
        struct entry* e = &TBL[i];
        if (e->meta == 0) { e->meta=marker; e->key=key; e->val=value; modCount++; return 0; }
        if (e->meta == marker && e->key == key) { void* o=e->val; e->val=value; return o; }
        perturb >>= 5;
        i = (i*5 + 1 + (int)perturb) & mask;
    }
}

int main(void){
    META=calloc(CAP,sizeof(int)); KEYS=calloc(CAP,sizeof(void*)); VALS=calloc(CAP,sizeof(void*));
    struct { const char* n; void*(*f)(void*,void*,int); } arms[]={
        {"full (as shipped)",p_full},{"- 3 keepalive clobbers",p_nokeep},
        {"- SATB barriers",p_nobar},{"- per-call String test",p_bare},
        {"ONE interleaved block",0}};
    TBL=calloc(CAP,sizeof(struct entry));
    printf("%-26s %10s %11s %9s\n","arm","ns/put","cycles@4GHz","delta");
    double prev=0, full=0;
    for(unsigned m=0;m<5;m++){
        double best=1e9;
        for(int rep=0;rep<7;rep++){
            memset(META,0,(size_t)CAP*sizeof(int));
            memset(TBL,0,(size_t)CAP*sizeof(struct entry));
            int live=0;
            double t0=now(); volatile void* sink=0;
            for(int i=0;i<ITERS;i++){
                int k=i&0x3FFFF;
                /* The real workload CLEARS at 50k live entries. Without that the
                 * table saturates, probes degenerate to O(n), and the loop -- not
                 * the barriers -- dominates: the first cut of this benchmark
                 * measured 159ns/put against the real 9.8ns and reported the GC
                 * machinery as 1% of a number that was 16x too big. */
                if (++live > 50000) {
                    if (arms[m].f) memset(META,0,(size_t)CAP*sizeof(int));
                    else           memset(TBL,0,(size_t)CAP*sizeof(struct entry));
                    live=0; }
                if (arms[m].f) sink=arms[m].f(TAG(k),TAG(k*31),k*2654435761u>>10);
                else            sink=p_interleaved(TAG(k),TAG(k*31),k*2654435761u>>10);
            }
            double dt=now()-t0; (void)sink; if(dt<best) best=dt;
        }
        double ns=best*1e9/ITERS; if(!m) full=ns;
        printf("%-26s %10.2f %11.1f %9s\n",arms[m].n,ns,ns*4.0,
               m?({static char b[16];snprintf(b,16,"%+.2f",ns-prev);b;}):"-");
        prev=ns;
    }
    printf("\nfull=%.2fns bare=%.2fns -> GC machinery costs %.2fns/put (%.0f%%)\n",
           full,prev,full-prev,100*(full-prev)/full);
    return 0;
}
