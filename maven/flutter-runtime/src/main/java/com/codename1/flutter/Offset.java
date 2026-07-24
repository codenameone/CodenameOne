package com.codename1.flutter;

/**
 * An immutable 2D floating-point offset (dx, dy) in logical pixels — Flutter's
 * dart:ui {@code Offset}. Used both as a displacement vector and, in painting
 * code, as a point in a coordinate space.
 */
public final class Offset {

    public static final Offset zero = new Offset(0, 0);
    public static final Offset infinite =
            new Offset(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    private final double dx;
    private final double dy;

    public Offset(double dx, double dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** {@code Offset.fromDirection(direction, distance)} — polar to cartesian. */
    public static Offset fromDirection(double direction, double distance) {
        return new Offset(distance * Math.cos(direction), distance * Math.sin(direction));
    }

    public double dx() {
        return dx;
    }

    public double dy() {
        return dy;
    }

    public double distance() {
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double distanceSquared() {
        return dx * dx + dy * dy;
    }

    public double direction() {
        return Math.atan2(dy, dx);
    }

    public Offset scale(double scaleX, double scaleY) {
        return new Offset(dx * scaleX, dy * scaleY);
    }

    public Offset translate(double translateX, double translateY) {
        return new Offset(dx + translateX, dy + translateY);
    }

    public Offset $plus(Offset other) {
        return new Offset(dx + other.dx, dy + other.dy);
    }

    public Offset $minus(Offset other) {
        return new Offset(dx - other.dx, dy - other.dy);
    }

    public Offset $times(double operand) {
        return new Offset(dx * operand, dy * operand);
    }

    public Offset $div(double operand) {
        return new Offset(dx / operand, dy / operand);
    }

    /**
     * Dart's {@code Offset & Size}: the rectangle whose top-left is this offset
     * and whose extent is the given size.
     */
    public Rect $bitAnd(com.codename1.flutter.rendering.Size other) {
        return Rect.fromLTWH(dx, dy, other.width(), other.height());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Offset)) {
            return false;
        }
        Offset p = (Offset) o;
        return p.dx == dx && p.dy == dy;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(dx) * 31 + Double.doubleToLongBits(dy);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public String toString() {
        return "Offset(" + dx + ", " + dy + ")";
    }
}
