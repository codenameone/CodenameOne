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
package com.codename1.flutter.animations;

import com.codename1.flutter.animation.AnimatedWidget;
import com.codename1.flutter.animation.AnimationController;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * On the outgoing page only the SECONDARY animation moves; SharedAxisTransition and
 * FadeThroughTransition listened to the primary alone, so that page never rebuilt and
 * neither faded nor moved.
 */
class SecondaryAnimationListenTest {

    private static boolean notifiedBySecondary(AnimatedWidget w, AnimationController secondary) {
        final int[] n = {0};
        w.listenable().addListener(() -> n[0]++);
        secondary.value(0.5);
        return n[0] > 0;
    }

    @Test
    void sharedAxisTransitionRebuildsOnItsSecondaryAnimation() {
        AnimationController primary = new AnimationController();
        primary.value(1.0);
        AnimationController secondary = new AnimationController();
        SharedAxisTransition t = new SharedAxisTransition();
        t.animation(primary);
        t.secondaryAnimation(secondary);
        assertTrue(notifiedBySecondary(t, secondary));
    }

    @Test
    void fadeThroughTransitionRebuildsOnItsSecondaryAnimation() {
        AnimationController primary = new AnimationController();
        primary.value(1.0);
        AnimationController secondary = new AnimationController();
        FadeThroughTransition t = new FadeThroughTransition();
        t.animation(primary);
        t.secondaryAnimation(secondary);
        assertTrue(notifiedBySecondary(t, secondary));
    }
}
