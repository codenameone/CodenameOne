package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;

public class ComponentReplaceFadeScreenshotTest extends AbstractComponentReplaceScreenshotTest {
    @Override
    protected Transition createTransition(int duration) {
        return CommonTransitions.createFade(duration);
    }
}
