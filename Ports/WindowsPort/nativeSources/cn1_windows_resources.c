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
 * Classpath-resource access for the native Windows port. The ParparVM "windows"
 * target embeds the app's classpath resources (theme.res, images, l10n, ...)
 * into the executable's PE resource section as RCDATA blobs and emits a
 * name -> id lookup (cn1WinFindResourceId, in the generated cn1_resources_table.c).
 * getResourceAsStream("/theme.res") resolves the id and returns the embedded
 * bytes, so a single self-contained .exe carries its resources the way an iOS
 * .app bundle does -- there is no bundle directory on Windows to sit them beside.
 */

#include "cn1_windows.h"

#ifdef _WIN32

/*
 * Provided by the translator-generated cn1_resources_table.c (always emitted for
 * the windows target, even when the app has no resources -- the table is then
 * empty and this returns 0). Maps a classpath path ("/theme.res") to the RCDATA
 * integer id the resource script assigned it.
 */
extern int cn1WinFindResourceId(const char* name);

/*
 * Returns the embedded resource named by the (classpath-style) String as a Java
 * byte[], or null when there is no such embedded resource. The Java side wraps a
 * non-null result in a ByteArrayInputStream; a null lets it fall back to a file
 * shipped next to the exe (dev/debug convenience).
 */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_resourceBytes___java_lang_String_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    if (__cn1Arg1 == JAVA_NULL) {
        return JAVA_NULL;
    }

    UINT32 wlen = 0;
    WCHAR* wname = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &wlen);
    if (wname == NULL) {
        return JAVA_NULL;
    }
    /* Resource paths are ASCII; convert to UTF-8 to match the generated table. */
    char nameBuf[1024];
    int n = WideCharToMultiByte(CP_UTF8, 0, wname, -1, nameBuf, (int) sizeof(nameBuf), NULL, NULL);
    free(wname);
    if (n <= 0) {
        return JAVA_NULL;
    }

    int id = cn1WinFindResourceId(nameBuf);
    if (id == 0) {
        return JAVA_NULL;
    }

    HMODULE module = GetModuleHandleW(NULL);
    /* RT_RCDATA expands to MAKEINTRESOURCE(10), which is the *narrow* (LPSTR)
     * MAKEINTRESOURCEA when UNICODE is not forced; FindResourceW wants LPCWSTR.
     * MSVC only warns (C4133), but clang-cl (the cross-compile path) errors, so
     * use the wide integer-resource form explicitly. */
    HRSRC info = FindResourceW(module, MAKEINTRESOURCEW(id), (LPCWSTR) RT_RCDATA);
    if (info == NULL) {
        return JAVA_NULL;
    }
    DWORD size = SizeofResource(module, info);
    HGLOBAL loaded = LoadResource(module, info);
    if (loaded == NULL || size == 0) {
        return JAVA_NULL;
    }
    void* data = LockResource(loaded);
    if (data == NULL) {
        return JAVA_NULL;
    }

    JAVA_OBJECT result = allocArray(threadStateData, (int) size,
            &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if (result != JAVA_NULL) {
        memcpy((*(JAVA_ARRAY) result).data, data, size);
    }
    return result;
}

#endif /* _WIN32 */
