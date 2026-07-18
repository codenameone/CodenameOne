package com.codename1.flutter;

import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Display;

/**
 * A snapshot of the display metrics {@link MediaQuery#of} returns, expressed
 * the way Flutter expresses them:
 * <ul>
 *   <li>{@link #size()} — the display size in LOGICAL pixels (CN1 device
 *       pixels divided by {@link Dp#scale()})</li>
 *   <li>{@link #devicePixelRatio()} — device pixels per logical pixel
 *       ({@link Dp#scale()}, bucketed like Android/Flutter density
 *       buckets)</li>
 *   <li>{@link #platformBrightness()} — the platform dark-mode setting
 *       (light when unknown)</li>
 * </ul>
 *
 * <p>Headless (no Display) the sensible defaults are a 0x0 size, ratio 1.0
 * and light brightness.</p>
 */
public class MediaQueryData {

    private final Size size;
    private final double devicePixelRatio;
    private final Brightness platformBrightness;

    public MediaQueryData(Size size, double devicePixelRatio, Brightness platformBrightness) {
        this.size = size;
        this.devicePixelRatio = devicePixelRatio;
        this.platformBrightness = platformBrightness == null ? Brightness.light : platformBrightness;
    }

    public Size size() {
        return size;
    }

    public double devicePixelRatio() {
        return devicePixelRatio;
    }

    public Brightness platformBrightness() {
        return platformBrightness;
    }

    /**
     * Builds the snapshot from the current CN1 Display, or the headless
     * defaults when no Display is initialized.
     */
    public static MediaQueryData fromDisplay() {
        if (!Display.isInitialized()) {
            return new MediaQueryData(new Size(0, 0), 1.0, Brightness.light);
        }
        Display d = Display.getInstance();
        Boolean dark = null;
        try {
            dark = d.isDarkMode();
        } catch (Throwable ignore) {
            // ports without dark-mode detection
        }
        return compute(d.getDisplayWidth(), d.getDisplayHeight(), Dp.scale(), dark);
    }

    /**
     * The pure metric math (headless-testable): logical size is the pixel
     * size divided by the scale; a non-positive scale falls back to 1; a null
     * or FALSE dark flag maps to light.
     */
    public static MediaQueryData compute(int widthPx, int heightPx, double scale, Boolean darkMode) {
        if (scale <= 0) {
            scale = 1;
        }
        return new MediaQueryData(
                new Size(widthPx / scale, heightPx / scale),
                scale,
                Boolean.TRUE.equals(darkMode) ? Brightness.dark : Brightness.light);
    }
}
