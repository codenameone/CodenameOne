package com.codename1.flutter.physics;

/**
 * The base of a one-dimensional physics simulation over time — Flutter's
 * {@code Simulation}. Subclasses model a value that evolves with time:
 * {@link #x(double)} is the position at {@code time} seconds, {@link #dx(double)}
 * the velocity, and {@link #isDone(double)} whether the simulation has settled.
 * The {@code tolerance} the simulation settles within is captured for API shape.
 */
public abstract class Simulation {

    private Tolerance tolerance = Tolerance.defaultTolerance;

    public void tolerance(Tolerance v) {
        this.tolerance = v;
    }

    public Tolerance tolerance() {
        return tolerance;
    }

    /** The position of the object at {@code time} seconds. */
    public abstract double x(double time);

    /** The velocity of the object at {@code time} seconds. */
    public abstract double dx(double time);

    /** Whether the simulation is done (settled) at {@code time} seconds. */
    public abstract boolean isDone(double time);
}
