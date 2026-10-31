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
package com.codename1.backend;

import com.codename1.realtime.Realtime;
import com.codename1.realtime.RealtimeAllocationError;
import com.codename1.realtime.RealtimeThread;

/// The headless backend's [Realtime]: the same scheduler calls as the Linux port,
/// through the same C (`cn1_realtime_impl.h`, wrapped in `cn1_backend_realtime.c`), so
/// a gateway can run a control loop beside its REST API in one binary. Off Linux the
/// collector isolation and the allocation check still apply; scheduling and memory
/// locking answer `-ENOSYS`. Every primitive acts on the calling thread.
public final class BoardRealtime extends Realtime {
    private static boolean installed;
    private int features = -1;

    private BoardRealtime() {
    }

    /// Makes this the instance `Realtime.getInstance()` returns. The server calls it
    /// as it starts; calling it again does nothing.
    public static void install() {
        if (!installed) {
            installed = true;
            Realtime.install(new BoardRealtime());
        }
    }

    @Override
    protected int getSupportedFeatures() {
        if (features < 0) {
            features = nFeatures();
        }
        return features;
    }

    @Override
    protected int rawEnter(RealtimeThread thread, int priority, int cpu, boolean isolate) {
        return nEnter(priority, cpu, isolate);
    }

    @Override
    protected void rawLeave(RealtimeThread thread) {
        nLeave();
    }

    @Override
    protected void rawStartPeriod(RealtimeThread thread, long periodNanos) {
        nStartPeriod(periodNanos);
    }

    @Override
    protected int rawWaitNextPeriod(RealtimeThread thread) {
        return nWaitNextPeriod();
    }

    @Override
    protected void rawFreeze(RealtimeThread thread, int budget, RealtimeAllocationError error) {
        nFreeze(budget, error);
    }

    @Override
    protected void rawThaw(RealtimeThread thread) {
        nThaw();
    }

    @Override
    protected void rawStatistics(RealtimeThread thread, long[] out) {
        nStatistics(out);
    }

    @Override
    protected String rawLastViolation(RealtimeThread thread) {
        return nLastViolation();
    }

    @Override
    protected int rawLockMemory() {
        return nLockMemory();
    }

    @Override
    protected void rawSetMemoryLimit(long bytes, OutOfMemoryError error) {
        nSetMemoryLimit(bytes, error);
    }

    private static native int nFeatures();

    private static native int nEnter(int priority, int cpu, boolean isolate);

    private static native void nLeave();

    private static native void nStartPeriod(long periodNanos);

    private static native int nWaitNextPeriod();

    private static native void nFreeze(int budget, Object error);

    private static native void nThaw();

    private static native int nStatistics(long[] out);

    private static native String nLastViolation();

    private static native int nLockMemory();

    private static native void nSetMemoryLimit(long bytes, Object error);
}
