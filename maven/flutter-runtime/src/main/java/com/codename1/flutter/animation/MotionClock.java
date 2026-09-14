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

/**
 * The clock every animation reads, and a way to take it off the wall.
 *
 * <p>Comparing motion between two stacks only means something if both are asked for the
 * SAME animation time. Against the wall clock they never are: a frame lands when it lands,
 * a slow build shifts every later frame, and two recordings of the same gesture are two
 * different samplings of it. Flutter's own widget tests solve this by owning the clock --
 * {@code tester.pump(d)} advances animations by exactly {@code d} -- and the reference
 * frames this port is measured against are captured that way.</p>
 *
 * <p>Frozen, this does the same here: the harness names an animation time, every running
 * controller is advanced to it, and the frame painted is the frame at that time. Nothing
 * else changes -- controllers still compute their own progress from it, so what is being
 * compared is still the runtime's real curve and duration arithmetic.</p>
 *
 * <p>Released, which is how an application always runs, this is
 * {@code System.currentTimeMillis()} and costs one boolean test per tick.</p>
 */
public final class MotionClock {

    private static volatile boolean frozen;
    private static volatile long nowMs;

    private MotionClock() {
    }

    /** The current animation time: the frozen one, or the wall clock. */
    public static long now() {
        return frozen ? nowMs : System.currentTimeMillis();
    }

    /**
     * Takes the clock off the wall, starting at the current wall time.
     *
     * <p>Starting from the wall clock rather than zero keeps any run already in flight
     * consistent: a controller that recorded its start time a moment ago would otherwise
     * see the clock jump backwards and measure a negative elapsed.</p>
     */
    public static void freeze() {
        nowMs = System.currentTimeMillis();
        frozen = true;
    }

    /** Moves the frozen clock forward by {@code deltaMs}. Ignored when not frozen. */
    public static void advance(long deltaMs) {
        if (frozen && deltaMs > 0) {
            nowMs += deltaMs;
        }
    }

    /** Hands the clock back to the wall. */
    public static void release() {
        frozen = false;
    }

    /**
     * Advances the frozen clock and runs one animation frame at the new time.
     *
     * <p>The single call a harness needs: FrameDriver is package private and staying that
     * way, because an application has no business pumping the frame clock by hand.</p>
     */
    public static void advanceAndPump(long deltaMs) {
        advance(deltaMs);
        FrameDriver.pump();
    }

    /** Whether the clock is currently frozen. */
    public static boolean isFrozen() {
        return frozen;
    }
}
