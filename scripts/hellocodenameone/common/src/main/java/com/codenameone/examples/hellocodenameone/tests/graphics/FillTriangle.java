package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class FillTriangle extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0xff0000);
        g.fillTriangle(bounds.getX(), bounds.getY(),
                       bounds.getX() + bounds.getWidth(), bounds.getY(),
                       bounds.getX() + bounds.getWidth() / 2, bounds.getY() + bounds.getHeight());

        g.setColor(0x00ff00);
        g.setAlpha(128);
        g.fillTriangle(bounds.getX() + bounds.getWidth() / 2, bounds.getY(),
                       bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight(),
                       bounds.getX(), bounds.getY() + bounds.getHeight());
    }

    @Override
    protected String screenshotName() {
        return "graphics-fill-triangle";
    }
}
