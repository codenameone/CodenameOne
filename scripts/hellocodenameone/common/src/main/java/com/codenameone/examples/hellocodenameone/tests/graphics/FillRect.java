package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class FillRect extends AbstractGraphicsScreenshotTest {
    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0xffffff);
        for (int iter = 0 ; iter < bounds.getWidth() / 2 ; iter++) {
            nextColor(g);
            g.fillRect(bounds.getX() + iter, bounds.getY() + iter, bounds.getX() + bounds.getWidth() - iter, bounds.getY() + bounds.getHeight() + iter);
        }
    }

    @Override
    protected String screenshotName() {
        return "graphics-fill-rect";
    }
}
