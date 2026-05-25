package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.svg.GeneratedSVGImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/**
 * Captures the SMIL-animated SVGs at a fixed frame offset so the screenshot
 * output is deterministic. {@link GeneratedSVGImage#setAnimationTimeMillis}
 * pins the animation clock to a chosen elapsed time before the frame paint.
 */
public class SVGAnimatedScreenshotTest extends BaseTest {

    /** Frame offset chosen so each animation is partway through its first cycle. */
    private static final long FRAME_MS = 250L;

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm("Animated SVG", BoxLayout.y(), "SVGAnimated");
        form.add(label("Spinner @ 250 ms", new com.codename1.generated.svg.SpinnerAnimated()));
        form.add(label("Pulse @ 250 ms", new com.codename1.generated.svg.PulsingCircle()));
        form.show();
        return true;
    }

    private Label label(String text, GeneratedSVGImage img) {
        img.setAnimationTimeMillis(FRAME_MS);
        Label l = new Label(text, img);
        Style s = l.getAllStyles();
        s.setMargin(8, 8, 8, 8);
        return l;
    }
}
