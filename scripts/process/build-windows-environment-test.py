#!/usr/bin/env python3
"""Build a Win32 regression for the actual environment and string native bindings.

Run the resulting executable on Windows (or Wine with a non-UTF-8 active code page).
Usage: python3 scripts/process/build-windows-environment-test.py OUTPUT.exe
Set CC to a Windows C compiler, e.g. x86_64-w64-mingw32-gcc.
"""
import os
from pathlib import Path
import shlex
import subprocess
import sys
import tempfile

from test_windows_clock import function, RUNTIME

source = r'''
#include <windows.h>
#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <wchar.h>
#define JAVA_OBJECT void *
#define JAVA_ARRAY_CHAR uint16_t
#define JAVA_NULL NULL
#define CODENAME_ONE_THREAD_STATE void *threadStateData
#define enteringNativeAllocations() ((void)0)
#define finishedNativeAllocations() ((void)0)
struct array { int length; void **items; };
static int class_array1__java_lang_String;
#define CN1_SET_ARRAY_ELEMENT_OBJECT(a, i, v) (((struct array *)(a))->items[i] = (v))
static JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int count, void *clazz, size_t size, int dimensions) {
    (void)threadStateData; (void)clazz; (void)size; (void)dimensions;
    struct array *a = calloc(1, sizeof(*a));
    a->length = count; a->items = calloc((size_t)count, sizeof(void *));
    return a;
}
static JAVA_OBJECT cn1StringFromUnits(CODENAME_ONE_THREAD_STATE, const JAVA_ARRAY_CHAR *units, int length) {
    (void)threadStateData;
    JAVA_ARRAY_CHAR *result = calloc((size_t)length + 1, sizeof(*result));
    memcpy(result, units, (size_t)length * sizeof(*result));
    return result;
}
JAVA_OBJECT newStringFromUtf8Len(CODENAME_ONE_THREAD_STATE, const char *str, int length);
'''
for signature in ("JAVA_OBJECT newStringFromUtf8Len(", "JAVA_OBJECT newStringFromNative("):
    source += "\n" + function(RUNTIME / "cn1_globals.m", signature)
source += "\n" + function(RUNTIME / "cn1_process_vm.h",
                         "JAVA_OBJECT com_codename1_impl_process_NativeProcessSupport_environmentImpl_")
source += r'''
int main(void) {
    /* Include non-ASCII names, BMP values, and a surrogate pair. */
    const wchar_t *name = L"CN1_INHERITED_\u05e9";
    const wchar_t *value = L"caf\u00e9-\u20ac-\U0001f642";
    const wchar_t *entry = L"CN1_INHERITED_\u05e9=caf\u00e9-\u20ac-\U0001f642";
    int found = 0;
    /* Ensure the regression actually distinguishes UTF-8 from the active code page. */
    assert(GetACP() != CP_UTF8);
    assert(SetEnvironmentVariableW(name, value));
    struct array *env = com_codename1_impl_process_NativeProcessSupport_environmentImpl___R_java_lang_String_1ARRAY(NULL);
    assert(env != NULL);
    for (int i = 0; i < env->length; ++i) {
        if (!wcscmp((wchar_t *)env->items[i], entry)) found = 1;
        free(env->items[i]);
    }
    free(env->items); free(env);
    assert(SetEnvironmentVariableW(name, NULL));
    if (!found) { fputs("Unicode environment entry was corrupted\n", stderr); return 1; }
    puts("WINDOWS_ENVIRONMENT_UTF8_OK");
    return 0;
}
'''
with tempfile.TemporaryDirectory(prefix="cn1-windows-environment-") as directory:
    c_file = Path(directory) / "environment.c"
    c_file.write_text(source)
    subprocess.run(shlex.split(os.environ.get("CC", "x86_64-w64-mingw32-gcc")) +
                   ["-std=c99", "-Wall", "-Wextra", "-Werror", str(c_file), "-o", sys.argv[1]], check=True)
