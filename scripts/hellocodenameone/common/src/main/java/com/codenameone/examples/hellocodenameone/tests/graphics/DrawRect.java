package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawRect extends AbstractGraphicsScreenshotTest {
    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0xffffff);
        for (int iter = 0 ; iter < bounds.getWidth() / 2 ; iter++) {
            nextColor(g);
            g.drawRect(bounds.getX() + iter, bounds.getY() + iter, bounds.getX() + bounds.getWidth() - iter, bounds.getY() + bounds.getHeight() + iter);
        }

        g.setColor(0);
        g.setAlpha(80);
        for (int iter = 0 ; iter < bounds.getWidth() / 2 ; iter += 20) {
            nextColor(g);
            g.drawRect(bounds.getX() + iter, bounds.getY() + iter, bounds.getX() + bounds.getWidth() - iter, bounds.getY() + bounds.getHeight() + iter, 4);
        }
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-rect";
    }
}
