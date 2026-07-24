package com.codename1.flutter.physics;

/**
 * A friction {@link Simulation} clamped to a scroll range — Flutter's
 * {@code ClampingScrollSimulation}. Models the deceleration of a fling on the
 * Android-style clamping scroll physics. This pass captures the parameters and
 * reports the resting {@code position}.
 */
public class ClampingScrollSimulation extends Simulation {

    private double position;
    private double velocity;
    private Double friction;

    public ClampingScrollSimulation() {
    }

    public void position(double v) {
        this.position = v;
    }

    public void velocity(double v) {
        this.velocity = v;
    }

    public void friction(Double v) {
        this.friction = v;
    }

    public double getPosition() {
        return position;
    }

    public double getVelocity() {
        return velocity;
    }

    public Double getFriction() {
        return friction;
    }

    @Override
    public double x(double time) {
        return position;
    }

    @Override
    public double dx(double time) {
        return 0.0;
    }

    @Override
    public boolean isDone(double time) {
        return true;
    }
}
