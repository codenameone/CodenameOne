package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class Scale extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        if(!g.isAffineSupported()) {
            g.drawString("Affine unsupported", 0, 0);
            return;
        }

        float xScale = 0.01f * ((float)bounds.getHeight());
        float yScale = 0.01f * ((float)bounds.getWidth());
        g.scale(xScale, yScale);
        int translateX = (int)(bounds.getX() / xScale);
        int translateY = (int)(bounds.getY() / yScale);
        g.translate(translateX, translateY);
        g.fillLinearGradient(0xff0000, 0xff, 0, 0, 100, 100, true);
        g.scale(-1, 1);
        g.fillLinearGradient(0xff0000, 0xff, 0, 100, 100, 100, true);

        g.translate(-translateX, -translateY);
        g.resetAffine();
    }

    @Override
    protected String screenshotName() {
        return "graphics-scale";
    }
}
