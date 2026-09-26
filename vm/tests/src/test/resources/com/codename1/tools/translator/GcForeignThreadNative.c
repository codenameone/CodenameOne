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

/* Registers threads the VM did not start, the way a port callback does: by touching the
   VM from them. See GcForeignThreadApp. */
#include "cn1_globals.h"
#include <pthread.h>
#if defined(__APPLE__)
#include <dispatch/dispatch.h>
#endif

/* Touches the VM, then EXITS. */
static void* cn1ForeignExitingThread(void* arg) {
    (void)arg;
    getThreadLocalData();
    return 0;
}

#if defined(__APPLE__)
/* Touches the VM from a libdispatch worker, which then goes back to the pool. */
static void cn1ForeignGcdTouch(void* done) {
    getThreadLocalData();
    dispatch_semaphore_signal((dispatch_semaphore_t)done);
}
#endif

JAVA_INT GcForeignThreadApp_registerForeignThreads___R_int(CODENAME_ONE_THREAD_STATE) {
    int registered = 0;
    /* Parked for the waits below, so a collection that starts meanwhile is not held up
       by this thread. */
    CN1_YIELD_THREAD;
    pthread_t exiting;
    if(pthread_create(&exiting, 0, cn1ForeignExitingThread, 0) == 0) {
        pthread_join(exiting, 0);
        registered++;
    }
#if defined(__APPLE__)
    dispatch_semaphore_t done = dispatch_semaphore_create(0);
    dispatch_async_f(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), done,
                     cn1ForeignGcdTouch);
    dispatch_semaphore_wait(done, DISPATCH_TIME_FOREVER);
    registered++;
#endif
    CN1_RESUME_THREAD;
    return registered;
}
