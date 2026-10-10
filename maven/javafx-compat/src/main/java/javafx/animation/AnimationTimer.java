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
package javafx.animation;

import com.codename1.fxcompat.runtime.FrameClock;

/// A callback that runs once per frame while the timer is started, on
/// the JavaFX application thread.
public abstract class AnimationTimer {

    private boolean active;
    private final FrameClock.Pulse pulse = new FrameClock.Pulse() {
        @Override
        public void pulse(long nowNanos) {
            handle(nowNanos);
        }
    };

    /// Creates a timer, which is not started.
    public AnimationTimer() {
    }

    /// Called once per frame with the time of the frame in nanoseconds.
    /// The time never runs backwards, and the same value is given to
    /// every timer in one frame; its origin is arbitrary.
    public abstract void handle(long now);

    /// Starts the timer; starting a started timer has no effect.
    public void start() {
        if (!active) {
            active = true;
            FrameClock.add(pulse);
        }
    }

    /// Stops the timer; it can be started again.
    public void stop() {
        if (active) {
            active = false;
            FrameClock.remove(pulse);
        }
    }
}
