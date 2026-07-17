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
 * Native child-process (subprocess) transport for the Codename One Windows port.
 * The native ports have full java.lang.Thread but no java.lang.Process /
 * ProcessBuilder (absent from vm/JavaAPI), so a Java NativeSubprocessTransport
 * that needs to spawn a helper binary (e.g. the Rust Bluetooth helper) and talk
 * to it over stdio drives these primitives instead.
 *
 * A subprocess peer is a CN1Subprocess* holding the PROCESS_INFORMATION and the
 * parent-side ends of two anonymous pipes wired to the child's stdin/stdout
 * (stderr is inherited). It mirrors the socket bridge (cn1_windows_socket.c):
 * blocking ReadFile/WriteFile are bracketed with CN1_YIELD_THREAD /
 * CN1_RESUME_THREAD so the concurrent GC is not stalled by a thread parked in a
 * blocking call, and the byte[] passed to a blocking read/write is anchored with
 * CN1_PROC_KEEP_ALIVE so the conservative GC cannot sweep it mid-I/O.
 */

#ifdef _WIN32

#include "cn1_windows.h"
#include <stdlib.h>
#include <string.h>

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

/* Forces a read/write buffer object to stay live across a parked blocking call so
 * the conservative GC can't sweep it mid-I/O -- identical idiom and rationale to
 * CN1_SOCKET_KEEP_ALIVE in cn1_windows_socket.c (an address-escape asm; clang-cl,
 * the compiler for this port, supports GNU inline asm). */
#define CN1_PROC_KEEP_ALIVE(obj) __asm__ __volatile__("" : : "r"(obj))

typedef struct CN1Subprocess {
    PROCESS_INFORMATION pi;
    HANDLE stdinWrite;  /* parent write end -> child stdin  (NULL once closed) */
    HANDLE stdoutRead;  /* parent read  end <- child stdout (NULL once closed) */
} CN1Subprocess;

/* --------------------------------------------------------- command line build */

/*
 * Growable UTF-8 byte buffer for assembling the command line. On allocation
 * failure the buffer marks itself failed and further appends are no-ops so the
 * caller only has to check once at the end.
 */
typedef struct {
    char* data;
    size_t len;
    size_t cap;
    int failed;
} Cn1CmdBuf;

static void cn1CmdEnsure(Cn1CmdBuf* b, size_t extra) {
    if (b->failed) {
        return;
    }
    if (b->len + extra + 1 > b->cap) {
        size_t ncap = b->cap ? b->cap * 2 : 256;
        char* nd;
        while (ncap < b->len + extra + 1) {
            ncap *= 2;
        }
        nd = (char*) realloc(b->data, ncap);
        if (!nd) {
            b->failed = 1;
            return;
        }
        b->data = nd;
        b->cap = ncap;
    }
}

static void cn1CmdPutc(Cn1CmdBuf* b, char c) {
    cn1CmdEnsure(b, 1);
    if (b->failed) {
        return;
    }
    b->data[b->len++] = c;
}

static void cn1CmdPutn(Cn1CmdBuf* b, char c, size_t n) {
    size_t i;
    for (i = 0; i < n; i++) {
        cn1CmdPutc(b, c);
    }
}

/*
 * Appends one argument, quoted per the CreateProcess / CommandLineToArgvW rules
 * (the "Everybody quotes command line arguments the wrong way" algorithm):
 * quote the argument when it is empty or contains whitespace/quotes; inside the
 * quotes, backslashes that precede a quote (or the closing quote) are doubled and
 * embedded quotes are backslash-escaped.
 */
static void cn1CmdAppendArg(Cn1CmdBuf* b, const char* arg) {
    size_t i;
    size_t n = strlen(arg);
    int needQuote = (n == 0);
    for (i = 0; i < n && !needQuote; i++) {
        char c = arg[i];
        if (c == ' ' || c == '\t' || c == '\n' || c == '\v' || c == '"') {
            needQuote = 1;
        }
    }
    if (!needQuote) {
        for (i = 0; i < n; i++) {
            cn1CmdPutc(b, arg[i]);
        }
        return;
    }
    cn1CmdPutc(b, '"');
    for (i = 0; i < n; i++) {
        size_t slashes = 0;
        while (i < n && arg[i] == '\\') {
            slashes++;
            i++;
        }
        if (i == n) {
            /* Trailing backslashes: double them so they don't escape the
             * closing quote. */
            cn1CmdPutn(b, '\\', slashes * 2);
            break;
        }
        if (arg[i] == '"') {
            /* Backslashes before a quote are doubled, then the quote escaped. */
            cn1CmdPutn(b, '\\', slashes * 2 + 1);
            cn1CmdPutc(b, '"');
        } else {
            cn1CmdPutn(b, '\\', slashes);
            cn1CmdPutc(b, arg[i]);
        }
    }
    cn1CmdPutc(b, '"');
}

/* Converts a UTF-8 string to a freshly malloc'd WCHAR*, or NULL on failure. */
static WCHAR* cn1Utf8ToWide(const char* utf8) {
    int needed;
    WCHAR* wide;
    needed = MultiByteToWideChar(CP_UTF8, 0, utf8, -1, NULL, 0);
    if (needed <= 0) {
        return NULL;
    }
    wide = (WCHAR*) malloc((size_t) needed * sizeof(WCHAR));
    if (!wide) {
        return NULL;
    }
    if (MultiByteToWideChar(CP_UTF8, 0, utf8, -1, wide, needed) <= 0) {
        free(wide);
        return NULL;
    }
    return wide;
}

/* ------------------------------------------------------------------ bridge */

JAVA_LONG com_codename1_impl_windows_WindowsNative_procSpawn___java_lang_String_1ARRAY_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    int argc;
    int i;
    JAVA_OBJECT* elements;
    Cn1CmdBuf cmd;
    WCHAR* wideCmd = NULL;
    HANDLE childStdinRead = NULL;
    HANDLE childStdinWrite = NULL;
    HANDLE childStdoutRead = NULL;
    HANDLE childStdoutWrite = NULL;
    SECURITY_ATTRIBUTES sa;
    STARTUPINFOW si;
    PROCESS_INFORMATION pi;
    CN1Subprocess* p;

    if (__cn1Arg1 == JAVA_NULL) {
        return 0;
    }
    argc = (int) (*(JAVA_ARRAY) __cn1Arg1).length;
    if (argc <= 0) {
        return 0;
    }
    elements = (JAVA_OBJECT*) (*(JAVA_ARRAY) __cn1Arg1).data;

    /* Build the command line. stringToUTF8 returns a per-thread reusable buffer
     * that the next call overwrites, so each element must be consumed (appended)
     * before the next conversion -- which cn1CmdAppendArg does. */
    memset(&cmd, 0, sizeof(cmd));
    for (i = 0; i < argc; i++) {
        const char* s = elements[i] == JAVA_NULL ? "" : stringToUTF8(threadStateData, elements[i]);
        if (i > 0) {
            cn1CmdPutc(&cmd, ' ');
        }
        cn1CmdAppendArg(&cmd, s ? s : "");
    }
    if (cmd.failed || cmd.data == NULL) {
        free(cmd.data);
        return 0;
    }
    cmd.data[cmd.len] = '\0';
    wideCmd = cn1Utf8ToWide(cmd.data);
    free(cmd.data);
    if (!wideCmd) {
        return 0;
    }

    ZeroMemory(&sa, sizeof(sa));
    sa.nLength = sizeof(sa);
    sa.bInheritHandle = TRUE; /* pipe ends must be inheritable to reach the child */
    sa.lpSecurityDescriptor = NULL;

    if (!CreatePipe(&childStdinRead, &childStdinWrite, &sa, 0)) {
        cn1WindowsLog("procSpawn: CreatePipe(stdin) failed");
        free(wideCmd);
        return 0;
    }
    if (!CreatePipe(&childStdoutRead, &childStdoutWrite, &sa, 0)) {
        cn1WindowsLog("procSpawn: CreatePipe(stdout) failed");
        CloseHandle(childStdinRead);
        CloseHandle(childStdinWrite);
        free(wideCmd);
        return 0;
    }
    /* The parent-owned ends must NOT be inherited by the child, otherwise the
     * child holds a copy and the pipe never signals EOF/broken. */
    SetHandleInformation(childStdinWrite, HANDLE_FLAG_INHERIT, 0);
    SetHandleInformation(childStdoutRead, HANDLE_FLAG_INHERIT, 0);

    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESTDHANDLES;
    si.hStdInput = childStdinRead;
    si.hStdOutput = childStdoutWrite;
    si.hStdError = GetStdHandle(STD_ERROR_HANDLE); /* inherit stderr */

    ZeroMemory(&pi, sizeof(pi));
    if (!CreateProcessW(NULL, wideCmd, NULL, NULL, TRUE, 0, NULL, NULL, &si, &pi)) {
        cn1WindowsLog("procSpawn: CreateProcess failed");
        CloseHandle(childStdinRead);
        CloseHandle(childStdinWrite);
        CloseHandle(childStdoutRead);
        CloseHandle(childStdoutWrite);
        free(wideCmd);
        return 0;
    }
    free(wideCmd);

    /* The child-side ends belong to the child now; the parent closes its copies
     * so the pipes signal EOF/broken correctly. */
    CloseHandle(childStdinRead);
    CloseHandle(childStdoutWrite);

    p = (CN1Subprocess*) calloc(1, sizeof(CN1Subprocess));
    if (!p) {
        TerminateProcess(pi.hProcess, 1);
        CloseHandle(pi.hProcess);
        CloseHandle(pi.hThread);
        CloseHandle(childStdinWrite);
        CloseHandle(childStdoutRead);
        return 0;
    }
    p->pi = pi;
    p->stdinWrite = childStdinWrite;
    p->stdoutRead = childStdoutRead;
    return (JAVA_LONG) (intptr_t) p;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_procRead___long_byte_1ARRAY_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Subprocess* p = (CN1Subprocess*) (intptr_t) __cn1Arg1;
    JAVA_ARRAY_BYTE* data;
    DWORD read = 0;
    BOOL ok;
    if (p == NULL || p->stdoutRead == NULL || __cn1Arg2 == JAVA_NULL || __cn1Arg4 <= 0) {
        return -1;
    }
    data = (JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY) __cn1Arg2).data;
    /* ReadFile blocks until the helper writes (or its stdout closes). Park the
     * thread across the call so the concurrent GC is not held up (see
     * cn1_windows_socket.c socketRead). */
    CN1_YIELD_THREAD;
    ok = ReadFile(p->stdoutRead, (char*) (data + __cn1Arg3), (DWORD) __cn1Arg4, &read, NULL);
    CN1_RESUME_THREAD;
    /* Keep the buffer array reachable across the parked read: only `data` (an
     * interior pointer) is used, so the optimizer may drop __cn1Arg2 and the GC,
     * scanning this parked thread, can sweep it mid-read (use-after-free). */
    CN1_PROC_KEEP_ALIVE(__cn1Arg2);
    if (!ok) {
        /* A closed/broken pipe after the child exits is a normal EOF. */
        DWORD err = GetLastError();
        if (err == ERROR_BROKEN_PIPE || err == ERROR_HANDLE_EOF) {
            return 0;
        }
        return -1;
    }
    return (JAVA_INT) read; /* 0 == EOF */
}

JAVA_INT com_codename1_impl_windows_WindowsNative_procWrite___long_byte_1ARRAY_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Subprocess* p = (CN1Subprocess*) (intptr_t) __cn1Arg1;
    JAVA_ARRAY_BYTE* data;
    int written = 0;
    if (p == NULL || p->stdinWrite == NULL || __cn1Arg2 == JAVA_NULL || __cn1Arg4 <= 0) {
        return -1;
    }
    data = (JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY) __cn1Arg2).data;
    /* Yield across the (potentially blocking) write loop -- a thread parked in
     * WriteFile must not stall a GC mark (see socketWrite). */
    CN1_YIELD_THREAD;
    while (written < __cn1Arg4) {
        DWORD wrote = 0;
        if (!WriteFile(p->stdinWrite, (char*) (data + __cn1Arg3 + written), (DWORD) (__cn1Arg4 - written), &wrote, NULL) || wrote == 0) {
            CN1_RESUME_THREAD;
            /* Keep the array reachable across the parked write, exactly like
             * socketWrite: only `data` (an interior pointer) is used after the
             * yield, so the GC could otherwise sweep it WHILE WriteFile is still
             * reading from it. */
            CN1_PROC_KEEP_ALIVE(__cn1Arg2);
            return written > 0 ? written : -1;
        }
        written += (int) wrote;
    }
    CN1_RESUME_THREAD;
    CN1_PROC_KEEP_ALIVE(__cn1Arg2);
    return written;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_procCloseStdin___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Subprocess* p = (CN1Subprocess*) (intptr_t) __cn1Arg1;
    if (p == NULL) {
        return;
    }
    if (p->stdinWrite != NULL) {
        CloseHandle(p->stdinWrite);
        p->stdinWrite = NULL;
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_procClose___long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Subprocess* p = (CN1Subprocess*) (intptr_t) __cn1Arg1;
    if (p == NULL) {
        return;
    }
    if (p->stdinWrite != NULL) {
        CloseHandle(p->stdinWrite);
        p->stdinWrite = NULL;
    }
    if (p->stdoutRead != NULL) {
        CloseHandle(p->stdoutRead);
        p->stdoutRead = NULL;
    }
    if (p->pi.hProcess != NULL) {
        if (WaitForSingleObject(p->pi.hProcess, 0) != WAIT_OBJECT_0) {
            TerminateProcess(p->pi.hProcess, 1);
        }
        CloseHandle(p->pi.hProcess);
        p->pi.hProcess = NULL;
    }
    if (p->pi.hThread != NULL) {
        CloseHandle(p->pi.hThread);
        p->pi.hThread = NULL;
    }
    free(p);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_procIsAlive___long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Subprocess* p = (CN1Subprocess*) (intptr_t) __cn1Arg1;
    if (p == NULL || p->pi.hProcess == NULL) {
        return 0;
    }
    return (WaitForSingleObject(p->pi.hProcess, 0) == WAIT_TIMEOUT) ? 1 : 0;
}

#endif /* _WIN32 */
