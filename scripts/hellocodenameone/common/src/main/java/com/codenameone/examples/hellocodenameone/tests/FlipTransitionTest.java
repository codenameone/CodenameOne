package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.animations.FlipTransition;
import com.codename1.ui.animations.Transition;

public class FlipTransitionTest extends AbstractTransitionScreenshotTest {
    private static final int FLIP_PHASE_DURATION = 300;

    @Override
    protected int getAnimationDurationMillis() {
        // FlipTransition runs three sequential phases (move away, flip, move
        // closer), so the wall-clock animation lasts 3x the configured duration.
        return FLIP_PHASE_DURATION * 3;
    }

    @Override
    protected Transition createTransition(int duration) {
        return new FlipTransition(0xff202020, FLIP_PHASE_DURATION);
    }
}
