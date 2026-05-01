package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawRoundRect extends AbstractGraphicsScreenshotTest {

    // Same rationale as FillRoundRect — skip the slide-and-fade transition
    // to avoid Metal's "doubled title" artefact under heavy CG-rasterised
    // round-rect rendering. See FillRoundRect.configureForm.
    @Override
    protected void configureForm(Form form) {
        form.setTransitionInAnimator(CommonTransitions.createEmpty());
        com.codename1.ui.Form prev = com.codename1.ui.Display.getInstance().getCurrent();
        if (prev != null) {
            prev.setTransitionOutAnimator(CommonTransitions.createEmpty());
        }
    }

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0xffffff);
        for (int iter = 0 ; iter < bounds.getWidth() / 2 ; iter++) {
            nextColor(g);
            g.drawRoundRect(bounds.getX() + iter, bounds.getY() + iter, bounds.getX() + bounds.getWidth() - iter, bounds.getY() + bounds.getHeight() + iter, iter % 20, iter % 20);
        }
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-round-rect";
    }
}
