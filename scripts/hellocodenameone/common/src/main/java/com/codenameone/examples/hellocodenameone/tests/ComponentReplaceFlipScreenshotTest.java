package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.animations.FlipTransition;
import com.codename1.ui.animations.Transition;

public class ComponentReplaceFlipScreenshotTest extends AbstractComponentReplaceScreenshotTest {
    private static final int FLIP_PHASE_DURATION = 250;

    @Override
    protected int getAnimationDurationMillis() {
        return FLIP_PHASE_DURATION * 3;
    }

    @Override
    protected Transition createTransition(int duration) {
        return new FlipTransition(0xff202020, FLIP_PHASE_DURATION);
    }
}
