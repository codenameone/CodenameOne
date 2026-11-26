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

    /// An animation that reports `isInProgress() == false` from the moment it is queued has never been
    /// stepped, so its whole payload is still sitting in `updateState()` and it has to be applied once
    /// before the animation completes. This is the shape of every deferred UI mutation `Container`
    /// queues while another animation is in flight - see issue #5606, where skipping the update made
    /// `add()` and `remove()` during an animation silent no-ops.
    @Test
    void testNeverSteppedAnimationIsAppliedOnceBeforeCompletion() {
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

        assertEquals(1, updateStateCalls.get(), "Never-stepped animation must apply its payload once");
        assertEquals(1, completionCalls.get(), "Completion callback should still run once");

        manager.updateAnimations();
        assertEquals(1, updateStateCalls.get(), "The payload must not be applied a second time");
        assertEquals(1, completionCalls.get(), "Completion callback should not run a second time");
    }

    /// A post-animation task that starts an animation makes the next task see the
    /// manager busy; that task re-queues itself through `flushAnimation` (as
    /// `Sheet.showOnHost` does) and must wait for the new animation rather than be
    /// run again in the same pass. The drain used to re-run it forever with the
    /// EDT pinned. The retry here stops itself after 50 rounds so a regression
    /// fails instead of hanging the suite.
    @FormTest
    void testTaskRequeuedDuringDrainWaitsForTheNextPass() {
        Form form = CN.getCurrentForm();
        final AnimationManager manager = form.getAnimationManager();
        final AtomicInteger retries = new AtomicInteger();
        final ComponentAnimation started = new ComponentAnimation() {
            private int remainingSteps = 2;

            @Override
            public boolean isInProgress() {
                return remainingSteps > 0;
            }

            @Override
            protected void updateState() {
                remainingSteps--;
            }
        };
        final Runnable[] retry = new Runnable[1];
        retry[0] = new Runnable() {
            @Override
            public void run() {
                if (retries.incrementAndGet() < 50 && manager.isAnimating()) {
                    manager.flushAnimation(retry[0]);
                }
            }
        };
        // Something already running, so both tasks below are queued rather than
        // run on the spot.
        manager.addAnimation(new ComponentAnimation() {
            private int remainingSteps = 1;

            @Override
            public boolean isInProgress() {
                return remainingSteps > 0;
            }

            @Override
            protected void updateState() {
                remainingSteps--;
            }
        });
        manager.flushAnimation(new Runnable() {
            @Override
            public void run() {
                manager.addAnimation(started);
            }
        });
        manager.flushAnimation(retry[0]);

        // step the blocker, retire it, then drain the queue
        manager.updateAnimations();
        manager.updateAnimations();
        manager.updateAnimations();
        assertEquals(0, retries.get(), "The task must wait while the animation a previous task started runs");
        for (int iter = 0; iter < 10 && retries.get() == 0; iter++) {
            manager.updateAnimations();
        }
        assertEquals(1, retries.get(), "The task runs once the animation has finished");
    }
}
