package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class FillShape extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        if (!g.isShapeSupported()) {
            return;
        }

        GeneralPath p = new GeneralPath();
        p.moveTo(bounds.getX(), bounds.getY());
        p.lineTo(bounds.getX() + bounds.getWidth(), bounds.getY());
        p.lineTo(bounds.getX() + bounds.getWidth() / 2, bounds.getY() + bounds.getHeight());
        p.closePath();

        g.setColor(0x0000ff);
        g.fillShape(p);

        // Star shape
        GeneralPath star = new GeneralPath();
        double centerX = bounds.getX() + bounds.getWidth() / 2.0;
        double centerY = bounds.getY() + bounds.getHeight() / 2.0;
        double radius = Math.min(bounds.getWidth(), bounds.getHeight()) / 3.0;
        double innerRadius = radius / 2.5;

        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5;
            double r = (i % 2 == 0) ? radius : innerRadius;
            double x = centerX + Math.cos(angle) * r;
            double y = centerY - Math.sin(angle) * r;
            if (i == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.closePath();

        g.setColor(0xffff00);
        g.setAlpha(128);
        g.fillShape(star);
    }

    @Override
    protected String screenshotName() {
        return "graphics-fill-shape";
    }
}
