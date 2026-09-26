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
 * WHAT STOPS CLANG INLINING A ONE-LINE JAVA GETTER?
 *
 * ArrayList.get is three statements of Java and emits three statements of C, yet
 * the shipping binary calls it out of line at 167 sites (ArrayList.add 152,
 * String.charAt 113). The emitted body carries a prologue the Java does not
 * suggest, and this isolates which part of it clang is actually weighing:
 *
 *   A  bare            the payload alone
 *   B  + frame array   DEFINE_METHOD_STACK_FRAMELESS's locals[]/stack[]
 *   C  + SOE guard     __builtin_frame_address(0) against nativeStackLimit
 *   D  + owner fence   CN1_KEEP_NATIVE_OWNER's cleanup + "memory" clobber
 *   E  all three       what the translator emits today
 *
 * Faithful on the two points that decide the answer: the callee lives in ANOTHER
 * translation unit and the link is ThinLTO, which is how the real build sees it
 * (-flto=thin, LLVM_LTO = YES_THIN in the Xcode template). Measuring this within
 * one TU would answer a question nobody is asking.
 *
 * The verdict is the `bl` count per variant in the linked binary, not a timing:
 * either the call survived or it did not.
 */
#include <stdio.h>
#include <stdint.h>

struct elementStruct { union { void* o; int i; long long l; double d; } data; int type; };
struct TLD { long long nativeStackLimit; void* slot[8]; };

extern struct TLD* cn1TestTld(void);
extern void* cn1TestThrow(struct TLD*);

static inline void ownerFence(void** owner) {
    __asm__ __volatile__("" : : "r"(*owner) : "memory");
}

/* A: the payload alone */
void* getA(struct TLD* t, void** storage, int i) {
    (void)t; return storage[i];
}

/* B: + the frameless frame array (locals[] and stack[] both address-taken) */
void* getB(struct TLD* t, void** storage, int i) {
    struct elementStruct frame[5];
    struct elementStruct* locals = &frame[0];
    struct elementStruct* stack = &frame[2];
    (void)t;
    locals[0].data.o = storage; locals[0].type = 1;
    stack[0].data.o = storage[i]; stack[0].type = 1;
    return stack[0].data.o;
}

/* C: + the stack-overflow guard */
void* getC(struct TLD* t, void** storage, int i) {
    long long fa = (long long)(intptr_t)__builtin_frame_address(0);
    if (__builtin_expect(fa < t->nativeStackLimit && fa >= t->nativeStackLimit - 4096, 0)) {
        return cn1TestThrow(t);
    }
    return storage[i];
}

/* D: + the native-owner fence (cleanup attribute, "memory" clobber) */
void* getD(struct TLD* t, void** storage, int i) {
    void* owner __attribute__((cleanup(ownerFence))) = storage;
    (void)t; (void)owner;
    return storage[i];
}

/* H: identical to A, but the CALLER will be enormous. This is the hypothesis:
 * the shipping binary's callers of ArrayList.get run to a median of 2,821
 * instructions and a p90 of 16,339, and clang's inliner backs off as the caller
 * grows regardless of how small the callee is. */
void* getH(struct TLD* t, void** storage, int i) {
    (void)t; return storage[i];
}

/* I: identical again, but always_inline, which overrides the cost heuristics
 * including the caller-size penalty. */
__attribute__((always_inline)) inline void* getI(struct TLD* t, void** storage, int i) {
    (void)t; return storage[i];
}

/* F: a noinline CONTROL. If this reports zero surviving calls the measurement
 * is broken and every other row is meaningless. */
__attribute__((noinline)) void* getF(struct TLD* t, void** storage, int i) {
    (void)t; return storage[i];
}

/* G: the real shape -- full prologue AND a call to a helper in this same TU,
 * which is what ArrayList.get does with checkIndex. Inlining G means ThinLTO
 * must import the helper across modules too. */
__attribute__((noinline)) static void checkIndexModel(struct TLD* t, int i) {
    if (__builtin_expect(i < 0, 0)) { cn1TestThrow(t); }
}
void* getG(struct TLD* t, void** storage, int i) {
    void* owner __attribute__((cleanup(ownerFence))) = storage;
    struct elementStruct frame[5];
    struct elementStruct* locals = &frame[0];
    struct elementStruct* stack = &frame[2];
    long long fa = (long long)(intptr_t)__builtin_frame_address(0);
    (void)owner;
    if (__builtin_expect(fa < t->nativeStackLimit && fa >= t->nativeStackLimit - 4096, 0)) {
        return cn1TestThrow(t);
    }
    checkIndexModel(t, i);
    locals[0].data.o = storage; locals[0].type = 1;
    stack[0].data.o = storage[i]; stack[0].type = 1;
    return stack[0].data.o;
}

/* E: everything, i.e. what java_util_ArrayList_get emits today */
void* getE(struct TLD* t, void** storage, int i) {
    void* owner __attribute__((cleanup(ownerFence))) = storage;
    struct elementStruct frame[5];
    struct elementStruct* locals = &frame[0];
    struct elementStruct* stack = &frame[2];
    long long fa = (long long)(intptr_t)__builtin_frame_address(0);
    (void)owner;
    if (__builtin_expect(fa < t->nativeStackLimit && fa >= t->nativeStackLimit - 4096, 0)) {
        return cn1TestThrow(t);
    }
    locals[0].data.o = storage; locals[0].type = 1;
    stack[0].data.o = storage[i]; stack[0].type = 1;
    return stack[0].data.o;
}
