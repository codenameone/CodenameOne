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
package com.codename1.realtime;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// A thread that runs a [RealtimeTask] once per period. Created and started by
/// [Realtime#start(String, RealtimeParameters, RealtimeTask)].
///
/// The thread applies its scheduling, runs the task's `setup()`, freezes allocation and
/// then loops: wait for the next release, run `cycle()`. [#stop()] ends the loop after
/// the cycle in progress.
@com.codename1.impl.SharedWithBackend
public final class RealtimeThread {
    /// [#getState()]: created, not yet running cycles.
    public static final int STATE_STARTING = 0;
    /// [#getState()]: running cycles.
    public static final int STATE_RUNNING = 1;
    /// [#getState()]: ended by [#stop()].
    public static final int STATE_STOPPED = 2;
    /// [#getState()]: ended by a failure, see [#getFailure()].
    public static final int STATE_FAILED = 3;

    private final Realtime platform;
    private final String name;
    private final RealtimeParameters params;
    private final RealtimeTask task;
    private final AtomicBoolean running = new AtomicBoolean(true);
    // Made before the freeze: the VM throws this instance at a refused allocation, so
    // the throw never needs the allocation it refused.
    private final RealtimeAllocationError allocationError =
            new RealtimeAllocationError("allocation in a real-time cycle");
    // Written by the real-time thread after every cycle, read by getStatistics().
    private final long[] live = new long[RealtimeStatistics.COUNT];
    private Thread thread;
    // Written by the real-time thread, read by any: atomics, not locks, so the
    // real-time side never blocks on a reader.
    private final AtomicInteger state = new AtomicInteger(STATE_STARTING);
    private final AtomicInteger applied = new AtomicInteger();
    private final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

    // The default (Java) timing in Realtime keeps its state here; a native
    // implementation keeps its own, per thread, and leaves these alone.
    long periodNanos;
    long nextReleaseNanos;
    long wokeNanos;
    final long[] javaStats = new long[RealtimeStatistics.COUNT];

    RealtimeThread(Realtime platform, String name, RealtimeParameters params, RealtimeTask task) {
        this.platform = platform;
        this.name = name;
        this.params = params.copy();
        this.task = task;
    }

    void begin() {
        thread = new Thread(new Runnable() {
            public void run() { //NOPMD MissingOverride - Java 5 source forbids @Override on an interface method
                body();
            }
        }, name);
        thread.start();
    }

    private void body() {
        int r = platform.rawEnter(this, params.getPriority(), params.getCpu(), params.isIsolateCpu());
        try {
            if (r < 0) {
                fail(new IllegalStateException("real-time scheduling rejected, errno " + (-r)));
                return;
            }
            applied.set(r);
            if (params.isSchedulingRequired() && (r & Realtime.APPLIED_SCHEDULING) == 0) {
                fail(new IllegalStateException("real-time scheduling was refused by the operating system;"
                        + " the process needs CAP_SYS_NICE or an rtprio limit"));
                return;
            }
            try {
                task.setup();
            } catch (Throwable t) {
                fail(t);
                return;
            }
            platform.rawStartPeriod(this, params.getPeriodNanos());
            state.set(STATE_RUNNING);
            loop();
        } finally {
            platform.rawThaw(this);
            platform.rawLeave(this);
            try {
                task.stopped();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    // The steady state. Nothing here allocates: the flag is an AtomicBoolean read, the
    // statistics are copied into an array made in the constructor, and every call is a
    // primitive or the task's own.
    private void loop() {
        platform.rawFreeze(this, params.getAllocationBudget(), allocationError);
        try {
            while (running.get()) {
                int missed = platform.rawWaitNextPeriod(this);
                if (!running.get()) {
                    break;
                }
                if (missed > 0) {
                    task.periodsMissed(missed);
                }
                task.cycle();
                platform.rawStatistics(this, live);
            }
            state.set(STATE_STOPPED);
        } catch (RealtimeAllocationError e) {
            platform.rawThaw(this);
            platform.rawStatistics(this, live);
            String cls = platform.rawLastViolation(this);
            fail(new RealtimeAllocationError("a real-time cycle allocated "
                    + (cls == null ? "an object" : cls)
                    + " beyond its budget of " + params.getAllocationBudget() + " per period"));
        } catch (Throwable t) {
            platform.rawThaw(this);
            fail(t);
        }
    }

    private void fail(Throwable t) {
        failure.set(t);
        state.set(STATE_FAILED);
        try {
            task.failed(t);
        } catch (Throwable inner) {
            inner.printStackTrace();
        }
    }

    /// The name given at start.
    public String getName() {
        return name;
    }

    /// One of the `STATE_` constants.
    public int getState() {
        return state.get();
    }

    /// What ended the task when [#getState()] is `STATE_FAILED`, otherwise `null`.
    public Throwable getFailure() {
        return state.get() == STATE_FAILED ? failure.get() : null;
    }

    /// `true` when the operating system granted real-time scheduling. `false` on a
    /// platform without it, or a process without the privilege, where the task runs at
    /// normal priority.
    public boolean isSchedulingApplied() {
        return (applied.get() & Realtime.APPLIED_SCHEDULING) != 0;
    }

    /// `true` when the thread was pinned to the CPU its parameters named.
    public boolean isCpuPinned() {
        return (applied.get() & Realtime.APPLIED_AFFINITY) != 0;
    }

    /// A snapshot of the thread's statistics, as of the last completed cycle.
    public RealtimeStatistics getStatistics() {
        long[] copy = new long[RealtimeStatistics.COUNT];
        System.arraycopy(live, 0, copy, 0, copy.length);
        return new RealtimeStatistics(copy);
    }

    /// Ends the loop after the cycle in progress. The thread then calls the task's
    /// `stopped()` and exits. Returns at once; use [#join(long)] to wait.
    public void stop() {
        running.set(false);
    }

    /// Waits for the thread to exit.
    ///
    /// #### Parameters
    ///
    /// - `millis`: the longest to wait, 0 for no limit
    public void join(long millis) throws InterruptedException {
        Thread t = thread;
        if (t != null) {
            t.join(millis);
        }
    }
}
