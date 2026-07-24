package com.codename1.flutter.gestures;

/**
 * The details at the end of a drag, carrying the fling velocity — Flutter's
 * {@code DragEndDetails}. The home splash and reply drawer read
 * {@code velocity.pixelsPerSecond}.
 */
public final class DragEndDetails {

    private Velocity velocity = Velocity.zero;
    private Double primaryVelocity;

    public DragEndDetails() {
    }

    public DragEndDetails(Velocity velocity, Double primaryVelocity) {
        this.velocity = velocity == null ? Velocity.zero : velocity;
        this.primaryVelocity = primaryVelocity;
    }

    public Velocity velocity() {
        return velocity;
    }

    public void velocity(Velocity v) {
        this.velocity = v;
    }

    public Double primaryVelocity() {
        return primaryVelocity;
    }

    public void primaryVelocity(Double v) {
        this.primaryVelocity = v;
    }
}
