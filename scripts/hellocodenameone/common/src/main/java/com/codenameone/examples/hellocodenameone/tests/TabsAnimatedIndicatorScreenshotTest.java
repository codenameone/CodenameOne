package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;

/// Walks the animated tabs indicator from tab 0 to tab 2 across six frames so
/// the under-line slide is captured deterministically. Reads
/// [com.codename1.ui.animations.AnimationTime] (set per-frame by the harness)
/// to interpolate `indicatorFromX/W -> indicatorToX/W` via the Motion the
/// Tabs class started in `prepareCapture`.
public class TabsAnimatedIndicatorScreenshotTest extends AbstractAnimationScreenshotTest {
    private Form host;
    private Tabs tabs;

    @Override
    protected int getAnimationDurationMillis() {
        return 200;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        host = new Form("Tabs Indicator", new BorderLayout());
        host.setWidth(frameWidth);
        host.setHeight(frameHeight);
        host.setVisible(true);
        Style cps = host.getContentPane().getAllStyles();
        cps.setBgColor(0xf0f4f8);
        cps.setBgTransparency(255);

        tabs = new Tabs();
        tabs.setAnimatedIndicator(true);
        tabs.addTab("Home", new Button("Home content"));
        tabs.addTab("Search", new Button("Search content"));
        tabs.addTab("Profile", new Button("Profile content"));
        host.add(BorderLayout.CENTER, tabs);
        host.layoutContainer();

        // Kick off the indicator slide -- the Motion this starts reads
        // AnimationTime which the harness advances per frame.
        tabs.setSelectedIndex(2, false);
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        host.paintComponent(g, true);
    }

    @Override
    protected void finishCapture() {
        host = null;
        tabs = null;
        super.finishCapture();
    }
}
