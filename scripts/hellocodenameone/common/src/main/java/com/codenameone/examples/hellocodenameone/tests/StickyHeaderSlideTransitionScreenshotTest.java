package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.StickyHeaderContainer;
import com.codename1.ui.Graphics;

/// Slow-scroll demo: holds section A pinned and steps the scroll position
/// through the push window so each frame samples a different overlap
/// fraction. Shows the next section's header rising into the slot and
/// pushing the pinned header up and out in sync with scroll.
public class StickyHeaderSlideTransitionScreenshotTest extends AbstractStickyHeaderScreenshotTest {

    @Override
    protected int getAnimationDurationMillis() {
        return 600;
    }

    @Override
    protected void configureTransition(StickyHeaderContainer sticky) {
        sticky.setTransitionStyle(StickyHeaderContainer.TRANSITION_SLIDE);
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        // Pin the first section so the captured push is a header-to-header
        // replacement rather than one materialising from nothing.
        sticky.setScrollPosition(1);
        sticky.updateSticky();
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        int sectionStride = sectionStrideHeight();
        int headerH = pinnedHeaderHeight();
        if (sectionStride > 0 && headerH > 0) {
            // Walk scroll from the moment the next header touches the slot
            // (relY == headerH, push == 0) to one pixel before the swap
            // (relY == 1, push == headerH - 1).
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
