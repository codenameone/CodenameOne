/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.ui.DevicePosture;

/// Drives the hinge angle of a foldable skin between postures. An animated
/// change eases the angle toward its target one step at a time; `step`
/// takes the clock as an argument so the motion is testable without a timer.
///
/// The host learns three things: every new angle (to repaint the 3D fold),
/// a posture change when the angle crosses a `FoldableSkin` threshold (the
/// app relays out mid-motion, as on a device), and when the device reaches
/// or leaves fully closed, which is when the display moves to the cover
/// screen and back. Leaving closed is reported before the first open frame,
/// so the inner screen is in place before it is drawn.
final class FoldController {

    interface Host {
        /// The angle moved; repaint.
        void angleChanged(int angle, boolean animating);

        /// The posture category changed (a `DevicePosture` POSTURE_ constant).
        void postureChanged(int posture);

        /// The device became fully closed (true) or started opening (false).
        void coverChanged(boolean closed);
    }

    static final long DEFAULT_DURATION_MS = 700;

    private final Host host;
    private double angle = 180;
    private int posture = DevicePosture.POSTURE_FLAT;
    private boolean closed;
    private double fromAngle;
    private double toAngle = 180;
    private long startMs;
    private long durationMs;
    private boolean animating;

    FoldController(Host host) {
        this.host = host;
    }

    int getAngle() {
        return (int) Math.round(angle);
    }

    double getExactAngle() {
        return angle;
    }

    int getPosture() {
        return posture;
    }

    boolean isClosed() {
        return closed;
    }

    boolean isAnimating() {
        return animating;
    }

    int getTargetAngle() {
        return (int) Math.round(toAngle);
    }

    /// Moves to `target` degrees (0 closed, 180 flat) over `duration`
    /// milliseconds, or at once when `duration` is 0 or less.
    void moveTo(int target, long duration, long nowMs) {
        double t = Math.max(0, Math.min(180, target));
        if (duration <= 0) {
            animating = false;
            toAngle = t;
            apply(t, false);
            return;
        }
        fromAngle = angle;
        toAngle = t;
        startMs = nowMs;
        // A partial move takes a proportional share of the full sweep.
        durationMs = Math.max(1, (long) (duration * Math.abs(t - angle) / 180.0));
        animating = Math.abs(t - angle) > 0.01;
        if (animating && closed) {
            // Leaving closed: back to the inner display before any open frame.
            closed = false;
            host.coverChanged(false);
        }
        if (!animating) {
            apply(t, false);
        }
    }

    /// Advances an animation to `nowMs`. Returns true while it is still running.
    boolean step(long nowMs) {
        if (!animating) {
            return false;
        }
        double f = (nowMs - startMs) / (double) durationMs;
        if (f >= 1) {
            animating = false;
            apply(toAngle, false);
            return false;
        }
        f = Math.max(0, f);
        // Ease in and out (smootherstep): zero velocity and acceleration at
        // both ends, so a hinge picks up and settles without a jolt.
        double e = f * f * f * (f * (f * 6 - 15) + 10);
        apply(fromAngle + (toAngle - fromAngle) * e, true);
        return true;
    }

    private void apply(double a, boolean moving) {
        angle = a;
        boolean nowClosed = !moving && a < 0.5;
        if (closed && !nowClosed) {
            // Opening: the inner display goes back first, so the posture
            // event below is delivered to an app already at the inner size.
            closed = false;
            host.coverChanged(false);
        }
        int p = FoldableSkin.postureForAngle((int) Math.round(a));
        if (p != posture) {
            posture = p;
            host.postureChanged(p);
        }
        if (nowClosed && !closed) {
            closed = true;
            host.coverChanged(true);
        }
        host.angleChanged(getAngle(), moving);
    }

    /// Puts the controller at rest at `a` without notifying the host, for a
    /// newly loaded skin.
    void reset(int a) {
        animating = false;
        angle = Math.max(0, Math.min(180, a));
        toAngle = angle;
        posture = FoldableSkin.postureForAngle(a);
        closed = angle < 0.5;
    }
}
