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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.animations.ComponentAnimation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AnimationManagerTest extends UITestBase {

    @FormTest
    void testFinishedAnimationDoesNotUpdateStateAgainBeforeRemoval() {
        Form form = CN.getCurrentForm();
        AnimationManager manager = form.getAnimationManager();
        AtomicInteger updateStateCalls = new AtomicInteger();
        AtomicInteger completionCalls = new AtomicInteger();

        ComponentAnimation animation = new ComponentAnimation() {
            private int remainingSteps = 1;

            @Override
            public boolean isInProgress() {
                return remainingSteps > 0;
            }

            @Override
            protected void updateState() {
                updateStateCalls.incrementAndGet();
                remainingSteps--;
            }
        };

        manager.addAnimation(animation, completionCalls::incrementAndGet);

        manager.updateAnimations();
        assertEquals(1, updateStateCalls.get(), "Animation should update once while in progress");
        assertEquals(1, completionCalls.get(), "Completion callback should run exactly once");
        assertFalse(manager.isAnimating(), "Animation should report as not animating once completed");

        manager.updateAnimations();
        assertEquals(1, updateStateCalls.get(), "Finished animation should not receive another update");
        assertEquals(1, completionCalls.get(), "Completion callback should not run a second time");
    }

    /**
     * An animation that reports {@code isInProgress() == false} from the moment it is queued has never
     * been stepped, so its whole effect is still pending - the deferred add/remove a {@link Container}
     * queues while another animation runs, and a theme refresh, are all shaped that way. Completing it
     * without that one {@code updateState()} call dropped the mutation on the floor (issue #5600), so
     * the manager owes it exactly one update, and no more.
     */
    @Test
    void testAlreadyFinishedAnimationAppliesItsStateOnceThenCompletes() {
        Form form = new Form();
        AnimationManager manager = form.getAnimationManager();
        AtomicInteger updateStateCalls = new AtomicInteger();
        AtomicInteger completionCalls = new AtomicInteger();

        ComponentAnimation animation = new ComponentAnimation() {
            @Override
            public boolean isInProgress() {
                return false;
            }

            @Override
            protected void updateState() {
                updateStateCalls.incrementAndGet();
            }
        };

        manager.addAnimation(animation, completionCalls::incrementAndGet);
        assertFalse(manager.isAnimating(), "Already-finished animation should not mark manager as animating");

        manager.updateAnimations();

        assertEquals(1, updateStateCalls.get(), "Never-stepped animation must still apply its state once");
        assertEquals(1, completionCalls.get(), "Completion callback should still run once");

        manager.updateAnimations();

        assertEquals(1, updateStateCalls.get(), "The one update must not be repeated after completion");
        assertEquals(1, completionCalls.get(), "Completion callback should not run a second time");
    }
}
