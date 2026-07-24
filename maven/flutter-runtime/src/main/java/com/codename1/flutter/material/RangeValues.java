package com.codename1.flutter.material;

/**
 * An immutable pair of {@code start}/{@code end} values for a
 * {@link RangeSlider} — Flutter's {@code RangeValues}.
 */
public class RangeValues {

    private final double start;
    private final double end;

    public RangeValues(double start, double end) {
        this.start = start;
        this.end = end;
    }

    public double start() {
        return start;
    }

    public double end() {
        return end;
    }
}
