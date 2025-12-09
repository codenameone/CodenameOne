package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class Clip extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        // Original clip
        int[] originalClip = g.getClip();

        g.setColor(0xcccccc);
        g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());

        g.pushClip();

        // Clip Rect
        g.clipRect(bounds.getX() + 10, bounds.getY() + 10, bounds.getWidth() / 2, bounds.getHeight() / 2);
        g.setColor(0xff0000);
        g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());

        g.popClip();

        // Should be back to normal
        g.setColor(0x00ff00);
        g.fillRect(bounds.getX() + bounds.getWidth() - 20, bounds.getY() + bounds.getHeight() - 20, 20, 20);

        if (g.isShapeClipSupported()) {
            g.pushClip();
            GeneralPath p = new GeneralPath();
            p.moveTo(bounds.getX() + bounds.getWidth() / 2, bounds.getY());
            p.lineTo(bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight());
            p.lineTo(bounds.getX(), bounds.getY() + bounds.getHeight());
            p.closePath();

            g.setClip(p);
            g.setColor(0x0000ff);
            g.setAlpha(100);
            g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
            g.popClip();
        }
    }

    @Override
    protected String screenshotName() {
        return "graphics-clip";
    }
}
