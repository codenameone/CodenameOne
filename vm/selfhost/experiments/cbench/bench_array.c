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
/* Every array element access costs a DEPENDENT LOAD that HotSpot does not pay.
 *
 *   #define CN1_ARRAY_DATA(a) ((char*)(a) + ((JAVA_ARRAY)(a))->dataOffset)
 *
 * so the element address needs dataOffset loaded out of the array header first.
 * HotSpot puts the payload at a fixed offset from the object and computes the
 * address with no intermediate load. This is the shape that shows up as "31%
 * more loads" in the disassembly comparison.
 *
 * dataOffset is 24 (the header size) for every ordinarily allocated array; only
 * allocArrayAligned and the SIMD stack path move it, to round the payload up to
 * a requested alignment.
 *
 * Arms: the indirect form; the constant form; and both again with the bounds
 * check still present, because BCE removes the length load but NOT the
 * dataOffset load, so the two interact.
 */
#include "bench.h"

typedef struct JavaArray {
    void* cls;
    int mark;
    int heapPosition;
    int length;
    unsigned char dimensions;
    unsigned char primitiveSize;
    unsigned short dataOffset;
} JavaArray;                    /* 24 bytes, matching JavaArrayPrototype */

#define HDR 24
static char* backing;
static JavaArray* arr;
static int LEN = 4096;

static uint64_t walkIndirect(long iters) {
    uint64_t acc = 0; int n = LEN;
    for(long i = 0; i < iters; i++) {
        int idx = (int)(i & (n - 1));
        acc += ((int*)((char*)arr + arr->dataOffset))[idx];
    }
    return acc;
}
static uint64_t walkConst(long iters) {
    uint64_t acc = 0; int n = LEN;
    for(long i = 0; i < iters; i++) {
        int idx = (int)(i & (n - 1));
        acc += ((int*)((char*)arr + HDR))[idx];
    }
    return acc;
}
static uint64_t walkIndirectChecked(long iters) {
    uint64_t acc = 0;
    for(long i = 0; i < iters; i++) {
        int idx = (int)(i & (LEN - 1));
        if(__builtin_expect((unsigned)idx < (unsigned)arr->length, 1))
            acc += ((int*)((char*)arr + arr->dataOffset))[idx];
    }
    return acc;
}
static uint64_t walkConstChecked(long iters) {
    uint64_t acc = 0;
    for(long i = 0; i < iters; i++) {
        int idx = (int)(i & (LEN - 1));
        if(__builtin_expect((unsigned)idx < (unsigned)arr->length, 1))
            acc += ((int*)((char*)arr + HDR))[idx];
    }
    return acc;
}

int main(void) {
    backing = (char*)malloc(HDR + sizeof(int) * 4096);
    arr = (JavaArray*)backing;
    arr->length = 4096; arr->dataOffset = HDR; arr->primitiveSize = 4; arr->dimensions = 1;
    for(int i = 0; i < 4096; i++) ((int*)(backing + HDR))[i] = i;

    cn1BenchArm a1[] = {
        { "a->dataOffset (today)", walkIndirect, 0 },
        { "constant header offset", walkConst, 0 },
    };
    cn1BenchRun("ARRAY ELEMENT ADDRESS, bounds check already eliminated", a1, 2, 40000000, 9);

    cn1BenchArm a2[] = {
        { "a->dataOffset + bounds check", walkIndirectChecked, 0 },
        { "constant offset + bounds check", walkConstChecked, 0 },
    };
    cn1BenchRun("ARRAY ELEMENT ADDRESS, bounds check present", a2, 2, 40000000, 9);
    if(cn1BenchSink == 12345) printf("unreachable\n");
    return 0;
}
