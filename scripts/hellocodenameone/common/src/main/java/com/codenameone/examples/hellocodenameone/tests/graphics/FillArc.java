package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class FillArc extends AbstractGraphicsScreenshotTest {
    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0xffffff);
        for (int iter = 0 ; iter < bounds.getWidth() / 2 ; iter++) {
            nextColor(g);
            int width = bounds.getWidth() - (iter * 2);
            int height = bounds.getHeight() - (iter * 2);
            if (width <= 0 || height <= 0) {
                break;
            }
            g.fillArc(bounds.getX() + iter, bounds.getY() + iter, width, height, iter, 180);
        }
    }

    @Override
    protected String screenshotName() {
        return "graphics-fill-arc";
    }
}
