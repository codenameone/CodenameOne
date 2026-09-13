/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
 * Waiting for SIGTERM, the way a container tells a process to stop.
 *
 * The self-pipe trick: the signal handler does one write() of a single byte -- one
 * of the few calls that is async-signal-safe -- and a Java thread turns the
 * asynchronous event into an ordinary blocking read.
 *
 * The obvious alternative, blocking the signals everywhere and calling sigwait()
 * on a dedicated thread, does NOT work here. pthread_sigmask only affects the
 * calling thread and threads created after it, and ParparVM has already started
 * its collector thread before main() runs. A signal delivered to that thread finds
 * it unblocked and takes the default action, which is to kill the process --
 * measured: SIGTERM terminated the server with an in-flight request still open and
 * no shutdown hook ever ran.
 *
 * A handler that called into the VM instead would be worse: it runs on whichever
 * thread the signal lands on, in async-signal-safe context, so allocating, taking
 * a monitor or touching the collector from it is undefined.
 */
#include "cn1_globals.h"
#include <stdio.h>
#include <errno.h>
#include <string.h>
#ifndef _WIN32
#include <signal.h>
#include <unistd.h>
#include <fcntl.h>
#endif

#ifndef _WIN32
static int cn1SignalPipe[2] = {-1, -1};

static void cn1SignalHandler(int signo) {
    unsigned char byte = (unsigned char)signo;
    /* write() is async-signal-safe; nothing else here would be. The result is
       deliberately ignored: a full pipe means a shutdown is already pending. */
    ssize_t ignored = write(cn1SignalPipe[1], &byte, 1);
    (void)ignored;
}
#endif

JAVA_INT com_codename1_backend_Signals_blockImpl___R_int(CODENAME_ONE_THREAD_STATE) {
#ifdef _WIN32
    return -1;
#else
    struct sigaction sa;
    if(cn1SignalPipe[0] >= 0) {
        return 0; /* already installed */
    }
    if(pipe(cn1SignalPipe) != 0) {
        return -1;
    }
    /* The write end must not block inside the handler. */
    fcntl(cn1SignalPipe[1], F_SETFL, fcntl(cn1SignalPipe[1], F_GETFL, 0) | O_NONBLOCK);
    fcntl(cn1SignalPipe[0], F_SETFD, FD_CLOEXEC);
    fcntl(cn1SignalPipe[1], F_SETFD, FD_CLOEXEC);

    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = cn1SignalHandler;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_RESTART; /* do not turn every blocking call into EINTR */
    if(sigaction(SIGINT, &sa, NULL) != 0 || sigaction(SIGTERM, &sa, NULL) != 0) {
        return -1;
    }
    /* SIGPIPE is ignored rather than caught: writing to a socket whose peer has
       gone is routine for a server, and the default action is to kill the process.
       Ignored, the write returns EPIPE like any other error. */
    signal(SIGPIPE, SIG_IGN);
    return 0;
#endif
}

/* Blocks until SIGINT or SIGTERM arrives; returns the signal number, or -1. */
JAVA_INT com_codename1_backend_Signals_awaitImpl___R_int(CODENAME_ONE_THREAD_STATE) {
#ifdef _WIN32
    return -1;
#else
    unsigned char byte = 0;
    ssize_t n;
    if(cn1SignalPipe[0] < 0) {
        return -1;
    }
    CN1_YIELD_THREAD;
    do {
        n = read(cn1SignalPipe[0], &byte, 1);
    } while(n < 0 && errno == EINTR);
    CN1_RESUME_THREAD;
    return n == 1 ? (JAVA_INT)byte : -1;
#endif
}
