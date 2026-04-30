package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.StickyHeaderContainer;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Motion;

/// Fast-scroll demo: drives the container's scroll position across the full
/// content range over the capture duration so each frame samples a different
/// scroll offset. With a long transition duration relative to the per-frame
/// scroll delta the slide animations are still in flight when each frame is
/// captured, surfacing the staged header replacement.
public class StickyHeaderScreenshotTest extends AbstractStickyHeaderScreenshotTest {
    private Motion scrollMotion;

    @Override
    protected int getAnimationDurationMillis() {
        return 900;
    }

    @Override
    protected void configureTransition(StickyHeaderContainer sticky) {
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_SLIDE);
        // Pick a duration shorter than the per-frame scroll delta
        // (900ms / 5 ≈ 180ms) so most frames capture the post-transition
        // settled state, with the occasional in-flight overlap when scroll
        // crosses a section boundary mid-frame.
        sticky.setTransitionDurationMillis(150);
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
