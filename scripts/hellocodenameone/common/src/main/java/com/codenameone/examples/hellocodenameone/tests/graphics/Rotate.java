package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class Rotate extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        if (!g.isAffineSupported()) {
             g.drawString("Affine unsupported", bounds.getX(), bounds.getY());
             return;
        }

        int cx = bounds.getX() + bounds.getWidth() / 2;
        int cy = bounds.getY() + bounds.getHeight() / 2;
        int size = Math.min(bounds.getWidth(), bounds.getHeight()) / 4;

        g.setColor(0xff0000);
        g.fillRect(cx - size/2, cy - size/2, size, size);

        // Rotate 45 degrees
        g.rotateRadians((float)(Math.PI / 4), cx, cy);
        g.setColor(0x00ff00);
        g.setAlpha(128);
        g.fillRect(cx - size/2, cy - size/2, size, size);

        // Rotate another 45 degrees (total 90)
        g.rotateRadians((float)(Math.PI / 4), cx, cy);
        g.setColor(0x0000ff);
        g.setAlpha(128);
        g.fillRect(cx - size/2, cy - size/2, size, size);

        g.resetAffine();
    }

    @Override
    protected String screenshotName() {
        return "graphics-rotate";
    }
}
