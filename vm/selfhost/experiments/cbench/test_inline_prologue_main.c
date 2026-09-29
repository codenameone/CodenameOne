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
#include <stdio.h>
#include <stdint.h>
struct TLD { long long nativeStackLimit; void* slot[8]; };
extern void* getA(struct TLD*, void**, int);
extern void* getB(struct TLD*, void**, int);
extern void* getC(struct TLD*, void**, int);
extern void* getD(struct TLD*, void**, int);
extern void* getE(struct TLD*, void**, int);
extern void* getF(struct TLD*, void**, int);
extern void* getG(struct TLD*, void**, int);
extern void* getH(struct TLD*, void**, int);
__attribute__((always_inline)) inline void* getI(struct TLD* t, void** storage, int i) {
    (void)t; return storage[i];
}

/* A deliberately ENORMOUS caller, in the size class the real generated methods
 * occupy. The two calls under test are identical and tiny; only the size of the
 * function around them differs from the runs above. */
__attribute__((noinline)) uintptr_t hugeCaller(struct TLD* t, void** store, int argc) {
    volatile uintptr_t a = 0;
    uintptr_t acc = 0;
#define BLOAT20(k) \
    a += (uintptr_t)(k*1u); a ^= (uintptr_t)(k*3u); a += (uintptr_t)(k*7u); a ^= (uintptr_t)(k*11u); \
    a += (uintptr_t)(k*13u); a ^= (uintptr_t)(k*17u); a += (uintptr_t)(k*19u); a ^= (uintptr_t)(k*23u); \
    a += (uintptr_t)(k*29u); a ^= (uintptr_t)(k*31u); a += (uintptr_t)(k*37u); a ^= (uintptr_t)(k*41u); \
    a += (uintptr_t)(k*43u); a ^= (uintptr_t)(k*47u); a += (uintptr_t)(k*53u); a ^= (uintptr_t)(k*59u); \
    a += (uintptr_t)(k*61u); a ^= (uintptr_t)(k*67u); a += (uintptr_t)(k*71u); a ^= (uintptr_t)(k*73u);
#define BLOAT100(k) BLOAT20(k) BLOAT20(k+1) BLOAT20(k+2) BLOAT20(k+3) BLOAT20(k+4)
#define BLOAT500(k) BLOAT100(k) BLOAT100(k+10) BLOAT100(k+20) BLOAT100(k+30) BLOAT100(k+40)
    BLOAT500(argc) BLOAT500(argc+100) BLOAT500(argc+200) BLOAT500(argc+300)
    acc ^= (uintptr_t)getH(t, store, argc & 63);
    BLOAT500(argc+400) BLOAT500(argc+500) BLOAT500(argc+600) BLOAT500(argc+700)
    acc ^= (uintptr_t)getI(t, store, (argc+1) & 63);
    BLOAT500(argc+800) BLOAT500(argc+900) BLOAT500(argc+1000) BLOAT500(argc+1100)
    return acc + a;
}


static struct TLD tld;
static void* store[64];
struct TLD* cn1TestTld(void) { return &tld; }
void* cn1TestThrow(struct TLD* t) { (void)t; return 0; }

/* Each loop must depend on the index or clang folds the arm away entirely and
 * the variant reports a vacuous zero. */
int main(int argc, char** argv) {
    (void)argv; uintptr_t acc = 0;
    tld.nativeStackLimit = 1;
    for (int i = 0; i < 64; i++) store[i] = (void*)(uintptr_t)(i + argc);
    for (int i = 0; i < 64; i++) acc ^= (uintptr_t)getA(&tld, store, i & 63);
    for (int i = 0; i < 64; i++) acc ^= (uintptr_t)getB(&tld, store, i & 63);
    for (int i = 0; i < 64; i++) acc ^= (uintptr_t)getC(&tld, store, i & 63);
    for (int i = 0; i < 64; i++) acc ^= (uintptr_t)getD(&tld, store, i & 63);
    for (int i = 0; i < 64; i++) acc ^= (uintptr_t)getE(&tld, store, i & 63);
    for (int i = 0; i < 64; i++) acc ^= (uintptr_t)getF(&tld, store, i & 63);
    for (int i = 0; i < 64; i++) acc ^= (uintptr_t)getG(&tld, store, i & 63);
    acc ^= hugeCaller(&tld, store, argc);
    printf("%llu\n", (unsigned long long)acc);
    return 0;
}
