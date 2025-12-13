package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class StrokeTest extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        if (!g.isShapeSupported()) {
            return;
        }

        int h = bounds.getHeight();
        int w = bounds.getWidth();
        int x = bounds.getX();
        int y = bounds.getY();

        g.setColor(0);

        // Caps
        float lineWidth = 15f;
        int yPos = y + 20;
        GeneralPath p1 = new GeneralPath();
        p1.moveTo(x + 20, yPos);
        p1.lineTo(x + w / 3 - 20, yPos);
        g.drawShape(p1, new Stroke(lineWidth, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f));

        GeneralPath p2 = new GeneralPath();
        p2.moveTo(x + w / 3 + 20, yPos);
        p2.lineTo(x + 2 * w / 3 - 20, yPos);
        g.drawShape(p2, new Stroke(lineWidth, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1f));

        GeneralPath p3 = new GeneralPath();
        p3.moveTo(x + 2 * w / 3 + 20, yPos);
        p3.lineTo(x + w - 20, yPos);
        g.drawShape(p3, new Stroke(lineWidth, Stroke.CAP_SQUARE, Stroke.JOIN_MITER, 1f));

        // Joins
        yPos += 50;
        int joinH = 40;
        int joinW = 40;

        // Miter
        GeneralPath j1 = new GeneralPath();
        j1.moveTo(x + 20, yPos + joinH);
        j1.lineTo(x + 20 + joinW / 2, yPos);
        j1.lineTo(x + 20 + joinW, yPos + joinH);
        g.drawShape(j1, new Stroke(lineWidth, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 10f));

        // Round
        GeneralPath j2 = new GeneralPath();
        j2.moveTo(x + w / 3 + 20, yPos + joinH);
        j2.lineTo(x + w / 3 + 20 + joinW / 2, yPos);
        j2.lineTo(x + w / 3 + 20 + joinW, yPos + joinH);
        g.drawShape(j2, new Stroke(lineWidth, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 10f));

        // Bevel
        GeneralPath j3 = new GeneralPath();
        j3.moveTo(x + 2 * w / 3 + 20, yPos + joinH);
        j3.lineTo(x + 2 * w / 3 + 20 + joinW / 2, yPos);
        j3.lineTo(x + 2 * w / 3 + 20 + joinW, yPos + joinH);
        g.drawShape(j3, new Stroke(lineWidth, Stroke.CAP_BUTT, Stroke.JOIN_BEVEL, 10f));

        // Miter Limit test
        yPos += 60;
        GeneralPath m1 = new GeneralPath();
        m1.moveTo(x + 20, yPos + joinH);
        m1.lineTo(x + 20 + joinW / 2, yPos + joinH / 2 + 5); // Sharp angle
        m1.lineTo(x + 20 + joinW, yPos + joinH);
        // Low miter limit -> Bevel
        g.drawShape(m1, new Stroke(lineWidth, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f));

        GeneralPath m2 = new GeneralPath();
        m2.moveTo(x + w/2 + 20, yPos + joinH);
        m2.lineTo(x + w/2 + 20 + joinW / 2, yPos + joinH / 2 + 5); // Sharp angle
        m2.lineTo(x + w/2 + 20 + joinW, yPos + joinH);
        // High miter limit -> Miter
        g.drawShape(m2, new Stroke(lineWidth, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 10f));

    }

    @Override
    protected String screenshotName() {
        return "graphics-stroke-test";
    }
}
