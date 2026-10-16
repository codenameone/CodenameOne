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
 * {@code animateTo} is always a FORWARD run, whichever side of the current value the
 * target falls on -- Flutter's own doc comment: "status is reported as
 * AnimationStatus.forward regardless of whether target > value or not", finishing
 * "completed" once the target is reached. {@code animateBack} is the method that runs
 * reverse/dismissed; picking the direction from the target (as if retreating meant
 * reversing) made a retreating animateTo use reverseDuration and finish dismissed
 * instead -- Reply's drawer animates with animateTo both opening AND closing.
 */
class AnimateToIsAlwaysForwardTest {

    @Test
    void retreatingStillFinishesForwardAndCompleted() {
        AnimationController c = new AnimationController();
        c.duration(Duration.of(0, 0, 0, 0, 200, 0));
        // A reverseDuration that would give the wrong answer AWAY, so a dir mistake
        // shows up as a timing mistake too, not only as a status mistake.
        c.reverseDuration(Duration.of(0, 0, 0, 0, 999, 0));
        c.value(0.8);

        // Headless: the run collapses synchronously, so the final status is directly
        // observable without a frame clock.
        c.animateTo(0.2, null, null);

        assertEquals(AnimationStatus.completed, c.status(),
                "a retreating animateTo must still finish completed, not dismissed");
    }

    @Test
    void advancingAlsoFinishesForwardAndCompleted() {
        AnimationController c = new AnimationController();
        c.duration(Duration.of(0, 0, 0, 0, 200, 0));
        c.value(0.2);

        c.animateTo(0.8, null, null);

        assertEquals(AnimationStatus.completed, c.status());
    }
}
