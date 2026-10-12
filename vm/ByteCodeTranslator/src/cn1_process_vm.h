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
/* ParparVM bindings for the shared Codename One process adapter.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0 */
#ifndef CN1_PROCESS_VM_H
#define CN1_PROCESS_VM_H
#if __has_include("com_codename1_impl_process_NativeProcessSupport.h")
#include "com_codename1_impl_process_NativeProcessSupport.h"
#include "java_lang_String.h"
#if defined(__APPLE__)
#include <TargetConditionals.h>
#endif
#if !defined(__APPLE__) || !TARGET_OS_IPHONE
#include "cn1_process.h"

JAVA_VOID com_codename1_impl_process_NativeProcessSupport_pauseImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG millis) {
#if defined(CN1_VIRTUAL_THREADS) && CN1_VIRTUAL_THREADS
    /* The backend owns the timer/poller registration. No Java monitor is held
       here, so it can park this task and run another on the same host. */
    extern int cn1BackendVtWait(int, const int *, const int *, long long);
    CN1_YIELD_THREAD;
    int parked = cn1BackendVtWait(0, NULL, NULL, millis);
    CN1_RESUME_THREAD;
    if (parked) return;
#endif
    CN1_YIELD_THREAD;
#ifdef _WIN32
    Sleep((DWORD)millis);
#else
    usleep((unsigned int)millis * 1000);
#endif
    CN1_RESUME_THREAD;
}

static void cn1p_free_strings(char **strings) {
    int i;
    if (!strings) return;
    for (i = 0; strings[i]; ++i) free(strings[i]);
    free(strings);
}
static char **cn1p_strings(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) {
    int i, size = ((JAVA_ARRAY)array)->length;
    char **out = (char **)calloc((size_t)size + 1, sizeof(char *));
    if (!out) return NULL;
    for (i = 0; i < size; ++i) {
        JAVA_OBJECT value = ((JAVA_ARRAY_OBJECT *)CN1_ARRAY_DATA((JAVA_ARRAY)array))[i];
        out[i] = cn1_strdup(stringToUTF8(threadStateData, value));
        if (!out[i]) { cn1p_free_strings(out); return NULL; }
    }
    return out;
}
JAVA_OBJECT com_codename1_impl_process_NativeProcessSupport_environmentImpl___R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE) {
    int count = 0, i;
    JAVA_OBJECT result;
#ifdef _WIN32
    wchar_t *block = GetEnvironmentStringsW(), *p;
    if (!block) return JAVA_NULL;
    for (p = block; *p; p += wcslen(p) + 1) ++count;
    result = allocArray(threadStateData, count, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    for (p = block, i = 0; *p; p += wcslen(p) + 1, ++i) {
        int size = WideCharToMultiByte(CP_UTF8, 0, p, -1, NULL, 0, NULL, NULL);
        char *utf8 = (char *)malloc((size_t)size);
        if (!utf8) { FreeEnvironmentStringsW(block); return JAVA_NULL; }
        WideCharToMultiByte(CP_UTF8, 0, p, -1, utf8, size, NULL, NULL);
        JAVA_OBJECT entry = newStringFromNative(threadStateData, utf8);
        free(utf8); CN1_SET_ARRAY_ELEMENT_OBJECT(result, i, entry);
    }
    FreeEnvironmentStringsW(block);
#else
    char **env = CN1P_ENVIRON;
    while (env[count]) ++count;
    result = allocArray(threadStateData, count, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    for (i = 0; i < count; ++i) {
        JAVA_OBJECT entry = newStringFromNative(threadStateData, env[i]);
        CN1_SET_ARRAY_ELEMENT_OBJECT(result, i, entry);
    }
#endif
    return result;
}
JAVA_LONG com_codename1_impl_process_NativeProcessSupport_launch___java_lang_String_1ARRAY_java_lang_String_java_lang_String_1ARRAY_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT command, JAVA_OBJECT directory, JAVA_OBJECT environment, JAVA_BOOLEAN merge) {
    char **args = cn1p_strings(threadStateData, command);
    char **env = environment == JAVA_NULL ? NULL : cn1p_strings(threadStateData, environment);
    char *cwd = directory == JAVA_NULL ? NULL : cn1_strdup(stringToUTF8(threadStateData, directory));
    cn1p_child *child = NULL;
    int error = ENOMEM;
    if (args && (environment == JAVA_NULL || env) && (directory == JAVA_NULL || cwd)) {
        CN1_YIELD_THREAD;
        child = cn1p_launch(args, cwd, env, merge, &error);
        CN1_RESUME_THREAD;
    }
    cn1p_free_strings(args); cn1p_free_strings(env); free(cwd);
    return child ? (JAVA_LONG)(intptr_t)child : -(JAVA_LONG)error;
}
JAVA_LONG com_codename1_impl_process_NativeProcessSupport_poll___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    cn1p_child *child = (cn1p_child *)(intptr_t)handle;
    return cn1p_poll(child) ? (JAVA_LONG)child->code : INT64_MIN;
}
JAVA_VOID com_codename1_impl_process_NativeProcessSupport_terminate___long_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_BOOLEAN force) {
    cn1p_terminate((cn1p_child *)(intptr_t)handle, force);
}
JAVA_INT com_codename1_impl_process_NativeProcessSupport_transferImpl___long_int_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_INT index, JAVA_OBJECT bytes, JAVA_INT offset, JAVA_INT length) {
    return cn1p_transfer((cn1p_child *)(intptr_t)handle, index,
            (unsigned char *)CN1_ARRAY_DATA((JAVA_ARRAY)bytes) + offset, length);
}
JAVA_VOID com_codename1_impl_process_NativeProcessSupport_closePipeImpl___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_INT index) {
    cn1p_close_pipe((cn1p_child *)(intptr_t)handle, index);
}
JAVA_VOID com_codename1_impl_process_NativeProcessSupport_release___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    cn1p_release((cn1p_child *)(intptr_t)handle);
}
JAVA_VOID com_codename1_impl_process_NativeProcessSupport_abandon___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    cn1p_child *child = (cn1p_child *)(intptr_t)handle;
    CN1_YIELD_THREAD;
    cn1p_terminate(child, 1);
    while (!cn1p_poll(child)) {
#ifdef _WIN32
        Sleep(1);
#else
        usleep(1000);
#endif
    }
    cn1p_release(child);
    CN1_RESUME_THREAD;
}
#else /* iOS: keep the shared class linkable without referencing forbidden APIs. */
JAVA_OBJECT com_codename1_impl_process_NativeProcessSupport_environmentImpl___R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE) {
    return JAVA_NULL;
}
JAVA_LONG com_codename1_impl_process_NativeProcessSupport_launch___java_lang_String_1ARRAY_java_lang_String_java_lang_String_1ARRAY_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT command, JAVA_OBJECT directory, JAVA_OBJECT environment, JAVA_BOOLEAN merge) {
    (void)command; (void)directory; (void)environment; (void)merge; return -ENOSYS;
}
JAVA_VOID com_codename1_impl_process_NativeProcessSupport_pauseImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG millis) { (void)millis; }
JAVA_LONG com_codename1_impl_process_NativeProcessSupport_poll___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) { (void)handle; return 0; }
JAVA_VOID com_codename1_impl_process_NativeProcessSupport_terminate___long_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_BOOLEAN force) { (void)handle; (void)force; }
JAVA_INT com_codename1_impl_process_NativeProcessSupport_transferImpl___long_int_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_INT index, JAVA_OBJECT bytes, JAVA_INT offset, JAVA_INT length) {
    (void)handle; (void)index; (void)bytes; (void)offset; (void)length; return -2;
}
JAVA_VOID com_codename1_impl_process_NativeProcessSupport_closePipeImpl___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_INT index) { (void)handle; (void)index; }
JAVA_VOID com_codename1_impl_process_NativeProcessSupport_release___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) { (void)handle; }
JAVA_VOID com_codename1_impl_process_NativeProcessSupport_abandon___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) { (void)handle; }
#endif
#endif
#endif
