package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawShape extends AbstractGraphicsScreenshotTest {

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
        g.drawShape(p, new Stroke(2f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f));

        GeneralPath curve = new GeneralPath();
        curve.moveTo(bounds.getX(), bounds.getY() + bounds.getHeight() / 2);
        curve.curveTo(bounds.getX() + bounds.getWidth() / 3, bounds.getY(),
                      bounds.getX() + 2 * bounds.getWidth() / 3, bounds.getY() + bounds.getHeight(),
                      bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight() / 2);

        g.setColor(0xff0000);
        g.drawShape(curve, new Stroke(3f, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1f));
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-shape";
    }
}
