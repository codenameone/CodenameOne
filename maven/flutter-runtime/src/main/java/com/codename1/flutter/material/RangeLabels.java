package com.codename1.flutter.material;

/**
 * Text labels shown above the two thumbs of a {@link RangeSlider} — Flutter's
 * {@code RangeLabels}.
 */
public class RangeLabels {

    private final String start;
    private final String end;

    public RangeLabels(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public String start() {
        return start;
    }

    public String end() {
        return end;
    }
}
