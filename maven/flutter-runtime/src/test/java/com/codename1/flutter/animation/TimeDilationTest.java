/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.animation;

import com.codename1.flutter.scheduler.SchedulerLib;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code scheduler.timeDilation} has to reach the controllers.
 *
 * <p>It was declared and never read, so the gallery's own "Slow motion" switch -- a
 * user-facing control whose entire purpose is to let a motion design be inspected --
 * moved nothing. A switch that does nothing is worse than an absent one.</p>
 *
 * <p>The controller's clock is not injectable, so these assert on the mapping from
 * elapsed time to t rather than by running a real animation: that mapping is the
 * whole of what dilation changes.</p>
 */
class TimeDilationTest {

    @AfterEach
    void reset() {
        SchedulerLib.timeDilation = 1.0;
    }

    /** The controller's own arithmetic -- not a copy of it. */
    private static double progress(long elapsedMs, long durationMs) {
        return AnimationController.progress(elapsedMs, durationMs);
    }

    @Test
    void undilatedIsRealTime() {
        assertEquals(0.5, progress(100, 200), 1e-9);
        assertEquals(1.0, progress(200, 200), 1e-9);
    }

    @Test
    void fiveTimesSlowerTakesFiveTimesAsLong() {
        SchedulerLib.timeDilation = 5.0;
        assertEquals(0.1, progress(100, 200), 1e-9);
        assertEquals(0.2, progress(200, 200), 1e-9);
        assertEquals(1.0, progress(1000, 200), 1e-9);
    }

    /// A zero or negative dilation would divide the animation by zero or run it
    /// backwards; Flutter asserts on it, and here it simply means real time.
    @Test
    void anImpossibleDilationIsIgnored() {
        SchedulerLib.timeDilation = 0.0;
        assertEquals(0.5, progress(100, 200), 1e-9);
        SchedulerLib.timeDilation = -2.0;
        assertEquals(0.5, progress(100, 200), 1e-9);
    }

    /// A zero-duration run is complete at its first tick whatever the dilation, so
    /// slow motion cannot wedge an animation that has no time to take.
    @Test
    void aZeroDurationRunStillCompletes() {
        SchedulerLib.timeDilation = 10.0;
        assertEquals(1.0, progress(0, 0), 1e-9);
    }
}
