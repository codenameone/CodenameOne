#!/usr/bin/env python3
"""Check native environment allocation guards with the actual binding/decoders.

Model a precise-GC allocation boundary and array publication, including nested
string-helper guards and Windows allocation failures after partial population.
"""
import os
from pathlib import Path
import shlex
import subprocess
import tempfile
import unittest

from test_windows_clock import function, RUNTIME


class EnvironmentAllocationsTest(unittest.TestCase):
    def test_guards_and_cleanup(self):
        source = r'''
#include <assert.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <wchar.h>
#define JAVA_OBJECT void *
#define JAVA_ARRAY_CHAR uint16_t
#define JAVA_BOOLEAN int
#define JAVA_TRUE 1
#define JAVA_FALSE 0
#define JAVA_NULL NULL
struct state { int nativeAllocationMode; };
#define CODENAME_ONE_THREAD_STATE struct state *threadStateData
static const char *cn1LastNamSetter;
'''
        header = (RUNTIME / "cn1_globals.h").read_text()
        for macro in ("enteringNativeAllocations", "finishedNativeAllocations"):
            prefix = "#define " + macro + "() do { threadStateData"
            start = header.index(prefix)
            source += header[start:header.index("\n", start)] + "\n"
        source += r'''
struct array { int length; void **items; };
static struct array *allocated;
static int class_array1__java_lang_String;
static JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int count, void *clazz, size_t size, int dimensions) {
    assert(threadStateData->nativeAllocationMode);
    (void)clazz; (void)size; (void)dimensions;
    allocated = calloc(1, sizeof(*allocated));
    allocated->length = count; allocated->items = calloc((size_t)count + 1, sizeof(void *));
    return allocated;
}
static void setEntry(CODENAME_ONE_THREAD_STATE, struct array *a, int i, void *value) {
    assert(threadStateData->nativeAllocationMode);
    a->items[i] = value;
}
#define CN1_SET_ARRAY_ELEMENT_OBJECT(a, i, v) setEntry(threadStateData, a, i, v)
static JAVA_OBJECT cn1StringFromUnits(CODENAME_ONE_THREAD_STATE, const JAVA_ARRAY_CHAR *units, int length) {
    assert(threadStateData->nativeAllocationMode);
    JAVA_ARRAY_CHAR *result = calloc((size_t)length + 1, sizeof(*result));
    memcpy(result, units, (size_t)length * sizeof(*result));
    return result;
}
JAVA_OBJECT newStringFromUtf8Len(CODENAME_ONE_THREAD_STATE, const char *str, int length);
'''
        for signature in ("JAVA_OBJECT newStringFromUtf8Len(", "JAVA_OBJECT newStringFromNative("):
            source += "\n" + function(RUNTIME / "cn1_globals.m", signature)
        source += r'''
#ifdef TEST_WINDOWS
#define _WIN32 1
#define CP_UTF8 65001
static int empty, failBlock, blocks, failAfter = -1;
static wchar_t *GetEnvironmentStringsW(void) {
    if (failBlock) return NULL;
    ++blocks;
    return empty ? L"\0" : L"FIRST=one\0SECOND=two\0";
}
static void FreeEnvironmentStringsW(wchar_t *block) { (void)block; --blocks; }
static int WideCharToMultiByte(int cp, int flags, const wchar_t *input, int length,
        char *out, int size, void *fallback, void *used) {
    (void)flags; (void)length; (void)fallback; (void)used; assert(cp == CP_UTF8);
    int n = (int)wcslen(input) + 1;
    if (out) { assert(size == n); for (int i = 0; i < n; ++i) out[i] = (char)input[i]; }
    return n;
}
static void *temporaryAllocation(size_t size) {
    if (failAfter == 0) return NULL;
    if (failAfter > 0) --failAfter;
    return malloc(size);
}
#define malloc temporaryAllocation
#else
static char *entries[] = { "FIRST=one", "SECOND=two", NULL };
static char **CN1P_ENVIRON = entries;
#endif
'''
        source += "\n" + function(RUNTIME / "cn1_process_vm.h",
                                 "JAVA_OBJECT com_codename1_impl_process_NativeProcessSupport_environmentImpl_")
        source += r'''
static void run(int initialMode, int expectNull, int count) {
    struct state state = { initialMode };
    struct array *result = com_codename1_impl_process_NativeProcessSupport_environmentImpl___R_java_lang_String_1ARRAY(&state);
    assert(state.nativeAllocationMode == initialMode);
    assert((result == NULL) == expectNull);
    if (result) {
        assert(result->length == count);
        for (int i = 0; i < count; ++i) assert(result->items[i] != NULL);
    }
    if (allocated) {
        for (int i = 0; i < allocated->length; ++i) free(allocated->items[i]);
        free(allocated->items); free(allocated); allocated = NULL;
    }
#ifdef TEST_WINDOWS
    assert(blocks == 0);
#endif
}
int main(void) {
    for (int initial = 0; initial < 2; ++initial) {
        run(initial, 0, 2);
#ifdef TEST_WINDOWS
        failAfter = 0; run(initial, 1, 0);
        failAfter = 1; run(initial, 1, 0);
        failAfter = -1;
        failBlock = 1; run(initial, 1, 0); failBlock = 0;
        empty = 1; run(initial, 0, 0); empty = 0;
#else
        CN1P_ENVIRON = entries + 2; run(initial, 0, 0); CN1P_ENVIRON = entries;
#endif
    }
    return 0;
}
'''
        with tempfile.TemporaryDirectory(prefix="cn1-environment-guards-") as directory:
            test = Path(directory) / "environment.c"
            test.write_text(source)
            for platform in ("posix", "windows"):
                with self.subTest(platform=platform):
                    binary = Path(directory) / platform
                    flags = ["-DTEST_WINDOWS"] if platform == "windows" else []
                    subprocess.run(shlex.split(os.environ.get("CC", "cc")) + flags +
                                   ["-std=c99", "-Wall", "-Wextra", "-Werror", str(test), "-o", str(binary)], check=True)
                    subprocess.run([str(binary)], check=True)


if __name__ == "__main__":
    unittest.main()
