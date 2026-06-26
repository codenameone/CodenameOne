/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
 * Windows-only glue for the simulator dll: symbols the port sources expect
 * from the translated executable environment that have no meaning (or a
 * simpler form) under the JVM-hosted simulator.
 */
#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include "cn1jni_runtime.h"

/*
 * String[] allocations from native code (e.g. fileList): handled directly in
 * allocArray via this identity token.
 */
struct clazz class_array1__java_lang_String;

/*
 * Bundled-resource lookup reads the PE resource section of the translated
 * executable; under the simulator resources come from the Java classpath, so
 * the native path reports "not found" and the Java side falls back.
 */
__attribute__((weak)) int cn1WinFindResourceId(const char *name) {
    (void) name;
    return 0;
}

/* the excluded WebView2 browser unit's window-message hook */
__attribute__((weak)) int cn1WinBrowserHandleMessage(void *hwnd, unsigned int msg,
        uintptr_t wParam, intptr_t lParam) {
    (void) hwnd;
    (void) msg;
    (void) wParam;
    (void) lParam;
    return 0;
}

/* POSIX usleep used by a couple of wait loops (declared in cn1_win_compat.h) */
int usleep(unsigned int usec) {
    Sleep(usec / 1000 > 0 ? usec / 1000 : 1);
    return 0;
}

#endif /* _WIN32 */
