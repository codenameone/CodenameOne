package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.GradientDescriptor;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

/// Exercises the multi-stop / angled / conic gradient primitives added to
/// {@link Graphics} - the underlying API that backs CSS `linear-gradient`,
/// `radial-gradient`, `conic-gradient`, and `repeating-*-gradient`. The
/// AbstractGraphicsScreenshotTest harness paints the same drawContent four
/// times (anti-alias on/off x direct/buffered) so per-port rasterization
/// differences in the stop interpolation, angle math, and shader matrices
/// surface as a pixel diff against the baseline.
public class DrawGradientStops extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int colCount = 3;
        int rowCount = 2;
        int cellW = bounds.getWidth() / colCount;
        int cellH = bounds.getHeight() / rowCount;
        int[] tri = {
                0xffff0080,  // pink
                0xffff8c00,  // orange
                0xff40e0d0   // teal
        };
        float[] triStops = {0f, 0.5f, 1f};

        // Row 0, col 0: angled multi-stop linear gradient at 45deg
        g.fillLinearGradientWithStops(tri, triStops,
                bounds.getX(), bounds.getY(), cellW, cellH,
                45f, GradientDescriptor.CYCLE_NONE);

        // Row 0, col 1: same stops, 135deg, REFLECT cycling fills both ways
        g.fillLinearGradientWithStops(tri, triStops,
                bounds.getX() + cellW, bounds.getY(), cellW, cellH,
                135f, GradientDescriptor.CYCLE_REFLECT);

        // Row 0, col 2: repeating linear gradient (tight stripe pattern)
        int[] stripeColors = {0xffeeeeee, 0xffeeeeee, 0xffcc3333, 0xffcc3333};
        float[] stripeStops = {0f, 0.5f, 0.5f, 1f};
        g.fillLinearGradientWithStops(stripeColors, stripeStops,
                bounds.getX() + 2 * cellW, bounds.getY(), cellW, cellH,
                45f, GradientDescriptor.CYCLE_REPEAT);

        // Row 1, col 0: multi-stop radial (circular), centered
        g.fillRadialGradientWithStops(tri, triStops,
                bounds.getX(), bounds.getY() + cellH, cellW, cellH,
                cellW * 0.5f, cellH * 0.5f,
                cellW * 0.5f, cellH * 0.5f,
                GradientDescriptor.CYCLE_NONE);

        // Row 1, col 1: elliptical radial offset to the upper-left
        g.fillRadialGradientWithStops(tri, triStops,
                bounds.getX() + cellW, bounds.getY() + cellH, cellW, cellH,
                cellW * 0.3f, cellH * 0.3f,
                cellW * 0.7f, cellH * 0.5f,
                GradientDescriptor.CYCLE_NONE);

        // Row 1, col 2: conic gradient (rainbow sweep) from 0deg at center
        int[] rainbow = {
                0xffff0000, 0xffffff00, 0xff00ff00,
                0xff00ffff, 0xff0000ff, 0xffff00ff,
                0xffff0000
        };
        float[] rainbowStops = {0f, 1f / 6f, 2f / 6f, 3f / 6f, 4f / 6f, 5f / 6f, 1f};
        g.fillConicGradient(rainbow, rainbowStops,
                bounds.getX() + 2 * cellW, bounds.getY() + cellH, cellW, cellH,
                cellW * 0.5f, cellH * 0.5f,
                0f);
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-gradient-stops";
    }
}
