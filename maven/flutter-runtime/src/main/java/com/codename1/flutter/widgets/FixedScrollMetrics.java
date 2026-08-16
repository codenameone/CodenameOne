package com.codename1.flutter.widgets;

/**
 * An immutable snapshot of a scrollable's extents — Flutter's {@code FixedScrollMetrics}.
 *
 * <p>Unlike a live {@link ScrollPosition} this describes a moment rather than tracking
 * one, which is what makes the physics testable: {@code applyPhysicsToUserOffset} is a
 * pure function of the metrics you hand it, so it can be sampled exactly without a
 * scrollable, a viewport or a frame.</p>
 */
public class FixedScrollMetrics extends ScrollMetrics {

    private com.codename1.flutter.AxisDirection axisDirection =
            com.codename1.flutter.AxisDirection.down;
    private double devicePixelRatio;

    public FixedScrollMetrics() {
        // The named setters below stand in for Dart's named parameters; until one is
        // called the extents are simply absent, as they are on a scrollable that has not
        // laid out yet.
    }

    public void minScrollExtent(double v) {
        this.minScrollExtent = v;
        this.hasContentDimensions = true;
    }

    public void maxScrollExtent(double v) {
        this.maxScrollExtent = v;
        this.hasContentDimensions = true;
    }

    public void pixels(double v) {
        this.pixels = v;
        this.hasPixels = true;
    }

    public void viewportDimension(double v) {
        this.viewportDimension = v;
        this.hasViewportDimension = true;
    }

    public void axisDirection(com.codename1.flutter.AxisDirection v) {
        this.axisDirection = v;
    }

    public void devicePixelRatio(double v) {
        this.devicePixelRatio = v;
    }

    public com.codename1.flutter.AxisDirection getAxisDirection() {
        return axisDirection;
    }

    public double getDevicePixelRatio() {
        return devicePixelRatio;
    }
}
