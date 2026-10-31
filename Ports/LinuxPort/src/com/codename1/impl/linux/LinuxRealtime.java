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
package com.codename1.impl.linux;

import com.codename1.realtime.Realtime;
import com.codename1.realtime.RealtimeAllocationError;
import com.codename1.realtime.RealtimeThread;

/// The Linux port's [Realtime], for the desktop and the embedded Linux targets: the
/// scheduler's half in `cn1_linux_realtime.c` over `cn1_realtime_impl.h`, the
/// collector's half in the VM. Every primitive acts on the calling thread, so the
/// `RealtimeThread` arguments are not needed here.
class LinuxRealtime extends Realtime {
    private int features = -1;

    @Override
    protected int getSupportedFeatures() {
        if (features < 0) {
            features = LinuxNative.rtFeatures();
        }
        return features;
    }

    @Override
    protected int rawEnter(RealtimeThread thread, int priority, int cpu, boolean isolate) {
        return LinuxNative.rtEnter(priority, cpu, isolate);
    }

    @Override
    protected void rawLeave(RealtimeThread thread) {
        LinuxNative.rtLeave();
    }

    @Override
    protected void rawStartPeriod(RealtimeThread thread, long periodNanos) {
        LinuxNative.rtStartPeriod(periodNanos);
    }

    @Override
    protected int rawWaitNextPeriod(RealtimeThread thread) {
        return LinuxNative.rtWaitNextPeriod();
    }

    @Override
    protected void rawFreeze(RealtimeThread thread, int budget, RealtimeAllocationError error) {
        LinuxNative.rtFreeze(budget, error);
    }

    @Override
    protected void rawThaw(RealtimeThread thread) {
        LinuxNative.rtThaw();
    }

    @Override
    protected void rawStatistics(RealtimeThread thread, long[] out) {
        LinuxNative.rtStatistics(out);
    }

    @Override
    protected String rawLastViolation(RealtimeThread thread) {
        return LinuxNative.rtLastViolation();
    }

    @Override
    protected int rawLockMemory() {
        return LinuxNative.rtLockMemory();
    }

    @Override
    protected void rawSetMemoryLimit(long bytes, OutOfMemoryError error) {
        LinuxNative.rtSetMemoryLimit(bytes, error);
    }
}
