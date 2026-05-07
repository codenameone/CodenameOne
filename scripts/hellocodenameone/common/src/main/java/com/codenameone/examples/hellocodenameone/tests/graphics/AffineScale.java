package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.AffineTransform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class AffineScale extends AbstractGraphicsScreenshotTest {

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

        // Same fix as Scale.java: the earlier formula crossed the axes so the
        // fill clipped to a thin strip on portrait screens.
        float xScale = w / 200f;
        float yScale = h / 200f;

        // AffineTransform with matrix [xScale 0 x ; 0 yScale y] -- equivalent
        // to translate(x, y) then scale(xScale, yScale).
        AffineTransform affine = new AffineTransform(
                xScale, 0f,
                0f, yScale,
                (float) x, (float) y);
        Transform transform = affine.toTransform();
        g.setTransform(transform);

        // Top half of cell.
        g.fillLinearGradient(0xff0000, 0x0000ff, 0, 0, 200, 100, true);

        // Mirror X via Transform.scale (composition) and draw the bottom half
        // so the gradient runs right-to-left.
        transform.scale(-1, 1);
        g.setTransform(transform);
        g.fillLinearGradient(0xff0000, 0x0000ff, -200, 100, 200, 100, true);

        g.resetAffine();
    }

    @Override
    protected String screenshotName() {
        return "graphics-affine-scale";
    }
}
