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
package com.demo;

import com.codename1.backend.BoardRealtime;
import com.codename1.realtime.Realtime;
import com.codename1.realtime.RealtimeAllocationError;
import com.codename1.realtime.RealtimeParameters;
import com.codename1.realtime.RealtimeStatistics;
import com.codename1.realtime.RealtimeTask;
import com.codename1.realtime.RealtimeThread;
import java.util.ArrayList;

/// The real-time check, translated and run natively.
///
/// 1. A 1 kHz loop runs while the main thread allocates hard enough to keep the
///    collector busy for the whole run. Its release latency is the result: with the
///    collector unable to stop it, the worst case is the scheduler's, not the GC's.
/// 2. A loop that allocates through an interface call -- a path the build cannot
///    resolve, so it compiles -- must end with a RealtimeAllocationError naming the
///    class, at run time.
/// 3. (run first) With a memory limit set, a thread that keeps allocating must get an
///    OutOfMemoryError rather than stall (Linux only: the limit reads the process's
///    resident memory).
///
/// Arguments: `seconds maxLatencyMicros`; a latency bound of 0 reports without
/// judging, for a machine whose kernel is not PREEMPT_RT.
public final class RtCheck {
    private RtCheck() {
    }

    /// The steady loop: a fixed amount of arithmetic over arrays made in setup().
    static final class Steady extends RealtimeTask {
        private double[] state;
        private long checksum;

        @Override
        public void setup() {
            state = new double[256];
            for (int i = 0; i < state.length; i++) {
                state[i] = i;
            }
        }

        @Override
        public void cycle() {
            double acc = 0;
            for (int i = 0; i < state.length; i++) {
                state[i] = state[i] * 0.999 + 0.001 * i;
                acc += state[i];
            }
            checksum += (long) acc;
        }
    }

    interface Sample {
        Object take();
    }

    // Not Integer.valueOf: boxed ints are immediates on this VM and never allocate.
    static final class Fresh implements Sample {
        public Object take() {
            return new long[4];
        }
    }

    static final class Cached implements Sample {
        private final Object value = new Object();

        public Object take() {
            return value;
        }
    }

    /// Allocates on its second cycle, through a call with two possible targets.
    static final class Violator extends RealtimeTask {
        private final Sample[] samples = {new Cached(), new Fresh()};
        private int cycles;
        volatile Throwable failure;

        @Override
        public void cycle() {
            Object o = samples[cycles++ & 1].take();
            if (o == null) {
                cycles = 0;
            }
        }

        @Override
        public void failed(Throwable t) {
            failure = t;
        }
    }

    public static void main(String[] args) throws Exception {
        int seconds = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        long maxLatencyMicros = args.length > 1 ? Long.parseLong(args[1]) : 0;
        BoardRealtime.install();
        Realtime rt = Realtime.getInstance();
        boolean linux = rt.isSupported(Realtime.FEATURE_SCHEDULING);
        System.out.println("RTCHECK features scheduling=" + linux
                + " preemptRt=" + rt.isSupported(Realtime.FEATURE_PREEMPT_RT_KERNEL)
                + " gcIsolation=" + rt.isSupported(Realtime.FEATURE_GC_ISOLATION)
                + " allocationCheck=" + rt.isSupported(Realtime.FEATURE_ALLOCATION_CHECK)
                + " memoryLocked=" + rt.lockMemory());
        boolean ok = true;

        // 3. memory limit -- first, while the process is small: the limit counts
        // resident memory, and after step 1 the heap still holds pages it has not
        // given back yet.
        if (linux) {
            // 512MB: far above this program at rest, far below the 4GB it then holds.
            rt.setMemoryLimit(512L * 1024 * 1024);
            ArrayList<byte[]> hoard = new ArrayList<byte[]>();
            String outcome = "no error after 4GB";
            try {
                for (int i = 0; i < 4096; i++) {
                    // Written to: an untouched array is zero pages the kernel has not
                    // backed yet, and costs no resident memory at all.
                    byte[] b = new byte[1024 * 1024];
                    for (int j = 0; j < b.length; j += 4096) {
                        b[j] = 1;
                    }
                    hoard.add(b);
                    if ((i & 15) == 0) {
                        System.gc();
                    }
                }
            } catch (OutOfMemoryError e) {
                outcome = "OutOfMemoryError after " + hoard.size() + "MB";
            }
            int hoardedMb = hoard.size();
            hoard = null;
            rt.setMemoryLimit(0);
            System.out.println("RTCHECK memory limit: " + outcome);
            if (outcome.startsWith("no error") || hoardedMb < 128) {
                ok = false;
            }
        }
        // 1. latency under collector load
        RealtimeThread steady = rt.start("steady",
                new RealtimeParameters().setPeriodNanos(1000000L).setPriority(80), new Steady());
        long end = System.currentTimeMillis() + seconds * 1000L;
        long churned = 0;
        ArrayList<Object> keep = new ArrayList<Object>();
        while (System.currentTimeMillis() < end) {
            for (int i = 0; i < 2000; i++) {
                keep.add(new StringBuilder().append("garbage ").append(i).toString());
            }
            churned += keep.size();
            if (keep.size() > 200000) {
                keep = new ArrayList<Object>();
            }
        }
        steady.stop();
        steady.join(5000);
        RealtimeStatistics s = steady.getStatistics();
        System.out.println("RTCHECK steady scheduling=" + steady.isSchedulingApplied()
                + " churned=" + churned + " " + s);
        long expected = seconds * 1000L;
        if (s.getCycles() + s.getMissedPeriods() < expected * 9 / 10) {
            System.out.println("RTCHECK FAIL steady ran " + s.getCycles() + " cycles, expected about " + expected);
            ok = false;
        }
        if (s.getAllocationViolations() != 0 || steady.getState() != RealtimeThread.STATE_STOPPED) {
            System.out.println("RTCHECK FAIL steady state=" + steady.getState() + " failure=" + steady.getFailure());
            ok = false;
        }
        if (maxLatencyMicros > 0 && s.getMaxLatencyNanos() > maxLatencyMicros * 1000L) {
            System.out.println("RTCHECK FAIL max latency " + s.getMaxLatencyNanos() / 1000
                    + "us over the bound of " + maxLatencyMicros + "us");
            ok = false;
        }

        // 2. allocation the build could not see
        if (rt.isSupported(Realtime.FEATURE_ALLOCATION_CHECK)) {
            Violator v = new Violator();
            RealtimeThread vt = rt.start("violator",
                    new RealtimeParameters().setPeriodNanos(1000000L).setPriority(70), v);
            vt.join(5000);
            vt.stop();
            vt.join(1000);
            Throwable f = vt.getFailure();
            System.out.println("RTCHECK violator state=" + vt.getState() + " failure=" + f);
            if (!(f instanceof RealtimeAllocationError) || f.getMessage().indexOf("long") < 0) {
                System.out.println("RTCHECK FAIL the allocation in the cycle was not refused");
                ok = false;
            }
        }

        System.out.println(ok ? "RTCHECK OK" : "RTCHECK FAILED");
        System.exit(ok ? 0 : 1);
    }
}
