package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

// Repro for GH-3302: a triangle inscribed in a rectangle should remain
// visually inscribed under non-uniform g.scale. Each cell paints:
//
//   - the rectangle (-20, -20, 40, 20) in solid BLACK ('frame'),
//   - the triangle (path-space (-20, 0), (0, -20), (20, 0)) filled in solid
//     GREEN ('inscribed fill'),
//   - the triangle outline in BLUE ('inscribed stroke').
//
// All four cells use the same path; only the matrix scale differs:
//
//   1x1 (top-left)     1x2 (top-right)
//   2x1 (bottom-left)  2x2 (bottom-right)
//
// On a correct port the green fill is exactly bounded by the black rectangle
// in every cell. The legacy iOS alpha-mask path rasterised the path at a
// uniform diagonal-ratio scale and then asked the GPU to stretch the
// resulting texture non-uniformly to recover the requested aspect, which
// drifted the inscribed shape off the axis-aligned drawRect. The Metal path
// now factors per-axis (sx, sy) out of the transform before rasterisation so
// the GPU only applies the residual rotation/shear.
//
// The test runs once on the form Graphics and once on a mutable-image
// Graphics, so the same rendering can be compared between the two backends
// the iOS port exposes for shape draws.
public class InscribedTriangleGrid extends AbstractGraphicsScreenshotTest {

    private static final int CELL_W = 80;
    private static final int CELL_H = 80;

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        // Fill a known background so the BLACK rectangle frame is visible on
        // every port (Android default form bg is dark, JavaSE/iOS lighter).
        g.setColor(0xeeeeee);
        g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());

        if (!g.isShapeSupported() || !g.isAffineSupported()) {
            g.setColor(0x000000);
            g.drawString("Shape or affine unsupported", bounds.getX() + 4, bounds.getY() + 4);
            return;
        }

        g.setColor(0x000000);
        g.drawString("Triangle should fit inside rectangle (sx,sy)", bounds.getX() + 4, bounds.getY() + 4);

        GeneralPath triangle = new GeneralPath();
        triangle.moveTo(-20, 0);
        triangle.lineTo(0, -20);
        triangle.lineTo(20, 0);
        triangle.closePath();

        Stroke pen = new Stroke(1f, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 1f);

        // Lay the four cells out in a 2x2 grid with enough gap that the 2x-
        // scaled cells (80x40 rect after scale) still fit. Each cell anchors
        // at (cellX, cellY) in the bounds; the matrix `g.scale(sx, sy)` is
        // what stresses the alpha-mask code path.
        int gridX = bounds.getX() + 30;
        int gridY = bounds.getY() + 40;
        for (int sxIdx = 0; sxIdx < 2; sxIdx++) {
            for (int syIdx = 0; syIdx < 2; syIdx++) {
                int sx = 1 + sxIdx;
                int sy = 1 + syIdx;
                int cellX = gridX + sxIdx * CELL_W + 30;
                int cellY = gridY + syIdx * CELL_H + 30;

                paintCell(g, cellX, cellY, sx, sy, triangle, pen);
            }
        }
    }

    private void paintCell(Graphics g, int cellX, int cellY, int sx, int sy,
                           GeneralPath triangle, Stroke pen) {
        // Label the cell with its (sx, sy) so the failure mode (e.g. drift
        // only at sx != sy) is identifiable from the screenshot alone.
        g.setColor(0x444444);
        g.drawString("(" + sx + "," + sy + ")", cellX - 30, cellY - 35);

        // translateMatrix composes T(cellX, cellY) onto the impl matrix --
        // it does NOT use the per-Graphics integer translate accumulator,
        // so a subsequent g.scale(sx, sy) doesn't multiply the cell anchor.
        // This makes the form-direct and mutable-image renderings produce
        // identical on-screen pixels (modulo the blit offset that places
        // the mutable image's content under the right component bounds).
        //
        // The translateMatrix API was added specifically to give CN1 apps
        // a way to express matrix-correct translate semantics across all
        // ports; see Graphics.translateMatrix javadoc for the rationale.
        // Ports that don't yet support it fall back to translate(int, int)
        // so the test still renders, just with the legacy column-position-
        // dependent layout.
        g.translateMatrix(cellX, cellY);
        g.scale(sx, sy);

        // Black rectangle frame -- the "ground truth" axis-aligned reference.
        g.setColor(0x000000);
        g.drawRect(-20, -20, 40, 20);

        // Green triangle fill -- exits the black frame iff the alpha-mask
        // texture stretch has drifted off the pixel grid.
        g.setColor(0x00aa00);
        g.fillShape(triangle);

        // Blue triangle outline -- same shape, drawShape path, exercises the
        // stroke widening code in nativeDrawShape too.
        g.setColor(0x0000aa);
        g.drawShape(triangle, pen);

        g.scale(1f / sx, 1f / sy);
        g.translateMatrix(-cellX, -cellY);
    }

    @Override
    protected String screenshotName() {
        return "graphics-inscribed-triangle-grid";
    }
}
