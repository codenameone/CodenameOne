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
package com.codename1.impl.javase.bluetooth;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * The wall-clock {@link SimScheduler} used when the simulator actually
 * runs: a single daemon thread named {@code cn1-bluetooth-sim}. This class
 * is the only place in the simulated stack that touches real time -- all
 * stack logic expresses time exclusively through
 * {@link SimScheduler#postDelayed(Runnable, long)}.
 */
public final class AutoScheduler implements SimScheduler {

    private final ScheduledThreadPoolExecutor executor;

    public AutoScheduler() {
        executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "cn1-bluetooth-sim");
                t.setDaemon(true);
                return t;
            }
        });
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    public void post(Runnable task) {
        postDelayed(task, 0);
    }

    public void postDelayed(Runnable task, long millis) {
        if (task == null) {
            return;
        }
        try {
            executor.schedule(task, Math.max(0, millis),
                    TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ignored) {
            // shut down -- drop the task, mirroring ManualScheduler
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
