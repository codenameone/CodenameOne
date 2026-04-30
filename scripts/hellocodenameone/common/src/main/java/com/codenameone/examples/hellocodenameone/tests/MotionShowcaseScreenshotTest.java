package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Graphics;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.animations.Motion;

/// Visualises the Motion curves available in the framework. Each frame shows a
/// dot for every motion type at the same animation time; together the six
/// frames make the relative pacing of each curve obvious.
public class MotionShowcaseScreenshotTest extends AbstractAnimationScreenshotTest {
    private static final int[] LINE_COLORS = {
            0xef476f, 0xffd166, 0x06d6a0, 0x118ab2, 0x073b4c, 0x8338ec, 0xfb5607
    };
    private static final String[] LABELS = {
            "linear",
            "easeIn",
            "easeOut",
            "easeInOut",
            "spline",
            "decel",
            "cubic"
    };

    private Motion[] motions;

    @Override
    protected int getAnimationDurationMillis() {
        return 1000;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        int duration = getAnimationDurationMillis();
        motions = new Motion[]{
                Motion.createLinearMotion(0, 1000, duration),
                Motion.createEaseInMotion(0, 1000, duration),
                Motion.createEaseOutMotion(0, 1000, duration),
                Motion.createEaseInOutMotion(0, 1000, duration),
                Motion.createSplineMotion(0, 1000, duration),
                Motion.createDecelerationMotion(0, 1000, duration),
                Motion.createCubicBezierMotion(0, 1000, duration, 0.42f, 0f, 0.58f, 1f),
        };
        for (Motion m : motions) {
            m.start();
        }
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        g.setColor(0xffffff);
        g.fillRect(0, 0, width, height);
        int rowHeight = height / motions.length;
        if (rowHeight < 16) {
            rowHeight = 16;
        }
        int trackPadding = Math.max(8, width / 32);
        int trackX = trackPadding;
        int trackW = Math.max(1, width - 2 * trackPadding);
        for (int i = 0; i < motions.length; i++) {
            int trackY = i * rowHeight + rowHeight / 2;
            g.setColor(0xeeeeee);
            g.fillRect(trackX, trackY - 1, trackW, 3);
            int value = motions[i].getValue();
            int dotX = trackX + (int) ((long) value * (long) trackW / 1000L);
            g.setColor(LINE_COLORS[i % LINE_COLORS.length]);
            int dotR = Math.max(4, rowHeight / 4);
            g.fillRect(dotX - dotR, trackY - dotR, dotR * 2, dotR * 2);
            g.setColor(0x222222);
            g.drawString(LABELS[i % LABELS.length], trackPadding, i * rowHeight + 2);
        }
        long elapsed = AnimationTime.now() - getAnimationStartTime();
        g.setColor(0x222222);
        g.drawString("t=" + elapsed + "ms", trackPadding, height - 18);
    }

    @Override
    protected void finishCapture() {
        motions = null;
        super.finishCapture();
    }
}
