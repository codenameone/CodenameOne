package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;

public class AnimateLayoutScreenshotTest extends AbstractContainerAnimationScreenshotTest {
    private static final int[] PALETTE = {0xef476f, 0xffd166, 0x06d6a0, 0x118ab2, 0x073b4c, 0x8338ec};

    @Override
    protected Container buildContainer(int frameWidth, int frameHeight) {
        Container c = new Container(BoxLayout.y());
        Style cs = c.getAllStyles();
        cs.setBgColor(0xfafafa);
        cs.setBgTransparency(255);
        // setPadding/setMargin keep whatever unit the style inherited from
        // the theme (Style.setPadding javadoc: "Units are specified by
        // setPaddingUnit"). The iOS/JS base themes declare Label padding in
        // millimeters, so there the 12 below meant 12mm (~217px per side on a
        // 460ppi phone): the text space went negative as the tiles narrowed
        // into the grid and the labels were clipped empty in the last frames,
        // while px-unit ports (JavaSE/mac/Android) kept the text. Pin pixels
        // so the scene renders identically on every port.
        cs.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        cs.setPadding(8, 8, 8, 8);
        for (int i = 0; i < PALETTE.length; i++) {
            Label tile = new Label("Tile " + (i + 1));
            Style ts = tile.getAllStyles();
            ts.setBgColor(PALETTE[i]);
            ts.setFgColor(0xffffff);
            ts.setBgTransparency(255);
            ts.setMarginUnit(Style.UNIT_TYPE_PIXELS);
            ts.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
            ts.setMargin(4, 4, 4, 4);
            ts.setPadding(12, 12, 12, 12);
            c.add(tile);
        }
        return c;
    }

    @Override
    protected ComponentAnimation startAnimation(Container container, int duration) {
        container.setLayout(new GridLayout(2, 3));
        return container.createAnimateLayout(duration);
    }
}
