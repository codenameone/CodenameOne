package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

// Repro for GH-3302. The 1x1 cell exercises the identity-transform alpha-mask
// path; the 2x1 / 1x2 / 2x2 cells exercise the non-identity branch under non-
// uniform scale. In each cell the triangle (path-space vertices (-20, 0),
// (0, -20), (20, 0)) should remain visually inscribed in the axis-aligned
// rectangle (drawn at -20, -20, 40x20). The legacy iOS alpha-mask path
// rasterised the shape at a uniform diagonal-ratio scale and then asked the
// GPU to stretch the resulting texture non-uniformly to recover the requested
// aspect, which left a sub-pixel drift between the triangle and the rectangle
// under non-uniform scale. The Metal path now factors per-axis (sx, sy) out of
// the transform before rasterisation so the GPU only applies the residual
// rotation/shear.
public class InscribedTriangleGrid extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        if (!g.isShapeSupported() || !g.isAffineSupported()) {
            g.drawString("Shape or affine unsupported", bounds.getX(), bounds.getY());
            return;
        }

        GeneralPath triangle = new GeneralPath();
        triangle.moveTo(-20, 0);
        triangle.lineTo(0, -20);
        triangle.lineTo(20, 0);
        triangle.closePath();

        Stroke pen = new Stroke(1f, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 1f);

        // Cell positions are at fixed (xp, yp) anchors before scale. On iOS the
        // framework Graphics composes g.translate as an integer accumulator
        // (isTranslationSupported() == false), so each cell's on-screen
        // position becomes (sx*xp, sy*yp). That's intentional -- the test is
        // checking the inscribed-shape property WITHIN each cell, not absolute
        // layout parity with Android/JavaSE.
        int originX = bounds.getX() + 30;
        int originY = bounds.getY() + 60;

        for (int sx = 1; sx <= 2; sx++) {
            for (int sy = 1; sy <= 2; sy++) {
                int xp = originX + (sx - 1) * 80 / sx;
                int yp = originY + (sy - 1) * 60 / sy;
                g.translate(xp, yp);
                g.scale(sx, sy);

                g.setColor(0x000000);
                g.drawRect(-20, -20, 40, 20);
                g.setColor(0xff0000);
                g.fillShape(triangle);
                g.setColor(0x0000ff);
                g.drawShape(triangle, pen);

                g.scale(1f / sx, 1f / sy);
                g.translate(-xp, -yp);
            }
        }
    }

    @Override
    protected String screenshotName() {
        return "graphics-inscribed-triangle-grid";
    }
}
