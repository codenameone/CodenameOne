package com.codename1.flutter.physics;

/**
 * A spring {@link Simulation} that carries a scrollable from a start offset to
 * an end offset under a {@link SpringDescription} — Flutter's
 * {@code ScrollSpringSimulation}. Used by the home carousel's snapping physics
 * to settle onto an item after a fling. The spring is captured for API shape;
 * this pass models the endpoint (the settled position is {@code end}).
 *
 * @see com.codename1.flutter.widgets.ScrollPhysics
 */
public class ScrollSpringSimulation extends Simulation {

    private final Object spring;
    private final double start;
    private final double end;
    private final double velocity;

    public ScrollSpringSimulation(Object spring, double start, double end, double velocity) {
        this.spring = spring;
        this.start = start;
        this.end = end;
        this.velocity = velocity;
    }

    public Object getSpring() {
        return spring;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public double getVelocity() {
        return velocity;
    }

    @Override
    public double x(double time) {
        return end;
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
