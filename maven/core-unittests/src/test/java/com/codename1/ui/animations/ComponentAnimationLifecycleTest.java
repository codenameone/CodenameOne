package com.codename1.ui.animations;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentAnimationLifecycleTest {

    @Test
    void testUpdateAnimationStateStillInvokesUpdateStateWhenAlreadyFinished() {
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
        animation.setOnCompletion(completionCalls::incrementAndGet);

        animation.updateAnimationState();

        assertEquals(1, updateStateCalls.get(), "Direct updateAnimationState() should still invoke updateState()");
        assertEquals(1, completionCalls.get(), "Completion callback should run when animation is finished");
    }

    @Test
    void testRestartedAnimationResetsCompletionLifecycle() {
        AtomicInteger completionCalls = new AtomicInteger();

        class RestartableAnimation extends ComponentAnimation {
            private int remainingSteps = 2;

            @Override
            public boolean isInProgress() {
                return remainingSteps > 0;
            }

            @Override
            protected void updateState() {
                remainingSteps--;
            }

            public void restart() {
                remainingSteps = 2;
            }
        }
        RestartableAnimation animation = new RestartableAnimation();
        animation.setOnCompletion(completionCalls::incrementAndGet);

        animation.updateAnimationState();
        animation.updateAnimationState();
        assertEquals(1, completionCalls.get(), "Completion should run for first lifecycle");

        animation.updateAnimationState();
        assertEquals(1, completionCalls.get(), "No extra completion should fire without restart");

        animation.restart();
        animation.updateAnimationState();
        animation.updateAnimationState();
        assertEquals(2, completionCalls.get(), "Completion should run again after restart");
    }
}
