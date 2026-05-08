package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class Scale extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int x = bounds.getX();
        int y = bounds.getY();
        int w = bounds.getWidth();
        int h = bounds.getHeight();

        g.setColor(0xffffff);
        g.fillRect(x, y, w, h);
        g.setColor(0x000000);
        g.drawRect(x, y, w - 1, h - 1);

        if (!g.isAffineSupported()) {
            g.drawString("Affine unsupported", x + 4, y + 4);
            return;
        }

        // The earlier test built a transform via separate g.translate + g.scale
        // calls. On the JavaSE port g.translate(int, int) is a no-op (translate
        // is expected to be embedded in the native graphics) and on iOS the
        // form-graphics path doesn't compose g.scale with the cell offset
        // either, so the gradient fill landed off-cell. Build a single
        // Transform that combines translate + scale and apply it once.
        float xScale = w / 200f;
        float yScale = h / 200f;
        Transform t = Transform.makeIdentity();
        t.translate(x, y);
        t.scale(xScale, yScale);
        g.setTransform(t);

        // Top half of cell.
        g.fillLinearGradient(0xff0000, 0x0000ff, 0, 0, 200, 100, true);

        // Mirror X via scale(-1, 1) and draw the bottom half so the gradient
        // runs right-to-left.
        t.scale(-1, 1);
        g.setTransform(t);
        g.fillLinearGradient(0xff0000, 0x0000ff, -200, 100, 200, 100, true);

        g.setTransform(Transform.makeIdentity());
    }

    @Override
    protected String screenshotName() {
        return "graphics-scale";
    }
}
