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
 * Native child-process (subprocess) transport for the Codename One Linux port.
 * The native ports have full java.lang.Thread but no java.lang.Process /
 * ProcessBuilder (absent from vm/JavaAPI), so a Java NativeSubprocessTransport
 * that needs to spawn a helper binary (e.g. the Rust Bluetooth helper) and talk
 * to it over stdio drives these primitives instead.
 *
 * A subprocess peer is a small heap struct holding the child pid and the
 * parent-side ends of two pipes wired to the child's stdin/stdout (stderr is
 * inherited). It mirrors the socket bridge (cn1_linux_socket.c) exactly:
 * blocking read/write are bracketed with CN1_YIELD_THREAD / CN1_RESUME_THREAD so
 * the concurrent GC is not stalled by a thread parked in a syscall, and the
 * byte[] passed to a blocking read/write is anchored with CN1_PROC_KEEP_ALIVE so
 * the conservative GC cannot sweep it mid-I/O.
 */

#define _GNU_SOURCE
#include "cn1_linux.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <spawn.h>
#include <sys/types.h>
#include <sys/wait.h>

extern char** environ;
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

/* Forces a read/write buffer object to stay live across a parked blocking call so
 * the conservative GC can't sweep it mid-I/O -- identical idiom and rationale to
 * CN1_SOCKET_KEEP_ALIVE in cn1_linux_socket.c (an address-escape asm that cannot
 * be optimized away and shares no cross-thread state). */
#define CN1_PROC_KEEP_ALIVE(obj) __asm__ __volatile__("" : : "r"(obj))

typedef struct {
    pid_t pid;
    int stdinFd;   /* parent write end -> child stdin  (-1 once closed) */
    int stdoutFd;  /* parent read  end <- child stdout (-1 once closed) */
    int exited;    /* 1 once the child has been reaped */
    int exitStatus;
} CN1Subprocess;

/* Non-blocking reap: if the child has exited, records it and returns 1. */
static int cn1ProcReap(CN1Subprocess* p) {
    int status;
    pid_t r;
    if (p->exited) {
        return 1;
    }
    r = waitpid(p->pid, &status, WNOHANG);
    if (r == p->pid) {
        p->exited = 1;
        p->exitStatus = status;
        return 1;
    }
    /* r == 0 -> still running; r < 0 (ECHILD etc.) -> treat as gone. */
    if (r < 0) {
        p->exited = 1;
        return 1;
    }
    return 0;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_procSpawn___java_lang_String_1ARRAY_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT argv) {
    int argc;
    int i;
    char** cargv = 0;
    JAVA_OBJECT* elements;
    int inPipe[2];
    int outPipe[2];
    posix_spawn_file_actions_t actions;
    int actionsInited = 0;
    pid_t pid = 0;
    CN1Subprocess* p;

    if (argv == JAVA_NULL) {
        return 0;
    }
    argc = (int) (*(JAVA_ARRAY) argv).length;
    if (argc <= 0) {
        return 0;
    }
    elements = (JAVA_OBJECT*) (*(JAVA_ARRAY) argv).data;

    /* Build a NULL-terminated char** from the String[]. stringToUTF8 returns a
     * per-thread reusable buffer that the next call overwrites, so each element
     * MUST be strdup'd immediately after conversion. */
    cargv = (char**) calloc((size_t) argc + 1, sizeof(char*));
    if (!cargv) {
        return 0;
    }
    for (i = 0; i < argc; i++) {
        const char* s = elements[i] == JAVA_NULL ? "" : stringToUTF8(threadStateData, elements[i]);
        cargv[i] = strdup(s ? s : "");
        if (!cargv[i]) {
            goto fail;
        }
    }
    cargv[argc] = 0;

    if (pipe(inPipe) != 0) {
        goto fail;
    }
    if (pipe(outPipe) != 0) {
        close(inPipe[0]);
        close(inPipe[1]);
        goto fail;
    }

    if (posix_spawn_file_actions_init(&actions) != 0) {
        close(inPipe[0]);
        close(inPipe[1]);
        close(outPipe[0]);
        close(outPipe[1]);
        goto fail;
    }
    actionsInited = 1;
    /* Child: stdin <- inPipe read end, stdout -> outPipe write end. The parent
     * ends (inPipe[1], outPipe[0]) are closed in the child so it sees EOF when
     * the parent closes its side. stderr is inherited. */
    posix_spawn_file_actions_adddup2(&actions, inPipe[0], STDIN_FILENO);
    posix_spawn_file_actions_adddup2(&actions, outPipe[1], STDOUT_FILENO);
    posix_spawn_file_actions_addclose(&actions, inPipe[0]);
    posix_spawn_file_actions_addclose(&actions, inPipe[1]);
    posix_spawn_file_actions_addclose(&actions, outPipe[0]);
    posix_spawn_file_actions_addclose(&actions, outPipe[1]);

    /* posix_spawnp so argv[0] is resolved against PATH, like execvp. */
    if (posix_spawnp(&pid, cargv[0], &actions, 0, cargv, environ) != 0) {
        posix_spawn_file_actions_destroy(&actions);
        close(inPipe[0]);
        close(inPipe[1]);
        close(outPipe[0]);
        close(outPipe[1]);
        goto fail;
    }
    posix_spawn_file_actions_destroy(&actions);
    actionsInited = 0;

    /* Parent keeps the write end of stdin and the read end of stdout; the
     * child-side ends are ours to close now. */
    close(inPipe[0]);
    close(outPipe[1]);

    for (i = 0; i < argc; i++) {
        free(cargv[i]);
    }
    free(cargv);

    p = (CN1Subprocess*) calloc(1, sizeof(CN1Subprocess));
    if (!p) {
        /* Spawn succeeded but we can't track it: terminate to avoid an orphan. */
        kill(pid, SIGTERM);
        close(inPipe[1]);
        close(outPipe[0]);
        return 0;
    }
    p->pid = pid;
    p->stdinFd = inPipe[1];
    p->stdoutFd = outPipe[0];
    p->exited = 0;
    return (JAVA_LONG) (intptr_t) p;

fail:
    if (actionsInited) {
        posix_spawn_file_actions_destroy(&actions);
    }
    if (cargv) {
        for (i = 0; i < argc; i++) {
            free(cargv[i]);
        }
        free(cargv);
    }
    return 0;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_procRead___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    CN1Subprocess* p = (CN1Subprocess*) (intptr_t) handle;
    char* data;
    ssize_t n;
    if (!p || p->stdoutFd < 0 || buffer == JAVA_NULL || length <= 0) {
        return -1;
    }
    data = (char*) (*(JAVA_ARRAY) buffer).data;
    /* read() blocks until the helper writes (or its stdout closes). Park the
     * thread across the syscall so the concurrent GC -- which waits for every
     * lightweight thread to pause before it can traverse stacks -- is not
     * deadlocked (see cn1_linux_socket.c socketRead). */
    CN1_YIELD_THREAD;
    n = read(p->stdoutFd, data + offset, (size_t) length);
    CN1_RESUME_THREAD;
    /* Keep the buffer array reachable across the parked read: only `data` (an
     * interior pointer) is used, so the optimizer may drop `buffer` and the GC,
     * scanning this parked thread, can sweep it mid-read (use-after-free). */
    CN1_PROC_KEEP_ALIVE(buffer);
    if (n < 0) {
        return -1;
    }
    return (JAVA_INT) n; /* 0 == EOF (child closed stdout / exited) */
}

JAVA_INT com_codename1_impl_linux_LinuxNative_procWrite___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    CN1Subprocess* p = (CN1Subprocess*) (intptr_t) handle;
    char* data;
    int written = 0;
    if (!p || p->stdinFd < 0 || buffer == JAVA_NULL || length <= 0) {
        return -1;
    }
    data = (char*) (*(JAVA_ARRAY) buffer).data;
    /* Ignore SIGPIPE for this write so a dead helper reports EPIPE instead of
     * killing the process; the return value already surfaces the error. */
    signal(SIGPIPE, SIG_IGN);
    /* Yield across the (potentially blocking) write loop -- a thread parked in
     * write() must not stall a GC mark (see socketWrite). */
    CN1_YIELD_THREAD;
    while (written < length) {
        ssize_t n = write(p->stdinFd, data + offset + written, (size_t) (length - written));
        if (n <= 0) {
            CN1_RESUME_THREAD;
            /* Keep the array reachable across the parked write, exactly like
             * socketWrite: only `data` (an interior pointer) is used after the
             * yield, so the GC could otherwise sweep the array WHILE write() is
             * still reading from it. */
            CN1_PROC_KEEP_ALIVE(buffer);
            return written > 0 ? written : -1;
        }
        written += (int) n;
    }
    CN1_RESUME_THREAD;
    CN1_PROC_KEEP_ALIVE(buffer);
    return written;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_procCloseStdin___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1Subprocess* p = (CN1Subprocess*) (intptr_t) handle;
    if (!p) {
        return;
    }
    if (p->stdinFd >= 0) {
        close(p->stdinFd);
        p->stdinFd = -1;
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_procClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1Subprocess* p = (CN1Subprocess*) (intptr_t) handle;
    if (!p) {
        return;
    }
    if (p->stdinFd >= 0) {
        close(p->stdinFd);
        p->stdinFd = -1;
    }
    if (p->stdoutFd >= 0) {
        close(p->stdoutFd);
        p->stdoutFd = -1;
    }
    if (!cn1ProcReap(p)) {
        /* Still running: ask it to stop, then reap so it does not become a
         * zombie. A brief blocking waitpid is acceptable here (close is not on a
         * hot path); park across it for the GC. */
        kill(p->pid, SIGTERM);
        CN1_YIELD_THREAD;
        waitpid(p->pid, &p->exitStatus, 0);
        CN1_RESUME_THREAD;
        p->exited = 1;
    }
    free(p);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_procIsAlive___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1Subprocess* p = (CN1Subprocess*) (intptr_t) handle;
    if (!p) {
        return 0;
    }
    return cn1ProcReap(p) ? 0 : 1;
}
