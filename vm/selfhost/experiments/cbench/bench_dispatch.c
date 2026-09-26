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
/* Virtual dispatch in a hot library loop -- the shape ArrayList.indexOf has.
 *
 * Today's thunk, verbatim from the generated java_lang_Object.c:
 *     struct clazz* c = CN1_CLASS_OF(this);
 *     return (*(fnptr)c->vtable[0])(threadStateData, this, arg);
 * Three dependent loads (class word, vtable pointer, slot) and an indirect call
 * whose target the predictor cannot pin at a polymorphic site.
 *
 * HotSpot's profile for the same workload has no equals() frame at all: it is
 * fused into ArrayList.indexOfRange. Ours shows it as 403 samples plus 162 in
 * the interface thunks.
 *
 * Arms: the thunk; a monomorphic inline cache (compare the class against the
 * last one seen, then a DIRECT call); and a direct call as the floor.
 */
#include "bench.h"

typedef struct Clazz Clazz;
typedef int (*eqFn)(void* self, void* other);
struct Clazz { void** vtable; int id; };

typedef struct Obj { Clazz* cls; int a, b; } Obj;

static int eqA(void* s, void* o) { return ((Obj*)s)->a == ((Obj*)o)->a; }
static int eqB(void* s, void* o) { return ((Obj*)s)->b == ((Obj*)o)->b; }

static void* vtA[4]; static void* vtB[4];
static Clazz clsA, clsB;
static Obj* objs;
static int N = 4096;

/* A: today -- class word, vtable, slot, indirect call. */
__attribute__((noinline)) static int thunkEq(Obj* self, Obj* other) {
    Clazz* c = self->cls;
    return ((eqFn)c->vtable[0])(self, other);
}

/* B: monomorphic inline cache at the call site. */
static Clazz* icCls;
static eqFn   icFn;
__attribute__((noinline)) static int icEq(Obj* self, Obj* other) {
    Clazz* c = self->cls;
    if(__builtin_expect(c == icCls, 1)) {
        return icFn(self, other);
    }
    icFn = (eqFn)c->vtable[0];
    icCls = c;
    return icFn(self, other);
}

/* C: the floor -- a direct call, what HotSpot's inlining approximates. */
__attribute__((noinline)) static int directEq(Obj* self, Obj* other) {
    return eqA(self, other);
}

static uint64_t runThunk(long n)  { uint64_t a=0; for(long i=0;i<n;i++) a += (uint64_t)thunkEq(&objs[i & (N-1)], &objs[(i+1) & (N-1)]); return a; }
static uint64_t runIc(long n)     { uint64_t a=0; for(long i=0;i<n;i++) a += (uint64_t)icEq(&objs[i & (N-1)], &objs[(i+1) & (N-1)]); return a; }
static uint64_t runDirect(long n) { uint64_t a=0; for(long i=0;i<n;i++) a += (uint64_t)directEq(&objs[i & (N-1)], &objs[(i+1) & (N-1)]); return a; }

/* Bimorphic: 1 in 16 objects is the other class, so the cache misses sometimes.
 * A real indexOf over a homogeneous list is monomorphic; this is the check that
 * the cache does not fall apart when it is not. */
static uint64_t runIcMixed(long n) { uint64_t a=0; for(long i=0;i<n;i++) a += (uint64_t)icEq(&objs[i & (N-1)], &objs[(i+1) & (N-1)]); return a; }

int main(void) {
    vtA[0] = (void*)eqA; vtB[0] = (void*)eqB;
    clsA.vtable = vtA; clsA.id = 1; clsB.vtable = vtB; clsB.id = 2;
    objs = (Obj*)malloc(sizeof(Obj) * N);
    for(int i = 0; i < N; i++) { objs[i].cls = &clsA; objs[i].a = i; objs[i].b = i; }

    cn1BenchArm arms[] = {
        { "A today: class+vtable+slot+indirect", runThunk, 0 },
        { "B monomorphic inline cache", runIc, 0 },
        { "C direct call (floor)", runDirect, 0 },
    };
    cn1BenchRun("VIRTUAL DISPATCH, monomorphic site", arms, 3, 40000000, 9);

    for(int i = 0; i < N; i += 16) objs[i].cls = &clsB;
    icCls = 0; icFn = 0;
    cn1BenchArm arms2[] = {
        { "A today, 1-in-16 other class", runThunk, 0 },
        { "B inline cache, 1-in-16 miss", runIcMixed, 0 },
    };
    cn1BenchRun("VIRTUAL DISPATCH, bimorphic site", arms2, 2, 40000000, 9);
    if(cn1BenchSink == 12345) printf("unreachable\n");
    return 0;
}
