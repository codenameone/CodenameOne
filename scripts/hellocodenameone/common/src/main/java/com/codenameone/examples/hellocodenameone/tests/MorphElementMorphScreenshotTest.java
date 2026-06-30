package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.animations.MorphTransition;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;

/// Exercises the {@link MorphTransition} arbitrary-element path together with
/// the opacity / rotation / scale tweens and scrubbing.
///
/// A pre-rendered badge image (an opaque blue disc with a white "play"
/// triangle, asymmetric so rotation is unmistakable -- exactly what a
/// rasterized SVG would look like) is morphed from a small tile in the
/// bottom-left of the form to a large tile in the upper-middle while it fades
/// slightly, rotates a quarter turn and overshoots in scale. The element is
/// not a named child of either form -- it is drawn as an overlay at the
/// interpolated transform, which is the whole point of
/// {@link MorphTransition#morph(MorphTransition.MorphElement)}.
///
/// Every frame (including the bookends) is driven through
/// `MorphTransition#setProgress(double)` so the grid is a deterministic,
/// order-independent sweep of the scrub API rather than a clock-driven run.
public class MorphElementMorphScreenshotTest extends AbstractTransitionScreenshotTest {

    private int frameW;
    private int frameH;
    private MorphTransition.MorphElement element;

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        // Captured BEFORE super, which calls createTransition() where they're read.
        this.frameW = frameWidth;
        this.frameH = frameHeight;
        super.prepareCapture(frameWidth, frameHeight);
    }

    @Override
    protected Transition createTransition(int duration) {
        Image badge = buildBadge(Math.max(24, frameW / 5));
        int smallW = Math.max(24, frameW / 5);
        int smallH = smallW;
        int largeW = Math.max(smallW + 1, frameW / 2);
        int largeH = largeW;
        element = MorphTransition.MorphElement.create(badge)
                .from(frameW / 12, frameH - smallH - frameH / 12, smallW, smallH)
                .to((frameW - largeW) / 2, frameH / 8, largeW, largeH)
                .opacity(1f, 0.65f)
                .rotation(0f, 90f)
                .scale(1f, 1.15f);
        return MorphTransition.create(duration).morph(element);
    }

    @Override
    protected boolean paintBookendDirectly(int frameIndex) {
        // Render the whole sweep through the transition so frame 0 / frame 5
        // show the element at its start / end transform too.
        return false;
    }

    @Override
    protected void advanceTransition(double progress) {
        ((MorphTransition) getTransition()).setProgress(progress);
    }

    @Override
    protected void buildSourceForm(Form form) {
        styleBackground(form, 0x10243f);
    }

    @Override
    protected void buildDestForm(Form form) {
        styleBackground(form, 0x3f1024);
    }

    private static void styleBackground(Form form, int bgColor) {
        form.setLayout(new BorderLayout());
        Style cps = form.getContentPane().getAllStyles();
        cps.setBgTransparency(255);
        cps.setBgColor(bgColor);
        cps.setFgColor(0xffffff);
    }

    /// Renders the badge once into a mutable image. Drawing an asymmetric shape
    /// (the play triangle) makes the rotation visible across frames.
    private static Image buildBadge(int size) {
        int s = Math.max(16, size);
        Image img = Image.createImage(s, s, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor(0x2563eb);
        g.fillArc(0, 0, s - 1, s - 1, 0, 360);
        g.setColor(0xffffff);
        int left = Math.round(s * 0.34f);
        int right = Math.round(s * 0.72f);
        int top = Math.round(s * 0.28f);
        int bottom = Math.round(s * 0.72f);
        int mid = s / 2;
        int[] xs = {left, left, right};
        int[] ys = {top, bottom, mid};
        g.fillPolygon(xs, ys, 3);
        return img;
    }
}
