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

#import "CN1CrashProtection.h"
#import "CodenameOne_GLViewController.h"

#import <execinfo.h>
#import <signal.h>
#import <stdatomic.h>
#import <stdio.h>
#import <pthread.h>
#import <unistd.h>
#import <fcntl.h>
#import <string.h>
#import <stdlib.h>
#import <mach-o/dyld.h>
#import <sys/time.h>

/*
 * --- Stderr / NSLog ring buffer ---------------------------------------
 *
 * iOS doesn't expose an easy way to read back recent NSLog/os_log output
 * inside the same process, so we splice stderr into a pipe at install
 * time. A background pthread drains the pipe, writes every byte back to
 * the original stderr (preserving Xcode console behaviour), and appends
 * the same bytes to an in-memory ring buffer protected by a mutex.
 * cn1_crash_protection_log_snapshot() copies that buffer into an
 * NSString for the crash payload.
 */

#define CN1_CP_LOG_BUFFER_BYTES (32 * 1024)

static char cn1_cp_log_buf[CN1_CP_LOG_BUFFER_BYTES];
static size_t cn1_cp_log_head;   /* next byte to write */
static size_t cn1_cp_log_filled; /* total bytes ever written, capped at buf size */
static pthread_mutex_t cn1_cp_log_mutex = PTHREAD_MUTEX_INITIALIZER;
static int cn1_cp_stderr_dup_fd = -1; /* original stderr, for tee-back */
static atomic_int cn1_cp_installed = 0;

static void cn1_cp_log_append(const char *bytes, ssize_t n) {
    if (n <= 0) {
        return;
    }
    pthread_mutex_lock(&cn1_cp_log_mutex);
    for (ssize_t i = 0; i < n; i++) {
        cn1_cp_log_buf[cn1_cp_log_head] = bytes[i];
        cn1_cp_log_head = (cn1_cp_log_head + 1) % CN1_CP_LOG_BUFFER_BYTES;
        if (cn1_cp_log_filled < CN1_CP_LOG_BUFFER_BYTES) {
            cn1_cp_log_filled++;
        }
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
    if (pipe(pipefds) != 0) {
        return;
    }
    cn1_cp_stderr_dup_fd = dup(STDERR_FILENO);
    if (cn1_cp_stderr_dup_fd < 0) {
        close(pipefds[0]);
        close(pipefds[1]);
        return;
    }
    if (dup2(pipefds[1], STDERR_FILENO) < 0) {
        close(cn1_cp_stderr_dup_fd);
        cn1_cp_stderr_dup_fd = -1;
        close(pipefds[0]);
        close(pipefds[1]);
        return;
    }
    close(pipefds[1]); /* keep STDERR_FILENO as the write end */
    pthread_t tid;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    pthread_create(&tid, &attr,
                   cn1_cp_log_reader_thread, (void *)(intptr_t)pipefds[0]);
    pthread_attr_destroy(&attr);
}

NSString * cn1_crash_protection_log_snapshot(void) {
    if (atomic_load(&cn1_cp_installed) == 0) {
        return nil;
    }
    /* Copy the ring buffer linearly so the snapshot reads oldest-first. */
    char *tmp = (char *)malloc(CN1_CP_LOG_BUFFER_BYTES + 1);
    if (!tmp) return nil;
    pthread_mutex_lock(&cn1_cp_log_mutex);
    size_t filled = cn1_cp_log_filled;
    if (filled == 0) {
        pthread_mutex_unlock(&cn1_cp_log_mutex);
        free(tmp);
        return nil;
    }
    size_t start = (cn1_cp_log_filled < CN1_CP_LOG_BUFFER_BYTES)
            ? 0
            : cn1_cp_log_head;
    for (size_t i = 0; i < filled; i++) {
        tmp[i] = cn1_cp_log_buf[(start + i) % CN1_CP_LOG_BUFFER_BYTES];
    }
    tmp[filled] = '\0';
    pthread_mutex_unlock(&cn1_cp_log_mutex);
    NSString *result = [[NSString alloc] initWithBytes:tmp
                                                length:filled
                                              encoding:NSUTF8StringEncoding];
    free(tmp);
    if (!result) {
        /* If the buffer contains non-UTF8 bytes from a misbehaving lib,
         * fall back to a lossy decode so we never lose the crash report. */
        result = [[NSString alloc] initWithBytes:tmp /* freed -- but Foundation copies first */
                                          length:filled
                                        encoding:NSISOLatin1StringEncoding];
    }
    return result;
}

/*
 * --- Signal-handler crash capture --------------------------------------
 */

/* Pre-resolved POSIX path to the pending-crash file. Cached at install
 * time so the signal handler never has to touch Foundation. */
static char cn1_cp_pending_path[1024];
static int cn1_cp_pending_path_set = 0;

/* The previously-registered SignalHandler() in CodenameOne_GLAppDelegate.m
 * converts the signal to a JVM exception. We chain to it so the in-process
 * Java path keeps working; if it succeeds, the JVM-exception flow runs and
 * the pending-crash file is also there as a belt-and-braces record. */
static struct sigaction cn1_cp_prev_handlers[NSIG];

#define CN1_CP_W_STR(fd, s) do { \
    const char *_s = (s); \
    size_t _l = strlen(_s); \
    ssize_t _r = write((fd), _s, _l); \
    (void)_r; \
} while (0)

static void cn1_cp_write_hex(int fd, uintptr_t v) {
    char buf[2 + sizeof(uintptr_t) * 2 + 1];
    buf[0] = '0';
    buf[1] = 'x';
    int pos = 2 + sizeof(uintptr_t) * 2;
    buf[pos] = '\0';
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
    if (v == 0) {
        buf[--pos] = '0';
    } else {
        long n = v < 0 ? -v : v;
        while (n > 0 && pos > 0) {
            buf[--pos] = (char)('0' + (n % 10));
            n /= 10;
        }
        if (v < 0 && pos > 0) {
            buf[--pos] = '-';
        }
    }
    ssize_t r = write(fd, buf + pos, sizeof(buf) - pos);
    (void)r;
}

static void cn1_cp_signal_handler(int sig, siginfo_t *info, void *ctx) {
    /* Open the pending-crash file. O_TRUNC so we always replace any prior
     * record -- only the most recent crash matters. */
    if (cn1_cp_pending_path_set) {
        int fd = open(cn1_cp_pending_path,
                      O_WRONLY | O_CREAT | O_TRUNC, 0600);
        if (fd >= 0) {
            CN1_CP_W_STR(fd, "CN1NATIVECRASH v1\n");
            CN1_CP_W_STR(fd, "SIGNAL ");
            cn1_cp_write_dec(fd, (long)sig);
            CN1_CP_W_STR(fd, "\n");
            /* dyld slide of the main image -- the server subtracts it
             * from each ADDR so it can look the address up in the dSYM
             * regardless of where ASLR placed the binary at runtime. */
            intptr_t slide = _dyld_get_image_vmaddr_slide(0);
            CN1_CP_W_STR(fd, "SLIDE ");
            cn1_cp_write_hex(fd, (uintptr_t)slide);
            CN1_CP_W_STR(fd, "\n");
            if (info != NULL) {
                CN1_CP_W_STR(fd, "SIGCODE ");
                cn1_cp_write_dec(fd, (long)info->si_code);
                CN1_CP_W_STR(fd, "\nFAULT ");
                cn1_cp_write_hex(fd, (uintptr_t)info->si_addr);
                CN1_CP_W_STR(fd, "\n");
            }
            /* Raw backtrace. backtrace() and backtrace_symbols_fd() are
             * both documented async-signal-safe on Darwin. */
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
            CN1_CP_W_STR(fd, "END\n");
            close(fd);
        }
    }
    /* Chain to whichever handler was registered before us (the existing
     * signal-to-exception conversion in CodenameOne_GLAppDelegate.m).
     * On the off-chance no previous handler is set, fall back to the
     * default disposition so the process actually dies and gets a chance
     * to come back fresh -- not doing this would hang the app. */
    if (sig >= 0 && sig < NSIG && cn1_cp_prev_handlers[sig].sa_handler != NULL) {
        struct sigaction prev = cn1_cp_prev_handlers[sig];
        if (prev.sa_flags & SA_SIGINFO) {
            if (prev.sa_sigaction != NULL) {
                prev.sa_sigaction(sig, info, ctx);
                return;
            }
        } else if (prev.sa_handler != SIG_DFL && prev.sa_handler != SIG_IGN) {
            prev.sa_handler(sig);
            return;
        }
    }
    signal(sig, SIG_DFL);
    raise(sig);
}

/*
 * --- Uncaught Objective-C NSException handler --------------------------
 *
 * This runs on the main thread before the process is torn down. We can
 * still use Foundation here, so we write a richer payload than the
 * signal-only path: the NSException class, reason, and call-stack
 * symbols.
 */

static NSUncaughtExceptionHandler *cn1_cp_prev_nsexception_handler = NULL;

static void cn1_cp_nsexception_handler(NSException *exception) {
    if (cn1_cp_pending_path_set) {
        @try {
            NSMutableString *body = [NSMutableString stringWithCapacity:4096];
            [body appendString:@"CN1NATIVECRASH v1\n"];
            [body appendString:@"NSEXCEPTION 1\n"];
            [body appendFormat:@"NAME %@\n", exception.name ?: @""];
            [body appendFormat:@"REASON %@\n",
                    [(exception.reason ?: @"")
                            stringByReplacingOccurrencesOfString:@"\n"
                                                      withString:@" "]];
            intptr_t slide = _dyld_get_image_vmaddr_slide(0);
            [body appendFormat:@"SLIDE 0x%lx\n", (unsigned long)slide];
            NSArray<NSNumber *> *addresses = exception.callStackReturnAddresses;
            [body appendFormat:@"FRAMES %lu\n",
                    (unsigned long)addresses.count];
            for (NSNumber *n in addresses) {
                [body appendFormat:@"ADDR 0x%lx\n",
                        (unsigned long)n.unsignedLongLongValue];
            }
            [body appendString:@"SYMBOLS\n"];
            NSArray<NSString *> *symbols = exception.callStackSymbols;
            for (NSString *s in symbols) {
                [body appendFormat:@"%@\n", s];
            }
            [body appendString:@"END\n"];
            NSData *data = [body dataUsingEncoding:NSUTF8StringEncoding];
            [data writeToFile:@(cn1_cp_pending_path) atomically:YES];
        }
        @catch (NSException *ignored) {
            /* Crash protection must never crash. */
        }
    }
    if (cn1_cp_prev_nsexception_handler) {
        cn1_cp_prev_nsexception_handler(exception);
    }
}

/*
 * --- Install / read / wipe --------------------------------------------
 */

static void cn1_cp_resolve_pending_path(void) {
    NSArray<NSString *> *dirs = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, YES);
    if (dirs.count == 0) {
        return;
    }
    NSString *path = [dirs[0] stringByAppendingPathComponent:
            @".cn1_pending_native_crash"];
    const char *cstr = [path fileSystemRepresentation];
    if (cstr == NULL) {
        return;
    }
    size_t len = strlen(cstr);
    if (len >= sizeof(cn1_cp_pending_path)) {
        /* Truncating a path is worse than failing -- bail. */
        return;
    }
    memcpy(cn1_cp_pending_path, cstr, len + 1);
    cn1_cp_pending_path_set = 1;
}

static void cn1_cp_install_signal_handlers(void) {
    int sigs[] = {SIGSEGV, SIGABRT, SIGBUS, SIGILL, SIGFPE, SIGTRAP, SIGPIPE};
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = cn1_cp_signal_handler;
    sa.sa_flags = SA_SIGINFO | SA_ONSTACK;
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
    if (!atomic_compare_exchange_strong(&cn1_cp_installed, &expected, 1)) {
        return;
    }
    cn1_cp_resolve_pending_path();
    cn1_cp_log_install();
    cn1_cp_install_signal_handlers();
    cn1_cp_prev_nsexception_handler = NSGetUncaughtExceptionHandler();
    NSSetUncaughtExceptionHandler(cn1_cp_nsexception_handler);
}

NSString * cn1_crash_protection_consume_pending(void) {
    if (!cn1_cp_pending_path_set) {
        return nil;
    }
    NSString *path = @(cn1_cp_pending_path);
    NSFileManager *fm = [NSFileManager defaultManager];
    if (![fm fileExistsAtPath:path]) {
        return nil;
    }
    NSError *err = nil;
    NSString *contents = [NSString stringWithContentsOfFile:path
                                                   encoding:NSUTF8StringEncoding
                                                      error:&err];
    /* Delete unconditionally -- a corrupt file would otherwise replay
     * forever and prevent any new native crash from being recorded. */
    [fm removeItemAtPath:path error:NULL];
    if (contents.length == 0) {
        return nil;
    }
    return contents;
}

/*
 * --- ParparVM JNI bridge ----------------------------------------------
 *
 * Backs the three native methods on IOSNative:
 *   crashProtectionInstall()
 *   crashProtectionLogSnapshot()
 *   crashProtectionConsumePending()
 */

/* fromNSString comes in via CodenameOne_GLViewController.h -> xmlvm.h ->
 * cn1_globals.h; no forward declaration needed. */

void com_codename1_impl_ios_IOSNative_crashProtectionInstall__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    cn1_crash_protection_install();
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_crashProtectionLogSnapshot___R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    __block JAVA_OBJECT result = JAVA_NULL;
    POOL_BEGIN();
    NSString *s = cn1_crash_protection_log_snapshot();
    if (s != nil) {
        result = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG s);
    }
    POOL_END();
    return result;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_crashProtectionConsumePending___R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    __block JAVA_OBJECT result = JAVA_NULL;
    POOL_BEGIN();
    NSString *s = cn1_crash_protection_consume_pending();
    if (s != nil) {
        result = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG s);
    }
    POOL_END();
    return result;
}
