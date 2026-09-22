#!/usr/bin/env python3
"""Fast heap-removal smoke check; GetClassIntegrationTest covers real descriptors."""
import os
from pathlib import Path
import shlex
import subprocess
import tempfile
import unittest


REPO = Path(__file__).resolve().parent.parent
RUNTIME = REPO / "vm/ByteCodeTranslator/src/cn1_globals.m"
NATIVES = REPO / "vm/ByteCodeTranslator/src/nativeMethods.m"


def function(source, signature):
    start = source.index("\n" + signature) + 1
    return source[start:source.index("\n}", start) + 2]


PRELUDE = r'''
#include <assert.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
/* The shared object/clazz header; the test never accesses the clazz payload. */
struct clazz;
struct JavaObjectPrototype {
    struct clazz *__codenameOneParentClsReference;
    int __codenameOneGcMark;
    int __heapPosition;
};
typedef struct JavaObjectPrototype *JAVA_OBJECT;
typedef int JAVA_BOOLEAN;
struct clazz {
    struct clazz *__codenameOneParentClsReference;
    int __codenameOneGcMark;
    int __heapPosition;
};
struct ThreadLocalData {
    int heapAllocationSize;
    JAVA_OBJECT *pendingHeapAllocations;
};
#define CODENAME_ONE_THREAD_STATE struct ThreadLocalData *threadStateData __attribute__((unused))
#define JAVA_NULL ((JAVA_OBJECT)0)
#define JAVA_TRUE 1
#define JAVA_FALSE 0
#define CN1_IS_TAGGED(o) (((uintptr_t)(o) & 1) != 0)
#define CN1_BIBOP_HEAP_POS (-3)
#define CN1_CONSERVATIVE_GC_ROOTS
#define CN1_TAGGED_ACTIVE 0
static struct clazz class__java_lang_Class;
static struct clazz ClazzClazz;
static void initClazzClazz(void) {}
static JAVA_OBJECT *allObjectsInHeap;
static int sizeOfAllObjectsInHeap = 4;
static int cn1SweepRemoving;
static JAVA_OBJECT registered, rooted;
static void cn1GcRegisterImmortalObj(JAVA_OBJECT o) { registered = o; }
static void cn1AddImmortalRoot(JAVA_OBJECT o) { rooted = o; }
'''


HARNESS = r'''
int main(void) {
    struct clazz ordinaryClass = { &class__java_lang_Class, 0, 0 };
    struct clazz classLiteral = { &class__java_lang_Class, 0, 0 };
    struct clazz arrayLiteral = { 0, 0, 0 };
    struct JavaObjectPrototype display = { &ordinaryClass, 2, 0 };
    struct JavaObjectPrototype buffer = { &ordinaryClass, 2, 1 };
    struct JavaObjectPrototype pending = { &ordinaryClass, -1, -1 };
    struct JavaObjectPrototype bibop = { &ordinaryClass, 2, CN1_BIBOP_HEAP_POS };
    JAVA_OBJECT pendingTable[] = { &pending };
    struct ThreadLocalData state = { 1, pendingTable };
    JAVA_OBJECT literals[] = { (JAVA_OBJECT)&classLiteral,
                              (JAVA_OBJECT)&arrayLiteral,
                              (JAVA_OBJECT)&class__java_lang_Class,
                              JAVA_NULL, (JAVA_OBJECT)(uintptr_t)3 };
    allObjectsInHeap = calloc(sizeOfAllObjectsInHeap, sizeof(JAVA_OBJECT));
    assert(allObjectsInHeap != NULL);
    allObjectsInHeap[0] = &display;
    allObjectsInHeap[1] = &buffer;
    /* Same operation as the generated CodenameOneThread.CODE setter. The
     * original code erased Display without changing Display's own heap index. */
    for (unsigned i = 0; i < sizeof(literals) / sizeof(literals[0]); i++) {
        assert(removeObjectFromHeapCollection(&state, literals[i]) == JAVA_TRUE);
        assert(allObjectsInHeap[0] == &display);
        assert(allObjectsInHeap[1] == &buffer);
        assert(display.__heapPosition == 0);
        assert(classLiteral.__heapPosition == 0);
        assert(arrayLiteral.__heapPosition == 0);
        assert(class__java_lang_Class.__heapPosition == 0);
        assert(registered == NULL && rooted == NULL);
    }
    /* Execute the real getClass() rewrite before a later static-final store.
     * The first version of the fix only recognized the original parent. */
    struct JavaObjectPrototype instance = { &classLiteral, -1, -1 };
    JAVA_OBJECT reflected = java_lang_Object_getClassImpl___R_java_lang_Class(&state, &instance);
    assert(reflected == (JAVA_OBJECT)&classLiteral);
    assert(classLiteral.__codenameOneParentClsReference == &ClazzClazz);
    assert(removeObjectFromHeapCollection(&state, reflected) == JAVA_TRUE);
    assert(allObjectsInHeap[0] == &display && allObjectsInHeap[1] == &buffer);
    assert(classLiteral.__heapPosition == 0 && registered == NULL && rooted == NULL);
    JAVA_OBJECT meta = java_lang_Object_getClassImpl___R_java_lang_Class(
            &state, (JAVA_OBJECT)&class__java_lang_Class);
    assert(meta == (JAVA_OBJECT)&ClazzClazz);
    assert(removeObjectFromHeapCollection(&state, meta) == JAVA_TRUE);
    assert(allObjectsInHeap[0] == &display && ClazzClazz.__heapPosition == 0);
    /* An ordinary object's class may also have the rewritten parent. */
    assert(java_lang_Object_getClassImpl___R_java_lang_Class(&state, &display)
            == (JAVA_OBJECT)&ordinaryClass);
    /* Genuine objects still follow their original removal/rooting paths. */
    assert(removeObjectFromHeapCollection(&state, &buffer) == JAVA_TRUE);
    assert(allObjectsInHeap[1] == NULL && registered == &buffer);
    assert(buffer.__heapPosition == -1 && allObjectsInHeap[0] == &display);
    registered = NULL;
    assert(removeObjectFromHeapCollection(&state, &pending) == JAVA_TRUE);
    assert(pendingTable[0] == NULL && registered == &pending);
    assert(removeObjectFromHeapCollection(&state, &bibop) == JAVA_TRUE);
    assert(rooted == &bibop && allObjectsInHeap[0] == &display);
    cn1SweepRemoving = JAVA_TRUE;
    registered = NULL;
    assert(removeObjectFromHeapCollection(&state, &display) == JAVA_TRUE);
    assert(allObjectsInHeap[0] == NULL && registered == NULL);
    assert(display.__heapPosition == -1);
    free(allObjectsInHeap);
    allObjectsInHeap = NULL;
    registered = rooted = NULL;
    /* Class literals must not even initialize an otherwise empty heap table. */
    for (unsigned i = 0; i < sizeof(literals) / sizeof(literals[0]); i++) {
        assert(removeObjectFromHeapCollection(&state, literals[i]) == JAVA_TRUE);
        assert(allObjectsInHeap == NULL);
        assert(registered == NULL && rooted == NULL);
    }
    assert(removeObjectFromHeapCollection(&state, meta) == JAVA_TRUE);
    assert(allObjectsInHeap == NULL);
    return 0;
}
'''


class GcHeapRemovalTest(unittest.TestCase):
    def test_class_literals_preserve_heap_slot_zero(self):
        source = RUNTIME.read_text()
        code = PRELUDE + function(NATIVES.read_text(),
                                "JAVA_OBJECT java_lang_Object_getClassImpl___R_java_lang_Class(") + "\n"
        code += function(source, "int findPointerPosInHeap(") + "\n"
        code += function(source, "JAVA_BOOLEAN removeObjectFromHeapCollection(")
        code += HARNESS
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "heap_removal.c"
            binary = Path(directory) / "heap_removal"
            path.write_text(code)
            subprocess.run(shlex.split(os.environ.get("CC", "cc")) +
                           ["-std=c11", "-O2", "-fno-strict-aliasing", "-Wall",
                            "-Wextra", "-Werror", str(path), "-o", str(binary)],
                           check=True)
            subprocess.run([str(binary)], check=True)


if __name__ == "__main__":
    unittest.main()
