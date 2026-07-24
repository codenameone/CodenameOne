package com.codename1.flutter.vectormath;

/**
 * A 3-component double vector from {@code package:vector_math}
 * ({@code Vector3}). new_gallery's transformations demo uses it for hex-grid cube
 * coordinates, reading {@link #x()} / {@link #y()} / {@link #z()}.
 */
public class Vector3 {

    private double x;
    private double y;
    private double z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Vector3 zero() {
        return new Vector3(0, 0, 0);
    }

    public static Vector3 all(double value) {
        return new Vector3(value, value, value);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public void x(double v) {
        this.x = v;
    }

    public void y(double v) {
        this.y = v;
    }

    public void z(double v) {
        this.z = v;
    }
}
