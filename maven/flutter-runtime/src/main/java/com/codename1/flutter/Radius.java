package com.codename1.flutter;

/**
 * A radius for the corner of a rounded rectangle, with independent x and y
 * components — Flutter's dart:ui {@code Radius}.
 */
public final class Radius {

    public static final Radius zero = new Radius(0, 0);

    private final double x;
    private final double y;

    private Radius(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Radius circular(double radius) {
        return new Radius(radius, radius);
    }

    public static Radius elliptical(double x, double y) {
        return new Radius(x, y);
    }

    /** Dart's {@code Radius.lerp(a, b, t)}: per-component linear interpolation. */
    public static Radius lerp(Radius a, Radius b, double t) {
        if (a == null && b == null) return null;
        if (a == null) return new Radius(b.x * t, b.y * t);
        if (b == null) return new Radius(a.x * (1.0 - t), a.y * (1.0 - t));
        return new Radius(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Radius)) {
            return false;
        }
        Radius r = (Radius) o;
        return r.x == x && r.y == y;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public String toString() {
        return "Radius.elliptical(" + x + ", " + y + ")";
    }
}
