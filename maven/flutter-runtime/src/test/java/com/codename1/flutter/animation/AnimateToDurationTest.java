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

import dart.core.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A controller's duration describes crossing its WHOLE range. An animateTo that covers
 * part of it takes the matching part of the time -- Flutter's
 * {@code directionDuration * remainingFraction}.
 *
 * <p>Reply opens its mailbox drawer with {@code animateTo(0.4)} on a 300ms controller,
 * which is a 120ms animation. Running the full 300 made the drawer take two and a half
 * times as long to arrive as the reference: measured frame by frame at the same animation
 * times, ours had barely moved at the point the reference had finished.</p>
 */
class AnimateToDurationTest {

    private static AnimationController controller(long ms) {
        AnimationController c = new AnimationController();
        c.duration(Duration.of(0, 0, 0, 0, ms, 0));
        return c;
    }

    /// Package-private seam so the arithmetic can be checked without a frame clock.
    private static long runMillis(AnimationController c, double target, Duration explicit,
            AnimationStatus dir) {
        return c.simulationMillisForTest(target, explicit, dir);
    }

    @Test
    void partOfTheRangeTakesThatPartOfTheTime() {
        AnimationController c = controller(300);
        assertEquals(120, runMillis(c, 0.4, null, AnimationStatus.forward),
                "0 -> 0.4 of a 300ms range is 120ms, which is what Reply's drawer asks for");
    }

    @Test
    void theWholeRangeTakesTheWholeDuration() {
        assertEquals(300, runMillis(controller(300), 1.0, null, AnimationStatus.forward));
    }

    @Test
    void theDistanceIsMeasuredFromWhereItIsNow() {
        AnimationController c = controller(300);
        c.value(0.5);
        assertEquals(150, runMillis(c, 1.0, null, AnimationStatus.forward),
                "half the range left is half the time");
        assertEquals(150, runMillis(c, 0.0, null, AnimationStatus.reverse),
                "and the same going back");
    }

    @Test
    void anExplicitDurationIsUsedWhole() {
        AnimationController c = controller(300);
        assertEquals(1000, runMillis(c, 0.4, Duration.of(0, 0, 0, 1, 0, 0),
                AnimationStatus.forward), "a stated duration is not scaled");
    }

    /// Flutter refuses to animate to where it already is, even when handed a duration.
    @Test
    void goingNowhereTakesNoTime() {
        AnimationController c = controller(300);
        c.value(0.4);
        assertEquals(0, runMillis(c, 0.4, Duration.of(0, 0, 0, 1, 0, 0),
                AnimationStatus.forward));
    }

    /// reverseDuration governs the way back, and is scaled the same way.
    @Test
    void theReverseDurationIsScaledToo() {
        AnimationController c = controller(300);
        c.reverseDuration(Duration.of(0, 0, 0, 0, 200, 0));
        c.value(0.5);
        assertEquals(100, runMillis(c, 0.0, null, AnimationStatus.reverse));
    }
}
