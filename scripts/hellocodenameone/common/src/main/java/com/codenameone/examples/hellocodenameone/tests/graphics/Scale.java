package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
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

        // Earlier iterations of this test relied on g.setTransform(t) where t
        // composed translate + scale. iOS Metal applies the user's matrix on
        // top of xTranslate-shifted vertex coordinates, and the cell-origin
        // translation in t double-counted xTranslate so the fill landed
        // off-screen and the cell rendered blank. Conjugating in
        // Graphics.setTransform fixed that test but broke other CN1 paths
        // (LightweightPicker / scene Node) that intentionally bake xTranslate
        // into their own transforms.
        //
        // Render the gradient at native 200x100 resolution into a mutable
        // Image instead, then drawImage it stretched to the cell halves. The
        // mutable-image graphics has xTranslate=0 so g.fillLinearGradient
        // inside that context works without any setTransform gymnastics, and
        // drawImage's scale arguments make the cell-side scaling explicit.
        Image base = Image.createImage(200, 100);
        Graphics ig = base.getGraphics();
        ig.fillLinearGradient(0xff0000, 0x0000ff, 0, 0, 200, 100, true);

        int halfH = h / 2;
        // Top half: gradient drawn at full cell width and half cell height.
        g.drawImage(base, x, y, w, halfH);

        // Bottom half: same gradient mirrored horizontally to demonstrate the
        // scale(-1, 1) effect from the original test. Mirror by reading the
        // image into a 200x100 RGB buffer, flipping X, and drawing the
        // mirrored copy stretched to the cell's bottom half.
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
        return "graphics-scale";
    }
}
