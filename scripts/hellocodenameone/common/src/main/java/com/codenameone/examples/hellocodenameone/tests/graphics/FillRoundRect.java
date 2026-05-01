package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class FillRoundRect extends AbstractGraphicsScreenshotTest {
    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0xffffff);
        for (int iter = 0 ; iter < bounds.getWidth() / 2 ; iter++) {
            nextColor(g);
            g.fillRoundRect(bounds.getX() + iter, bounds.getY() + iter, bounds.getX() + bounds.getWidth() - iter, bounds.getY() + bounds.getHeight() + iter, iter % 20, iter % 20);
        }
    }

    // Round-rect rendering rasterises ~1200 CG bitmaps in this test (4 panels
    // x ~300 iterations). On the iOS Metal port the rendering completes after
    // the slide-in transition has begun but before the screen is fully
    // settled, so the 1500ms default in BaseTest captures the title still
    // mid-transition (previous form's title still partially visible).
    // Bumping to 3000ms gives the slide animation enough headroom on slower
    // CI runners.
    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        UITimer.timer(3000, false, parent, run);
    }

    @Override
    protected String screenshotName() {
        return "graphics-fill-round-rect";
    }
}
