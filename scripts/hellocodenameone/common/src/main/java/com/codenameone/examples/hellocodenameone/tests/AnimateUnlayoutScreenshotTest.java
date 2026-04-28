package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Demonstrates `Container.createAnimateUnlayout` - the components start at their
/// natural positions (top of the grid) and animate out to off-screen positions
/// the test sets manually before kicking off the animation. The container is
/// left in an "invalid" state at the end on purpose; the typical caller follows
/// up with `removeAll` + `revalidate` to settle it.
public class AnimateUnlayoutScreenshotTest extends AbstractContainerAnimationScreenshotTest {
    private static final int[] PALETTE = {0xef476f, 0xffd166, 0x06d6a0, 0x118ab2, 0x073b4c, 0x8338ec};

    @Override
    protected Container buildContainer(int frameWidth, int frameHeight) {
        Container c = new Container(BoxLayout.y());
        Style cs = c.getAllStyles();
        cs.setBgColor(0xfafafa);
        cs.setBgTransparency(255);
        cs.setPadding(8, 8, 8, 8);
        for (int i = 0; i < PALETTE.length; i++) {
            Label tile = new Label("Tile " + (i + 1));
            Style ts = tile.getAllStyles();
            ts.setBgColor(PALETTE[i]);
            ts.setFgColor(0xffffff);
            ts.setBgTransparency(255);
            ts.setMargin(4, 4, 4, 4);
            ts.setPadding(12, 12, 12, 12);
            c.add(tile);
        }
        return c;
    }

    @Override
    protected ComponentAnimation startAnimation(Container container, int duration) {
        // Push every child above the visible area; createAnimateUnlayout will
        // capture these as the "destination" positions and animate from the
        // current natural layout to them, so we see the tiles fly upward.
        int containerHeight = container.getHeight();
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component child = container.getComponentAt(i);
            child.setY(-containerHeight);
        }
        return container.createAnimateUnlayout(duration, 0, null);
    }
}
