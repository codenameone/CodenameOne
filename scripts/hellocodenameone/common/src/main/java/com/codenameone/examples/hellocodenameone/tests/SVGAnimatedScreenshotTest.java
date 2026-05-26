package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.GeneratedSVGImage;
import com.codename1.ui.Label;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/**
 * Captures the SMIL-animated SVGs at a fixed frame offset so the screenshot
 * output is deterministic. Both images read their progress from
 * {@link AnimationTime#now()}, so pinning the global animation clock with
 * {@link AnimationTime#setTime(long)} freezes the spinner / pulse at a
 * predictable moment regardless of the device's wall-clock speed.
 *
 * <p>The two images are constructed against a pinned baseline so their
 * per-instance start timestamps are captured at the same {@code t = 0};
 * advancing the clock to {@code FRAME_OFFSET_MS} before the form paints
 * is what makes {@code progress()} return the desired fraction of the
 * first cycle.</p>
 */
public class SVGAnimatedScreenshotTest extends BaseTest {

    /** Frame offset chosen so each animation is partway through its first cycle. */
    private static final long FRAME_OFFSET_MS = 250L;

    @Override
    public boolean runTest() throws Exception {
        long pinned = 1_000_000L;
        try {
            // Pin the clock before constructing the images so their first paint
            // sees the start timestamp == pinned. The actual frame is captured
            // at pinned + FRAME_OFFSET_MS (set just before form.show()).
            AnimationTime.setTime(pinned);
            GeneratedSVGImage spinner = new com.codename1.generated.svg.SpinnerAnimated();
            GeneratedSVGImage pulse = new com.codename1.generated.svg.PulsingCircle();

            Form form = createForm("Animated SVG", BoxLayout.y(), "SVGAnimated");
            form.add(label("Spinner @ " + FRAME_OFFSET_MS + " ms", spinner));
            form.add(label("Pulse @ " + FRAME_OFFSET_MS + " ms", pulse));

            // Advance so progress() returns the desired fraction during paint.
            AnimationTime.setTime(pinned + FRAME_OFFSET_MS);
            form.show();
        } catch (RuntimeException ex) {
            AnimationTime.reset();
            throw ex;
        }
        // BaseTest.createForm registers an async screenshot callback that fires
        // ~1.5s after show. The clock stays pinned through that window; the
        // next test (or any production code) can call AnimationTime.reset()
        // when they need wall-clock semantics back.
        return true;
    }

    private Label label(String text, GeneratedSVGImage img) {
        Label l = new Label(text, img);
        Style s = l.getAllStyles();
        s.setMargin(8, 8, 8, 8);
        return l;
    }
}
