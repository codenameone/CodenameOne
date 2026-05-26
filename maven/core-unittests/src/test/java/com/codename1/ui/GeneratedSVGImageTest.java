package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.geom.GeneralPath;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CI-wired coverage of the {@link GeneratedSVGImage} runtime that the
 * build-time SVG transcoder targets. Exercises:
 *
 * <ul>
 *   <li>DPI-aware default sizing -- the constructor scales the SVG-declared
 *       width/height by the device density relative to DENSITY_MEDIUM.</li>
 *   <li>{@link GeneratedSVGImage#scaled(int, int)} -- returns a view whose
 *       {@code getWidth}/{@code getHeight} report the caller-supplied
 *       dimensions so layout sees the right size.</li>
 *   <li>{@link AnimationTime}-driven animation time -- with the clock pinned
 *       the per-instance start timestamp is deterministic and {@code paintSVG}
 *       observes a known elapsed offset.</li>
 *   <li>The static SMIL helpers ({@code progress}, {@code lerp},
 *       {@code lerpColor}, {@code lerpValues}, {@code svgArc}) consumed by
 *       generated code.</li>
 * </ul>
 */
public class GeneratedSVGImageTest extends UITestBase {

    @AfterEach
    void releaseAnimationClock() {
        AnimationTime.reset();
    }

    @FormTest
    public void intrinsicSizeIsRecorded() {
        RecordingSVG img = new RecordingSVG(24, 24);
        assertEquals(24, img.getIntrinsicWidth(),
                "constructor preserves the SVG-declared width as intrinsic");
        assertEquals(24, img.getIntrinsicHeight(),
                "constructor preserves the SVG-declared height as intrinsic");
    }

    @FormTest
    public void defaultSizeScalesWithDensity() {
        // At DENSITY_MEDIUM the displayed size equals the intrinsic size.
        // On higher-density devices it scales up proportionally.
        int density = Display.getInstance().getDeviceDensity();
        RecordingSVG img = new RecordingSVG(24, 24);
        int expected = Math.max(1,
                (int) Math.floor((24.0 * density) / CN1Constants.DENSITY_MEDIUM + 0.5));
        assertEquals(expected, img.getWidth(),
                "getWidth reflects DPI-scaled intrinsic width at density " + density);
        assertEquals(expected, img.getHeight(),
                "getHeight reflects DPI-scaled intrinsic height at density " + density);
    }

    @FormTest
    public void scaledReportsRequestedDimensions() {
        RecordingSVG img = new RecordingSVG(24, 24);
        Image small = img.scaled(8, 12);
        assertNotSame(img, small, "scaled() must return a distinct view");
        assertEquals(8, small.getWidth(), "scaled view reports the requested width");
        assertEquals(12, small.getHeight(), "scaled view reports the requested height");
        // The source's own dimensions are not mutated.
        assertEquals(img.getWidth(), new RecordingSVG(24, 24).getWidth());
    }

    @FormTest
    public void scaledViewDelegatesAnimationFlag() {
        RecordingSVG animated = new RecordingSVG(24, 24, true);
        Image scaled = animated.scaled(32, 32);
        assertTrue(scaled.isAnimation(), "scaled view inherits the animation flag");
        assertTrue(scaled.animate(), "scaled view requests repaints when animated");
    }

    @FormTest
    public void explicitPixelDimensionsOverrideDensityHeuristic() {
        // The cn1-svg-width / cn1-svg-height -> mm -> pixels path lands on the
        // (intrinsic..., int, int) constructor. The reported size is exactly
        // what was passed, regardless of intrinsic SVG dimensions or device
        // density.
        RecordingSVG img = new RecordingSVG(24, 24, false, 64, 48);
        assertEquals(64, img.getWidth());
        assertEquals(48, img.getHeight());
    }

    @FormTest
    public void mmToPixelsHonorsDeviceDpi() {
        // mmToPixels is the helper the generated mm-constructor uses. It
        // routes through Display.convertToPixels(float) so the result tracks
        // the device's actual DPI. We can't assert exact pixels (depends on
        // density) but the result must be >= 1 and grow with mm.
        int small = GeneratedSVGImage.mmToPixels(1f);
        int large = GeneratedSVGImage.mmToPixels(20f);
        assertTrue(small >= 1, "1mm always rounds to at least 1 pixel");
        assertTrue(large > small, "20mm produces more pixels than 1mm");
    }

    @FormTest
    public void scaledViewChainsToFreshViewWithoutLeakingDimensions() {
        // scaled(a).scaled(b) must report b's dimensions; the intermediate a
        // shouldn't haunt the inner view.
        RecordingSVG img = new RecordingSVG(24, 24);
        Image inner = img.scaled(40, 40).scaled(16, 16);
        assertEquals(16, inner.getWidth());
        assertEquals(16, inner.getHeight());
    }

    @FormTest
    public void animationClockDrivenByAnimationTime() {
        // Pin the clock so the first-paint timestamp is deterministic.
        AnimationTime.setTime(5_000L);
        RecordingSVG img = new RecordingSVG(10, 10, true);
        // currentAnimationOffsetMs() captures the start time on first call,
        // mirroring what drawImage() does at the start of every paint.
        assertEquals(0L, img.currentAnimationOffsetMs(),
                "first paint sees zero elapsed (clock = start time)");

        AnimationTime.setTime(5_750L);
        assertEquals(750L, img.currentAnimationOffsetMs(),
                "subsequent paint sees the AnimationTime delta from the first paint");

        // Rewinding the clock clamps elapsed to zero (no negative time).
        AnimationTime.setTime(4_000L);
        assertEquals(0L, img.currentAnimationOffsetMs(),
                "rewound clock clamps elapsed to zero");
    }

    @FormTest
    public void resetAnimationDropsTheFirstPaintTimestamp() {
        AnimationTime.setTime(1_000L);
        RecordingSVG img = new RecordingSVG(10, 10, true);
        img.currentAnimationOffsetMs();
        AnimationTime.setTime(1_500L);
        assertEquals(500L, img.currentAnimationOffsetMs());

        img.resetAnimation();
        // After reset the next call becomes the new t=0.
        AnimationTime.setTime(2_000L);
        assertEquals(0L, img.currentAnimationOffsetMs());

        AnimationTime.setTime(2_250L);
        assertEquals(250L, img.currentAnimationOffsetMs());
    }

    @FormTest
    public void nonAnimatedAlwaysReportsZeroElapsed() {
        AnimationTime.setTime(10_000L);
        RecordingSVG img = new RecordingSVG(10, 10, false);
        assertEquals(0L, img.currentAnimationOffsetMs());
        AnimationTime.setTime(20_000L);
        assertEquals(0L, img.currentAnimationOffsetMs(),
                "non-animated SVGs ignore the SMIL clock");
    }

    // ---------------------------------------------------------------------
    // Static SMIL helpers
    // ---------------------------------------------------------------------

    @FormTest
    public void progressClampsBeforeBegin() {
        assertEquals(0f, GeneratedSVGImage.progress(50L, 200L, 1000L, 1, false));
    }

    @FormTest
    public void progressLinearWithinCycle() {
        assertEquals(0.5f, GeneratedSVGImage.progress(500L, 0L, 1000L, 1, false), 1e-6f);
    }

    @FormTest
    public void progressFreezeHoldsLastValueWhenDone() {
        assertEquals(1f, GeneratedSVGImage.progress(2000L, 0L, 1000L, 1, true));
    }

    @FormTest
    public void progressNoFreezeReturnsZeroPastEnd() {
        assertEquals(0f, GeneratedSVGImage.progress(2000L, 0L, 1000L, 1, false));
    }

    @FormTest
    public void progressIndefiniteWraps() {
        assertEquals(0.25f, GeneratedSVGImage.progress(1250L, 0L, 1000L,
                GeneratedSVGImage.REPEAT_INDEFINITE, false), 1e-6f);
    }

    @FormTest
    public void lerpLinear() {
        assertEquals(0f, GeneratedSVGImage.lerp(0f, 100f, 0f));
        assertEquals(100f, GeneratedSVGImage.lerp(0f, 100f, 1f));
        assertEquals(25f, GeneratedSVGImage.lerp(0f, 100f, 0.25f));
    }

    @FormTest
    public void lerpColorChannelByChannel() {
        int mid = GeneratedSVGImage.lerpColor(0xFF000000, 0xFFFFFFFF, 0.5f);
        // Each channel should be ~0x80
        assertEquals(0xFF, (mid >>> 24) & 0xFF);
        assertEquals(0x80, (mid >>> 16) & 0xFF);
        assertEquals(0x80, (mid >>> 8) & 0xFF);
        assertEquals(0x80, mid & 0xFF);
    }

    @FormTest
    public void lerpValuesEvenlySpaced() {
        float[] stops = new float[]{0f, 10f, 20f};
        // t=0 -> first stop, t=1 -> last stop, t=0.5 -> middle stop
        assertEquals(0f, GeneratedSVGImage.lerpValues(stops, 0f));
        assertEquals(20f, GeneratedSVGImage.lerpValues(stops, 1f));
        assertEquals(10f, GeneratedSVGImage.lerpValues(stops, 0.5f), 1e-5f);
        // Halfway between stop 0 and stop 1
        assertEquals(5f, GeneratedSVGImage.lerpValues(stops, 0.25f), 1e-5f);
    }

    @FormTest
    public void svgArcAppendsCubicSegments() {
        // 90-degree quarter circle in the upper-right quadrant.
        GeneralPath path = new GeneralPath();
        path.moveTo(10f, 0f);
        GeneratedSVGImage.svgArc(path,
                10f, 0f,        // start (current point)
                10f, 10f,       // rx, ry
                0f,             // x-axis rotation
                false, true,    // largeArc=0, sweep=1
                0f, 10f);       // end
        // We don't have a way to inspect the cubic segments directly without
        // a PathIterator round-trip, but the end-point should be at (0, 10).
        float[] bounds = new float[4];
        path.getBounds2D(bounds);
        // Bounds should at least include the start and end and not extend
        // wildly outside the unit circle.
        assertTrue(bounds[0] <= 0f + 0.01f, "bounds x reaches the end x");
        assertTrue(bounds[1] <= 0f + 0.01f, "bounds y reaches the start y");
        assertTrue(bounds[0] + bounds[2] >= 10f - 0.01f, "bounds reach the start x");
        assertTrue(bounds[1] + bounds[3] >= 10f - 0.01f, "bounds reach the end y");
    }

    @FormTest
    public void svgArcDegeneratesToLineForZeroRadius() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0f, 0f);
        GeneratedSVGImage.svgArc(path, 0f, 0f, 0f, 0f, 0f, false, false, 10f, 10f);
        float[] bounds = new float[4];
        path.getBounds2D(bounds);
        assertEquals(0f, bounds[0]);
        assertEquals(0f, bounds[1]);
        assertEquals(10f, bounds[2], 1e-4f);
        assertEquals(10f, bounds[3], 1e-4f);
    }

    /**
     * Concrete subclass used to inspect what paintSVG sees and how
     * dimensions are reported, without needing a real generated class.
     */
    static final class RecordingSVG extends GeneratedSVGImage {
        long lastElapsedMs = Long.MIN_VALUE;
        int paintCalls;

        RecordingSVG(int w, int h) {
            this(w, h, false);
        }

        RecordingSVG(int w, int h, boolean animated) {
            super(w, h, 0f, 0f, w, h, animated);
        }

        RecordingSVG(int intrinsicW, int intrinsicH, boolean animated,
                     int explicitW, int explicitH) {
            super(intrinsicW, intrinsicH, 0f, 0f, intrinsicW, intrinsicH,
                    animated, explicitW, explicitH);
        }

        @Override
        protected void paintSVG(Graphics g, long elapsedMs) {
            this.lastElapsedMs = elapsedMs;
            this.paintCalls++;
        }
    }
}
