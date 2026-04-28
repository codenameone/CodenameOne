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
/// scroll position from a value past the top edge (-tensilePull) back to 0.
/// `Component.startTensile` is package-private and only fires from real touch
/// release events, so we mirror its math (same Motion factory and duration
/// calculation) and apply it via a Container subclass exposing setScrollY.
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

    private static final int OVER_PULL = 80;

    private Form scrollHost;
    private ScrollContainer scrollContainer;
    private Motion bounceMotion;

    @Override
    protected int getAnimationDurationMillis() {
        return 600;
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
        cs.setBgColor(0xfafafa);
        cs.setBgTransparency(255);
        cs.setPadding(4, 4, 4, 4);
        for (int i = 0; i < 12; i++) {
            Label tile = new Label("Pulled " + (i + 1));
            Style ts = tile.getAllStyles();
            ts.setBgColor(0x118ab2);
            ts.setFgColor(0xffffff);
            ts.setBgTransparency(255);
            ts.setMargin(2, 2, 2, 2);
            ts.setPadding(14, 14, 12, 12);
            scrollContainer.add(tile);
        }
        scrollHost.add(BorderLayout.CENTER, scrollContainer);
        scrollHost.layoutContainer();

        // Critically-damped spring matches the framework's iOS-style tensile
        // configuration in Component.startTensile.
        bounceMotion = Motion.createCriticalDampedSpringMotion(-OVER_PULL, 0, getAnimationDurationMillis());
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
}
