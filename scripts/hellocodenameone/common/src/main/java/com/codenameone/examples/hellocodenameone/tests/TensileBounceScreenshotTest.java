package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/// Visualises the tensile bounce-back: a critically-damped spring eases the
/// scroll position from a value past the top edge back to 0.
/// `Component.startTensile` is package-private and only fires from real touch
/// release events, so we mirror its math (same Motion factory and duration
/// calculation) and apply it via a Container subclass exposing setScrollY.
/// The over-pull is a substantial fraction of the viewport so the gap above
/// the first tile is plainly visible in every intermediate frame and snaps
/// closed by the last.
public class TensileBounceScreenshotTest extends AbstractAnimationScreenshotTest {
    private static class ScrollContainer extends Container {
        ScrollContainer(Layout l) {
            super(l);
            setScrollableY(true);
        }

        void scrollTo(int y) {
            setScrollY(y);
        }
    }

    private static final int OVER_PULL_FRACTION = 3;
    private static final int TILE_COUNT = 18;

    private Form scrollHost;
    private ScrollContainer scrollContainer;
    private Motion bounceMotion;

    @Override
    protected int getAnimationDurationMillis() {
        return 700;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        scrollHost = new Form("Tensile Bounce");
        scrollHost.setLayout(new BorderLayout());
        scrollHost.setWidth(frameWidth);
        scrollHost.setHeight(frameHeight);
        scrollHost.setVisible(true);

        scrollContainer = new ScrollContainer(BoxLayout.y());
        Style cs = scrollContainer.getAllStyles();
        // A bold backdrop colour so the bounce gap reads as obvious empty space
        // above the tiles rather than blending into a pale page background.
        cs.setBgColor(0x0b132b);
        cs.setBgTransparency(255);
        cs.setPadding(4, 4, 4, 4);
        for (int i = 0; i < TILE_COUNT; i++) {
            Label tile = new Label("Pulled " + (i + 1));
            Style ts = tile.getAllStyles();
            ts.setBgColor(rowColor(i));
            ts.setFgColor(0xffffff);
            ts.setBgTransparency(255);
            ts.setMargin(2, 2, 2, 2);
            ts.setPadding(18, 18, 16, 16);
            scrollContainer.add(tile);
        }
        scrollHost.add(BorderLayout.CENTER, scrollContainer);
        scrollHost.layoutContainer();

        // Pull a third of the visible viewport - matching what a user can drag
        // past the top edge before lifting off in iOS. Anything smaller (<10%)
        // ends up as a single row of whitespace that disappears in scaled-down
        // grid cells.
        int overPull = Math.max(1, scrollContainer.getHeight() / OVER_PULL_FRACTION);
        bounceMotion = Motion.createCriticalDampedSpringMotion(-overPull, 0, getAnimationDurationMillis());
        bounceMotion.start();
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        scrollContainer.scrollTo(bounceMotion.getValue());
        scrollHost.paintComponent(g, true);
    }

    @Override
    protected void finishCapture() {
        scrollHost = null;
        scrollContainer = null;
        bounceMotion = null;
        super.finishCapture();
    }

    private static int rowColor(int i) {
        int[] palette = {0x118ab2, 0x06d6a0, 0xffd166, 0xef476f, 0x8338ec, 0xfb5607};
        return palette[i % palette.length];
    }
}
