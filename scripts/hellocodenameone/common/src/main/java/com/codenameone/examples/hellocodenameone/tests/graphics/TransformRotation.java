package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class TransformRotation extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        if (!Transform.isSupported()) {
            return;
        }

        int cx = bounds.getX() + 100;
        int cy = bounds.getY() + 100;

        // Rotate 45 degrees around Z axis (2D rotation)
        Transform t = Transform.makeIdentity();
        t.translate(cx, cy);
        t.rotate((float)(Math.PI / 4), 0, 0, 1);
        t.translate(-cx, -cy);

        g.setTransform(t);
        g.setColor(0xff0000);
        g.fillRect(cx - 25, cy - 25, 50, 50);

        g.setTransform(Transform.makeIdentity());
    }

    @Override
    protected String screenshotName() {
        return "graphics-transform-rotation";
    }
}
