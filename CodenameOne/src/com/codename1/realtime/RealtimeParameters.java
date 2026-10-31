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

/// How a [RealtimeThread] is scheduled: its period, its operating-system priority, the
/// CPU it runs on and how much it may allocate per period.
///
/// ```java
/// RealtimeParameters p = new RealtimeParameters()
///         .setPeriodNanos(1000000L)      // 1 kHz
///         .setPriority(80)
///         .setCpu(3)                     // pinned, and other threads moved off it
///         .setSchedulingRequired(true);  // fail rather than run at normal priority
/// ```
@com.codename1.impl.SharedWithBackend
public final class RealtimeParameters {
    private long periodNanos = 1000000L;
    private int priority = 50;
    private int cpu = -1;
    private boolean isolateCpu = true;
    private int allocationBudget;
    private boolean schedulingRequired;

    /// The interval between releases, in nanoseconds. The default is one millisecond.
    public long getPeriodNanos() {
        return periodNanos;
    }

    /// Sets the interval between releases. Releases are absolute -- each is one period
    /// after the previous one, however long the cycle ran -- so the loop does not drift.
    ///
    /// #### Parameters
    ///
    /// - `periodNanos`: the period, at least 10 microseconds
    ///
    /// #### Returns
    ///
    /// this object, for chaining
    public RealtimeParameters setPeriodNanos(long periodNanos) {
        if (periodNanos < 10000L) {
            throw new IllegalArgumentException("period below 10 microseconds: " + periodNanos);
        }
        this.periodNanos = periodNanos;
        return this;
    }

    /// The operating-system priority, `SCHED_FIFO` on Linux. The default is 50.
    public int getPriority() {
        return priority;
    }

    /// Sets the operating-system priority, from 1 to 99 on Linux. A higher number
    /// preempts a lower one, and any of them preempts every normal thread, the
    /// collector's included. Leave the top of the range to the kernel's own
    /// interrupt threads unless the loop must preempt them.
    ///
    /// #### Parameters
    ///
    /// - `priority`: the priority, 1 to 99
    ///
    /// #### Returns
    ///
    /// this object, for chaining
    public RealtimeParameters setPriority(int priority) {
        if (priority < 1 || priority > 99) {
            throw new IllegalArgumentException("priority out of 1..99: " + priority);
        }
        this.priority = priority;
        return this;
    }

    /// The CPU the thread is pinned to, or -1 for none (the default).
    public int getCpu() {
        return cpu;
    }

    /// Pins the thread to one CPU. Combine it with the kernel's `isolcpus=` or a
    /// cpuset that keeps other processes off that CPU for the lowest latency.
    ///
    /// #### Parameters
    ///
    /// - `cpu`: the CPU number, or -1 for none
    ///
    /// #### Returns
    ///
    /// this object, for chaining
    public RealtimeParameters setCpu(int cpu) {
        if (cpu < -1) {
            throw new IllegalArgumentException("cpu: " + cpu);
        }
        this.cpu = cpu;
        return this;
    }

    /// Whether the application's other threads are moved off the pinned CPU. The
    /// default is `true`.
    public boolean isIsolateCpu() {
        return isolateCpu;
    }

    /// Sets whether the application's other threads -- the event dispatch thread, the
    /// collector, network threads -- are moved off the CPU this thread is pinned to,
    /// so they never compete with it there. Has no effect without [#setCpu(int)].
    ///
    /// #### Parameters
    ///
    /// - `isolateCpu`: `true` to move the other threads off
    ///
    /// #### Returns
    ///
    /// this object, for chaining
    public RealtimeParameters setIsolateCpu(boolean isolateCpu) {
        this.isolateCpu = isolateCpu;
        return this;
    }

    /// How many allocations a period may make. The default is zero.
    public int getAllocationBudget() {
        return allocationBudget;
    }

    /// Sets how many allocations each period may make before the thread fails with a
    /// [RealtimeAllocationError]. Zero, the default, is the setting to aim for: an
    /// allocation is a call into the collector's slow path, whose time is not bounded
    /// the way the cycle's own code is. A small budget is an escape hatch for a cycle
    /// that allocates on a rare path, not a way to allocate every period.
    ///
    /// #### Parameters
    ///
    /// - `allocationBudget`: allocations allowed per period, zero or more
    ///
    /// #### Returns
    ///
    /// this object, for chaining
    public RealtimeParameters setAllocationBudget(int allocationBudget) {
        if (allocationBudget < 0) {
            throw new IllegalArgumentException("allocationBudget: " + allocationBudget);
        }
        this.allocationBudget = allocationBudget;
        return this;
    }

    /// Whether the thread fails when real-time scheduling cannot be applied. The
    /// default is `false`.
    public boolean isSchedulingRequired() {
        return schedulingRequired;
    }

    /// Sets whether the thread fails, through [RealtimeTask#failed(Throwable)], when
    /// the operating system refuses real-time scheduling -- on Linux, a process
    /// without `CAP_SYS_NICE` or an `rtprio` limit. When `false`, the default, the
    /// task runs at normal priority instead, which is what a development machine and
    /// the simulator do; [RealtimeThread#isSchedulingApplied()] says which happened.
    ///
    /// #### Parameters
    ///
    /// - `schedulingRequired`: `true` to fail without real-time scheduling
    ///
    /// #### Returns
    ///
    /// this object, for chaining
    public RealtimeParameters setSchedulingRequired(boolean schedulingRequired) {
        this.schedulingRequired = schedulingRequired;
        return this;
    }

    RealtimeParameters copy() {
        RealtimeParameters p = new RealtimeParameters();
        p.periodNanos = periodNanos;
        p.priority = priority;
        p.cpu = cpu;
        p.isolateCpu = isolateCpu;
        p.allocationBudget = allocationBudget;
        p.schedulingRequired = schedulingRequired;
        return p;
    }
}
