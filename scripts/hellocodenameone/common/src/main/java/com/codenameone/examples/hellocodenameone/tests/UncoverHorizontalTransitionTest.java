package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;

public class UncoverHorizontalTransitionTest extends AbstractTransitionScreenshotTest {
    @Override
    protected Transition createTransition(int duration) {
        return CommonTransitions.createUncover(CommonTransitions.SLIDE_HORIZONTAL, true, duration);
    }
}
