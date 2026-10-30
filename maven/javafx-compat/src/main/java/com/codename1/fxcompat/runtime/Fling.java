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
package com.codename1.fxcompat.runtime;

import com.codename1.util.MathUtil;

/// The glide a touch scroll keeps after the finger lifts, as a phone's
/// native list does. A view feeds it the position of every drag, calls
/// [#release()] when the finger lifts and [#stop()] when a new press
/// arrives; while it glides it moves the view through [Target] once a
/// frame, slowing down the way iOS does, until the speed is spent or the
/// view reaches an end.
///
/// Positions are the view's scroll offsets in logical pixels, growing as
/// the content moves up.
public final class Fling implements FrameClock.Pulse {

    /// What a fling moves.
    public interface Target {

        /// Scrolls by a distance; answers false when the view could not
        /// move the whole way, which ends the glide.
        boolean flingBy(double delta);
    }

    /// The fraction of its speed a glide keeps per millisecond, the
    /// "normal" deceleration of a UIScrollView.
    private static final double DECAY = 0.998;
    private static final double LOG_DECAY = MathUtil.log(DECAY);
    /// Below this speed, in pixels per millisecond, the glide stops.
    private static final double MIN_SPEED = 0.02;
    /// A speed is read over the drags of this many last milliseconds; a
    /// finger that paused before lifting does not fling.
    private static final long WINDOW_NANOS = 100000000L;
    private static final double MAX_SPEED = 8;
    private static final int SAMPLES = 8;

    private final Target target;
    private final long[] times = new long[SAMPLES];
    private final double[] positions = new double[SAMPLES];
    private int count;
    private int next;
    private double speed;
    private long last;
    private boolean running;

    public Fling(Target target) {
        this.target = target;
    }

    /// Ends a glide and forgets the drags before it, for a new press.
    public void stop() {
        count = 0;
        speed = 0;
        if (running) {
            running = false;
            FrameClock.remove(this);
        }
    }

    /// Records where a drag has moved the view to.
    public void track(double position) {
        times[next] = FrameClock.nowNanos();
        positions[next] = position;
        next = (next + 1) % SAMPLES;
        if (count < SAMPLES) {
            count++;
        }
    }

    /// The finger lifted: glides on at the speed of the last drags.
    public void release() {
        speed = speedNow();
        count = 0;
        if (Math.abs(speed) < MIN_SPEED) {
            speed = 0;
            return;
        }
        last = FrameClock.nowNanos();
        if (!running) {
            running = true;
            FrameClock.add(this);
        }
    }

    /// Whether a glide is under way.
    public boolean isRunning() {
        return running;
    }

    private double speedNow() {
        if (count < 2) {
            return 0;
        }
        int newest = (next - 1 + SAMPLES) % SAMPLES;
        long now = FrameClock.nowNanos();
        if (now - times[newest] > WINDOW_NANOS) {
            return 0;
        }
        int oldest = newest;
        for (int i = 1; i < count; i++) {
            int at = (newest - i + SAMPLES) % SAMPLES;
            if (times[newest] - times[at] > WINDOW_NANOS) {
                break;
            }
            oldest = at;
        }
        long span = times[newest] - times[oldest];
        if (span <= 0) {
            return 0;
        }
        double v = (positions[newest] - positions[oldest]) / (span / 1000000.0);
        return Math.max(-MAX_SPEED, Math.min(MAX_SPEED, v));
    }

    @Override
    public void pulse(long nowNanos) {
        double ms = (nowNanos - last) / 1000000.0;
        last = nowNanos;
        if (!(ms > 0)) {
            return;
        }
        // The distance covered while the speed decays over the frame:
        // the integral of v * DECAY^t, so a slow frame covers the same
        // ground as two fast ones.
        double k = LOG_DECAY;
        double kept = MathUtil.pow(DECAY, ms);
        double delta = speed * (kept - 1) / k;
        speed *= kept;
        if (!target.flingBy(delta) || Math.abs(speed) < MIN_SPEED) {
            stop();
        }
    }
}
