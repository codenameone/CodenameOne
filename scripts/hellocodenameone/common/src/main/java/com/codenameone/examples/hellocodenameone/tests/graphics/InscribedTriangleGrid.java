package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

// Repro for GH-3302: a triangle inscribed in a rectangle should remain
// visually inscribed under non-uniform g.scale. Each cell paints:
//
//   - the rectangle (-BASE_HALF_W, -BASE_H, 2*BASE_HALF_W, BASE_H) in BLACK,
//   - the triangle ((-BASE_HALF_W, 0), (0, -BASE_H), (BASE_HALF_W, 0)) filled
//     in GREEN ('inscribed fill'),
//   - the triangle outline in BLUE ('inscribed stroke').
//
// The cells form a 2x2 grid:
//   1x1 (top-left)     1x2 (top-right)
//   2x1 (bottom-left)  2x2 (bottom-right)
//
// On a correct port the green fill stays exactly within the black rectangle
// in every cell. The legacy iOS alpha-mask path rasterised the path at a
// uniform diagonal-ratio scale and stretched the resulting texture non-
// uniformly, drifting the inscribed shape off the axis-aligned drawRect.
// The Metal path now factors per-axis (sx, sy) out of the transform before
// rasterisation so the GPU only applies the residual rotation/shear.
//
// Cell layout is computed from `bounds` so the grid fits inside small
// simulator panels (Android CI runs at 320x640 -> 160x320 per cell, which
// rules out fixed pixel offsets). Cell anchors come from
// `g.translateMatrix(...)` so the same code produces the same on-screen
// positions whether the underlying Graphics is the form's GlobalGraphics or
// a mutable Image's NativeGraphics (which would otherwise diverge because
// `g.translate(int, int)` is a per-Graphics integer accumulator that gets
// multiplied by subsequent g.scale calls -- see Graphics.translateMatrix
// javadoc).
public class InscribedTriangleGrid extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0xeeeeee);
        g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());

        if (!g.isShapeSupported() || !g.isAffineSupported()) {
            g.setColor(0x000000);
            g.drawString("Shape or affine unsupported", bounds.getX() + 4, bounds.getY() + 4);
            return;
        }

        int panelW = bounds.getWidth();
        int panelH = bounds.getHeight();

        // Fit the 2x2 grid into the panel. The widest cell is 2x scaled so it
        // needs 2 * baseHalfWidth on each side; tallest is 2x so it needs
        // 2 * baseHeight. Reserve ~10% of the panel for margins between cells.
        int headerHeight = Math.max(12, panelH / 20);
        int gridW = panelW;
        int gridH = panelH - headerHeight;
        // Two cell columns; the right column has sx=2 so its width is double
        // the base. Total horizontal demand = baseW * (1 + 2) plus three
        // gutters. Solve baseW * 3 + gutters * 3 == gridW with gutters == baseW / 2.
        int baseW = Math.max(8, gridW * 2 / 9);            // == gridW / 4.5
        // Two cell rows; bottom row has sy=2. Same arithmetic on height.
        int baseH = Math.max(6, gridH * 2 / 9);
        int baseHalfW = baseW / 2;

        int cellGutterX = baseHalfW;
        int cellGutterY = baseH / 2;
        // Column centres: column 0 sits at baseHalfW + gutter; column 1 sits
        // far enough right that the 2x-scaled cell still fits (baseW from
        // centre).
        int col0 = bounds.getX() + cellGutterX + baseHalfW;
        int col1 = col0 + baseHalfW + cellGutterX + baseW;
        // Row centres: row 0 baseline at baseH + gutter; row 1 baseline far
        // enough down that the 2x-scaled cell still fits.
        int row0 = bounds.getY() + headerHeight + cellGutterY + baseH;
        int row1 = row0 + cellGutterY + baseH * 2;

        g.setColor(0x000000);
        g.drawString("Triangle inscribed in rect (sx,sy)", bounds.getX() + 4, bounds.getY() + 2);

        GeneralPath triangle = new GeneralPath();
        triangle.moveTo(-baseHalfW, 0);
        triangle.lineTo(0, -baseH);
        triangle.lineTo(baseHalfW, 0);
        triangle.closePath();

        Stroke pen = new Stroke(1f, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 1f);

        int[][] cells = new int[][]{
                {col0, row0, 1, 1},
                {col1, row0, 2, 1},
                {col0, row1, 1, 2},
                {col1, row1, 2, 2}
        };
        for (int[] cell : cells) {
            paintCell(g, cell[0], cell[1], cell[2], cell[3], baseHalfW, baseH, triangle, pen);
        }
    }

    private void paintCell(Graphics g, int cellX, int cellY, int sx, int sy,
                           int baseHalfW, int baseH, GeneralPath triangle, Stroke pen) {
        // translateMatrix composes T(cellX, cellY) onto the impl matrix --
        // it does NOT use the per-Graphics integer translate accumulator,
        // so a subsequent g.scale(sx, sy) doesn't multiply the cell anchor.
        // This makes the form-direct and mutable-image renderings produce
        // identical on-screen pixels (modulo the blit offset that places
        // the mutable image's content under the right component bounds).
        g.translateMatrix(cellX, cellY);
        g.scale(sx, sy);

        // Black rectangle frame -- the "ground truth" axis-aligned reference.
        g.setColor(0x000000);
        g.drawRect(-baseHalfW, -baseH, baseHalfW * 2, baseH);

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
