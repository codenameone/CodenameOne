package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class FillRoundRect extends AbstractGraphicsScreenshotTest {

    // Disable the slide-and-fade form transition for this test. On the iOS
    // Metal port the transition's cross-fading title paints (source + dest
    // titles painted across multiple frames into the persistent screenTexture)
    // leave residual pixels in the title bar after the heavy ~1200-CG-bitmap
    // round-rect rasterisation completes, producing a "doubled title" artefact
    // that doesn't reproduce on GL or Java2D. The visible content of these
    // tests doesn't depend on the transition; skipping it sidesteps the issue.
    @Override
    protected void configureForm(Form form) {
        form.setTransitionInAnimator(CommonTransitions.createEmpty());
        // Also disable the OUTGOING transition on whatever form is currently
        // showing so the previous test's title doesn't cross-fade into ours
        // — the doubled-title artefact is driven by both halves of the
        // slide-and-fade-title transition (source fading out, dest fading in)
        // accumulating into the persistent screenTexture, not just one.
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
            g.fillRoundRect(bounds.getX() + iter, bounds.getY() + iter, bounds.getX() + bounds.getWidth() - iter, bounds.getY() + bounds.getHeight() + iter, iter % 20, iter % 20);
        }
    }

    @Override
    protected String screenshotName() {
        return "graphics-fill-round-rect";
    }
}
