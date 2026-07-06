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

#define _GNU_SOURCE
#include "cn1_linux_crash_protection.h"

#include <dlfcn.h>
/* execinfo.h (backtrace + backtrace_symbols_fd) is a glibc extension;
 * musl libc does not ship the header. Detect glibc explicitly and fall
 * back to a no-op stack capture on Alpine / musl builds -- the signal
 * handler still runs, persists signal + SLIDE + FAULT, and reports
 * zero frames. CN1 picks symbolication back up server-side via the
 * uploaded ELF debug info, so the missing on-device unwind is a
 * graceful degradation rather than a feature regression. */
#if defined(__GLIBC__)
#include <execinfo.h>
#define CN1_CP_HAVE_BACKTRACE 1
#else
#define CN1_CP_HAVE_BACKTRACE 0
#endif
#include <fcntl.h>
#include <pthread.h>
#include <pwd.h>
#include <signal.h>
#include <stdatomic.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#include "cn1_linux.h"

/* C-string -> JAVA_OBJECT helper. Same one used by cn1_linux_browser.c
 * and cn1_linux_edit.c -- emitted by the ParparVM translator. */
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);

/* ----------------------------------------------------------------------
 * stderr / GTK-stderr ring buffer
 * ---------------------------------------------------------------------- */

#define CN1_CP_LOG_BUFFER_BYTES (32 * 1024)

static char cn1_cp_log_buf[CN1_CP_LOG_BUFFER_BYTES];
static size_t cn1_cp_log_head;
static size_t cn1_cp_log_filled;
static pthread_mutex_t cn1_cp_log_mutex = PTHREAD_MUTEX_INITIALIZER;
static int cn1_cp_stderr_dup_fd = -1;
static atomic_int cn1_cp_installed = 0;
static char cn1_cp_pending_path[1024];
static int cn1_cp_pending_path_set = 0;

static void cn1_cp_log_append(const char *bytes, ssize_t n) {
    if (n <= 0) return;
    pthread_mutex_lock(&cn1_cp_log_mutex);
    for (ssize_t i = 0; i < n; i++) {
        cn1_cp_log_buf[cn1_cp_log_head] = bytes[i];
        cn1_cp_log_head = (cn1_cp_log_head + 1) % CN1_CP_LOG_BUFFER_BYTES;
        if (cn1_cp_log_filled < CN1_CP_LOG_BUFFER_BYTES) cn1_cp_log_filled++;
    }
    pthread_mutex_unlock(&cn1_cp_log_mutex);
}

static void *cn1_cp_log_reader_thread(void *arg) {
    int readFd = (int)(intptr_t)arg;
    char buf[1024];
    ssize_t n;
    while ((n = read(readFd, buf, sizeof(buf))) > 0) {
        if (cn1_cp_stderr_dup_fd >= 0) {
            ssize_t off = 0;
            while (off < n) {
                ssize_t w = write(cn1_cp_stderr_dup_fd, buf + off, n - off);
                if (w <= 0) break;
                off += w;
            }
        }
        cn1_cp_log_append(buf, n);
    }
    return NULL;
}

static void cn1_cp_log_install(void) {
    int pipefds[2];
    if (pipe(pipefds) != 0) return;
    cn1_cp_stderr_dup_fd = dup(STDERR_FILENO);
    if (cn1_cp_stderr_dup_fd < 0) {
        close(pipefds[0]); close(pipefds[1]);
        return;
    }
    if (dup2(pipefds[1], STDERR_FILENO) < 0) {
        close(cn1_cp_stderr_dup_fd); cn1_cp_stderr_dup_fd = -1;
        close(pipefds[0]); close(pipefds[1]);
        return;
    }
    close(pipefds[1]);
    pthread_t tid;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    pthread_create(&tid, &attr, cn1_cp_log_reader_thread, (void *)(intptr_t)pipefds[0]);
    pthread_attr_destroy(&attr);
}

char * cn1_crash_protection_log_snapshot(void) {
    if (atomic_load(&cn1_cp_installed) == 0) return NULL;
    pthread_mutex_lock(&cn1_cp_log_mutex);
    size_t filled = cn1_cp_log_filled;
    if (filled == 0) { pthread_mutex_unlock(&cn1_cp_log_mutex); return NULL; }
    char *out = (char *)malloc(filled + 1);
    if (!out) { pthread_mutex_unlock(&cn1_cp_log_mutex); return NULL; }
    size_t start = (cn1_cp_log_filled < CN1_CP_LOG_BUFFER_BYTES) ? 0 : cn1_cp_log_head;
    for (size_t i = 0; i < filled; i++) {
        out[i] = cn1_cp_log_buf[(start + i) % CN1_CP_LOG_BUFFER_BYTES];
    }
    out[filled] = '\0';
    pthread_mutex_unlock(&cn1_cp_log_mutex);
    return out;
}

/* ----------------------------------------------------------------------
 * Pending-crash file path
 *
 * Prefer $XDG_CACHE_HOME/cn1_pending_native_crash, fall back to
 * ~/.cache/cn1_pending_native_crash, fall back to /tmp/<pid>.
 * The xdg-cache-home path survives upgrades, /tmp is the safety net.
 * ---------------------------------------------------------------------- */

static void cn1_cp_resolve_pending_path(void) {
    const char *xdg = getenv("XDG_CACHE_HOME");
    if (xdg && xdg[0] == '/') {
        mkdir(xdg, 0700);  /* idempotent */
        snprintf(cn1_cp_pending_path, sizeof(cn1_cp_pending_path),
                 "%s/.cn1_pending_native_crash", xdg);
        cn1_cp_pending_path_set = 1;
        return;
    }
    const char *home = getenv("HOME");
    if (!home || home[0] != '/') {
        struct passwd *pw = getpwuid(getuid());
        home = pw ? pw->pw_dir : NULL;
    }
    if (home && home[0] == '/') {
        char dir[1024];
        snprintf(dir, sizeof(dir), "%s/.cache", home);
        mkdir(dir, 0700);
        snprintf(cn1_cp_pending_path, sizeof(cn1_cp_pending_path),
                 "%s/.cache/.cn1_pending_native_crash", home);
        cn1_cp_pending_path_set = 1;
        return;
    }
    snprintf(cn1_cp_pending_path, sizeof(cn1_cp_pending_path),
             "/tmp/.cn1_pending_native_crash_%d", (int)getuid());
    cn1_cp_pending_path_set = 1;
}

/* ----------------------------------------------------------------------
 * Signal handler -- async-signal-safe only
 * ---------------------------------------------------------------------- */

static struct sigaction cn1_cp_prev_handlers[NSIG];
static uintptr_t cn1_cp_load_base = 0;

#define CN1_CP_W_STR(fd, s) do { \
    const char *_s = (s); \
    ssize_t _r = write((fd), _s, strlen(_s)); \
    (void)_r; \
} while (0)

static void cn1_cp_write_hex(int fd, uintptr_t v) {
    char buf[2 + sizeof(uintptr_t) * 2];
    buf[0] = '0'; buf[1] = 'x';
    int hexLen = sizeof(uintptr_t) * 2;
    for (int i = 0; i < hexLen; i++) {
        int nyb = (int)((v >> (i * 4)) & 0xf);
        buf[2 + hexLen - 1 - i] = (char)(nyb < 10 ? '0' + nyb : 'a' + (nyb - 10));
    }
    ssize_t r = write(fd, buf, 2 + hexLen);
    (void)r;
}

static void cn1_cp_write_dec(int fd, long v) {
    char buf[24];
    int pos = (int)sizeof(buf);
    if (v == 0) buf[--pos] = '0';
    else {
        long n = v < 0 ? -v : v;
        while (n > 0 && pos > 0) { buf[--pos] = (char)('0' + (n % 10)); n /= 10; }
        if (v < 0 && pos > 0) buf[--pos] = '-';
    }
    ssize_t r = write(fd, buf + pos, sizeof(buf) - pos);
    (void)r;
}

static void cn1_cp_signal_handler(int sig, siginfo_t *info, void *ctx) {
    if (cn1_cp_pending_path_set) {
        int fd = open(cn1_cp_pending_path,
                      O_WRONLY | O_CREAT | O_TRUNC, 0600);
        if (fd >= 0) {
            CN1_CP_W_STR(fd, "CN1NATIVECRASH v1\nSIGNAL ");
            cn1_cp_write_dec(fd, (long)sig);
            CN1_CP_W_STR(fd, "\n");
            CN1_CP_W_STR(fd, "SLIDE ");
            cn1_cp_write_hex(fd, cn1_cp_load_base);
            CN1_CP_W_STR(fd, "\n");
            if (info != NULL) {
                CN1_CP_W_STR(fd, "SIGCODE ");
                cn1_cp_write_dec(fd, (long)info->si_code);
                CN1_CP_W_STR(fd, "\nFAULT ");
                cn1_cp_write_hex(fd, (uintptr_t)info->si_addr);
                CN1_CP_W_STR(fd, "\n");
            }
#if CN1_CP_HAVE_BACKTRACE
            void *frames[64];
            int n = backtrace(frames, 64);
            CN1_CP_W_STR(fd, "FRAMES ");
            cn1_cp_write_dec(fd, (long)n);
            CN1_CP_W_STR(fd, "\n");
            for (int i = 0; i < n; i++) {
                CN1_CP_W_STR(fd, "ADDR ");
                cn1_cp_write_hex(fd, (uintptr_t)frames[i]);
                CN1_CP_W_STR(fd, "\n");
            }
            CN1_CP_W_STR(fd, "SYMBOLS\n");
            backtrace_symbols_fd(frames, n, fd);
#else
            /* musl libc has no backtrace(); persist a zero-frame
             * record so the BuildCloud ingest still recognises the
             * payload as a valid CN1NATIVECRASH v1 and groups it
             * with the other unwind-less native crashes. */
            CN1_CP_W_STR(fd, "FRAMES 0\nSYMBOLS\n");
#endif
            CN1_CP_W_STR(fd, "END\n");
            close(fd);
        }
    }
    if (sig >= 0 && sig < NSIG && cn1_cp_prev_handlers[sig].sa_handler != NULL) {
        struct sigaction prev = cn1_cp_prev_handlers[sig];
        if (prev.sa_flags & SA_SIGINFO) {
            if (prev.sa_sigaction != NULL) { prev.sa_sigaction(sig, info, ctx); return; }
        } else if (prev.sa_handler != SIG_DFL && prev.sa_handler != SIG_IGN) {
            prev.sa_handler(sig); return;
        }
    }
    signal(sig, SIG_DFL);
    raise(sig);
}

static void cn1_cp_install_signal_handlers(void) {
    /* dladdr() against an address we control gives us the load base of the
     * executable -- the equivalent of dyld_get_image_vmaddr_slide on iOS. */
    Dl_info info;
    if (dladdr((void *)&cn1_cp_install_signal_handlers, &info) != 0 && info.dli_fbase != NULL) {
        cn1_cp_load_base = (uintptr_t)info.dli_fbase;
    }
    int sigs[] = {SIGSEGV, SIGABRT, SIGBUS, SIGILL, SIGFPE, SIGTRAP, SIGPIPE};
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = cn1_cp_signal_handler;
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK | SA_RESTART;
    sigemptyset(&sa.sa_mask);
    for (size_t i = 0; i < sizeof(sigs) / sizeof(sigs[0]); i++) {
        int s = sigs[i];
        struct sigaction prev;
        memset(&prev, 0, sizeof(prev));
        if (sigaction(s, &sa, &prev) == 0) {
            cn1_cp_prev_handlers[s] = prev;
        }
    }
}

void cn1_crash_protection_install(void) {
    int expected = 0;
    if (!atomic_compare_exchange_strong(&cn1_cp_installed, &expected, 1)) return;
    cn1_cp_resolve_pending_path();
    cn1_cp_log_install();
    cn1_cp_install_signal_handlers();
    /* Alternate signal stack for THIS (main) thread, so the SA_ONSTACK handler runs even
     * on a stack-overflow SIGSEGV when the main stack is exhausted. Per-thread; the CN1
     * worker/EDT/GC threads register their own in threadRunner/gcMarkWorkerMain. Without
     * it a stack overflow dies silently instead of dumping the recursion backtrace. */
    {
        stack_t ss;
        ss.ss_sp = malloc(512 * 1024);
        if (ss.ss_sp != NULL) {
            ss.ss_size = 512 * 1024;
            ss.ss_flags = 0;
            sigaltstack(&ss, NULL);
        }
    }
}

/* ----------------------------------------------------------------------
 * Consume pending
 * ---------------------------------------------------------------------- */

char * cn1_crash_protection_consume_pending(void) {
    if (!cn1_cp_pending_path_set) return NULL;
    FILE *f = fopen(cn1_cp_pending_path, "r");
    if (!f) return NULL;
    fseek(f, 0, SEEK_END);
    long sz = ftell(f);
    if (sz < 0) { fclose(f); unlink(cn1_cp_pending_path); return NULL; }
    if (sz > 1024 * 1024) sz = 1024 * 1024;  /* cap defensively */
    fseek(f, 0, SEEK_SET);
    char *buf = (char *)malloc((size_t)sz + 1);
    if (!buf) { fclose(f); unlink(cn1_cp_pending_path); return NULL; }
    size_t read = fread(buf, 1, (size_t)sz, f);
    buf[read] = '\0';
    fclose(f);
    unlink(cn1_cp_pending_path);  /* always delete; corrupt file shouldn't loop */
    if (read == 0) { free(buf); return NULL; }
    return buf;
}

/* ----------------------------------------------------------------------
 * ParparVM JNI bridges -- back the three native methods on LinuxNative.
 * ---------------------------------------------------------------------- */

JAVA_VOID com_codename1_impl_linux_LinuxNative_crashProtectionInstall__(CODENAME_ONE_THREAD_STATE) {
    cn1_crash_protection_install();
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_crashProtectionLogSnapshot___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    char *s = cn1_crash_protection_log_snapshot();
    if (s == NULL) return JAVA_NULL;
    JAVA_OBJECT out = newStringFromCString(threadStateData, s);
    free(s);
    return out;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_crashProtectionConsumePending___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    char *s = cn1_crash_protection_consume_pending();
    if (s == NULL) return JAVA_NULL;
    JAVA_OBJECT out = newStringFromCString(threadStateData, s);
    free(s);
    return out;
}
