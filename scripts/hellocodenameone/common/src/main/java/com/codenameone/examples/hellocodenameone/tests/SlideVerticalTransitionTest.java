package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;

public class SlideVerticalTransitionTest extends AbstractTransitionScreenshotTest {
    @Override
    protected Transition createTransition(int duration) {
        return CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, true, duration);
    }
}
