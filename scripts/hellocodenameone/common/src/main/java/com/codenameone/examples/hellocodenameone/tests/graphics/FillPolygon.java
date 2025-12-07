package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class FillPolygon extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int[] xPoints = new int[360];
        int[] yPoints = new int[360];
        int centerX = bounds.getX() + bounds.getWidth() / 2;
        int centerY = bounds.getY() + bounds.getHeight() / 2;
        int radius = bounds.getWidth() / 3;
        for(int i = 0; i < 360 ; i++) {
            double angle = 2 * Math.PI * i / 360.0;
            xPoints[i] = centerX + (int) Math.round(radius * Math.cos(angle));
            yPoints[i] = centerY + (int) Math.round(radius * Math.sin(angle));
        }

        g.setColor(0xffffff);
        g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());

        g.setColor(0);
        g.fillPolygon(xPoints, yPoints, 360);
        
        g.setColor(0xff);
        g.drawPolygon(xPoints, yPoints, 360);
    }

    @Override
    protected String screenshotName() {
        return "graphics-fill-polygon";
    }
}
