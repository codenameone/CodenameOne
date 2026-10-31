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

/// The stretch a touch scroll shows past either end, as a phone's native
/// list does: a drag beyond the end moves the content less and less the
/// further it goes, a glide that reaches the end carries on a little way,
/// and either way the content springs back once the finger is off.
///
/// The value is in logical pixels, beyond the end the view is at: negative
/// above the first content, positive below the last. A view adds it to
/// where it places its content and asks the [Target] to do that again
/// whenever it changes.
public final class Overscroll implements FrameClock.Pulse {

    /// What the stretch moves.
    public interface Target {

        /// The value changed; place the content again.
        void overscrollChanged();
    }

    /// How hard a drag past the end is held back, the constant of
    /// UIScrollView's rubber band.
    private static final double RESISTANCE = 0.55;
    /// The spring's natural frequency per millisecond, critically damped:
    /// it settles in about half a second without swinging past the end.
    private static final double OMEGA = 0.014;
    /// The share of a glide's speed that carries on past the end.
    private static final double CARRY = 0.4;
    private static final double MAX_BEYOND = 0.35;
    private static final double STEP_MS = 4;

    private final Target target;
    private double value;
    private double velocity;
    private double size = 1;
    private long last;
    private boolean running;

    public Overscroll(Target target) {
        this.target = target;
    }

    /// The stretch now, in logical pixels.
    public double value() {
        return value;
    }

    /// Whether the content is stretched past an end or springing back.
    public boolean isActive() {
        return value != 0 || running;
    }

    /// A new press holds the content where the stretch has it; answers how
    /// far past the end a drag would have to be to show the same stretch,
    /// which the view adds to where the drag starts.
    public double grab() {
        stopSpring();
        if (value == 0) {
            return 0;
        }
        double r = Math.min(Math.abs(value) / size, 0.999);
        double beyond = size / RESISTANCE * (1 / (1 - r) - 1);
        return value < 0 ? -beyond : beyond;
    }

    /// A drag has reached `beyond` pixels past an end of a view `size`
    /// pixels long, or is back within it at 0.
    public void drag(double beyond, double size) {
        stopSpring();
        this.size = size > 0 ? size : 1;
        double a = Math.abs(beyond);
        double r = (1 - 1 / (a * RESISTANCE / this.size + 1)) * this.size;
        set(beyond < 0 ? -r : r);
    }

    /// The finger lifted: springs back to the end.
    public void release() {
        if (value != 0) {
            velocity = 0;
            startSpring();
        }
    }

    /// A glide reached an end moving at `speed` pixels a millisecond, in
    /// the direction the value grows; it carries on past the end and
    /// springs back.
    public void bounce(double speed, double size) {
        this.size = size > 0 ? size : 1;
        double v = speed * CARRY;
        // The farthest a critically damped spring launched at v travels
        // is v / (omega * e); a hard glide is held to a share of the view.
        double most = MAX_BEYOND * this.size * OMEGA * Math.E;
        velocity = Math.max(-most, Math.min(most, v));
        startSpring();
    }

    /// Drops the stretch at once, for a view that scrolls by other means.
    public void reset() {
        stopSpring();
        velocity = 0;
        set(0);
    }

    private void set(double v) {
        if (Double.compare(v, value) != 0) {
            value = v;
            target.overscrollChanged();
        }
    }

    private void startSpring() {
        last = FrameClock.nowNanos();
        if (!running) {
            running = true;
            FrameClock.add(this);
        }
    }

    private void stopSpring() {
        if (running) {
            running = false;
            FrameClock.remove(this);
        }
    }

    @Override
    public void pulse(long nowNanos) {
        double ms = (nowNanos - last) / 1000000.0;
        last = nowNanos;
        if (!(ms > 0)) {
            return;
        }
        double x = value;
        double v = velocity;
        // Small fixed steps, so a slow frame follows the same curve.
        while (ms > 0) {
            double dt = Math.min(STEP_MS, ms);
            ms -= dt;
            double a = -OMEGA * OMEGA * x - 2 * OMEGA * v;
            v += a * dt;
            x += v * dt;
        }
        velocity = v;
        if (Math.abs(x) < 0.25 && Math.abs(v) < 0.005) {
            velocity = 0;
            stopSpring();
            set(0);
            return;
        }
        set(x);
    }
}
