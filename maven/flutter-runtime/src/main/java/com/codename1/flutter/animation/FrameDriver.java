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

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;

import java.util.ArrayList;
import java.util.List;

/**
 * One clock for every running animation, driven by Codename One's animation loop.
 *
 * <p>This registers a single {@link com.codename1.ui.animations.Animation} on the current
 * Form. That is the EDT's own frame clock: a Form with a registered animation does not let
 * the EDT sleep ({@code Display.shouldEDTSleep} consults {@code Form.hasAnimations}), so
 * {@code animate()} is called once per frame for as long as anything is animating, and the
 * driver deregisters itself the moment the last animation finishes.</p>
 *
 * <p>It used to chain {@code CN.setTimeout(16)} per frame instead, which was wrong twice
 * over. {@code Display.setTimeout} allocates a whole {@code java.util.Timer} thread per
 * call, so this created and abandoned one thread per animation frame; and measured in the
 * simulator that path delivered a tick every ~300ms whatever delay was asked for - 1ms and
 * 16ms both came back at ~290ms. At ~3fps a 300ms curve reaches t >= 1 on its FIRST tick,
 * so every animation snapped straight to its end value instead of tweening, and anything
 * driven by the same clock stuttered.</p>
 */
final class FrameDriver {

    private static final List<AnimationController> RUNNING =
            new ArrayList<AnimationController>();

    /** The form the clock is currently registered on, or null when detached. */
    private static Form registeredOn;
    private static boolean attachPending;

    private static final com.codename1.ui.animations.Animation CLOCK =
            new com.codename1.ui.animations.Animation() {
        @Override
        public boolean animate() {
            frame();
            // TRUE while anything is running, because that is what keeps the
            // clock being called.
            //
            // Form.loopAnimations only calls Display.repaint(this) for a
            // non-Component animation when animate() returns true, and the EDT
            // only keeps looping while something is pending. Returning false
            // left the clock dependent on some OTHER component repainting, so an
            // animation on an otherwise still page advanced a few frames a
            // second: measured, a 500ms feature-discovery open took over two
            // seconds and had barely started before its route was gone, against
            // Flutter's fully-open at 0.4s. An animation that is running is by
            // definition about to change something, so asking for the frame is
            // not waste.
            // FALSE: a true here makes Form.loopAnimations call
            // Display.repaint(this), and for a bare Animation the port turns
            // that into a FULL-FORM repaint every frame -- measured, that alone
            // held the whole app to 14-19fps. The listeners a controller
            // notifies already repaint exactly the elements that changed; what
            // was missing was only something to keep the EDT looping, and a
            // pending serial call does that for free (see requestFrame).
            return false;
        }

        @Override
        public void paint(Graphics g) {
        }
    };

    private FrameDriver() {
    }

    // ------------------------------------------------------------------
    // Diagnostics
    // ------------------------------------------------------------------
    //
    // "The animation snaps to its end state" and "the animation is choppy" are the same
    // question asked twice: how many times did the clock tick between the start of a run
    // and its end? A 200ms curve wants ~12 ticks. One tick means the first frame already
    // measured t >= 1. So the counter that matters is ticks-per-run, not ticks-per-second.

    private static boolean trace;
    private static long ticks;
    private static long lastTickTime;
    private static long gapSum;
    private static long gaps;
    private static long worstGap;
    private static long runs;
    private static long ticksThisRun;
    private static long minTicksPerRun = Long.MAX_VALUE;
    private static long worstFirstTickLag;

    static void trace(boolean on) {
        trace = on;
        ticks = 0;
        gapSum = 0;
        gaps = 0;
        worstGap = 0;
        runs = 0;
        ticksThisRun = 0;
        lastTickTime = 0;
        minTicksPerRun = Long.MAX_VALUE;
        worstFirstTickLag = 0;
    }

    /// How stale a controller already was the first time the clock reached it. An animation
    /// that snaps has nothing wrong with its curve - it was simply handed a first frame
    /// whose elapsed time already exceeded its duration.
    static void noteAdvance(long elapsedMs) {
        if (trace && ticksThisRun == 1) {
            worstFirstTickLag = Math.max(worstFirstTickLag, elapsedMs);
        }
    }

    /// The clock's own numbers, as JSON members (no braces) for embedding in a larger report.
    static String stats() {
        return "\"animTicks\":" + ticks
                + ",\"animRuns\":" + runs
                + ",\"animMinTicksPerRun\":" + (minTicksPerRun == Long.MAX_VALUE ? 0 : minTicksPerRun)
                + ",\"animWorstFirstTickLagMs\":" + worstFirstTickLag
                + ",\"animMeanGapMs\":" + (gaps == 0 ? 0 : gapSum / gaps)
                + ",\"animWorstGapMs\":" + worstGap;
    }

    private static void traceTick() {
        long now = System.currentTimeMillis();
        ticks++;
        ticksThisRun++;
        if (lastTickTime != 0) {
            long gap = now - lastTickTime;
            gapSum += gap;
            gaps++;
            worstGap = Math.max(worstGap, gap);
        }
        lastTickTime = now;
    }

    private static void traceRunEnded() {
        if (ticksThisRun > 0) {
            runs++;
            minTicksPerRun = Math.min(minTicksPerRun, ticksThisRun);
            ticksThisRun = 0;
        }
        lastTickTime = 0;
    }

    /** Adds a controller to the frame loop, starting the clock if it was idle. */
    static synchronized void add(AnimationController c) {
        if (!RUNNING.contains(c)) {
            RUNNING.add(c);
        }
        attach();
        // ...and start the chain here rather than waiting for the Form to
        // deliver the first tick. An animation that starts DURING a route push
        // registers on the form that is still current at that instant, so that
        // first tick never arrives, the self-scheduling chain never starts, and
        // the controller sits frozen at its start value for good. Measured on
        // the feature-discovery overlay: its open stalled around a quarter of
        // the way through, which is why the coach mark came out at roughly half
        // the radius Flutter draws.
        requestFrame();
    }

    /** Removes a controller; the clock stops once none are left. */
    static synchronized void remove(AnimationController c) {
        RUNNING.remove(c);
        if (RUNNING.isEmpty()) {
            detach();
        }
    }

    private static synchronized void attach() {
        if (!Display.isInitialized()) {
            return;
        }
        Form f = Display.getInstance().getCurrent();
        if (f == null) {
            // Animations normally start inside a mounted form; if one somehow starts first,
            // retry on the next EDT pass rather than spinning up a timer.
            retryAttach();
            return;
        }
        if (registeredOn == f) {
            return;
        }
        detach();
        f.registerAnimated(CLOCK);
        registeredOn = f;
    }

    private static void retryAttach() {
        if (attachPending) {
            return;
        }
        attachPending = true;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                synchronized (FrameDriver.class) {
                    attachPending = false;
                    if (!RUNNING.isEmpty()) {
                        attach();
                    }
                }
            }
        });
    }

    private static synchronized void detach() {
        if (registeredOn != null) {
            registeredOn.deregisterAnimated(CLOCK);
            registeredOn = null;
            if (trace) {
                traceRunEnded();
            }
        }
    }

    /**
     * Runs one animation frame now, instead of waiting for the clock to come round.
     *
     * <p>Reached through {@link MotionClock#advanceAndPump}: freeze the clock, advance
     * it, pump. The controllers then see exactly the elapsed time the harness named. Does
     * nothing an ordinary tick would not do -- it IS the ordinary tick.</p>
     */
    static void pump() {
        frame();
    }

    private static boolean framePending;

    /**
     * Queues the next animation frame without painting anything.
     *
     * <p>Display.shouldEDTSleep refuses to park while a serial call is pending,
     * so this keeps the loop turning at whatever rate the EDT can manage while
     * leaving the painting to the elements that actually changed.</p>
     */
    private static void requestFrame() {
        if (framePending || !Display.isInitialized()) {
            return;
        }
        framePending = true;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                synchronized (FrameDriver.class) {
                    framePending = false;
                }
                frame();
            }
        });
    }

    private static void frame() {
        AnimationController[] due;
        synchronized (FrameDriver.class) {
            if (RUNNING.isEmpty()) {
                detach();
                return;
            }
            // A form switch mid-animation would otherwise leave the clock on the form that
            // is no longer being animated, and it would never tick again.
            if (Display.isInitialized() && Display.getInstance().getCurrent() != registeredOn) {
                attach();
            }
            due = RUNNING.toArray(new AnimationController[RUNNING.size()]);
            if (trace) {
                traceTick();
            }
        }
        // Advance every animation before anything rebuilds: the build owner coalesces the
        // dirty elements, so the whole frame costs one flush.
        for (int i = 0; i < due.length; i++) {
            try {
                due[i].advance();
            } catch (Throwable t) {
                // One misbehaving animation must not stop the clock for the others.
                com.codename1.flutter.FlutterErrorReport.record(t);
                remove(due[i]);
            }
        }
        synchronized (FrameDriver.class) {
            if (RUNNING.isEmpty()) {
                detach();
            } else {
                requestFrame();
            }
        }
    }
}
