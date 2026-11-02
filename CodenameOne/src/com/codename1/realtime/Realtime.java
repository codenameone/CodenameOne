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

/// Entry point to real-time threads: periodic tasks that run at a fixed operating-system
/// priority, are released on an absolute clock, and are not paused by the garbage
/// collector.
///
/// ```java
/// Realtime rt = Realtime.getInstance();
/// rt.lockMemory();                          // no page faults in a period
/// rt.setMemoryLimit(64L * 1024 * 1024);     // fail fast instead of thrashing
/// RealtimeThread motor = rt.start("motor",
///         new RealtimeParameters().setPeriodNanos(1000000L).setPriority(80).setCpu(3),
///         new RealtimeTask() {
///             public void cycle() {
///                 // every period; must not allocate (see RealtimeTask)
///             }
///         });
/// // the application runs; the motor keeps its period meanwhile
/// Log.p(motor.getStatistics().toString());
/// ```
///
/// #### How the collector is kept away
///
/// A real-time task allocates in `setup()` and never in `cycle()`. The build rejects an
/// allocation that `cycle()` reaches, and at run time an allocation it could not see
/// ends the steady state with a [RealtimeAllocationError] -- so the thread never needs
/// the collector to make room. The collector in turn never stops it for a whole cycle:
/// collections stay concurrent while a real-time thread exists, its stack is scanned
/// while it sleeps between periods, and it is released as soon as that one stack has
/// been read. Normal threads run below it, the collector's included, so a busy
/// collector slows them, not it.
///
/// #### What this is and is not
///
/// On Linux with a `PREEMPT_RT` kernel this is firm real time: release latency in tens
/// of microseconds, measured, not proven. It is not certified hard real time and makes
/// no claim under any safety standard. Measure the loop on the device, under load,
/// with [RealtimeThread#getStatistics()].
///
/// #### Platforms
///
/// - **Linux** (the embedded and desktop Linux targets, the backend on Linux): every
///   feature. Real-time scheduling needs `CAP_SYS_NICE` or an `rtprio` limit.
/// - **Other translated targets** (iOS, macOS, Windows, the backend elsewhere): the
///   collector isolation and the allocation check, with normal-priority threads and a
///   relative sleep.
/// - **The simulator, Android, JavaScript**: a best-effort Java thread with the same
///   API, so the application runs and can be tested anywhere; there is no isolation
///   from the collector and no allocation check at run time.
///
/// #### For port implementors
///
/// A port subclasses this class and overrides the protected primitives, which act on
/// the calling thread. The defaults are the best-effort Java implementation.
@com.codename1.impl.SharedWithBackend
public class Realtime {

    /// Real-time scheduling (`SCHED_FIFO` on Linux).
    public static final int FEATURE_SCHEDULING = 1;
    /// Locking the process's memory so it is never paged out ([#lockMemory()]).
    public static final int FEATURE_MEMORY_LOCK = 2;
    /// Real-time threads are not paused by the garbage collector.
    public static final int FEATURE_GC_ISOLATION = 4;
    /// Allocation in a cycle is detected at run time.
    public static final int FEATURE_ALLOCATION_CHECK = 8;
    /// The kernel is a `PREEMPT_RT` kernel, which bounds its own latency.
    public static final int FEATURE_PREEMPT_RT_KERNEL = 16;
    /// Threads can be pinned to a CPU.
    public static final int FEATURE_CPU_AFFINITY = 32;

    /// [#rawEnter(RealtimeThread, int, int, boolean)] result bit: scheduling applied.
    protected static final int APPLIED_SCHEDULING = 1;
    /// [#rawEnter(RealtimeThread, int, int, boolean)] result bit: CPU pinned.
    protected static final int APPLIED_AFFINITY = 2;
    /// [#rawEnter(RealtimeThread, int, int, boolean)] result bit: other threads moved off.
    protected static final int APPLIED_ISOLATION = 4;

    private static final Realtime DEFAULT = new Realtime();
    private static Realtime installed;

    /// Ports construct subclasses; applications call [#getInstance()].
    protected Realtime() {
    }

    /// Returns the real-time entry point of the current platform. Never `null`: where
    /// nothing is installed this is the best-effort Java implementation.
    public static Realtime getInstance() {
        Realtime r = installed;
        return r != null ? r : DEFAULT;
    }

    /// Makes `realtime` the instance [#getInstance()] returns; `null` restores the
    /// default. `Display` installs the port's instance when it starts and the backend
    /// installs its own, which is why this class never names `Display`.
    public static void install(Realtime realtime) {
        installed = realtime;
    }

    /// `true` when the platform provides the feature, one of the `FEATURE_` constants.
    public boolean isSupported(int feature) {
        return (getSupportedFeatures() & feature) == feature;
    }

    /// Bit mask of the `FEATURE_` constants this platform provides. The default
    /// provides none.
    protected int getSupportedFeatures() {
        return 0;
    }

    /// Starts a real-time thread running `task`.
    ///
    /// #### Parameters
    ///
    /// - `name`: the thread's name, for logs and debuggers
    ///
    /// - `params`: its scheduling; copied, so later changes do not affect it
    ///
    /// - `task`: the work it runs
    ///
    /// #### Returns
    ///
    /// the running thread
    public RealtimeThread start(String name, RealtimeParameters params, RealtimeTask task) {
        if (name == null || params == null || task == null) {
            throw new IllegalArgumentException("name, params and task are required");
        }
        RealtimeThread t = new RealtimeThread(this, name, params, task);
        t.begin();
        return t;
    }

    /// Locks every page the process has and will have into memory, so a period never
    /// waits for one to be paged in. Call it early: it also makes every later
    /// allocation of memory resident at once.
    ///
    /// #### Returns
    ///
    /// `true` when the memory was locked; `false` where unsupported or refused (Linux
    /// needs `CAP_IPC_LOCK` or a large enough `memlock` limit)
    public boolean lockMemory() {
        return rawLockMemory() == 0;
    }

    /// Caps the memory of the process. After a collection that leaves the process
    /// above `bytes`, a normal thread that asks for more memory gets an
    /// `OutOfMemoryError` at once, instead of waiting for collections that cannot
    /// free enough -- a failure the application can handle, in place of a device that
    /// slows to a crawl. Real-time threads are never refused: they do not allocate.
    ///
    /// The limit is on the process's resident memory, because that is the memory a
    /// device runs out of; it includes memory the heap holds and has not returned
    /// yet. An allocation is refused when a collection leaves the process over the
    /// limit, or without waiting for one once the process is a quarter over it.
    /// Linux only; elsewhere this does nothing.
    ///
    /// #### Parameters
    ///
    /// - `bytes`: the limit, or 0 for none
    public void setMemoryLimit(long bytes) {
        rawSetMemoryLimit(bytes, new OutOfMemoryError("memory limit of " + bytes + " bytes reached"));
    }

    // ---------------------------------------------------------------- primitives

    /// Makes the calling thread real-time.
    ///
    /// #### Returns
    ///
    /// the `APPLIED_` bits for what took effect, or a negative errno
    protected int rawEnter(RealtimeThread thread, int priority, int cpu, boolean isolate) {
        return 0;
    }

    /// Returns the calling thread to normal scheduling.
    protected void rawLeave(RealtimeThread thread) {
    }

    /// Starts the periodic clock on the calling thread; the first release is one
    /// period from now.
    protected void rawStartPeriod(RealtimeThread thread, long periodNanos) {
        thread.periodNanos = periodNanos;
        thread.nextReleaseNanos = System.nanoTime() + periodNanos;
        thread.wokeNanos = 0;
    }

    /// Sleeps the calling thread to its next release.
    ///
    /// #### Returns
    ///
    /// how many releases were missed
    protected int rawWaitNextPeriod(RealtimeThread thread) {
        long now = System.nanoTime();
        long[] s = thread.javaStats;
        if (thread.wokeNanos != 0) {
            long exec = now - thread.wokeNanos;
            s[RealtimeStatistics.SUM_EXEC] += exec;
            if (exec > s[RealtimeStatistics.MAX_EXEC]) {
                s[RealtimeStatistics.MAX_EXEC] = exec;
            }
        }
        long left = thread.nextReleaseNanos - now;
        if (left > 0) {
            try {
                Thread.sleep(left / 1000000L);
            } catch (InterruptedException e) {
                // the release is absolute; carry on from wherever we woke
            }
            while (System.nanoTime() < thread.nextReleaseNanos) {
                Thread.yield();
            }
        }
        long woke = System.nanoTime();
        long latency = woke - thread.nextReleaseNanos;
        if (latency < 0) {
            latency = 0;
        }
        s[RealtimeStatistics.CYCLES]++;
        s[RealtimeStatistics.SUM_LATENCY] += latency;
        if (latency > s[RealtimeStatistics.MAX_LATENCY]) {
            s[RealtimeStatistics.MAX_LATENCY] = latency;
        }
        int b = 0;
        long us = latency / 1000L;
        while (us > 0 && b < RealtimeStatistics.HISTOGRAM_BUCKETS - 1) {
            us >>= 1;
            b++;
        }
        s[RealtimeStatistics.HIST + b]++;
        int missed = 0;
        thread.nextReleaseNanos += thread.periodNanos;
        while (thread.nextReleaseNanos <= woke) {
            thread.nextReleaseNanos += thread.periodNanos;
            missed++;
        }
        s[RealtimeStatistics.MISSED] += missed;
        thread.wokeNanos = woke;
        return missed;
    }

    /// Enters the steady state on the calling thread: from here an allocation beyond
    /// `budget` per period throws `error`. The default has no run-time check.
    protected void rawFreeze(RealtimeThread thread, int budget, RealtimeAllocationError error) {
    }

    /// Leaves the steady state on the calling thread. Safe to call when not frozen.
    protected void rawThaw(RealtimeThread thread) {
    }

    /// Copies the calling thread's statistics into `out`
    /// (`RealtimeStatistics.COUNT` values).
    protected void rawStatistics(RealtimeThread thread, long[] out) {
        System.arraycopy(thread.javaStats, 0, out, 0, out.length);
    }

    /// The class of the allocation that ended the calling thread's last steady state,
    /// or `null`.
    protected String rawLastViolation(RealtimeThread thread) {
        return null;
    }

    /// Locks the process's memory. Returns 0 or a negative errno; the default answers
    /// -38 (ENOSYS).
    protected int rawLockMemory() {
        return -38;
    }

    /// Installs the memory limit; `error` is the instance to throw. The default does
    /// nothing.
    protected void rawSetMemoryLimit(long bytes, OutOfMemoryError error) {
    }
}
