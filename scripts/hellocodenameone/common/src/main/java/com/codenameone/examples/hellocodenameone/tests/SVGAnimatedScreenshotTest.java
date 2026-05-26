package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.generated.svg.PulsingCircle;
import com.codename1.generated.svg.SpinnerAnimated;
import com.codename1.ui.GeneratedSVGImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

/// Captures the SMIL-animated SVGs across six frames of their first
/// animation cycle so a single screenshot proves the animation is actually
/// running, not just rendering a static first frame.
///
/// Delegates the framing logic to [AbstractAnimationScreenshotTest], which
/// pins [com.codename1.ui.animations.AnimationTime] to deterministic values
/// per frame. Because [GeneratedSVGImage] reads its animation offset from
/// `AnimationTime.now()`, every frame in the grid lands on a different
/// rotation / radius regardless of how slow the test runner is.
public class SVGAnimatedScreenshotTest extends AbstractAnimationScreenshotTest {

    private static final int ANIM_DURATION_MS = 1000;

    private GeneratedSVGImage spinner;
    private GeneratedSVGImage pulse;

    @Override
    protected int getAnimationDurationMillis() {
        // Both SVGs declare dur="1s", so spread the six frames across one cycle.
        return ANIM_DURATION_MS;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        // Sets the AnimationTime clock to the per-test base; the SVG images'
        // first paint (in the next call to renderFrame) records this base as
        // their t=0 reference.
        super.prepareCapture(frameWidth, frameHeight);
        spinner = new SpinnerAnimated();
        pulse = new PulsingCircle();
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height,
                               double progress, int frameIndex) {
        // White backdrop so the spinner / pulse silhouettes are unambiguous.
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, width, height);

        int half = width / 2;
        // Stretch each SVG into its half of the frame using the standard
        // drawImage(image, x, y, w, h) path -- this routes through
        // GeneratedSVGImage.drawImage which reads AnimationTime to pick a
        // rotation / radius for this specific frame.
        Image scaledSpinner = spinner.scaled(half, height);
        Image scaledPulse = pulse.scaled(width - half, height);
        g.drawImage(scaledSpinner, 0, 0);
        g.drawImage(scaledPulse, half, 0);
    }
}
