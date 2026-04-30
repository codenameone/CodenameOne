package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.StickyHeaderContainer;
import com.codename1.ui.Graphics;

/// Slow-scroll demo: holds the scroll position just past one section
/// boundary and steps `AnimationTime` through a single forward slide
/// transition so each frame samples the swap at progress 0%, 20%, …, 100%.
/// Shows the outgoing header sliding upward out of the slot while the
/// incoming header rises into place.
public class StickyHeaderSlideTransitionScreenshotTest extends AbstractStickyHeaderScreenshotTest {
    private boolean transitionTriggered;
    private int targetScrollY;

    @Override
    protected int getAnimationDurationMillis() {
        return 600;
    }

    @Override
    protected void configureTransition(StickyHeaderContainer sticky) {
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_SLIDE);
        // Match the capture window so progress maps 1:1 onto the 6 frames.
        sticky.setTransitionDurationMillis(getAnimationDurationMillis());
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        // Pre-pin the first section so the captured swap is the second
        // boundary - that way the demo shows a header-to-header replacement
        // rather than a header materialising from nothing.
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
