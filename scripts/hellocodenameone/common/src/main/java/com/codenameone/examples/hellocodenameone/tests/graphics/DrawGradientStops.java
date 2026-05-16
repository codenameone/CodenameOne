package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.ConicGradient;
import com.codename1.ui.Gradient;
import com.codename1.ui.Graphics;
import com.codename1.ui.LinearGradient;
import com.codename1.ui.RadialGradient;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

/// Exercises the multi-stop / angled / conic gradient primitives added to
/// {@link Graphics} - the underlying API that backs CSS `linear-gradient`,
/// `radial-gradient`, `conic-gradient`, and `repeating-*-gradient`. The
/// AbstractGraphicsScreenshotTest harness paints the same drawContent four
/// times (anti-alias on/off x direct/buffered) so per-port rasterization
/// differences surface as a pixel diff against the baseline.
public class DrawGradientStops extends AbstractGraphicsScreenshotTest {

    private static final int[] TRI = {
            0xffff0080,  // pink
            0xffff8c00,  // orange
            0xff40e0d0   // teal
    };
    private static final float[] TRI_STOPS = {0f, 0.5f, 1f};

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int colCount = 3;
        int rowCount = 2;
        int cellW = bounds.getWidth() / colCount;
        int cellH = bounds.getHeight() / rowCount;
        int x0 = bounds.getX();
        int y0 = bounds.getY();

        // Row 0, col 0: angled multi-stop linear at 45 deg
        g.fillGradient(new LinearGradient(45f, TRI, TRI_STOPS),
                x0, y0, cellW, cellH);

        // Row 0, col 1: 135 deg with REFLECT cycle method
        LinearGradient reflected = new LinearGradient(135f, TRI, TRI_STOPS);
        reflected.setCycleMethod(Gradient.CYCLE_REFLECT);
        g.fillGradient(reflected, x0 + cellW, y0, cellW, cellH);

        // Row 0, col 2: repeating linear (tight stripes)
        int[] stripeColors = {0xffeeeeee, 0xffeeeeee, 0xffcc3333, 0xffcc3333};
        float[] stripeStops = {0f, 0.5f, 0.5f, 1f};
        LinearGradient stripes = new LinearGradient(45f, stripeColors, stripeStops);
        stripes.setCycleMethod(Gradient.CYCLE_REPEAT);
        g.fillGradient(stripes, x0 + 2 * cellW, y0, cellW, cellH);

        // Row 1, col 0: multi-stop radial (circular), centered
        RadialGradient circle = new RadialGradient(TRI, TRI_STOPS);
        circle.setShape(RadialGradient.SHAPE_CIRCLE)
              .setExtent(RadialGradient.EXTENT_FARTHEST_CORNER);
        g.fillGradient(circle, x0, y0 + cellH, cellW, cellH);

        // Row 1, col 1: elliptical radial offset to upper-left
        RadialGradient ellipse = new RadialGradient(TRI, TRI_STOPS);
        ellipse.setShape(RadialGradient.SHAPE_ELLIPSE)
               .setExtent(RadialGradient.EXTENT_EXPLICIT)
               .setRelativeCenterX(0.3f).setRelativeCenterY(0.3f)
               .setRelativeRadiusX(0.7f).setRelativeRadiusY(0.5f);
        g.fillGradient(ellipse, x0 + cellW, y0 + cellH, cellW, cellH);

        // Row 1, col 2: conic gradient (rainbow sweep) from 0 deg at center
        int[] rainbow = {
                0xffff0000, 0xffffff00, 0xff00ff00,
                0xff00ffff, 0xff0000ff, 0xffff00ff,
                0xffff0000
        };
        float[] rainbowStops = {0f, 1f / 6f, 2f / 6f, 3f / 6f, 4f / 6f, 5f / 6f, 1f};
        g.fillGradient(new ConicGradient(rainbow, rainbowStops),
                x0 + 2 * cellW, y0 + cellH, cellW, cellH);
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-gradient-stops";
    }
}
