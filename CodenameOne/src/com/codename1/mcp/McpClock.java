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
package com.codename1.mcp;

import com.codename1.ui.Display;
import com.codename1.ui.animations.AnimationTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/// The agent's clock: lets an MCP client freeze animation time and move it itself.
///
/// Motions, transitions, timers, tickers and caret blinks read
/// [AnimationTime][com.codename1.ui.animations.AnimationTime]. While the clock is
/// frozen that time only moves when a client advances it, so a screenshot taken after
/// `ui_clock_settle` always shows the finished state instead of whatever frame a
/// sleep happened to land on, and the same script gives the same pictures on a fast
/// machine and a slow one. Input timing (long press, double tap, fling velocity) keeps
/// reading the wall clock, so taps and drags behave exactly as they do unfrozen.
///
/// Every method here runs on the server's own thread, never the EDT: stepping waits for
/// the EDT to paint, which it could not do while blocked on itself.
final class McpClock {
    /// One frame at 60 fps, the step `advance` and `settle` move time by.
    static final int FRAME_MS = 16;

    /// How long one EDT round trip may take before it is read as "a transition is
    /// running". A form transition is painted by a loop that runs no serial calls until
    /// it ends, so a call that is not picked up in this time is waiting behind one --
    /// and that loop repaints without sleeping, so this much real time has covered a
    /// frame of it. Reading a merely slow loop as a transition costs nothing: the time
    /// was already set, and the next step paints whatever the loop did not.
    private static final int EDT_ROUND_TRIP_MS = 25;

    /// Virtual time between two pixel comparisons while settling.
    private static final int SETTLE_CHECK_MS = 96;

    private McpClock() {
    }

    static Map<String, Object> freeze() {
        requireOffEdt();
        if (!AnimationTime.isOverridden()) {
            AnimationTime.setTime(System.currentTimeMillis());
        }
        pump();
        return state("frozen");
    }

    static Map<String, Object> release() {
        requireOffEdt();
        AnimationTime.reset();
        pump();
        return state("released");
    }

    static Map<String, Object> advance(long ms) {
        requireFrozen();
        long target = AnimationTime.now() + Math.max(0L, ms);
        while (AnimationTime.now() < target) {
            AnimationTime.setTime(Math.min(target, AnimationTime.now() + FRAME_MS));
            pump();
        }
        return state("advanced");
    }

    /// Steps frame by frame until no transition is running and the screen has not
    /// changed across two checks, or until `maxMs` of virtual time has gone by.
    ///
    /// "Settled" is judged by pixels rather than by asking whether anything is still
    /// registered as animated: a repeating timer or a ticker stays registered for good
    /// while drawing nothing new, and would keep an "until idle" wait going forever.
    static Map<String, Object> settle(long maxMs) {
        requireFrozen();
        long start = AnimationTime.now();
        long limit = start + Math.max(FRAME_MS, maxMs);
        long nextCheck = start + SETTLE_CHECK_MS;
        int[] last = null;
        int stableChecks = 0;
        boolean settled = false;
        while (AnimationTime.now() < limit) {
            AnimationTime.setTime(Math.min(limit, AnimationTime.now() + FRAME_MS));
            if (!pump()) {
                // Still inside a transition; its frames are changing by definition.
                stableChecks = 0;
                last = null;
                continue;
            }
            if (AnimationTime.now() < nextCheck) {
                continue;
            }
            nextCheck = AnimationTime.now() + SETTLE_CHECK_MS;
            int[] pixels = McpUiTools.currentFramePixels();
            if (pixels != null && last != null && samePixels(pixels, last)) {
                stableChecks++;
                if (stableChecks >= 2) {
                    settled = true;
                    break;
                }
            } else {
                stableChecks = 0;
            }
            last = pixels;
        }
        Map<String, Object> out = state(settled ? "settled" : "limit");
        out.put("settled", Boolean.valueOf(settled));
        out.put("elapsedMs", Long.valueOf(AnimationTime.now() - start));
        return out;
    }

    /// Lets the EDT run one full loop at the current time. Two consecutive serial calls
    /// have a whole loop between them -- input, revalidation, painting and the
    /// animation pass -- so when the second returns the frame for this time is on the
    /// screen. Returns false when the EDT did not pick a call up because a form
    /// transition is being painted; that loop repaints on every pass, so the time spent
    /// waiting has already covered at least one frame of it.
    private static boolean pump() {
        return roundTrip() && roundTrip();
    }

    private static boolean roundTrip() {
        final AtomicInteger state = new AtomicInteger(0);
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                state.compareAndSet(0, 1);
            }
        });
        long deadline = System.currentTimeMillis() + EDT_ROUND_TRIP_MS;
        while (state.get() == 0 && System.currentTimeMillis() < deadline) {
            if (!sleep(2)) {
                break;
            }
        }
        // A call left queued behind the transition runs later and does nothing.
        return state.get() == 1;
    }

    static boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean samePixels(int[] a, int[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static void requireOffEdt() {
        if (Display.getInstance().isEdt()) {
            throw new IllegalStateException("The agent clock is driven from the MCP server thread, never the EDT");
        }
    }

    private static void requireFrozen() {
        requireOffEdt();
        if (!AnimationTime.isOverridden()) {
            throw new IllegalStateException("The clock is not frozen; call ui_clock_freeze first");
        }
    }

    private static Map<String, Object> state(String status) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("status", status);
        out.put("frozen", Boolean.valueOf(AnimationTime.isOverridden()));
        out.put("timeMs", Long.valueOf(AnimationTime.now()));
        return out;
    }
}
