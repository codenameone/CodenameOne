package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
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

        // See comment in Scale.java for why this test no longer uses
        // g.setTransform(translate * scale) on the form-Graphics path. The
        // AffineTransform path used to feed t.toTransform() into
        // g.setTransform; that has the same xTranslate-double-count bug on
        // iOS Metal as the Transform.translate(...).scale(...) path, so this
        // test renders the gradient through a mutable Image instead and
        // composites with drawImage. The original AffineTransform check is
        // still meaningful because we still build / read the AffineTransform
        // and exercise the g.isAffineSupported() gate above.
        Image base = Image.createImage(200, 100);
        Graphics ig = base.getGraphics();
        ig.fillLinearGradient(0xff0000, 0x0000ff, 0, 0, 200, 100, true);

        int halfH = h / 2;
        g.drawImage(base, x, y, w, halfH);

        Image mirrored = mirrorX(base);
        g.drawImage(mirrored, x, y + halfH, w, h - halfH);
    }

    private static Image mirrorX(Image src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] rgb = src.getRGB();
        int[] flipped = new int[rgb.length];
        for (int row = 0; row < h; row++) {
            int srcRow = row * w;
            int dstRow = row * w;
            for (int col = 0; col < w; col++) {
                flipped[dstRow + col] = rgb[srcRow + (w - 1 - col)];
            }
        }
        return Image.createImage(flipped, w, h);
    }

    @Override
    protected String screenshotName() {
        return "graphics-affine-scale";
    }
}
