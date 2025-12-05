package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawLine extends AbstractGraphicsScreenshotTest {
    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0xffffff);
        // horizontal lines
        for (int iter = 0 ; iter < bounds.getHeight() ; iter++) {
            nextColor(g);
            g.drawLine(bounds.getX(), bounds.getY() + iter, bounds.getX() + bounds.getWidth(), bounds.getY() + iter);
        }

        g.setColor(0);
        // vertical lines gapped by 5
        for (int iter = 0 ; iter < bounds.getWidth() ; iter += 5) {
            nextColor(g);
            g.drawLine(bounds.getX() + iter, bounds.getY(), bounds.getX() + iter, bounds.getY() + bounds.getHeight());
        }

        // diagonal lines down slope with alpha
        g.setColor(0xfffff);
        g.setAlpha(128);
        for (int iter = 0 ; iter < bounds.getWidth() ; iter += 20) {
            g.drawLine(bounds.getX(), bounds.getY(), bounds.getX() + iter, bounds.getY() + bounds.getHeight());
        }

        // diagonal lines up slope with alpha
        g.setColor(0);
        g.setAlpha(128);
        for (int iter = 0 ; iter < bounds.getWidth() ; iter += 20) {
            g.drawLine(bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight(), bounds.getX() + iter, bounds.getY());
        }
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-line";
    }
}
