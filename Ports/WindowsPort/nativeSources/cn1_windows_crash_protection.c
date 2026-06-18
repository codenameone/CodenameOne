/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

/*
 * Native Win32 crash protection -- the PE-binary analog of the iOS / Linux
 * crash protection modules. Writes the same CN1NATIVECRASH v1 text record
 * so the BuildCloud NativeStackParser handles all three desktop ports
 * through one code path.
 *
 * Win32 surfaces faults through Structured Exception Handling (SEH), NOT
 * POSIX signals; the C runtime exposes a small subset of signals
 * (SIGABRT/SIGFPE/SIGSEGV/SIGILL) but they don't fire on every fault you
 * care about. SetUnhandledExceptionFilter is the only reliable catch-all
 * for access violations, stack overflows, divide-by-zero, etc.
 *
 * We register both:
 *   1. SetUnhandledExceptionFilter   -- catches all SEH-raised faults
 *   2. signal()                       -- catches abort()/raise() paths
 *
 * For the load base we use GetModuleHandleW(NULL) which equals the
 * runtime base of the main executable on Windows. The server subtracts
 * it from each ADDR to feed the .pdb -> llvm-symbolizer mapping. PIE /
 * ASLR is handled automatically because Windows still resolves the
 * .pdb against the slid base.
 *
 * For stderr capture we splice _fileno(stderr) into a _pipe() and a
 * reader thread (CreateThread) appends to a ring buffer; the thread
 * also tees the data back to the original stderr fd so the console
 * output keeps working.
 */

#include "cn1_windows.h"

#include <windows.h>
#include <dbghelp.h>
#include <io.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <signal.h>
#include <shlobj.h>
#include <process.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);

/* ----------------------------------------------------------------------
 * stderr ring buffer
 * ---------------------------------------------------------------------- */

#define CN1_CP_LOG_BUFFER_BYTES (32 * 1024)

static char cn1_cp_log_buf[CN1_CP_LOG_BUFFER_BYTES];
static volatile size_t cn1_cp_log_head;
static volatile size_t cn1_cp_log_filled;
static CRITICAL_SECTION cn1_cp_log_lock;
static int cn1_cp_stderr_dup_fd = -1;
static volatile LONG cn1_cp_installed = 0;
static char cn1_cp_pending_path[MAX_PATH];
static int cn1_cp_pending_path_set = 0;

static void cn1_cp_log_append(const char *bytes, int n) {
    if (n <= 0) return;
    EnterCriticalSection(&cn1_cp_log_lock);
    for (int i = 0; i < n; i++) {
        cn1_cp_log_buf[cn1_cp_log_head] = bytes[i];
        cn1_cp_log_head = (cn1_cp_log_head + 1) % CN1_CP_LOG_BUFFER_BYTES;
        if (cn1_cp_log_filled < CN1_CP_LOG_BUFFER_BYTES) cn1_cp_log_filled++;
    }
    LeaveCriticalSection(&cn1_cp_log_lock);
}

static unsigned __stdcall cn1_cp_log_reader_thread(void *arg) {
    int readFd = (int)(intptr_t)arg;
    char buf[1024];
    int n;
    while ((n = _read(readFd, buf, sizeof(buf))) > 0) {
        if (cn1_cp_stderr_dup_fd >= 0) {
            int off = 0;
            while (off < n) {
                int w = _write(cn1_cp_stderr_dup_fd, buf + off, n - off);
                if (w <= 0) break;
                off += w;
            }
        }
        cn1_cp_log_append(buf, n);
    }
    return 0;
}

static void cn1_cp_log_install(void) {
    int pipefds[2];
    if (_pipe(pipefds, 4096, _O_BINARY) != 0) return;
    cn1_cp_stderr_dup_fd = _dup(_fileno(stderr));
    if (cn1_cp_stderr_dup_fd < 0) {
        _close(pipefds[0]); _close(pipefds[1]); return;
    }
    if (_dup2(pipefds[1], _fileno(stderr)) < 0) {
        _close(cn1_cp_stderr_dup_fd); cn1_cp_stderr_dup_fd = -1;
        _close(pipefds[0]); _close(pipefds[1]); return;
    }
    _close(pipefds[1]);
    /* Detach the reader thread; let the process death reclaim it. */
    HANDLE h = (HANDLE)_beginthreadex(NULL, 0, cn1_cp_log_reader_thread,
                                       (void *)(intptr_t)pipefds[0], 0, NULL);
    if (h != NULL) CloseHandle(h);
}

char * cn1_crash_protection_log_snapshot(void) {
    if (InterlockedCompareExchange(&cn1_cp_installed, 1, 1) == 0) return NULL;
    EnterCriticalSection(&cn1_cp_log_lock);
    size_t filled = cn1_cp_log_filled;
    if (filled == 0) { LeaveCriticalSection(&cn1_cp_log_lock); return NULL; }
    char *out = (char *)malloc(filled + 1);
    if (!out) { LeaveCriticalSection(&cn1_cp_log_lock); return NULL; }
    size_t start = (cn1_cp_log_filled < CN1_CP_LOG_BUFFER_BYTES) ? 0 : cn1_cp_log_head;
    for (size_t i = 0; i < filled; i++) {
        out[i] = cn1_cp_log_buf[(start + i) % CN1_CP_LOG_BUFFER_BYTES];
    }
    out[filled] = '\0';
    LeaveCriticalSection(&cn1_cp_log_lock);
    return out;
}

/* ----------------------------------------------------------------------
 * Pending-crash file path: %LOCALAPPDATA%\cn1\.cn1_pending_native_crash
 * ---------------------------------------------------------------------- */

static void cn1_cp_resolve_pending_path(void) {
    wchar_t *wlocal = NULL;
    if (SHGetKnownFolderPath(&FOLDERID_LocalAppData, 0, NULL, &wlocal) != S_OK) {
        snprintf(cn1_cp_pending_path, sizeof(cn1_cp_pending_path),
                 "C:\\Temp\\.cn1_pending_native_crash");
        cn1_cp_pending_path_set = 1;
        return;
    }
    char local[MAX_PATH];
    int n = WideCharToMultiByte(CP_UTF8, 0, wlocal, -1, local, sizeof(local), NULL, NULL);
    CoTaskMemFree(wlocal);
    if (n <= 0) {
        cn1_cp_pending_path_set = 0;
        return;
    }
    char dir[MAX_PATH];
    snprintf(dir, sizeof(dir), "%s\\cn1", local);
    CreateDirectoryA(dir, NULL);  /* idempotent */
    snprintf(cn1_cp_pending_path, sizeof(cn1_cp_pending_path),
             "%s\\.cn1_pending_native_crash", dir);
    cn1_cp_pending_path_set = 1;
}

/* ----------------------------------------------------------------------
 * Record writer -- async-signal/exception-safe-ish.
 *
 * We use Win32 file APIs (CreateFile / WriteFile / CloseHandle) which
 * are not formally async-safe but are documented as safe in
 * UnhandledExceptionFilter contexts (Microsoft's own crash reporters
 * use them). No malloc, no CRT formatting.
 * ---------------------------------------------------------------------- */

static uintptr_t cn1_cp_load_base = 0;
static LPTOP_LEVEL_EXCEPTION_FILTER cn1_cp_prev_seh = NULL;
static void (*cn1_cp_prev_signal_handlers[NSIG])(int);

static void cn1_cp_write(HANDLE h, const char *s) {
    DWORD written = 0;
    WriteFile(h, s, (DWORD)strlen(s), &written, NULL);
}

static void cn1_cp_write_hex(HANDLE h, uintptr_t v) {
    char buf[2 + sizeof(uintptr_t) * 2 + 1];
    buf[0] = '0'; buf[1] = 'x';
    int hexLen = sizeof(uintptr_t) * 2;
    for (int i = 0; i < hexLen; i++) {
        int nyb = (int)((v >> (i * 4)) & 0xf);
        buf[2 + hexLen - 1 - i] = (char)(nyb < 10 ? '0' + nyb : 'a' + (nyb - 10));
    }
    buf[2 + hexLen] = '\n';
    DWORD written = 0;
    WriteFile(h, buf, 2 + hexLen + 1, &written, NULL);
}

static void cn1_cp_write_dec(HANDLE h, long v) {
    char buf[24];
    int pos = (int)sizeof(buf);
    if (v == 0) buf[--pos] = '0';
    else {
        long n = v < 0 ? -v : v;
        while (n > 0 && pos > 0) { buf[--pos] = (char)('0' + (n % 10)); n /= 10; }
        if (v < 0 && pos > 0) buf[--pos] = '-';
    }
    buf[sizeof(buf) - 1] = '\n';
    DWORD written = 0;
    WriteFile(h, buf + pos, (DWORD)(sizeof(buf) - pos), &written, NULL);
}

static void cn1_cp_emit_record(DWORD exceptionCode, void *faultAddr) {
    if (!cn1_cp_pending_path_set) return;
    HANDLE h = CreateFileA(cn1_cp_pending_path,
                           GENERIC_WRITE, 0, NULL,
                           CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    if (h == INVALID_HANDLE_VALUE) return;
    cn1_cp_write(h, "CN1NATIVECRASH v1\n");
    cn1_cp_write(h, "SEH ");
    cn1_cp_write_hex(h, (uintptr_t)exceptionCode);
    cn1_cp_write(h, "SLIDE ");
    cn1_cp_write_hex(h, cn1_cp_load_base);
    if (faultAddr != NULL) {
        cn1_cp_write(h, "FAULT ");
        cn1_cp_write_hex(h, (uintptr_t)faultAddr);
    }
    /* CaptureStackBackTrace skips this frame + our SEH dispatcher. */
    void *frames[64];
    USHORT n = CaptureStackBackTrace(2, 64, frames, NULL);
    cn1_cp_write(h, "FRAMES ");
    cn1_cp_write_dec(h, (long)n);
    for (USHORT i = 0; i < n; i++) {
        cn1_cp_write(h, "ADDR ");
        cn1_cp_write_hex(h, (uintptr_t)frames[i]);
    }
    cn1_cp_write(h, "END\n");
    CloseHandle(h);
}

static LONG WINAPI cn1_cp_seh_filter(EXCEPTION_POINTERS *ep) {
    void *faultAddr = NULL;
    DWORD code = 0;
    if (ep != NULL && ep->ExceptionRecord != NULL) {
        code = ep->ExceptionRecord->ExceptionCode;
        faultAddr = ep->ExceptionRecord->ExceptionAddress;
    }
    cn1_cp_emit_record(code, faultAddr);
    if (cn1_cp_prev_seh != NULL) {
        return cn1_cp_prev_seh(ep);
    }
    return EXCEPTION_EXECUTE_HANDLER;
}

static void cn1_cp_signal_handler(int sig) {
    cn1_cp_emit_record((DWORD)sig, NULL);
    if (sig >= 0 && sig < NSIG && cn1_cp_prev_signal_handlers[sig] != NULL
            && cn1_cp_prev_signal_handlers[sig] != SIG_DFL
            && cn1_cp_prev_signal_handlers[sig] != SIG_IGN) {
        cn1_cp_prev_signal_handlers[sig](sig);
    }
    signal(sig, SIG_DFL);
    raise(sig);
}

void cn1_crash_protection_install(void) {
    if (InterlockedCompareExchange(&cn1_cp_installed, 1, 0) != 0) return;
    InitializeCriticalSection(&cn1_cp_log_lock);
    cn1_cp_resolve_pending_path();
    cn1_cp_log_install();

    cn1_cp_load_base = (uintptr_t)GetModuleHandleW(NULL);
    cn1_cp_prev_seh = SetUnhandledExceptionFilter(cn1_cp_seh_filter);

    int sigs[] = {SIGABRT, SIGFPE, SIGSEGV, SIGILL};
    for (size_t i = 0; i < sizeof(sigs) / sizeof(sigs[0]); i++) {
        int s = sigs[i];
        cn1_cp_prev_signal_handlers[s] = signal(s, cn1_cp_signal_handler);
    }
}

char * cn1_crash_protection_consume_pending(void) {
    if (!cn1_cp_pending_path_set) return NULL;
    HANDLE h = CreateFileA(cn1_cp_pending_path, GENERIC_READ,
                           FILE_SHARE_READ, NULL, OPEN_EXISTING,
                           FILE_ATTRIBUTE_NORMAL, NULL);
    if (h == INVALID_HANDLE_VALUE) return NULL;
    LARGE_INTEGER sz;
    if (!GetFileSizeEx(h, &sz) || sz.QuadPart <= 0) {
        CloseHandle(h);
        DeleteFileA(cn1_cp_pending_path);
        return NULL;
    }
    LONGLONG cap = sz.QuadPart;
    if (cap > 1024 * 1024) cap = 1024 * 1024;
    char *buf = (char *)malloc((size_t)cap + 1);
    if (!buf) {
        CloseHandle(h);
        DeleteFileA(cn1_cp_pending_path);
        return NULL;
    }
    DWORD totalRead = 0;
    DWORD r;
    while (totalRead < (DWORD)cap
            && ReadFile(h, buf + totalRead, (DWORD)cap - totalRead, &r, NULL)
            && r > 0) {
        totalRead += r;
    }
    buf[totalRead] = '\0';
    CloseHandle(h);
    DeleteFileA(cn1_cp_pending_path);
    if (totalRead == 0) { free(buf); return NULL; }
    return buf;
}

/* ----------------------------------------------------------------------
 * ParparVM JNI bridges -- back the three native methods on WindowsNative.
 * ---------------------------------------------------------------------- */

JAVA_VOID com_codename1_impl_windows_WindowsNative_crashProtectionInstall__(CODENAME_ONE_THREAD_STATE) {
    cn1_crash_protection_install();
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_crashProtectionLogSnapshot___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    char *s = cn1_crash_protection_log_snapshot();
    if (s == NULL) return JAVA_NULL;
    JAVA_OBJECT out = newStringFromCString(threadStateData, s);
    free(s);
    return out;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_crashProtectionConsumePending___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    char *s = cn1_crash_protection_consume_pending();
    if (s == NULL) return JAVA_NULL;
    JAVA_OBJECT out = newStringFromCString(threadStateData, s);
    free(s);
    return out;
}
