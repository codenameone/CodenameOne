package com.codename1.flutter.physics;

/**
 * A {@link Simulation} of a spring settling from a start offset to an end
 * offset under a {@link SpringDescription} — Flutter's {@code SpringSimulation}
 * (the base of {@link ScrollSpringSimulation}). This pass models the endpoint;
 * the settled position is {@code end}.
 */
public class SpringSimulation extends Simulation {

    private final SpringDescription spring;
    private final double start;
    private final double end;
    private final double velocity;

    public SpringSimulation(SpringDescription spring, double start, double end, double velocity) {
        this.spring = spring;
        this.start = start;
        this.end = end;
        this.velocity = velocity;
    }

    public SpringDescription getSpring() {
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
