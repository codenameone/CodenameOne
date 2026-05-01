package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawRoundRect extends AbstractGraphicsScreenshotTest {
    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0xffffff);
        for (int iter = 0 ; iter < bounds.getWidth() / 2 ; iter++) {
            nextColor(g);
            g.drawRoundRect(bounds.getX() + iter, bounds.getY() + iter, bounds.getX() + bounds.getWidth() - iter, bounds.getY() + bounds.getHeight() + iter, iter % 20, iter % 20);
        }
    }

    // Same rationale as FillRoundRect — heavy CG-rasterised round-rect
    // rendering competes with the slide-in transition; default 1500ms can
    // capture the screen mid-transition. See FillRoundRect.
    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        UITimer.timer(5000, false, parent, run);
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-round-rect";
    }
}
