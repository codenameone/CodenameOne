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
 * Native filesystem, storage, drive enumeration and clipboard services for the
 * Codename One Windows (Win32) port. All paths cross the bridge as Java Strings
 * and are converted to UTF-16 via cn1WinJavaStringToWide so the wide Win32 APIs
 * (CreateFileW, FindFirstFileW, ...) are used throughout and Unicode paths work.
 * File handles are passed back to Java as (JAVA_LONG)(intptr_t)HANDLE; 0 means
 * failure.
 */

#ifdef _WIN32

#include "cn1_windows.h"
#include <shlobj.h>
#include <stdio.h>

/*
 * The element class for a String[] in generated ParparVM code is
 * class_array1__java_lang_String (verified in vm/.../cn1_globals.m initConstantPool,
 * which builds the String[] constant pool with
 *   allocArray(threadStateData, n, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1)).
 * It is not declared in cn1_globals.h, so we forward-declare it here; this should
 * be verified at the first Windows build.
 */
extern struct clazz class_array1__java_lang_String;

/* ------------------------------------------------------------ small helpers */

/*
 * Convert a freshly produced wide string into a Java String. Returns JAVA_NULL
 * on conversion failure. The caller still owns (and must free) the WCHAR* input.
 */
static JAVA_OBJECT cn1WinWideToJavaString(CODENAME_ONE_THREAD_STATE, const WCHAR* wide) {
    int needed;
    char* utf8;
    JAVA_OBJECT result;
    if (wide == NULL) {
        return JAVA_NULL;
    }
    needed = WideCharToMultiByte(CP_UTF8, 0, wide, -1, NULL, 0, NULL, NULL);
    if (needed <= 0) {
        return JAVA_NULL;
    }
    utf8 = (char*)malloc((size_t)needed);
    if (utf8 == NULL) {
        return JAVA_NULL;
    }
    if (WideCharToMultiByte(CP_UTF8, 0, wide, -1, utf8, needed, NULL, NULL) <= 0) {
        free(utf8);
        return JAVA_NULL;
    }
    result = newStringFromCString(threadStateData, utf8);
    free(utf8);
    return result;
}

/* ------------------------------------------------------------------- files */

JAVA_LONG com_codename1_impl_windows_WindowsNative_fileOpenRead___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    HANDLE h;
    if (path == NULL) {
        return 0;
    }
    h = CreateFileW(path, GENERIC_READ, FILE_SHARE_READ, NULL,
                    OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    free(path);
    if (h == INVALID_HANDLE_VALUE) {
        return 0;
    }
    return (JAVA_LONG)(intptr_t)h;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_fileOpenWrite___java_lang_String_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_BOOLEAN __cn1Arg2) {
    UINT32 len = 0;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    HANDLE h;
    if (path == NULL) {
        return 0;
    }
    if (__cn1Arg2) {
        /* append: open or create, then seek to end */
        h = CreateFileW(path, GENERIC_WRITE, FILE_SHARE_READ, NULL,
                        OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
        if (h != INVALID_HANDLE_VALUE) {
            SetFilePointer(h, 0, NULL, FILE_END);
        }
    } else {
        /* truncate: CREATE_ALWAYS discards any existing content */
        h = CreateFileW(path, GENERIC_WRITE, FILE_SHARE_READ, NULL,
                        CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    }
    free(path);
    if (h == INVALID_HANDLE_VALUE) {
        return 0;
    }
    return (JAVA_LONG)(intptr_t)h;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_fileRead___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    HANDLE h = (HANDLE)(intptr_t)__cn1Arg1;
    char* data;
    DWORD readBytes = 0;
    BOOL ok;
    if (h == NULL || h == INVALID_HANDLE_VALUE || __cn1Arg2 == JAVA_NULL || __cn1Arg4 <= 0) {
        return -1;
    }
    data = (char*)(*(JAVA_ARRAY)__cn1Arg2).data;
    ok = ReadFile(h, data + __cn1Arg3, (DWORD)__cn1Arg4, &readBytes, NULL);
    if (!ok) {
        return -1;
    }
    /* 0 bytes read at this point means EOF; Java InputStream contract wants -1 */
    if (readBytes == 0) {
        return -1;
    }
    return (JAVA_INT)readBytes;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_fileWrite___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    HANDLE h = (HANDLE)(intptr_t)__cn1Arg1;
    char* data;
    DWORD written = 0;
    if (h == NULL || h == INVALID_HANDLE_VALUE || __cn1Arg2 == JAVA_NULL || __cn1Arg4 <= 0) {
        return 0;
    }
    data = (char*)(*(JAVA_ARRAY)__cn1Arg2).data;
    if (!WriteFile(h, data + __cn1Arg3, (DWORD)__cn1Arg4, &written, NULL)) {
        return 0;
    }
    return (JAVA_INT)written;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_fileClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    HANDLE h = (HANDLE)(intptr_t)__cn1Arg1;
    if (h != NULL && h != INVALID_HANDLE_VALUE) {
        CloseHandle(h);
    }
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_fileExists___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    DWORD attrs;
    if (path == NULL) {
        return JAVA_FALSE;
    }
    attrs = GetFileAttributesW(path);
    free(path);
    return (attrs != INVALID_FILE_ATTRIBUTES) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_fileIsDirectory___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    DWORD attrs;
    if (path == NULL) {
        return JAVA_FALSE;
    }
    attrs = GetFileAttributesW(path);
    free(path);
    if (attrs == INVALID_FILE_ATTRIBUTES) {
        return JAVA_FALSE;
    }
    return (attrs & FILE_ATTRIBUTE_DIRECTORY) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_fileLength___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    WIN32_FILE_ATTRIBUTE_DATA fad;
    JAVA_LONG result;
    if (path == NULL) {
        return 0;
    }
    if (!GetFileAttributesExW(path, GetFileExInfoStandard, &fad)) {
        free(path);
        return 0;
    }
    free(path);
    result = ((JAVA_LONG)fad.nFileSizeHigh << 32) | (JAVA_LONG)fad.nFileSizeLow;
    return result;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_fileDelete___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    DWORD attrs;
    if (path == NULL) {
        return;
    }
    attrs = GetFileAttributesW(path);
    if (attrs != INVALID_FILE_ATTRIBUTES && (attrs & FILE_ATTRIBUTE_DIRECTORY)) {
        RemoveDirectoryW(path);
    } else {
        DeleteFileW(path);
    }
    free(path);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_fileMkdir___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    if (path == NULL) {
        return;
    }
    CreateDirectoryW(path, NULL);
    free(path);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_fileRename___java_lang_String_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    UINT32 len1 = 0, len2 = 0;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len1);
    /*
     * Per the CN1 FileSystemStorage contract newName is the full target path,
     * so it is passed directly to MoveFileExW. If a caller ever passes a bare
     * leaf name the move would land in the process working directory; that
     * would be a contract violation on the Java side.
     */
    WCHAR* newName = cn1WinJavaStringToWide(threadStateData, __cn1Arg2, &len2);
    if (path != NULL && newName != NULL) {
        MoveFileExW(path, newName, MOVEFILE_REPLACE_EXISTING | MOVEFILE_COPY_ALLOWED);
    }
    if (path != NULL) {
        free(path);
    }
    if (newName != NULL) {
        free(newName);
    }
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_fileList___java_lang_String_R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* dir = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    WCHAR* pattern;
    size_t patternLen;
    WIN32_FIND_DATAW fd;
    HANDLE find;
    /* dynamically grown list of malloc'd Java String references */
    JAVA_OBJECT* names = NULL;
    int count = 0;
    int capacity = 0;
    JAVA_OBJECT arr;
    int i;

    if (dir == NULL) {
        return JAVA_NULL;
    }

    /* build "<dir>\*" search pattern; +3 covers a separator, '*' and NUL */
    patternLen = (size_t)len + 3;
    pattern = (WCHAR*)malloc(patternLen * sizeof(WCHAR));
    if (pattern == NULL) {
        free(dir);
        return JAVA_NULL;
    }
    if (len > 0 && (dir[len - 1] == L'\\' || dir[len - 1] == L'/')) {
        _snwprintf(pattern, patternLen, L"%s*", dir);
    } else {
        _snwprintf(pattern, patternLen, L"%s\\*", dir);
    }
    free(dir);

    find = FindFirstFileW(pattern, &fd);
    free(pattern);
    if (find == INVALID_HANDLE_VALUE) {
        DWORD err = GetLastError();
        /* an empty directory yields ERROR_FILE_NOT_FOUND: return empty array */
        if (err == ERROR_FILE_NOT_FOUND || err == ERROR_NO_MORE_FILES) {
            return allocArray(threadStateData, 0, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
        }
        return JAVA_NULL;
    }

    do {
        JAVA_OBJECT str;
        if (wcscmp(fd.cFileName, L".") == 0 || wcscmp(fd.cFileName, L"..") == 0) {
            continue;
        }
        str = cn1WinWideToJavaString(threadStateData, fd.cFileName);
        if (str == JAVA_NULL) {
            continue;
        }
        if (count == capacity) {
            int newCap = (capacity == 0) ? 16 : capacity * 2;
            JAVA_OBJECT* grown = (JAVA_OBJECT*)realloc(names, (size_t)newCap * sizeof(JAVA_OBJECT));
            if (grown == NULL) {
                /* out of memory: abandon, return what we cannot safely keep */
                free(names);
                FindClose(find);
                return JAVA_NULL;
            }
            names = grown;
            capacity = newCap;
        }
        names[count++] = str;
    } while (FindNextFileW(find, &fd));

    FindClose(find);

    /* allocate the Java String[] and copy collected references in */
    arr = allocArray(threadStateData, count, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    if (arr != JAVA_NULL) {
        JAVA_OBJECT* elements = (JAVA_OBJECT*)(*(JAVA_ARRAY)arr).data;
        for (i = 0; i < count; i++) {
            elements[i] = names[i];
        }
    }
    free(names);
    return arr;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_storageDir___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    WCHAR base[MAX_PATH];
    WCHAR dir[MAX_PATH];
    if (SHGetFolderPathW(NULL, CSIDL_LOCAL_APPDATA, NULL, SHGFP_TYPE_CURRENT, base) != S_OK) {
        return JAVA_NULL;
    }
    _snwprintf(dir, MAX_PATH, L"%s\\CodenameOne", base);
    dir[MAX_PATH - 1] = L'\0';
    /* ensure the directory exists; ignore "already exists" */
    CreateDirectoryW(dir, NULL);
    return cn1WinWideToJavaString(threadStateData, dir);
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_fileRoots___R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE) {
    /* GetLogicalDriveStringsW fills a NUL-separated, double-NUL-terminated list */
    DWORD needed = GetLogicalDriveStringsW(0, NULL);
    WCHAR* buffer;
    DWORD written;
    WCHAR* p;
    JAVA_OBJECT* names = NULL;
    int count = 0;
    int capacity = 0;
    JAVA_OBJECT arr;
    int i;

    if (needed == 0) {
        return JAVA_NULL;
    }
    buffer = (WCHAR*)malloc((size_t)needed * sizeof(WCHAR));
    if (buffer == NULL) {
        return JAVA_NULL;
    }
    written = GetLogicalDriveStringsW(needed, buffer);
    if (written == 0 || written >= needed) {
        free(buffer);
        return JAVA_NULL;
    }

    for (p = buffer; *p != L'\0'; p += wcslen(p) + 1) {
        JAVA_OBJECT str = cn1WinWideToJavaString(threadStateData, p);
        if (str == JAVA_NULL) {
            continue;
        }
        if (count == capacity) {
            int newCap = (capacity == 0) ? 8 : capacity * 2;
            JAVA_OBJECT* grown = (JAVA_OBJECT*)realloc(names, (size_t)newCap * sizeof(JAVA_OBJECT));
            if (grown == NULL) {
                free(names);
                free(buffer);
                return JAVA_NULL;
            }
            names = grown;
            capacity = newCap;
        }
        names[count++] = str;
    }
    free(buffer);

    arr = allocArray(threadStateData, count, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    if (arr != JAVA_NULL) {
        JAVA_OBJECT* elements = (JAVA_OBJECT*)(*(JAVA_ARRAY)arr).data;
        for (i = 0; i < count; i++) {
            elements[i] = names[i];
        }
    }
    free(names);
    return arr;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_fileRootSize___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* root = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    ULARGE_INTEGER freeToCaller, totalBytes, totalFree;
    if (root == NULL) {
        return 0;
    }
    if (!GetDiskFreeSpaceExW(root, &freeToCaller, &totalBytes, &totalFree)) {
        free(root);
        return 0;
    }
    free(root);
    return (JAVA_LONG)totalBytes.QuadPart;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_fileRootFree___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* root = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    ULARGE_INTEGER freeToCaller, totalBytes, totalFree;
    if (root == NULL) {
        return 0;
    }
    if (!GetDiskFreeSpaceExW(root, &freeToCaller, &totalBytes, &totalFree)) {
        free(root);
        return 0;
    }
    free(root);
    /* free bytes available to the calling user (honours quotas) */
    return (JAVA_LONG)freeToCaller.QuadPart;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_fileIsHidden___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    DWORD attrs;
    if (path == NULL) {
        return JAVA_FALSE;
    }
    attrs = GetFileAttributesW(path);
    free(path);
    if (attrs == INVALID_FILE_ATTRIBUTES) {
        return JAVA_FALSE;
    }
    return (attrs & FILE_ATTRIBUTE_HIDDEN) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_fileSetHidden___java_lang_String_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_BOOLEAN __cn1Arg2) {
    UINT32 len = 0;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    DWORD attrs;
    if (path == NULL) {
        return;
    }
    attrs = GetFileAttributesW(path);
    if (attrs == INVALID_FILE_ATTRIBUTES) {
        free(path);
        return;
    }
    if (__cn1Arg2) {
        attrs |= FILE_ATTRIBUTE_HIDDEN;
    } else {
        attrs &= ~FILE_ATTRIBUTE_HIDDEN;
    }
    SetFileAttributesW(path, attrs);
    free(path);
}

/* --------------------------------------------------------------- clipboard */

JAVA_VOID com_codename1_impl_windows_WindowsNative_clipboardSetText___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    UINT32 len = 0;
    WCHAR* text = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &len);
    size_t bytes;
    HGLOBAL mem;
    void* dst;
    if (text == NULL) {
        return;
    }
    if (!OpenClipboard(cn1Win.hwnd)) {
        free(text);
        return;
    }
    EmptyClipboard();
    /* CF_UNICODETEXT requires a NUL-terminated WCHAR block in moveable memory */
    bytes = ((size_t)len + 1) * sizeof(WCHAR);
    mem = GlobalAlloc(GMEM_MOVEABLE, bytes);
    if (mem == NULL) {
        CloseClipboard();
        free(text);
        return;
    }
    dst = GlobalLock(mem);
    if (dst == NULL) {
        GlobalFree(mem);
        CloseClipboard();
        free(text);
        return;
    }
    memcpy(dst, text, bytes);
    GlobalUnlock(mem);
    if (SetClipboardData(CF_UNICODETEXT, mem) == NULL) {
        /* ownership only transfers on success; free on failure */
        GlobalFree(mem);
    }
    CloseClipboard();
    free(text);
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_clipboardGetText___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    HANDLE data;
    const WCHAR* wide;
    JAVA_OBJECT result;
    if (!OpenClipboard(cn1Win.hwnd)) {
        return JAVA_NULL;
    }
    data = GetClipboardData(CF_UNICODETEXT);
    if (data == NULL) {
        CloseClipboard();
        return JAVA_NULL;
    }
    wide = (const WCHAR*)GlobalLock(data);
    if (wide == NULL) {
        CloseClipboard();
        return JAVA_NULL;
    }
    result = cn1WinWideToJavaString(threadStateData, wide);
    GlobalUnlock(data);
    CloseClipboard();
    return result;
}

#endif /* _WIN32 */
