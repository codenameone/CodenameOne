package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.StickyHeaderContainer;
import com.codename1.ui.Graphics;

/// Slow-scroll demo for the fade transition style: holds the scroll past one
/// boundary and steps `AnimationTime` through the swap so each frame samples
/// the cross-fade at progress 0%, 20%, …, 100%. Variants of
/// [StickyHeaderSlideTransitionScreenshotTest] capturing
/// [StickyHeaderContainer#TRANSITION_FADE] instead.
public class StickyHeaderFadeTransitionScreenshotTest extends AbstractStickyHeaderScreenshotTest {
    private boolean transitionTriggered;
    private int targetScrollY;

    @Override
    protected int getAnimationDurationMillis() {
        return 600;
    }

    @Override
    protected void configureTransition(StickyHeaderContainer sticky) {
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_FADE);
        sticky.setTransitionDurationMillis(getAnimationDurationMillis());
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        int stride = sectionStrideHeight();
        sticky.setScrollPosition(stride - 1);
        sticky.updateSticky();
        targetScrollY = stride * 2 - 1;
        transitionTriggered = false;
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        if (frameIndex == 0 && !transitionTriggered) {
            sticky.setScrollPosition(targetScrollY);
            sticky.updateSticky();
            transitionTriggered = true;
        }
        host.paintComponent(g, true);
    }
}
