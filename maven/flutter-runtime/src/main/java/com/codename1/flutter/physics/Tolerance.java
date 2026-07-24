package com.codename1.flutter.physics;

/**
 * The error tolerances a physics simulation settles within — Flutter's
 * {@code Tolerance}: {@code distance}, {@code time} and {@code velocity}
 * thresholds below which a simulation is considered to have come to rest. The
 * home carousel physics compares the fling velocity against
 * {@link #velocity()}.
 */
public class Tolerance {

    /** Flutter's {@code Tolerance.defaultTolerance}. */
    public static final Tolerance defaultTolerance = new Tolerance(1e-3, 1e-3, 1e-3);

    private double distance = 1e-3;
    private double time = 1e-3;
    private double velocity = 1e-3;

    public Tolerance() {
    }

    public Tolerance(double distance, double time, double velocity) {
        this.distance = distance;
        this.time = time;
        this.velocity = velocity;
    }

    public void distance(double v) {
        this.distance = v;
    }

    public void time(double v) {
        this.time = v;
    }

    public void velocity(double v) {
        this.velocity = v;
    }

    public double distance() {
        return distance;
    }

    public double time() {
        return time;
    }

    public double velocity() {
        return velocity;
    }
}
