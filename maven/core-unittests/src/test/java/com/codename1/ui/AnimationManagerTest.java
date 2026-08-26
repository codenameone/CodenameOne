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

    @Test
    void testAlreadyFinishedAnimationRunsCompletionWithoutUpdateState() {
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

        assertEquals(0, updateStateCalls.get(), "Already-finished animation must not mutate state");
        assertEquals(1, completionCalls.get(), "Completion callback should still run once");
    }
}
