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
package android.os;

import android.view.KeyEvent;
import android.view.MotionEvent;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/// The uptime clocks read the monotonic nanosecond source, not the wall
/// clock, and input events are stamped in the same base. Uptime used to be
/// a difference of two `currentTimeMillis()` readings, which a wall-clock
/// change moves; a JVM test cannot move the wall clock, so this checks the
/// source by its resolution (whole milliseconds times a million never carry
/// sub-millisecond digits) and checks that events agree with uptime.
public class SystemClockMonotonicTest {

    @Test
    public void elapsedRealtimeNanosHasNanosecondResolution() {
        boolean subMillisecond = false;
        for (int i = 0; i < 1000 && !subMillisecond; i++) {
            subMillisecond = SystemClock.elapsedRealtimeNanos() % 1000000L != 0;
        }
        assertTrue("elapsedRealtimeNanos is whole milliseconds: not read from System.nanoTime()", subMillisecond);
        long nanos = SystemClock.elapsedRealtimeNanos();
        long millis = SystemClock.uptimeMillis();
        assertTrue(millis >= nanos / 1000000L && millis - nanos / 1000000L < 1000);
    }

    @Test
    public void inputEventsAreStampedInUptime() {
        long before = SystemClock.uptimeMillis();
        MotionEvent m = MotionEvent.create(MotionEvent.ACTION_DOWN, 1, 1, 1, 1, before);
        KeyEvent k = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER);
        long after = SystemClock.uptimeMillis();
        assertTrue("motion event time " + m.getEventTime() + " not in [" + before + ", " + after + "]",
                m.getEventTime() >= before && m.getEventTime() <= after);
        assertTrue("key event time " + k.getEventTime() + " not in [" + before + ", " + after + "]",
                k.getEventTime() >= before && k.getEventTime() <= after);
    }

    @Test
    public void sleepPreservesInterruptAfterWaiting() {
        long start = SystemClock.uptimeMillis();
        Thread.currentThread().interrupt();
        try {
            SystemClock.sleep(30);
            assertTrue(SystemClock.uptimeMillis() - start >= 25);
            assertTrue("sleep discarded the caller's interrupt", Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
