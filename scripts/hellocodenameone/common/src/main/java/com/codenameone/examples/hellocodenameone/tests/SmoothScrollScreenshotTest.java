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

/// Visualises a smooth-scroll animation. Off-screen forms aren't initialized,
/// so `scrollRectToVisible` would jump rather than tween; instead we drive the
/// container's scrollY directly with the same linear motion the framework's
/// `initScrollMotion` uses, then paint a frame for each motion sample.
public class SmoothScrollScreenshotTest extends AbstractAnimationScreenshotTest {
    private static class ScrollContainer extends Container {
        ScrollContainer(Layout l) {
            super(l);
            setScrollableY(true);
        }

        void scrollTo(int y) {
            setScrollY(y);
        }
    }

    private Form scrollHost;
    private ScrollContainer scrollContainer;
    private Motion scrollMotion;

    @Override
    protected int getAnimationDurationMillis() {
        return 800;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        scrollHost = new Form("Smooth Scroll");
        scrollHost.setLayout(new BorderLayout());
        scrollHost.setWidth(frameWidth);
        scrollHost.setHeight(frameHeight);
        scrollHost.setVisible(true);

        scrollContainer = new ScrollContainer(BoxLayout.y());
        Style cs = scrollContainer.getAllStyles();
        cs.setBgColor(0xfafafa);
        cs.setBgTransparency(255);
        cs.setPadding(4, 4, 4, 4);
        int tileCount = 24;
        for (int i = 0; i < tileCount; i++) {
            Label tile = new Label("Item " + (i + 1));
            Style ts = tile.getAllStyles();
            ts.setBgColor(rowColor(i));
            ts.setFgColor(0xffffff);
            ts.setBgTransparency(255);
            ts.setMargin(2, 2, 2, 2);
            ts.setPadding(14, 14, 12, 12);
            scrollContainer.add(tile);
        }
        scrollHost.add(BorderLayout.CENTER, scrollContainer);
        scrollHost.layoutContainer();

        int contentHeight = scrollContainer.getScrollDimension().getHeight();
        int maxScroll = Math.max(0, contentHeight - scrollContainer.getHeight());
        scrollMotion = Motion.createEaseInOutMotion(0, maxScroll, getAnimationDurationMillis());
        scrollMotion.start();
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        scrollContainer.scrollTo(scrollMotion.getValue());
        scrollHost.paintComponent(g, true);
    }

    @Override
    protected void finishCapture() {
        scrollHost = null;
        scrollContainer = null;
        scrollMotion = null;
        super.finishCapture();
    }

    private static int rowColor(int i) {
        int[] palette = {0x118ab2, 0x06d6a0, 0xffd166, 0xef476f, 0x8338ec, 0x073b4c};
        return palette[i % palette.length];
    }
}
