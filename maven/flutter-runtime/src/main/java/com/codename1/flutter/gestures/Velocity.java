package com.codename1.flutter.gestures;

import com.codename1.flutter.Offset;

/**
 * A 2-D velocity in logical pixels per second — Flutter's {@code Velocity}.
 * Carried by {@link DragEndDetails} so fling handlers can read
 * {@code velocity.pixelsPerSecond}.
 */
public final class Velocity {

    /** {@code Velocity.zero} — no motion. */
    public static final Velocity zero = new Velocity(Offset.zero);

    private final Offset pixelsPerSecond;

    public Velocity() {
        this(Offset.zero);
    }

    public Velocity(Offset pixelsPerSecond) {
        this.pixelsPerSecond = pixelsPerSecond == null ? Offset.zero : pixelsPerSecond;
    }

    /** Dart's static {@code Velocity.zero} getter. */
    public static Velocity zero() {
        return zero;
    }

    public Offset pixelsPerSecond() {
        return pixelsPerSecond;
    }

    /**
     * Dart's {@code Velocity.clampMagnitude(min, max)} — returns a velocity with
     * the same direction but magnitude clamped to {@code [minValue, maxValue]}.
     */
    public Velocity clampMagnitude(double minValue, double maxValue) {
        double valueSquared = pixelsPerSecond.distanceSquared();
        if (valueSquared > maxValue * maxValue) {
            return new Velocity(pixelsPerSecond.$div(pixelsPerSecond.distance()).$times(maxValue));
        }
        if (valueSquared < minValue * minValue) {
            return new Velocity(pixelsPerSecond.$div(pixelsPerSecond.distance()).$times(minValue));
        }
        return this;
    }

    @Override
    public String toString() {
        return "Velocity(" + pixelsPerSecond + ")";
    }
}
