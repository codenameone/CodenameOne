package com.codename1.flutter.gestures;

/**
 * The details at the end of a scale/pan gesture, carrying the fling velocity —
 * Flutter's {@code ScaleEndDetails}.
 */
public final class ScaleEndDetails {

    private Velocity velocity = Velocity.zero;
    private long pointerCount;

    public ScaleEndDetails() {
    }

    public ScaleEndDetails(Velocity velocity, long pointerCount) {
        this.velocity = velocity == null ? Velocity.zero : velocity;
        this.pointerCount = pointerCount;
    }

    public Velocity velocity() {
        return velocity;
    }

    public void velocity(Velocity v) {
        this.velocity = v;
    }

    public long pointerCount() {
        return pointerCount;
    }

    public void pointerCount(long v) {
        this.pointerCount = v;
    }
}
