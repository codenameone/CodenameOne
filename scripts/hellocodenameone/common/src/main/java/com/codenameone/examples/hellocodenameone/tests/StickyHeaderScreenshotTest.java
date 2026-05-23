package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.StickyHeaderContainer;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Motion;

/// Fast-scroll demo: drives the container's scroll position across the full
/// content range over the capture duration so each frame samples a different
/// scroll offset. With scroll-driven transitions, frames whose scroll lands
/// inside a section's push window will surface the partial overlap between
/// the outgoing pinned header and the incoming one.
public class StickyHeaderScreenshotTest extends AbstractStickyHeaderScreenshotTest {
    private Motion scrollMotion;

    @Override
    protected int getAnimationDurationMillis() {
        return 900;
    }

    @Override
    protected void configureTransition(StickyHeaderContainer sticky) {
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_SLIDE);
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        int contentHeight = sticky.getScrollContainer().getScrollDimension().getHeight();
        int viewportHeight = sticky.getScrollContainer().getHeight();
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollMotion = Motion.createEaseInOutMotion(0, maxScroll, getAnimationDurationMillis());
        scrollMotion.start();
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        sticky.setScrollPosition(scrollMotion.getValue());
        host.paintComponent(g, true);
    }

    @Override
    protected void finishCapture() {
        scrollMotion = null;
        super.finishCapture();
    }
}
