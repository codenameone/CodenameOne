package com.codename1.flutter.physics;

/**
 * Structural parameters of a spring — Flutter's {@code SpringDescription}:
 * {@code mass}, {@code stiffness} and {@code damping}. Fed to a
 * {@code ScrollSpringSimulation} to model overscroll / fling settling.
 */
public class SpringDescription {

    private double mass;
    private double stiffness;
    private double damping;

    public SpringDescription() {
    }

    public void mass(double v) {
        this.mass = v;
    }

    public void stiffness(double v) {
        this.stiffness = v;
    }

    public void damping(double v) {
        this.damping = v;
    }

    public double getMass() {
        return mass;
    }

    public double getStiffness() {
        return stiffness;
    }

    public double getDamping() {
        return damping;
    }

    /**
     * {@code SpringDescription.withDampingRatio}: builds a spring from a mass,
     * stiffness and damping ratio (1.0 = critically damped).
     */
    public static SpringDescription withDampingRatio(double mass, double stiffness, double ratio) {
        SpringDescription s = new SpringDescription();
        s.mass(mass);
        s.stiffness(stiffness);
        s.damping(ratio * 2.0 * Math.sqrt(mass * stiffness));
        return s;
    }
}
