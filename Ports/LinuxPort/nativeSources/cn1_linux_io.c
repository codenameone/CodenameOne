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
 * Lifecycle helpers, POSIX filesystem and embedded-resource access for the
 * native Codename One Linux port. This translation unit is pure C + POSIX (no
 * GTK), so it backs the LinuxNative file/storage/lifecycle bridge on any musl or
 * glibc Linux. File handles are FILE* cast to the Java long peer.
 */

#define _GNU_SOURCE
#include "cn1_linux.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>
#include <errno.h>
#include <dirent.h>
#include <time.h>
#include <libgen.h>
#include <sys/stat.h>
#include <sys/statvfs.h>

/* ParparVM runtime symbols (declared in cn1_globals.h, restated for clarity). */
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim);
extern struct clazz class_array1__java_lang_String;
extern struct clazz class_array1__JAVA_BYTE;

/* ----------------------------------------------------------- diagnostics */

void cn1LinuxLog(const char* message) {
    if (message != 0) {
        fputs(message, stderr);
        fputc('\n', stderr);
        fflush(stderr);
    }
}

/* A bounded set of already-reported stub tags so each prints exactly once. */
#define CN1_STUB_MAX 256
static const char* cn1StubSeen[CN1_STUB_MAX];
static int cn1StubCount = 0;

void cn1LinuxStubOnce(const char* tag) {
    int i;
    for (i = 0; i < cn1StubCount; i++) {
        if (cn1StubSeen[i] == tag || (tag && cn1StubSeen[i] && strcmp(cn1StubSeen[i], tag) == 0)) {
            return;
        }
    }
    if (cn1StubCount < CN1_STUB_MAX) {
        cn1StubSeen[cn1StubCount++] = tag;
    }
    fprintf(stderr, "[cn1-linux] capability not yet implemented: %s\n", tag ? tag : "?");
    fflush(stderr);
}

JAVA_OBJECT cn1LinuxNewByteArray(CODENAME_ONE_THREAD_STATE, const void* src, int n) {
    JAVA_OBJECT arr;
    if (n < 0) {
        n = 0;
    }
    arr = allocArray(threadStateData, n, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if (arr != JAVA_NULL && n > 0 && src != 0) {
        memcpy((*(JAVA_ARRAY) arr).data, src, (size_t) n);
    }
    return arr;
}

/* ----------------------------------------------------------- lifecycle */

JAVA_VOID com_codename1_impl_linux_LinuxNative_nativeLog___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT message) {
    cn1LinuxLog(message == JAVA_NULL ? "" : stringToUTF8(threadStateData, message));
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_faultSelfTestEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    const char* v = getenv("CN1_FAULT_SELFTEST");
    return (v != 0 && v[0] != 0) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_sleepMillis___int(CODENAME_ONE_THREAD_STATE, JAVA_INT millis) {
    struct timespec ts;
    if (millis <= 0) {
        return;
    }
    ts.tv_sec = millis / 1000;
    ts.tv_nsec = (long) (millis % 1000) * 1000000L;
    /* Yield to the GC across the sleep so a parked thread can't stall a mark. */
    CN1_YIELD_THREAD;
    nanosleep(&ts, 0);
    CN1_RESUME_THREAD;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_exitProcess___int(CODENAME_ONE_THREAD_STATE, JAVA_INT code) {
    fflush(stderr);
    fflush(stdout);
    _exit((int) code);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_parkMainThread___int(CODENAME_ONE_THREAD_STATE, JAVA_INT timeoutMillis) {
    /* Keep the process alive for up to timeoutMillis while workers run, then
     * force-exit; callers leave early via exitProcess when their work completes. */
    int remaining = timeoutMillis > 0 ? (int) timeoutMillis : 0;
    while (remaining > 0) {
        struct timespec ts;
        int slice = remaining > 100 ? 100 : remaining;
        ts.tv_sec = 0;
        ts.tv_nsec = (long) slice * 1000000L;
        nanosleep(&ts, 0);
        remaining -= slice;
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_runHeadlessLoop__(CODENAME_ONE_THREAD_STATE) {
    /* No window: park the main thread indefinitely while the EDT renders into the
     * offscreen buffer; the EDT exits the process once the screenshot is saved. */
    for (;;) {
        struct timespec ts;
        ts.tv_sec = 1;
        ts.tv_nsec = 0;
        nanosleep(&ts, 0);
    }
}

/* ------------------------------------------------------------- helpers */

static const char* cn1JStr(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s) {
    return s == JAVA_NULL ? 0 : stringToUTF8(threadStateData, s);
}

/* ------------------------------------------------------------ file io */

JAVA_LONG com_codename1_impl_linux_LinuxNative_fileOpenRead___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    const char* p = cn1JStr(threadStateData, path);
    FILE* f = p ? fopen(p, "rb") : 0;
    return (JAVA_LONG) (intptr_t) f;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_fileOpenWrite___java_lang_String_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_BOOLEAN append) {
    const char* p = cn1JStr(threadStateData, path);
    FILE* f = p ? fopen(p, append ? "ab" : "wb") : 0;
    return (JAVA_LONG) (intptr_t) f;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_fileRead___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    FILE* f = (FILE*) (intptr_t) handle;
    char* data;
    size_t n;
    if (f == 0 || buffer == JAVA_NULL || length <= 0) {
        return -1;
    }
    data = (char*) (*(JAVA_ARRAY) buffer).data;
    n = fread(data + offset, 1, (size_t) length, f);
    if (n == 0) {
        return feof(f) ? -1 : -1;
    }
    return (JAVA_INT) n;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_fileWrite___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    FILE* f = (FILE*) (intptr_t) handle;
    char* data;
    size_t n;
    if (f == 0 || buffer == JAVA_NULL || length <= 0) {
        return -1;
    }
    data = (char*) (*(JAVA_ARRAY) buffer).data;
    n = fwrite(data + offset, 1, (size_t) length, f);
    return (JAVA_INT) n;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_fileClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    FILE* f = (FILE*) (intptr_t) handle;
    if (f != 0) {
        fclose(f);
    }
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_fileExists___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    const char* p = cn1JStr(threadStateData, path);
    struct stat st;
    return (p && stat(p, &st) == 0) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_fileIsDirectory___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    const char* p = cn1JStr(threadStateData, path);
    struct stat st;
    return (p && stat(p, &st) == 0 && S_ISDIR(st.st_mode)) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_fileLength___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    const char* p = cn1JStr(threadStateData, path);
    struct stat st;
    return (p && stat(p, &st) == 0) ? (JAVA_LONG) st.st_size : 0;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_fileDelete___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    const char* p = cn1JStr(threadStateData, path);
    if (p) {
        if (remove(p) != 0) {
            rmdir(p);
        }
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_fileMkdir___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    const char* p = cn1JStr(threadStateData, path);
    if (p) {
        mkdir(p, 0755);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_fileRename___java_lang_String_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_OBJECT newName) {
    const char* p = cn1JStr(threadStateData, path);
    const char* n;
    char dir[4096];
    char dest[4096];
    char* slash;
    if (!p || newName == JAVA_NULL) {
        return;
    }
    n = stringToUTF8(threadStateData, newName);
    /* newName is a leaf name; rename within the same parent directory. */
    strncpy(dir, p, sizeof(dir) - 1);
    dir[sizeof(dir) - 1] = 0;
    slash = strrchr(dir, '/');
    if (slash) {
        *slash = 0;
        snprintf(dest, sizeof(dest), "%s/%s", dir, n);
    } else {
        snprintf(dest, sizeof(dest), "%s", n);
    }
    rename(p, dest);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_fileList___java_lang_String_R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    const char* p = cn1JStr(threadStateData, path);
    DIR* d;
    struct dirent* ent;
    char* names[8192];
    int count = 0;
    int i;
    JAVA_OBJECT arr;
    JAVA_OBJECT* elements;
    if (!p || (d = opendir(p)) == 0) {
        return allocArray(threadStateData, 0, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    }
    while ((ent = readdir(d)) != 0 && count < 8192) {
        if (strcmp(ent->d_name, ".") == 0 || strcmp(ent->d_name, "..") == 0) {
            continue;
        }
        names[count++] = strdup(ent->d_name);
    }
    closedir(d);
    arr = allocArray(threadStateData, count, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    if (arr != JAVA_NULL) {
        elements = (JAVA_OBJECT*) (*(JAVA_ARRAY) arr).data;
        for (i = 0; i < count; i++) {
            elements[i] = newStringFromCString(threadStateData, names[i]);
        }
    }
    for (i = 0; i < count; i++) {
        free(names[i]);
    }
    return arr;
}

/* The per-user app storage directory ($XDG_DATA_HOME/codenameone, else
 * ~/.local/share/codenameone), created on first use. Backs Storage + the
 * FileSystemStorage app-home. */
static const char* cn1StorageDir(void) {
    static char dir[4096];
    if (dir[0] == 0) {
        const char* base = getenv("XDG_DATA_HOME");
        const char* home = getenv("HOME");
        char share[4096];
        if (base && base[0]) {
            snprintf(share, sizeof(share), "%s", base);
        } else if (home && home[0]) {
            snprintf(share, sizeof(share), "%s/.local/share", home);
        } else {
            snprintf(share, sizeof(share), "/tmp");
        }
        mkdir(share, 0755);
        snprintf(dir, sizeof(dir), "%s/codenameone", share);
        mkdir(dir, 0755);
    }
    return dir;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_storageDir___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    return newStringFromCString(threadStateData, cn1StorageDir());
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_executableDir___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    char buf[4096];
    ssize_t n = readlink("/proc/self/exe", buf, sizeof(buf) - 1);
    if (n > 0) {
        buf[n] = 0;
        return newStringFromCString(threadStateData, dirname(buf));
    }
    return newStringFromCString(threadStateData, ".");
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_fileRoots___R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE) {
    JAVA_OBJECT arr = allocArray(threadStateData, 1, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    if (arr != JAVA_NULL) {
        ((JAVA_OBJECT*) (*(JAVA_ARRAY) arr).data)[0] = newStringFromCString(threadStateData, "/");
    }
    return arr;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_fileRootSize___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT root) {
    const char* p = cn1JStr(threadStateData, root);
    struct statvfs vfs;
    if (p && statvfs(p, &vfs) == 0) {
        return (JAVA_LONG) vfs.f_blocks * (JAVA_LONG) vfs.f_frsize;
    }
    return 0;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_fileRootFree___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT root) {
    const char* p = cn1JStr(threadStateData, root);
    struct statvfs vfs;
    if (p && statvfs(p, &vfs) == 0) {
        return (JAVA_LONG) vfs.f_bavail * (JAVA_LONG) vfs.f_frsize;
    }
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_fileIsHidden___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    const char* p = cn1JStr(threadStateData, path);
    const char* base;
    if (!p) {
        return JAVA_FALSE;
    }
    base = strrchr(p, '/');
    base = base ? base + 1 : p;
    return base[0] == '.' ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_fileSetHidden___java_lang_String_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_BOOLEAN hidden) {
    /* On Linux "hidden" is purely a leading-dot naming convention; there is no
     * hidden attribute to toggle in place, so this is intentionally a no-op
     * (matches the POSIX leg of the VM's java_io_File). */
    (void) path;
    (void) hidden;
}

/* ------------------------------------------------------------ resources */

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_resourceBytes___java_lang_String_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name) {
    const char* n = cn1JStr(threadStateData, name);
    int len = 0;
    const unsigned char* data;
    if (!n) {
        return JAVA_NULL;
    }
    data = cn1LinuxFindResource(n, &len);
    if (data == 0 || len <= 0) {
        return JAVA_NULL;
    }
    return cn1LinuxNewByteArray(threadStateData, data, len);
}
