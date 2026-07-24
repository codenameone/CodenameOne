package com.codename1.flutter;

/** A circular (or elliptical) border — Flutter's {@code CircleBorder}. */
public class CircleBorder extends OutlinedBorder {

    private double eccentricity;

    public void eccentricity(double v) {
        this.eccentricity = v;
    }

    public double getEccentricity() {
        return eccentricity;
    }
}
