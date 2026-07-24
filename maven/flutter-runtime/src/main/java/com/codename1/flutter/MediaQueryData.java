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
    private final double textScaleFactor;
    private final EdgeInsets padding;

    public MediaQueryData(Size size, double devicePixelRatio, Brightness platformBrightness) {
        this(size, devicePixelRatio, platformBrightness, 1.0, EdgeInsets.all(0));
    }

    public MediaQueryData(Size size, double devicePixelRatio, Brightness platformBrightness,
                          double textScaleFactor, EdgeInsets padding) {
        this.size = size;
        this.devicePixelRatio = devicePixelRatio;
        this.platformBrightness = platformBrightness == null ? Brightness.light : platformBrightness;
        this.textScaleFactor = textScaleFactor;
        this.padding = padding == null ? EdgeInsets.all(0) : padding;
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
     * The number of font pixels per logical pixel (legacy Flutter accessor;
     * defaults to 1.0 — this pass does not read the platform text-scale).
     */
    public double textScaleFactor() {
        return textScaleFactor;
    }

    /**
     * The parts of the display partially obscured by system UI (defaults to
     * {@link EdgeInsets#all(double) EdgeInsets.all(0)}).
     */
    public EdgeInsets padding() {
        return padding;
    }

    /**
     * The parts of the display obscured by system UI that the app can still
     * draw under (e.g. the on-screen keyboard). This pass does not track the
     * keyboard, so it reports no insets.
     */
    public EdgeInsets viewInsets() {
        return EdgeInsets.all(0);
    }

    /**
     * The parts of the display obscured by system UI regardless of whether the
     * app can draw under them (e.g. a hardware notch). This pass does not track
     * system insets, so it reports none.
     */
    public EdgeInsets viewPadding() {
        return EdgeInsets.all(0);
    }

    /**
     * Returns a copy with the supplied (non-null) values overridden. Parameter
     * order matches the Dart stub.
     */
    public MediaQueryData copyWith(Size size, Double devicePixelRatio, Double textScaleFactor,
                                   EdgeInsets padding, Brightness platformBrightness) {
        return new MediaQueryData(
                size != null ? size : this.size,
                devicePixelRatio != null ? devicePixelRatio : this.devicePixelRatio,
                platformBrightness != null ? platformBrightness : this.platformBrightness,
                textScaleFactor != null ? textScaleFactor : this.textScaleFactor,
                padding != null ? padding : this.padding);
    }

    /**
     * Returns a copy with the selected padding edges zeroed — Flutter's
     * {@code MediaQueryData.removePadding}. This runtime does not scope media
     * metrics through the element tree, so a same-metrics copy is returned
     * (the removed edges are treated as a no-op).
     */
    public MediaQueryData removePadding(Boolean removeLeft, Boolean removeTop,
            Boolean removeRight, Boolean removeBottom) {
        return this;
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
