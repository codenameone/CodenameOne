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

import java.util.ArrayList;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Animation;

/// The one frame clock of the JavaFX animation classes, the role of the
/// JavaFX pulse: every running animation and animation timer is called
/// once per frame with the time of that frame.
///
/// Codename One only ticks the animations registered with the form on
/// screen, so the clock registers a single Codename One animation with
/// the current form while something is running and removes it when the
/// last receiver leaves; an idle application costs nothing. A receiver is
/// commonly added before any form is shown, and forms change, so the
/// stage hosts call [#ensureCurrent()] whenever they are laid out.
///
/// Tests switch the clock to manual mode ([#setManual(boolean)]): time
/// then stands still until [#advance(long)] moves it and delivers one
/// pulse, so a test states the exact time of every frame and never
/// sleeps.
public final class FrameClock {

    /// A receiver of frames.
    public interface Pulse {

        /// Called once per frame with the time of the frame in
        /// nanoseconds. The origin of the time is arbitrary; only
        /// differences mean something.
        void pulse(long nowNanos);
    }

    private static final ArrayList<Pulse> PULSES = new ArrayList<Pulse>();
    private static Form form;
    private static boolean manual;
    private static long manualNanos;
    private static final Animation TICK = new Animation() {
        @Override
        public boolean animate() {
            if (!manual) {
                tick();
            }
            return false;
        }

        @Override
        public void paint(Graphics g) {
        }
    };

    private FrameClock() {
    }

    /// Adds a receiver; adding one twice has no effect.
    public static void add(Pulse pulse) {
        if (!PULSES.contains(pulse)) {
            PULSES.add(pulse);
        }
        ensureCurrent();
    }

    /// Removes a receiver. It is not called again, not even by a frame
    /// that is being delivered.
    public static void remove(Pulse pulse) {
        PULSES.remove(pulse);
        if (PULSES.isEmpty()) {
            unregister();
        }
    }

    /// Returns whether any receiver is registered.
    public static boolean isActive() {
        return !PULSES.isEmpty();
    }

    /// Returns the current time in nanoseconds: the time the last manual
    /// advance reached in manual mode, the system's otherwise.
    public static long nowNanos() {
        return manual ? manualNanos : System.nanoTime();
    }

    /// Switches manual mode on or off. In manual mode nothing but
    /// [#advance(long)] moves time or delivers a frame.
    public static void setManual(boolean value) {
        manual = value;
        if (value) {
            unregister();
        } else {
            ensureCurrent();
        }
    }

    /// Returns whether the clock is in manual mode.
    public static boolean isManual() {
        return manual;
    }

    /// Moves manual time forward and delivers one frame at the new time.
    public static void advance(long millis) {
        if (!manual) {
            throw new IllegalStateException("The frame clock is not in manual mode");
        }
        if (millis < 0) {
            throw new IllegalArgumentException("Time does not run backwards");
        }
        manualNanos += millis * 1000000L;
        tick();
    }

    /// Drops every receiver and leaves manual mode; for the end of a test.
    public static void reset() {
        PULSES.clear();
        unregister();
        manual = false;
    }

    /// Moves the clock to the form on screen when it is not there
    /// already.
    public static void ensureCurrent() {
        if (PULSES.isEmpty() || manual || !Display.isInitialized()) {
            return;
        }
        Form current = Display.getInstance().getCurrent();
        if (current == form) {
            return;
        }
        unregister();
        form = current;
        if (form != null) {
            form.registerAnimated(TICK);
        }
    }

    private static void unregister() {
        if (form != null) {
            form.deregisterAnimated(TICK);
            form = null;
        }
    }

    private static void tick() {
        long now = nowNanos();
        Pulse[] snapshot = PULSES.toArray(new Pulse[PULSES.size()]);
        for (int i = 0; i < snapshot.length; i++) {
            if (PULSES.contains(snapshot[i])) {
                snapshot[i].pulse(now);
            }
        }
        if (PULSES.isEmpty()) {
            unregister();
        }
    }
}
