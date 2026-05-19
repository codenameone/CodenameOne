package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

// Repro for the GH-3302 follow-up: in direct-to-screen mode the framework's
// container/component painting chain stacks g.translate(absX, absY) calls
// into the per-Graphics integer accumulator before user paint() runs. A
// user g.translate(...) + g.scale(...) inside paint then scales the
// component's chrome offset (status bar + title-area height), pushing the
// drawing off the Y axis. Buffered (mutable-image) drawing skips this
// because the offscreen graphics starts with xTranslate=0.
//
// Critically this test uses g.translate(int, int) -- the legacy API -- NOT
// g.translateMatrix. With Graphics.useMatrixTranslation off, the
// form-direct panels and the mutable-image panels diverge: the form-direct
// inner squares land outside their reference frames. With the flag on,
// every g.translate composes onto the impl matrix and the four panels
// produce identical pixels (modulo the blit offset).
//
// Each cell paints a black reference rect of size 2W x 2H centred on
// (cellX, cellY). Then g.translate(cellX, cellY) + g.scale(2, 2), and a
// red inner rect of size W x H centred at (0, 0). On a correct port the
// red rect inscribes exactly in the black frame in every cell.
public class TranslateThenScale extends AbstractGraphicsScreenshotTest {

    @Override
    public boolean shouldTakeScreenshot() {
        // This test exists specifically to verify matrix-translation mode
        // (Graphics.useMatrixTranslation). Under legacy mode the form-direct
        // panels render the bug it documents -- there is no useful golden
        // to compare against. Skip the screenshot capture in legacy mode so
        // the CI legacy-mode pass doesn't flag a missing reference for a
        // test that's intentionally matrix-mode-only.
        return Graphics.useMatrixTranslation;
    }

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

        // 2x2 grid of cells, each cell hosts a scale(2,2) drawing. Reserve
        // 1/9 of the panel for margins so the 2x-scaled rect doesn't bleed
        // outside cell bounds on small Android CI panels (160x320).
        int headerH = Math.max(12, panelH / 20);
        int gridW = panelW;
        int gridH = panelH - headerH;
        int baseHalfW = Math.max(6, gridW / 12);
        int baseHalfH = Math.max(5, gridH / 12);

        int col0 = bounds.getX() + 2 * baseHalfW;
        int col1 = bounds.getX() + gridW - 2 * baseHalfW;
        int row0 = bounds.getY() + headerH + 2 * baseHalfH;
        int row1 = bounds.getY() + gridH - 2 * baseHalfH + headerH;

        g.setColor(0x000000);
        g.drawString("translate+scale (direct vs buffered)", bounds.getX() + 4, bounds.getY() + 2);

        GeneralPath diamond = new GeneralPath();
        diamond.moveTo(0, -baseHalfH);
        diamond.lineTo(baseHalfW, 0);
        diamond.lineTo(0, baseHalfH);
        diamond.lineTo(-baseHalfW, 0);
        diamond.closePath();

        Stroke pen = new Stroke(1f, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 1f);

        paintCell(g, col0, row0, baseHalfW, baseHalfH, diamond, pen);
        paintCell(g, col1, row0, baseHalfW, baseHalfH, diamond, pen);
        paintCell(g, col0, row1, baseHalfW, baseHalfH, diamond, pen);
        paintCell(g, col1, row1, baseHalfW, baseHalfH, diamond, pen);
    }

    private void paintCell(Graphics g, int cellX, int cellY,
                           int baseHalfW, int baseHalfH,
                           GeneralPath diamond, Stroke pen) {
        // Black 2x-sized reference frame -- not transformed, drawn at
        // absolute panel coords. The inner red drawing must inscribe in it
        // after translate + scale(2x).
        g.setColor(0x000000);
        g.drawRect(cellX - baseHalfW * 2, cellY - baseHalfH * 2,
                baseHalfW * 4, baseHalfH * 4);

        // Legacy translate API on purpose: under useMatrixTranslation==true
        // this composes onto the impl matrix and the subsequent scale does
        // NOT multiply the chrome-offset that the framework already pushed
        // into the per-Graphics integer accumulator. Under the flag off,
        // the direct-to-screen panels drift.
        g.translate(cellX, cellY);
        g.scale(2f, 2f);

        // Red inner half-size rect -- if scale only widens local-coord
        // drawing, this lands inside the black frame exactly.
        g.setColor(0xaa0000);
        g.drawRect(-baseHalfW, -baseHalfH, baseHalfW * 2, baseHalfH * 2);

        // Blue diamond fill + green diamond stroke exercise fillShape and
        // drawShape on the same transform stack.
        g.setColor(0x0000aa);
        g.fillShape(diamond);
        g.setColor(0x00aa00);
        g.drawShape(diamond, pen);

        g.scale(0.5f, 0.5f);
        g.translate(-cellX, -cellY);
    }

    @Override
    protected String screenshotName() {
        return "graphics-translate-then-scale";
    }
}
