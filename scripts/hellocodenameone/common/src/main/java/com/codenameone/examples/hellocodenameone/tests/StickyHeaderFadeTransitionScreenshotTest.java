package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.StickyHeaderContainer;
import com.codename1.ui.Graphics;

/// Slow-scroll demo for the fade transition style: holds section A pinned
/// and steps the scroll position through the push window so each frame
/// samples the pinned header's opacity dropping from fully opaque to fully
/// transparent as the next section closes in. The next header rises into
/// the slot through the scroller and is revealed as the pinned one fades.
public class StickyHeaderFadeTransitionScreenshotTest extends AbstractStickyHeaderScreenshotTest {

    @Override
    protected int getAnimationDurationMillis() {
        return 600;
    }

    @Override
    protected void configureTransition(StickyHeaderContainer sticky) {
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_FADE);
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        sticky.setScrollPosition(1);
        sticky.updateSticky();
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        int sectionStride = sectionStrideHeight();
        int headerH = pinnedHeaderHeight();
        if (sectionStride > 0 && headerH > 0) {
            int startScroll = sectionStride - headerH;
            int span = headerH - 1;
            int scrollY = startScroll + (int) Math.round(progress * span);
            sticky.setScrollPosition(scrollY);
            sticky.updateSticky();
        }
        host.paintComponent(g, true);
    }

    private int pinnedHeaderHeight() {
        int h = sticky.getStickyHost().getHeight();
        if (h <= 0 && !sticky.getStickyHeaders().isEmpty()) {
            h = sticky.getStickyHeaders().get(0).getPreferredH();
        }
        return h;
    }
}
