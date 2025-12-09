package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class TransformTranslation extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        if (!Transform.isSupported()) {
            return;
        }

        Transform t = Transform.makeTranslation(20, 20);
        g.setTransform(t);

        g.setColor(0xff0000);
        g.fillRect(bounds.getX(), bounds.getY(), 50, 50);

        t.translate(20, 20);
        g.setTransform(t);
        g.setColor(0x00ff00);
        g.fillRect(bounds.getX(), bounds.getY(), 50, 50);

        g.setTransform(Transform.makeIdentity());
    }

    @Override
    protected String screenshotName() {
        return "graphics-transform-translation";
    }
}
