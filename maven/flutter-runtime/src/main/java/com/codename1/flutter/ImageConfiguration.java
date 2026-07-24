package com.codename1.flutter;

import com.codename1.flutter.rendering.Size;

/**
 * The context handed to a custom {@code Decoration}'s box painter
 * ({@code ImageConfiguration} in Flutter): the target size, device pixel ratio,
 * text direction and locale. new_gallery's tab-indicator and pie-chart painters
 * read {@link #size()} to lay out their geometry.
 */
public class ImageConfiguration {

    /** The empty configuration (Dart's {@code ImageConfiguration.empty}). */
    public static final ImageConfiguration empty = new ImageConfiguration();

    private Size size;
    private Double devicePixelRatio;
    private TextDirection textDirection;
    private Locale locale;

    public ImageConfiguration() {
    }

    // Named-parameter setters.
    public void size(Size v) {
        this.size = v;
    }

    public void devicePixelRatio(double v) {
        this.devicePixelRatio = v;
    }

    public void textDirection(TextDirection v) {
        this.textDirection = v;
    }

    public void locale(Locale v) {
        this.locale = v;
    }

    public Size size() {
        return size;
    }

    public Double devicePixelRatio() {
        return devicePixelRatio;
    }
}
