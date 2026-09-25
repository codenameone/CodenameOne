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
#include "cn1_globals.h"
#include <assert.h>

// The backend's plain C build cannot rely on Foundation or platform MAX macros.
#undef MAX

static int popped;
struct elementStruct* cn1PopMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct* sp) {
    (void)threadStateData;
    popped = count;
    return sp - count;
}

#define CHECK_REPLACEMENT(OP, FIELD, TYPE, VALUE, OFFSET) do { \
    struct elementStruct stack[8] = { 0 }; \
    struct elementStruct* SP = stack + 5; \
    OP(VALUE, OFFSET); \
    assert(popped == ((OFFSET) > 1 ? (OFFSET) - 1 : 0)); \
    assert(SP == stack + 5 - popped); \
    assert(stack[5 - (OFFSET)].type == TYPE); \
    assert(stack[5 - (OFFSET)].data.FIELD == (VALUE)); \
} while (0)

int main(void) {
    struct ThreadLocalData* threadStateData = NULL;
    for (int count = 1; count <= 3; count++) {
        CHECK_REPLACEMENT(POP_MANY_AND_PUSH_OBJ, o, CN1_TYPE_OBJECT, (JAVA_OBJECT)(intptr_t)1234, count);
        CHECK_REPLACEMENT(POP_MANY_AND_PUSH_INT, i, CN1_TYPE_INT, 42, count);
        CHECK_REPLACEMENT(POP_MANY_AND_PUSH_LONG, l, CN1_TYPE_LONG, 1234567890123LL, count);
        CHECK_REPLACEMENT(POP_MANY_AND_PUSH_FLOAT, f, CN1_TYPE_FLOAT, 1.5f, count);
        CHECK_REPLACEMENT(POP_MANY_AND_PUSH_DOUBLE, d, CN1_TYPE_DOUBLE, 2.5, count);
    }
    return 0;
}
