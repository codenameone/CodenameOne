package com.codename1.flutter.material;

/**
 * A per-axis density adjustment applied to a component's compactness,
 * mirroring Flutter's {@code VisualDensity}. Values are in abstract density
 * units in the range [-4, 4] where 0 is the un-adjusted baseline.
 */
public class VisualDensity {

    /** The standard, un-adjusted density. */
    public static final VisualDensity standard = new VisualDensity(0.0, 0.0);
    /** A looser density for pointer-first platforms. */
    public static final VisualDensity comfortable = new VisualDensity(-1.0, -1.0);
    /** A tighter density. */
    public static final VisualDensity compact = new VisualDensity(-2.0, -2.0);
    /**
     * The platform-appropriate default (compact on desktop, standard on
     * touch). This pass has no adaptive backend, so it aliases
     * {@link #standard}.
     */
    public static final VisualDensity adaptivePlatformDensity = standard;

    private double horizontal;
    private double vertical;

    public VisualDensity() {
    }

    public VisualDensity(double horizontal, double vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public void horizontal(double v) {
        this.horizontal = v;
    }

    public void vertical(double v) {
        this.vertical = v;
    }

    public double getHorizontal() {
        return horizontal;
    }

    public double getVertical() {
        return vertical;
    }
}
