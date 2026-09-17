"""Exercise the actual C storage implementation under ASan/UBSan."""
from pathlib import Path
import subprocess
import tempfile
import unittest


class NativeStorageTests(unittest.TestCase):
    def test_reference_range_keeps_conservative_and_verifier_paths(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        start = source.index('int cn1GcFieldMarkEpoch(')
        end = source.index('// Trace `count` elements.', start)
        header = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.h').read_text()
        helper = header[header.index('static inline __attribute__((always_inline)) void cn1GcMarkField('):]
        helper = helper[:helper.index('\n}') + 2]
        program = r'''
#include <stdint.h>
#include <stdlib.h>
#include <assert.h>
#define CN1_GC_VERIFY 1
#define CODENAME_ONE_THREAD_STATE void* threadStateData
#define JAVA_NULL NULL
#define JAVA_FALSE 0
#define CN1_IS_TAGGED(o) ((uintptr_t)(o) & 7)
typedef int JAVA_INT;
typedef int JAVA_BOOLEAN;
struct Object { int __codenameOneGcMark; int pad; };
typedef struct Object* JAVA_OBJECT;
static int cn1GcPreciseTrace, cn1GcVerifyActive, cn1GcFaultFreeLive;
static void* gcMarkLocalBuf;
static int currentGcMarkValue = 7, calls, forced;
static void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o, JAVA_BOOLEAN force) {
    calls++; forced += force;
}
''' + helper + source[start:end] + r'''
int main(void) {
    _Alignas(8) struct Object marked = {7, 0}, old = {6, 0}, fresh = {-1, 0};
    JAVA_OBJECT refs[] = {NULL, (JAVA_OBJECT)1, &marked, &old, &fresh, &marked};
    cn1GcPreciseTrace = 1;
    cn1GcMarkReferenceRange(NULL, refs, 6, 0);
    assert(calls == 2 && forced == 0);
    calls = 0;
    cn1GcMarkReferenceRange(NULL, refs, 6, 1);
    assert(calls == 4 && forced == 4);
    calls = forced = 0; gcMarkLocalBuf = &marked;
    cn1GcMarkReferenceRange(NULL, refs, 6, 1);
    assert(calls == 2 && forced == 0);
    calls = forced = 0;
    for (int i = 0; i < 6; i++) cn1GcMarkField(NULL, refs[i], 1, cn1GcFieldMarkEpoch(1));
    assert(calls == 2 && forced == 2);
    gcMarkLocalBuf = NULL; calls = forced = 0;
    for (int i = 0; i < 6; i++) cn1GcMarkField(NULL, refs[i], 1, cn1GcFieldMarkEpoch(1));
    assert(calls == 4 && forced == 4);
    // An arbitrary aligned word must reach validation without a header read.
    JAVA_OBJECT suspect[] = {(JAVA_OBJECT)(uintptr_t)0x1000};
    calls = 0; cn1GcPreciseTrace = 0;
    cn1GcMarkReferenceRange(NULL, suspect, 1, 0);
    assert(calls == 1);
    cn1GcMarkField(NULL, suspect[0], 0, cn1GcFieldMarkEpoch(0));
    assert(calls == 2);
    calls = 0; cn1GcPreciseTrace = 1; cn1GcVerifyActive = 1;
    cn1GcMarkReferenceRange(NULL, suspect, 1, 0);
    assert(calls == 1);
    cn1GcMarkField(NULL, suspect[0], 0, cn1GcFieldMarkEpoch(0));
    assert(calls == 2);
    calls = 0; cn1GcVerifyActive = 0; cn1GcFaultFreeLive = 1;
    cn1GcMarkReferenceRange(NULL, refs, 6, 0);
    assert(calls == 4);
    cn1GcMarkReferenceRange(NULL, NULL, 0, 0);
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'range.c').write_text(program)
            subprocess.run(['clang', '-std=c11', '-O3', '-fsanitize=address,undefined',
                            str(root / 'range.c'), '-o', str(root / 'range')], check=True)
            subprocess.run([str(root / 'range')], check=True)

    def test_adopted_extents_keep_unindexed_page_fallback(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        start = source.index('static void cn1ConsExtAdd(')
        end = source.index('static int cn1ConsExtCmp(', start)
        program = r'''
#include <stdint.h>
#include <stdlib.h>
#include <assert.h>
#define JAVA_NULL NULL
#define CN1_BIBOP_ADOPTED (-4)
#define CN1_BIBOP_PAGE_SIZE 65536
struct clazz { int isArray; };
struct JavaObjectPrototype { struct clazz* __codenameOneParentClsReference; int mark, __heapPosition; };
typedef struct JavaObjectPrototype* JAVA_OBJECT;
struct JavaArrayPrototype { struct JavaObjectPrototype object; int length, primitiveSize; void* data; };
typedef struct JavaArrayPrototype* JAVA_ARRAY;
typedef struct { char* lo; char* hi; JAVA_OBJECT base; } CN1ConsExtent;
static CN1ConsExtent* cn1ConsExt;
static int cn1ConsExtN, cn1ConsExtCap, indexed;
static void* cn1ConsPgFind(char* page) { return indexed ? page : NULL; }
''' + source[start:end] + r'''
int main(void) {
    struct clazz cls = {0};
    struct JavaObjectPrototype object = {&cls, 1, CN1_BIBOP_ADOPTED};
    indexed = 1;
    cn1ConsExtAdd(&object);
    assert(cn1ConsExtN == 0);
    // A failed or not-yet-complete page index must retain the legacy fallback.
    indexed = 0;
    cn1ConsExtAdd(&object);
    assert(cn1ConsExtN == 1 && cn1ConsExt[0].base == &object);
    indexed = 1;
    object.__heapPosition = 0;
    cn1ConsExtAdd(&object);
    assert(cn1ConsExtN == 2 && cn1ConsExt[1].base == &object);
    free(cn1ConsExt);
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'extent.c').write_text(program)
            subprocess.run(['clang', '-std=c11', '-O3', '-fsanitize=address,undefined',
                            str(root / 'extent.c'), '-o', str(root / 'extent')], check=True)
            subprocess.run([str(root / 'extent')], check=True)

    def test_fused_string_leaf_rejects_adjacent_allocations(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        start = source.index('static int cn1GcStringIsFusedLeaf(')
        end = source.index('void gcMarkObject(', start)
        program = r'''
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#define CN1_BIBOP_HEAP_POS (-3)
#define CN1_BIBOP_ADOPTED (-4)
#define CN1_GC_EMBEDDED_PRIMITIVE (-5)
#define CN1_BIBOP_PAGE_SIZE 65536
struct Object { void* cls; int mark; int __heapPosition; };
typedef struct Object* JAVA_OBJECT;
struct obj__java_lang_String {
    struct Object header; JAVA_OBJECT java_lang_String_value;
    int offset, count, hash; long peer;
};
struct JavaArrayPrototype { struct Object header; int length; char dims, width; void* data; };
typedef struct { int slotSize; } CN1BibopPage;
''' + source[start:end] + r'''
int main(void) {
    void* allocation = NULL;
    assert(posix_memalign(&allocation, CN1_BIBOP_PAGE_SIZE, CN1_BIBOP_PAGE_SIZE) == 0);
    memset(allocation, 0, CN1_BIBOP_PAGE_SIZE);
    CN1BibopPage* page = allocation;
    struct obj__java_lang_String* string = (void*)((char*)allocation + 256);
    JAVA_OBJECT owner = (JAVA_OBJECT)string;
    JAVA_OBJECT child = (JAVA_OBJECT)((char*)owner + sizeof(*string));
    owner->__heapPosition = CN1_BIBOP_HEAP_POS;
    string->java_lang_String_value = child;
    // The exact adjacency that an address-only proof misclassified.
    page->slotSize = sizeof(*string);
    child->__heapPosition = CN1_BIBOP_HEAP_POS;
    assert(!cn1GcStringIsFusedLeaf(owner));
    child->__heapPosition = CN1_GC_EMBEDDED_PRIMITIVE;
    assert(!cn1GcStringIsFusedLeaf(owner));
    page->slotSize = sizeof(*string) + sizeof(struct JavaArrayPrototype);
    assert(cn1GcStringIsFusedLeaf(owner));
    owner->__heapPosition = CN1_BIBOP_ADOPTED;
    assert(cn1GcStringIsFusedLeaf(owner));
    child->__heapPosition = CN1_BIBOP_HEAP_POS;
    assert(!cn1GcStringIsFusedLeaf(owner));
    owner->__heapPosition = 0;
    assert(!cn1GcStringIsFusedLeaf(owner));
    free(allocation);
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'leaf.c').write_text(program)
            subprocess.run(['clang', '-D_POSIX_C_SOURCE=200112L', '-std=c11', '-O3', '-fsanitize=address,undefined',
                            str(root / 'leaf.c'), '-o', str(root / 'leaf')], check=True)
            subprocess.run([str(root / 'leaf')], check=True)

    def test_fused_array_payload_fits_exact_block(self):
        header = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.h').read_text()
        struct_start = header.index('struct JavaArrayPrototype {')
        struct_end = header.index('};', struct_start) + 2
        start = header.index('#define CN1_FUSED_ARR_BYTES')
        end = header.index('// Register an object', start)
        program = r''' 
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#define DEBUG_GC_VARIABLES
struct clazz { int unused; };
typedef void* JAVA_OBJECT;
''' + header[struct_start:struct_end] + '\n' + header[start:end] + r'''
int main(void) {
    struct clazz cls;
    for(int width = 1; width <= 8; width *= 2) {
        for(int len = 0; len <= 1024; len++) {
            const int offset = 48;
            size_t bytes = offset + CN1_FUSED_ARR_BYTES(len, width);
            char* owner = malloc(bytes);
            memset(owner, 0, bytes);
            struct JavaArrayPrototype* child = cn1FusedInstallPrimArray(owner, offset, &cls, width, len);
            assert(child->length == len && child->primitiveSize == width);
            assert(child->__heapPosition == CN1_GC_EMBEDDED_PRIMITIVE);
            if(len) {
                assert((char*)child->data == (char*)child + sizeof(*child));
                assert((char*)child->data + len * width <= owner + bytes);
                memset(child->data, 0xff, len * width);
            } else assert(child->data == NULL);
            free(owner);
        }
    }
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'fused.c').write_text(program)
            subprocess.run(['clang', '-std=c11', '-O3', '-fsanitize=address,undefined',
                            str(root / 'fused.c'), '-o', str(root / 'fused')], check=True)
            subprocess.run([str(root / 'fused')], check=True)

    def test_extent_radix_preserves_records(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        start = source.index('static int cn1ConsExtCmp(')
        end = source.index('// Randomised self-test for the extent sort', start)
        program = r'''
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <stdio.h>
#define CN1_GC_VERIFY 1
typedef struct { char* lo; char* hi; void* base; } CN1ConsExtent;
''' + source[start:end] + r'''
int main(void) {
    const int sizes[] = {0, 1, 2, 31, 32, 33, 256, 10000, 100003};
    for(int pattern = 0; pattern < 7; pattern++) {
        for(unsigned test = 0; test < sizeof(sizes)/sizeof(sizes[0]); test++) {
            int n = sizes[test];
            CN1ConsExtent* values = calloc(n + 1, sizeof(*values));
            CN1ConsExtent* sorted = calloc(n + 1, sizeof(*sorted));
            unsigned char* seen = calloc(n + 1, 1);
            uintptr_t random = 1234567;
            for(int i = 0; i < n; i++) {
                random = random * (uintptr_t)6364136223846793005ULL + 1;
                uintptr_t key = pattern == 0 ? (uintptr_t)i
                    : pattern == 1 ? (uintptr_t)(n - i)
                    : pattern == 2 ? 8192
                    : pattern == 3 ? random % 8
                    : pattern == 4 ? ((random & 3) << 32) + (uintptr_t)i * 48
                    : pattern == 5 ? UINTPTR_MAX - (uintptr_t)i : random;
                values[i].lo = (char*)key;
                values[i].hi = (char*)(key ^ (uintptr_t)0x12345);
                values[i].base = (void*)(uintptr_t)(i + 1);
            }
            memcpy(sorted, values, n * sizeof(*values));
            qsort(sorted, n, sizeof(*sorted), cn1ConsExtCmp);
            cn1ConsExtSort(values, n);
            for(int i = 0; i < n; i++) {
                assert(values[i].lo == sorted[i].lo);
                assert((uintptr_t)values[i].hi == ((uintptr_t)values[i].lo ^ 0x12345));
                uintptr_t id = (uintptr_t)values[i].base;
                assert(id > 0 && id <= (uintptr_t)n && !seen[id]);
                seen[id] = 1;
            }
            free(seen); free(sorted); free(values);
        }
    }
    return 0;
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'sort.c').write_text(program)
            subprocess.run(['clang', '-std=c11', '-O3', '-fsanitize=address,undefined',
                            str(root / 'sort.c'), '-o', str(root / 'sort')], check=True)
            subprocess.run([str(root / 'sort')], check=True)

    def test_native_pending_append_survives_migration_unlock(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        start = source.index('static int cn1MigratePendingAllocations(')
        end = source.index('static void gcMarkDrain(CODENAME_ONE_THREAD_STATE);', start)
        functions = source[start:end]
        prefix = r'''
#include <assert.h>
#include <stddef.h>
typedef void* JAVA_OBJECT;
#define JAVA_NULL NULL
struct ThreadLocalData { int lightweightThread, heapAllocationSize; void* pendingHeapAllocations[4]; };
static struct ThreadLocalData state;
static int held, injectAppend, registered, first, second;
static void lockThreadHeapMutex(void) { assert(!held); held = 1; }
static void unlockThreadHeapMutex(void);
static void placeObjectInHeapCollection(JAVA_OBJECT object) {
    assert(held);
    assert(object == (registered ? (void*)&second : (void*)&first));
    registered++;
}
'''
        suffix = r'''
static void unlockThreadHeapMutex(void) {
    assert(held); held = 0;
    // Schedule the native producer at the exact unlock/reset boundary.
    if(injectAppend) {
        injectAppend = 0;
        cn1AppendPendingAllocation(&state, &second);
    }
}
int main(void) {
    cn1AppendPendingAllocation(&state, &first);
    injectAppend = 1;
    assert(cn1MigratePendingAllocations(&state) == 1);
    if(state.heapAllocationSize != 1 || state.pendingHeapAllocations[0] != &second) return 7;
    assert(cn1MigratePendingAllocations(&state) == 1);
    assert(registered == 2 && state.heapAllocationSize == 0 && !held);
    return 0;
}
'''
        broken = functions.replace(
            '    t->heapAllocationSize = 0;\n    if(!t->lightweightThread) unlockThreadHeapMutex();',
            '    if(!t->lightweightThread) unlockThreadHeapMutex();\n    t->heapAllocationSize = 0;')
        self.assertNotEqual(broken, functions)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name, body, expected in [('fixed', functions, 0), ('old-reset-order', broken, 7)]:
                c = root / (name + '.c')
                c.write_text(prefix + body + suffix)
                executable = root / name
                subprocess.run(['clang', '-std=c11', '-O3', '-fsanitize=address,undefined',
                                str(c), '-o', str(executable)], check=True)
                self.assertEqual(expected, subprocess.run([str(executable)]).returncode)

    def test_mark_frontier_growth_preserves_entries_on_failure(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        start = source.index('struct gcMarkWorklistEntry {')
        end = source.index('static int gcMarkWorklistTop = 0;', start)
        program = r'''
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <assert.h>
typedef void* JAVA_OBJECT;
typedef int JAVA_BOOLEAN;
#define CN1_GC_MARK_WORKLIST_SIZE 4
static int gcMarkWorklistTop, gcMarkWorklistCapacity = CN1_GC_MARK_WORKLIST_SIZE;
static int failAllocation;
static void* checkedMalloc(size_t size) { return failAllocation ? NULL : malloc(size); }
static void* checkedRealloc(void* p, size_t size) { return failAllocation ? NULL : realloc(p, size); }
#define malloc checkedMalloc
#define realloc checkedRealloc
''' + source[start:end] + r'''
int main(void) {
    for(int i = 0; i < 4096; i++) {
        if(gcMarkWorklistTop == gcMarkWorklistCapacity) {
            struct gcMarkWorklistEntry* previous = gcMarkWorklist;
            failAllocation = 1;
            assert(!gcMarkWorklistGrow());
            assert(gcMarkWorklist == previous && gcMarkWorklistTop == gcMarkWorklistCapacity);
            failAllocation = 0;
            assert(gcMarkWorklistGrow());
        }
        gcMarkWorklist[gcMarkWorklistTop++] = (struct gcMarkWorklistEntry){(void*)(uintptr_t)(i + 1), i & 1};
    }
    for(int i = 4095; i >= 0; i--) {
        struct gcMarkWorklistEntry entry = gcMarkWorklist[--gcMarkWorklistTop];
        assert(entry.obj == (void*)(uintptr_t)(i + 1) && entry.force == (i & 1));
    }
    free(gcMarkWorklist);
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'test.c').write_text(program)
            subprocess.run(['clang', '-std=c11', '-O2', '-fsanitize=address,undefined',
                            str(root / 'test.c'), '-o', str(root / 'test')], check=True)
            subprocess.run([str(root / 'test')], check=True)

    def test_compact_string_comparisons(self):
        intrinsics = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_intrinsics.h').read_text()
        header = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.h').read_text()
        short_equal = header[header.index('static inline __attribute__((always_inline)) int cn1CompactBytesEqual'):header.index('static inline JAVA_INT cn1RefBlockCount')]
        inline_equals = intrinsics[intrinsics.index('static inline __attribute__((always_inline)) JAVA_BOOLEAN cn1InlStrEquals'):intrinsics.index('static inline JAVA_INT cn1InlStrHash')]
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/nativeMethods.m').read_text()
        start = source.index('static inline __attribute__((always_inline)) JAVA_BOOLEAN cn1StringEquals')
        end = source.index('JAVA_INT java_lang_Character_toLowerCase', start)
        ignore_start = source.index('JAVA_BOOLEAN java_lang_String_equalsIgnoreCase___')
        ignore_end = source.index('JAVA_INT java_lang_String_hashCode___', ignore_start)
        program = r'''
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
typedef int JAVA_INT;
typedef int JAVA_BOOLEAN;
typedef uint16_t JAVA_ARRAY_CHAR;
#define JAVA_TRUE 1
#define JAVA_FALSE 0
#define JAVA_NULL NULL
#define CODENAME_ONE_THREAD_STATE void* threadStateData
struct clazz { int classId; };
struct Object { struct clazz* __codenameOneParentClsReference; };
typedef struct Object* JAVA_OBJECT;
struct Array { struct clazz* __codenameOneParentClsReference; void* data; };
typedef struct Array* JAVA_ARRAY;
struct obj__java_lang_String {
    struct clazz* __codenameOneParentClsReference;
    JAVA_OBJECT java_lang_String_value;
    int java_lang_String_offset, java_lang_String_count, java_lang_String_hashCode;
};
#define CN1_CLASS_OF(o) ((o)->__codenameOneParentClsReference)
static struct clazz latinClass = {1}, wideClass = {2}, stringClass = {3};
static int cn1StrIsLatin1(JAVA_OBJECT s) { return ((JAVA_ARRAY)((struct obj__java_lang_String*)s)->java_lang_String_value)->__codenameOneParentClsReference == &latinClass; }
static int cn1StrCharAtRaw(JAVA_OBJECT object, int index) {
    struct obj__java_lang_String* s = (struct obj__java_lang_String*)object;
    JAVA_ARRAY a = (JAVA_ARRAY)s->java_lang_String_value;
    int i = index + s->java_lang_String_offset;
    return cn1StrIsLatin1(object) ? ((uint8_t*)a->data)[i] : ((uint16_t*)a->data)[i];
}
''' + short_equal + source[start:end] + source[ignore_start:ignore_end] + r'''
#define class__java_lang_String stringClass
#define class_array1__JAVA_BYTE latinClass
#define CN1_IS_TAGGED(o) (((uintptr_t)(o) & 7) != 0)
''' + inline_equals + r'''
int main(void) {
    for (size_t n = 0; n < 97; n++) for (size_t offset = 0; offset < 8; offset++) {
        uint8_t* left = malloc(offset + n + (n == 0));
        uint8_t* right = malloc(offset + n + (n == 0));
        for (size_t i = 0; i < n; i++) left[offset+i] = right[offset+i] = (uint8_t)(i * 31);
        assert(cn1CompactBytesEqual(left+offset, right+offset, n));
        for (size_t i = 0; i < n; i++) {
            right[offset+i] ^= 0x80;
            assert(!cn1CompactBytesEqual(left+offset, right+offset, n));
            right[offset+i] ^= 0x80;
        }
        free(left); free(right);
    }
    uint8_t bytesA[160], bytesB[160]; uint16_t wideA[160], wideB[160];
    for(int a = 0; a < 2; a++) for(int b = 0; b < 2; b++) for(int n = 0; n < 130; n++) {
        for(int i = 0; i < n; i++) {
            bytesA[3+i] = bytesB[5+i] = (uint8_t)(i * 31);
            wideA[3+i] = wideB[5+i] = bytesA[3+i];
        }
        struct Array aa = {a ? &wideClass : &latinClass, a ? (void*)wideA : (void*)bytesA};
        struct Array ab = {b ? &wideClass : &latinClass, b ? (void*)wideB : (void*)bytesB};
        struct obj__java_lang_String sa = {&stringClass, (JAVA_OBJECT)&aa, 3, n, 0};
        struct obj__java_lang_String sb = {&stringClass, (JAVA_OBJECT)&ab, 5, n, 0};
        JAVA_OBJECT x = (JAVA_OBJECT)&sa, y = (JAVA_OBJECT)&sb;
        assert(java_lang_String_equals___java_lang_Object_R_boolean(NULL, x, y));
        assert(cn1InlStrEquals(NULL, x, y));
        assert(!cn1InlStrEquals(NULL, x, NULL));
        assert(!cn1InlStrEquals(NULL, x, (JAVA_OBJECT)(uintptr_t)7));
        assert(java_lang_String_equalsIgnoreCase___java_lang_String_R_boolean(NULL, x, y));
        assert(java_lang_String_compareTo___java_lang_String_R_int(NULL, x, y) == 0);
        for (int i = 0; i < n; i++) {
            int old = cn1StrCharAtRaw(y, i);
            if (b) wideB[5+i] = old + 256; else bytesB[5+i] = old ^ 128;
            assert(!cn1InlStrEquals(NULL, x, y));
            assert(!java_lang_String_equals___java_lang_Object_R_boolean(NULL, x, y));
            if (b) wideB[5+i] = old; else bytesB[5+i] = old;
        }
        if(n) {
            int old = cn1StrCharAtRaw(y, n-1), changed = (old + 129) & 255;
            if(b) wideB[5+n-1] = changed; else bytesB[5+n-1] = changed;
            assert(!java_lang_String_equals___java_lang_Object_R_boolean(NULL, x, y));
            assert(!cn1InlStrEquals(NULL, x, y));
            assert(java_lang_String_compareTo___java_lang_String_R_int(NULL, x, y) == old - changed);
        }
        if(n) {
            if(a) wideA[3+n-1] = 'A'; else bytesA[3+n-1] = 'A';
            if(b) wideB[5+n-1] = 'a'; else bytesB[5+n-1] = 'a';
            assert(java_lang_String_equalsIgnoreCase___java_lang_String_R_boolean(NULL, x, y));
            if(b) wideB[5+n-1] = 'b'; else bytesB[5+n-1] = 'b';
            assert(!java_lang_String_equalsIgnoreCase___java_lang_String_R_boolean(NULL, x, y));
        }
    }
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'test.c').write_text(program)
            subprocess.run(['clang', '-std=c11', '-O2', '-fsanitize=address,undefined',
                            str(root / 'test.c'), '-o', str(root / 'test')], check=True)
            subprocess.run([str(root / 'test')], check=True)

    def test_satb_filters_only_live_epoch_and_keeps_old_references(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        start = source.index('static inline int cn1SatbNeedsEnqueue(')
        end = source.index('// Atomically take the current SATB batch', start)
        program = r'''
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdatomic.h>
#include <pthread.h>
#include <assert.h>
struct Object { int __codenameOneGcMark; };
typedef struct Object* JAVA_OBJECT;
typedef JAVA_OBJECT JAVA_ARRAY_OBJECT;
#define JAVA_NULL NULL
#define CN1_IS_TAGGED(o) 0
#define CN1_GC_CONFORM
#define CN1_SATB_BULK_CHUNK 256
static volatile int gcSatbActive = 1;
static _Atomic int bibopGcEpoch = 3;
static _Atomic long cn1GcSatbAlready, cn1GcSatbFresh, cn1GcSatbLocks, cn1SatbDrops;
static JAVA_OBJECT* gcSatbStack;
static long gcSatbTop, gcSatbCap;
static pthread_mutex_t gcSatbMutex = PTHREAD_MUTEX_INITIALIZER;
static int failAllocation;
static void* checkedRealloc(void* p, size_t size) { return failAllocation ? NULL : realloc(p, size); }
#define realloc checkedRealloc
void cn1SatbEnqueue(JAVA_OBJECT old);
''' + source[start:end] + r'''
int main(void) {
    struct Object fresh = {-1}, marked = {3}, old = {2}, older = {1};
    JAVA_OBJECT range[] = {NULL, &fresh, &marked, &old, &older};
    // Moving a long-lived collection repeatedly must not build a log of
    // millions of already-marked pointers, but every old edge must survive.
    for(int i = 0; i < 100000; i++) cn1SatbEnqueue(&marked);
    assert(gcSatbTop == 0 && gcSatbCap == 0);
    cn1SatbEnqueueRangeLocked(range, 5);
    assert(gcSatbTop == 2 && gcSatbStack[0] == &old && gcSatbStack[1] == &older);
    gcSatbTop = 0;
    atomic_store(&bibopGcEpoch, 4);
    cn1SatbEnqueueRangeLocked(range, 5);
    assert(gcSatbTop == 3 && gcSatbStack[0] == &marked);
    cn1SatbEnqueue(&fresh);
    assert(gcSatbTop == 3);
    free(gcSatbStack); gcSatbStack = NULL; gcSatbTop = gcSatbCap = 0;
    failAllocation = 1;
    cn1SatbEnqueueRangeLocked(range, 5);
    assert(cn1SatbDrops == 1 && gcSatbTop == 0);
    cn1SatbEnqueue(&old);
    assert(cn1SatbDrops == 2 && gcSatbTop == 0);
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'test.c').write_text(program)
            subprocess.run(['clang', '-std=c11', '-O2', '-pthread', '-fsanitize=address,undefined',
                            str(root / 'test.c'), '-o', str(root / 'test')], check=True)
            subprocess.run([str(root / 'test')], check=True)

    def test_incomplete_root_capture(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        start = source.index('static void cn1GcScanThreadNativeStack(CODENAME_ONE_THREAD_STATE, struct ThreadLocalData* t) {')
        end = source.index('// Scan the GC thread', start)
        program = r'''
#include <stddef.h>
#include <stdatomic.h>
#include <assert.h>
#define CN1_DISABLE_BIBOP
#define CN1_GC_CAN_FORCE_STOP
#define JAVA_TRUE 1
#define CODENAME_ONE_THREAD_STATE void* threadStateData
struct ThreadLocalData {
    int gcPthreadValid, gcPthread, gcMarkForcedStop, gcParkCaptured;
    void *gcSigStackBase, *gcSigStackPointer, *gcStackPointerAtPark;
    size_t gcSigStackSize, gcSigRegsLen;
    char gcSigRegs[32], gcRegisterSnapshot[32];
};
static int cn1GcRootsIncomplete, cn1GcSignalStopMode, cn1GcVtSnapshotCount;
static _Atomic int cn1GcFreezeHeld;
static void* cn1GcVtSnapshot;
static char stack[256];
static char *signalSp;
static int ranges, releases, noBounds;
static void cn1ConservativeMarkRange(void* state, char* lo, char* hi) {
    assert(lo && hi && hi > lo); ranges++;
}
static char* cn1GcStackBase(int thread, size_t* size) {
    *size = noBounds ? 0 : sizeof(stack); return noBounds ? NULL : stack + sizeof(stack);
}
static void cn1GcBuildRootSnapshots(void) {}
struct cn1VirtualThread;
static struct cn1VirtualThread* cn1VirtualThreadForStackAddress(void* sp, int count, void* snapshot) { return NULL; }
static void* cn1VirtualThreadStackHigh(struct cn1VirtualThread* vt) { return NULL; }
static void* cn1VirtualThreadResumerSp(struct cn1VirtualThread* vt) { return NULL; }
static void cn1GcAdoptReserve(int count) {}
static char* cn1GcSignalStopOne(struct ThreadLocalData* t) { return signalSp; }
static void cn1GcSignalReleaseOne(struct ThreadLocalData* t) { releases++; }
''' + source[start:end] + r'''
int main(void) {
    struct ThreadLocalData t = {0}; t.gcPthreadValid = 1;
    // Failed signal capture must invalidate the mark, rather than silently sweep.
    cn1GcScanThreadNativeStack(NULL, &t);
    assert(cn1GcRootsIncomplete && !ranges && !releases && !cn1GcFreezeHeld);
    cn1GcRootsIncomplete = 0;
    // An actual cooperative capture is a valid fallback when signal delivery fails.
    cn1GcSignalStopMode = 1; t.gcParkCaptured = 1; t.gcStackPointerAtPark = stack + 32;
    cn1GcScanThreadNativeStack(NULL, &t);
    assert(!cn1GcRootsIncomplete && ranges == 2 && !releases);
    // Captures outside the registered stack must not count as successful.
    t.gcParkCaptured = 0; signalSp = stack + sizeof(stack);
    cn1GcScanThreadNativeStack(NULL, &t);
    assert(cn1GcRootsIncomplete && releases == 1 && !cn1GcFreezeHeld);
    cn1GcRootsIncomplete = 0; signalSp = stack + 32; t.gcSigRegsLen = 32;
    cn1GcScanThreadNativeStack(NULL, &t);
    assert(!cn1GcRootsIncomplete && ranges == 4 && releases == 2 && !cn1GcFreezeHeld);
    // Already-frozen threads need valid bounds as well.
    t.gcMarkForcedStop = 1;
    cn1GcScanThreadNativeStack(NULL, &t);
    assert(cn1GcRootsIncomplete);
    cn1GcRootsIncomplete = 0; t.gcMarkForcedStop = 0; noBounds = 1;
    cn1GcScanThreadNativeStack(NULL, &t);
    assert(cn1GcRootsIncomplete);
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'test.c').write_text(program)
            subprocess.run(['clang', '-std=c11', '-O2', '-fsanitize=address,undefined',
                            str(root / 'test.c'), '-o', str(root / 'test')], check=True)
            subprocess.run([str(root / 'test')], check=True)

    def test_unswept_dead_slots_cannot_be_resurrected(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        start = source.index('static _Atomic int cn1GcReclaimedBefore')
        end = source.index('\n}', start) + 2
        program = r'''
#include <stdatomic.h>
#include <assert.h>
''' + source[start:end] + r'''
int main(void) {
    assert(!cn1GcWasReclaimed(-1));
    assert(!cn1GcWasReclaimed(0));
    atomic_store(&cn1GcReclaimedBefore, 9);
    // An old slot can remain physically present while its children are gone.
    assert(cn1GcWasReclaimed(8));
    assert(!cn1GcWasReclaimed(9));
    assert(!cn1GcWasReclaimed(10));
    assert(!cn1GcWasReclaimed(-1));
    // A skipped sweep leaves the cutoff unchanged, even if mark epochs advance.
    assert(!cn1GcWasReclaimed(9));
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'age.c').write_text(program)
            subprocess.run(['clang', '-std=c11', '-O3', '-fsanitize=address,undefined',
                            str(root / 'age.c'), '-o', str(root / 'age')], check=True)
            subprocess.run([str(root / 'age')], check=True)

    def test_byte_interior_roots(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        start = source.index('JAVA_OBJECT cn1ConservativeResolve(void* w) {')
        end = source.index('\n}', start) + 2
        program = r'''
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include <assert.h>
#define CN1_DISABLE_BIBOP
#define CN1_CONS_EXT_NO_BLOOM
#define JAVA_NULL NULL
typedef void* JAVA_OBJECT;
typedef struct { char* lo; char* hi; JAVA_OBJECT base; } Extent;
static Extent cn1ConsExt[1];
static int cn1ConsExtN = 1, cn1ConsExtHashMask = -1;
static char** cn1ConsExtHash;
static unsigned cn1PtrMix(uintptr_t value) { return (unsigned)value; }
''' + source[start:end] + r'''
int main(void) {
    char* allocation = malloc(257);
    cn1ConsExt[0] = (Extent){allocation, allocation + 257, allocation};
    // Optimized native string loops can retain only a byte-interior pointer.
    // Its low bits can resemble a tagged value; the allocation range decides.
    for(int offset = 0; offset < 257; offset++)
        assert(cn1ConservativeResolve(allocation + offset) == allocation);
    assert(cn1ConservativeResolve(NULL) == NULL);
    assert(cn1ConservativeResolve(allocation + 257) == NULL);
    assert(cn1ConservativeResolve((void*)(uintptr_t)1) == NULL);
    free(allocation);
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'test.c').write_text(program)
            subprocess.run(['clang', '-std=c11', '-O2', '-fsanitize=address,undefined',
                            str(root / 'test.c'), '-o', str(root / 'test')], check=True)
            subprocess.run([str(root / 'test')], check=True)

    def test_retirement_failure_and_accounting(self):
        source = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.m').read_text()
        header = (Path(__file__).resolve().parents[1] / 'ByteCodeTranslator/src/cn1_globals.h').read_text()
        header_start = header.index('typedef struct __attribute__((aligned(16))) CN1NativeBlock {')
        header_end = header.index('\n}', header.index('static inline JAVA_INT cn1RefBlockCount', header_start)) + 2
        start = source.index('static pthread_mutex_t cn1RetiredMutex')
        end = source.index('// STORE. The SATB', start)
        program = r'''
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <stdatomic.h>
#include <assert.h>
typedef int32_t JAVA_INT;
typedef int64_t JAVA_LONG;
typedef void* JAVA_OBJECT;
static size_t allocated;
static int failAllocation;
static void* checkedCalloc(size_t count, size_t size) {
    return failAllocation ? NULL : calloc(count, size);
}
static void* checkedRealloc(void* old, size_t size) {
    return failAllocation ? NULL : realloc(old, size);
}
typedef int JAVA_BOOLEAN;
typedef JAVA_OBJECT JAVA_ARRAY_OBJECT;
#define CODENAME_ONE_THREAD_STATE void* threadStateData
#define CN1_SATB_DELETE(slot) ((void)0)
#define CN1_WRITE_BARRIER(slot, value) ((void)0)
static int writers, barrierActive, logged;
static int cn1SatbBulkBegin(void) { writers++; return barrierActive; }
static void cn1SatbBulkEnd(void) { assert(writers == 1); writers--; }
static void cn1SatbEnqueueRangeLocked(JAVA_ARRAY_OBJECT* data, int count) {
    assert(writers == 1); logged += count;
}
#define calloc checkedCalloc
#define realloc checkedRealloc
''' + header[header.index('struct CN1StackBuffer {'):header.index('// indicates a try/catch', header.index('struct CN1StackBuffer {'))] + '''
struct ThreadLocalData { struct CN1StackBuffer* nativeBuffers; };
void cn1RefBlockFree(JAVA_LONG block);
''' + header[header.index('static inline void cn1StackBufferReset'):header.index('// All standalone buffers', header.index('static inline void cn1StackBufferReset'))] + header[header_start:header_end] + '\n' + source[start:end] + source[end:source.index('// A precisely reached owner', end)] + r'''
#undef calloc
#undef realloc
static void cn1NoteNativeAllocation(size_t bytes) { allocated += bytes; }
int main(void) {
    size_t live, retired, released;
    assert(cn1RefBlockAlloc(0) == 0);
    JAVA_LONG first = cn1RefBlockAlloc(17);
    assert(first != 0 && cn1RefBlockCount(first) == 17);
    for(int i = 0; i < 17; i++) assert(((void**)(uintptr_t)first)[i] == NULL);
    cn1RefBlockBeginCycle();
    cn1RefBlockRetire(first);
    cn1NativeBlockAccounting(&live, &retired, &released);
    assert(live == retired && live > 17 * sizeof(void*) && released == 0);
    // A marker that already read the old pointer can still access the allocation.
    assert(cn1RefBlockCount(first) == 17);
    cn1RefBlockEndCycle();
    cn1NativeBlockAccounting(&live, &retired, &released);
    assert(live == 0 && retired == 0 && released == allocated);
    JAVA_LONG second = cn1IntBlockAlloc(19);
    assert(cn1RefBlockCount(second) == 19);
    for(int i = 0; i < 19; i++) assert(((int*)(uintptr_t)second)[i] == 0);
    cn1RefBlockRetire(second);
    cn1NativeBlockAccounting(&live, &retired, &released);
    assert(live == 0 && retired == 0 && released == allocated);
    for(int ordered = 0; ordered < 2; ordered++) {
        for(int capacity = 1; capacity < 66; capacity++) {
            JAVA_LONG table = cn1TableAlloc(capacity, ordered);
            assert(table != 0 && cn1TablePart(table, 0) == table);
            for(int part = 0; part < (ordered ? 5 : 3); part++) {
                JAVA_LONG slice = cn1TablePart(table, part);
                assert(cn1RefBlockCount(slice) == capacity);
                assert((uintptr_t)slice % 16 == 0);
                if(part < 2) {
                    for(int i = 0; i < capacity; i++) assert(((void**)(uintptr_t)slice)[i] == NULL);
                    ((void**)(uintptr_t)slice)[capacity - 1] = (void*)(uintptr_t)(part + 1);
                } else {
                    for(int i = 0; i < capacity; i++) assert(((int*)(uintptr_t)slice)[i] == 0);
                    ((int*)(uintptr_t)slice)[capacity - 1] = part;
                }
            }
            JAVA_LONG values = cn1TablePart(table, 1);
            cn1RefBlockBeginCycle(); cn1RefBlockRetire(table);
            assert(((void**)(uintptr_t)values)[capacity - 1] == (void*)2);
            cn1RefBlockEndCycle();
        }
    }
    for(int active = 0; active < 2; active++) {
        barrierActive = active;
        JAVA_LONG buffer = cn1RefBlockAlloc(4);
        for(int i = 0; i < 4; i++) ((void**)(uintptr_t)buffer)[i] = (void*)(uintptr_t)(i + 1);
        cn1RefBlockMove(NULL, buffer, 0, 1, 3);
        assert(writers == 0);
        assert(((void**)(uintptr_t)buffer)[3] == (void*)3);
        cn1RefBlockClear(NULL, buffer, 1, 2);
        assert(writers == 0);
        assert(((void**)(uintptr_t)buffer)[1] == NULL);
        assert(((void**)(uintptr_t)buffer)[2] == NULL);
        cn1RefBlockFree(buffer);
    }
    assert(logged == 8);
    struct ThreadLocalData thread = {0};
    struct CN1StackBuffer outer = {0}, inner = {0};
    outer.thread = inner.thread = &thread;
    outer.owned = cn1PrimitiveBlockResize(0, 1024);
    inner.owned = cn1PrimitiveBlockResize(0, 8192);
    inner.previous = &outer;
    thread.nativeBuffers = &inner;
    cn1StackBufferUnwind(&thread, &outer);
    assert(thread.nativeBuffers == &outer && inner.owned == 0 && outer.owned != 0);
    cn1StackBufferLeave(&outer);
    assert(thread.nativeBuffers == NULL && outer.owned == 0);
    cn1NativeBlockAccounting(&live, &retired, &released);
    assert(live == 0 && released == allocated);

    JAVA_LONG primitive = cn1PrimitiveBlockResize(0, 127);
    assert(primitive && (uintptr_t)primitive % 16 == 0);
    for(int i = 0; i < 127; i++) ((unsigned char*)(uintptr_t)primitive)[i] = (unsigned char)i;
    primitive = cn1PrimitiveBlockResize(primitive, 4097);
    assert(primitive && cn1RefBlockCount(primitive) == 4097 && (uintptr_t)primitive % 16 == 0);
    for(int i = 0; i < 127; i++) assert(((unsigned char*)(uintptr_t)primitive)[i] == (unsigned char)i);
    failAllocation = 1;
    assert(cn1PrimitiveBlockResize(primitive, 8193) == 0);
    assert(cn1RefBlockCount(primitive) == 4097);
    failAllocation = 0;
    primitive = cn1PrimitiveBlockResize(primitive, 65);
    assert(cn1RefBlockCount(primitive) == 65);
    for(int i = 0; i < 65; i++) assert(((unsigned char*)(uintptr_t)primitive)[i] == (unsigned char)i);
    assert(cn1PrimitiveBlockResize(primitive, 0) == 0);
    cn1NativeBlockAccounting(&live, &retired, &released);
    assert(live == 0 && retired == 0 && released == allocated);
    failAllocation = 1;
    assert(cn1RefBlockAlloc(5) == 0);
    assert(cn1TableAlloc(5, 1) == 0);
    failAllocation = 0;
    assert(cn1BlockAlloc(2, SIZE_MAX) == 0);
    for(int cycle = 0; cycle < 100; cycle++) {
        cn1RefBlockBeginCycle();
        for(int n = 1; n < 100; n++) cn1RefBlockRetire(cn1RefBlockAlloc(n));
        // Retirement itself must never allocate, even when allocation is failing.
        JAVA_LONG last = cn1RefBlockAlloc(7);
        failAllocation = 1;
        cn1RefBlockRetire(last);
        cn1RefBlockEndCycle();
        failAllocation = 0;
    }
    cn1NativeBlockAccounting(&live, &retired, &released);
    assert(live == 0 && retired == 0 && released == allocated);
    return 0;
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'test.c').write_text(program)
            for standard in ('c99', 'c11'):
                subprocess.run(['clang', '-std=' + standard, '-O2', '-g', '-fsanitize=address,undefined',
                                '-pthread', str(root / 'test.c'), '-o', str(root / 'test')], check=True)
                subprocess.run([str(root / 'test')], check=True)


if __name__ == '__main__':
    unittest.main()
