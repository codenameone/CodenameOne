package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;

public class AnimateHierarchyScreenshotTest extends AbstractContainerAnimationScreenshotTest {
    @Override
    protected Container buildContainer(int frameWidth, int frameHeight) {
        Container outer = new Container(BoxLayout.y());
        Style os = outer.getAllStyles();
        os.setBgColor(0xfafafa);
        os.setBgTransparency(255);
        os.setPadding(6, 6, 6, 6);
        Container inner = new Container(BoxLayout.y());
        Style is = inner.getAllStyles();
        is.setBgColor(0xe5e7eb);
        is.setBgTransparency(255);
        is.setPadding(4, 4, 4, 4);
        for (int i = 0; i < 4; i++) {
            Label tile = new Label("Inner " + (i + 1));
            tile.getAllStyles().setBgColor(0x4cc9f0);
            tile.getAllStyles().setFgColor(0x0b132b);
            tile.getAllStyles().setBgTransparency(255);
            tile.getAllStyles().setPadding(8, 8, 8, 8);
            tile.getAllStyles().setMargin(2, 2, 2, 2);
            inner.add(tile);
        }
        outer.add(inner);
        return outer;
    }

    @Override
    protected ComponentAnimation startAnimation(Container container, int duration) {
        Container inner = (Container) container.getComponentAt(0);
        inner.setLayout(new GridLayout(2, 2));
        return container.createAnimateHierarchy(duration);
    }
}
