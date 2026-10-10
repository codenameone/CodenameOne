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
 * SAME animation time. Against the wall clock they never are: a frame lands when it
 * lands, and two recordings of one gesture are two different samplings of it. Flutter's
 * own widget tests solve this by owning the clock -- {@code tester.pump(d)} advances
 * animations by exactly {@code d} -- and the reference frames this port is measured
 * against are captured that way.</p>
 *
 * <p>This is a thin face over Codename One's own {@code AnimationTime}, the framework's
 * pluggable animation clock, which exists for exactly this. Going through it rather than
 * keeping a clock of our own is the point: a Flutter animation and the Codename One FORM
 * TRANSITION carrying the page it lives on then advance together off one time source.
 * With a separate clock, freezing it held the widgets still while the transition around
 * them ran on regardless, so a captured frame was half of one moment and half of another
 * -- and a page push, which is entirely a form transition, did not hold still at all.</p>
 *
 * <p>Released, which is how an application always runs, this is
 * {@code System.currentTimeMillis()} and costs one boolean test per tick.</p>
 */
public final class MotionClock {

    private MotionClock() {
    }

    /** The current animation time: the frozen one, or the wall clock. */
    public static long now() {
        return com.codename1.ui.animations.AnimationTime.now();
    }

    /**
     * Takes the clock off the wall, starting at the current wall time.
     *
     * <p>Starting from the wall clock rather than zero keeps any run already in flight
     * consistent: a controller that recorded its start time a moment ago would otherwise
     * see the clock jump backwards and measure a negative elapsed.</p>
     */
    public static void freeze() {
        com.codename1.ui.animations.AnimationTime.setTime(System.currentTimeMillis());
    }

    /** Moves the frozen clock forward by {@code deltaMs}. Ignored when not frozen. */
    public static void advance(long deltaMs) {
        if (isFrozen() && deltaMs > 0) {
            com.codename1.ui.animations.AnimationTime.setTime(now() + deltaMs);
        }
    }

    /**
     * Advances the frozen clock and runs one animation frame at the new time.
     *
     * <p>The single call a harness needs: FrameDriver is package private and staying that
     * way, because an application has no business pumping the frame clock by hand. A form
     * transition needs no pumping here -- Codename One's own painting loop drives it, and
     * that loop now reads the same clock.</p>
     */
    public static void advanceAndPump(long deltaMs) {
        advance(deltaMs);
        FrameDriver.pump();
    }

    /** Hands the clock back to the wall. */
    public static void release() {
        com.codename1.ui.animations.AnimationTime.reset();
    }

    /** Whether the clock is currently frozen. */
    public static boolean isFrozen() {
        return com.codename1.ui.animations.AnimationTime.isOverridden();
    }
}
