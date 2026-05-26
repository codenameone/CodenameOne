package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.util.Resources;

/// Animated SVG end-to-end test. Like {@link SVGStaticScreenshotTest} this
/// uses only the developer-facing APIs:
///
/// 1. SVGs land in `src/main/css/` and are referenced from `theme.css` --
///    no Java-side hardcoding.
/// 2. The build emits a `SVGRegistry`; we call `install(globalResources)`
///    during {@link #prepare} so the theme's placeholder entries are
///    replaced with the transcoded SVGs.
/// 3. {@link Resources#getImage(String)} returns the animated SVG, which is
///    painted into each cell of the standard
///    {@link AbstractAnimationScreenshotTest} grid. Because both SVGs read
///    from {@code AnimationTime.now()}, the base class's per-frame clock
///    pinning produces a deterministic capture.
public class SVGAnimatedScreenshotTest extends AbstractAnimationScreenshotTest {

    private static final int ANIM_DURATION_MS = 1000;

    private Image spinner;
    private Image pulse;

    @Override
    protected int getAnimationDurationMillis() {
        return ANIM_DURATION_MS;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        Resources res = SVGStaticScreenshotTest.resolveGlobalResources();
        spinner = res == null ? null : res.getImage("spinner_animated.svg");
        pulse = res == null ? null : res.getImage("pulsing_circle.svg");
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height,
                               double progress, int frameIndex) {
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, width, height);

        if (spinner == null || pulse == null) {
            g.setColor(0xFF0000);
            g.drawString("SVGRegistry not installed", 10, 20);
            return;
        }

        int half = width / 2;
        Image scaledSpinner = spinner.scaled(half, height);
        Image scaledPulse = pulse.scaled(width - half, height);
        g.drawImage(scaledSpinner, 0, 0);
        g.drawImage(scaledPulse, half, 0);
    }
}
