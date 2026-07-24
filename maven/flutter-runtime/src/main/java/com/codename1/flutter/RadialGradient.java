package com.codename1.flutter;

/** A 2D radial gradient — Flutter's {@code RadialGradient}. */
public class RadialGradient extends Gradient {

    double radius = 0.5;
    Object focal;
    double focalRadius;

    public void radius(double v) {
        this.radius = v;
    }

    public void focal(Object v) {
        this.focal = v;
    }

    public void focalRadius(double v) {
        this.focalRadius = v;
    }
}
