package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawGradient extends AbstractGraphicsScreenshotTest {
    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int height = bounds.getHeight() / 3;
        int width = bounds.getWidth() / 2;
        int y = bounds.getY();
        g.fillRadialGradient(0xff, 0xff00, bounds.getX(), y, width, height);
        g.fillRadialGradient(0xff, 0xff00, bounds.getX() + width, y, width, height, 20, 200);
        y += height;

        g.fillRectRadialGradient(0xff0000, 0xcccccc, bounds.getX() + width, y, width, height, 0.5f, 0.5f, 2);
        g.fillLinearGradient(0xff, 0x999999, bounds.getX(), y, width, height, true);
        y += height;

        g.fillLinearGradient(0xff, 0x999999, bounds.getX(), y, width, height, false);
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-gradient";
    }
}
