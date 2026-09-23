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

/* Appended only to the integration test's generated cn1_globals.c. This uses
 * actual runtime/generated descriptors, layouts, getClass, and removal code.
 * Run before VM startup so no collector can race the controlled heap table.
 * Explicit checks remain active in Release builds (unlike assert()). */
#include "GetClassApp_Entry.h"
#include "java_lang_Class.h"
#include "java_lang_Object.h"

#define HEAP_CHECK(condition) do { \
    if (!(condition)) { \
        fprintf(stderr, "Heap removal failed for %s (%d dimensions), line %d: %s\n", \
                descriptor->clsName, descriptor->dimensions, __LINE__, #condition); \
        return 1; \
    } \
} while (0)

static struct JavaObjectPrototype heapRemovalDisplay;
static struct JavaObjectPrototype heapRemovalBuffer;
static JAVA_OBJECT heapRemovalSlots[2];
static struct ThreadLocalData heapRemovalState;

static int checkDescriptorRemoval(struct clazz *descriptor, int populated) {
    struct clazz *parent = descriptor->__codenameOneParentClsReference;
    int mark = descriptor->__codenameOneGcMark;
    int position = descriptor->__heapPosition;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    int immortalCount = atomic_load(&cn1ImmortalObjSetCount);
#endif
    int rootCount = cn1ImmortalRootsN;
    allObjectsInHeap = populated ? heapRemovalSlots : NULL;
    HEAP_CHECK(removeObjectFromHeapCollection(&heapRemovalState,
            (JAVA_OBJECT)descriptor) == JAVA_TRUE);
    HEAP_CHECK(allObjectsInHeap == (populated ? heapRemovalSlots : NULL));
    HEAP_CHECK(heapRemovalSlots[0] == &heapRemovalDisplay);
    HEAP_CHECK(heapRemovalSlots[1] == &heapRemovalBuffer);
    HEAP_CHECK(heapRemovalDisplay.__heapPosition == 0);
    HEAP_CHECK(heapRemovalBuffer.__heapPosition == 1);
    HEAP_CHECK(descriptor->__codenameOneParentClsReference == parent);
    HEAP_CHECK(descriptor->__codenameOneGcMark == mark);
    HEAP_CHECK(descriptor->__heapPosition == position);
    HEAP_CHECK(cn1ImmortalRootsN == rootCount);
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    HEAP_CHECK(atomic_load(&cn1ImmortalObjSetCount) == immortalCount);
    HEAP_CHECK(!cn1GcImmortalObjContains((JAVA_OBJECT)descriptor));
#endif
    return 0;
}

#define ARRAY_DESCRIPTORS(type) \
    &class_array1__##type, &class_array2__##type, &class_array3__##type

int cn1TestHeapRemoval(void) {
    struct clazz *descriptors[] = {
        ARRAY_DESCRIPTORS(JAVA_BOOLEAN), ARRAY_DESCRIPTORS(JAVA_BYTE),
        ARRAY_DESCRIPTORS(JAVA_CHAR), ARRAY_DESCRIPTORS(JAVA_SHORT),
        ARRAY_DESCRIPTORS(JAVA_INT), ARRAY_DESCRIPTORS(JAVA_LONG),
        ARRAY_DESCRIPTORS(JAVA_FLOAT), ARRAY_DESCRIPTORS(JAVA_DOUBLE),
        ARRAY_DESCRIPTORS(java_lang_Class), ARRAY_DESCRIPTORS(GetClassApp_Entry),
        &class__GetClassApp_Entry, &class__java_lang_Class, &ClazzClazz
    };
    heapRemovalDisplay.__codenameOneParentClsReference = &class__GetClassApp_Entry;
    heapRemovalDisplay.__codenameOneGcMark = 2;
    heapRemovalDisplay.__heapPosition = 0;
    heapRemovalBuffer = heapRemovalDisplay;
    heapRemovalBuffer.__heapPosition = 1;
    heapRemovalSlots[0] = &heapRemovalDisplay;
    heapRemovalSlots[1] = &heapRemovalBuffer;
    sizeOfAllObjectsInHeap = 2;
    currentSizeOfAllObjectsInHeap = 2;

    unsigned count = sizeof(descriptors) / sizeof(descriptors[0]);
    for (unsigned i = 0; i < count; i++) {
        struct clazz *descriptor = descriptors[i];
        // First use the unmodified runtime/generated initializer, including
        // null-parent primitive arrays and java.lang.Class arrays.
        if (checkDescriptorRemoval(descriptor, 1)
                || checkDescriptorRemoval(descriptor, 0)) return 1;
        // Then execute the real getClass rewrite before a later static-final
        // store. An object's own class parent changing must not unroot it.
        struct JavaObjectPrototype instance = {0};
        instance.__codenameOneParentClsReference = descriptor;
        HEAP_CHECK(java_lang_Object_getClassImpl___R_java_lang_Class(
                &heapRemovalState, &instance) == (JAVA_OBJECT)descriptor);
        HEAP_CHECK(descriptor->__codenameOneParentClsReference == &ClazzClazz);
        if (checkDescriptorRemoval(descriptor, 1)
                || checkDescriptorRemoval(descriptor, 0)) return 1;
    }

    // Prove this is not an unconditional no-op: genuine heap objects whose
    // class descriptor now has a ClazzClazz parent must still be removed/rooted.
    struct clazz *descriptor = &class__GetClassApp_Entry;
    allObjectsInHeap = heapRemovalSlots;
    HEAP_CHECK(removeObjectFromHeapCollection(&heapRemovalState,
            &heapRemovalBuffer) == JAVA_TRUE);
    HEAP_CHECK(heapRemovalSlots[1] == NULL);
    HEAP_CHECK(heapRemovalSlots[0] == &heapRemovalDisplay);
    HEAP_CHECK(heapRemovalBuffer.__heapPosition == -1);
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    HEAP_CHECK(cn1GcImmortalObjContains(&heapRemovalBuffer));
#endif
    HEAP_CHECK(removeObjectFromHeapCollection(&heapRemovalState,
            &heapRemovalDisplay) == JAVA_TRUE);
    HEAP_CHECK(heapRemovalSlots[0] == NULL);
    HEAP_CHECK(heapRemovalDisplay.__heapPosition == -1);
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    HEAP_CHECK(cn1GcImmortalObjContains(&heapRemovalDisplay));
#endif
    printf("HEAP_REMOVAL_OK: %u descriptors\n", count);
    return 0;
}
