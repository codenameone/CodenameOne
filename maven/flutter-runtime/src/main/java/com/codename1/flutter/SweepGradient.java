package com.codename1.flutter;

/** A 2D sweep (angular) gradient — Flutter's {@code SweepGradient}. */
public class SweepGradient extends Gradient {

    double startAngle;
    double endAngle = 2 * Math.PI;

    public void startAngle(double v) {
        this.startAngle = v;
    }

    public void endAngle(double v) {
        this.endAngle = v;
    }
}
