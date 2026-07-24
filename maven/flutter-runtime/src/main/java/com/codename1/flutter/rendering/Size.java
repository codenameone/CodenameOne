package com.codename1.flutter.rendering;

import com.codename1.flutter.Offset;

/**
 * An immutable width/height pair, in the same unit as the constraints that
 * produced it (device pixels at runtime, raw logical values in unit tests).
 */
public final class Size {

    public static final Size ZERO = new Size(0, 0);

    private final double width;
    private final double height;

    public Size(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /** {@code Size.fromRadius}: a square that bounds a circle of {@code radius}. */
    public static Size fromRadius(double radius) {
        return new Size(radius * 2, radius * 2);
    }

    /** {@code Size.fromHeight}: a fixed height, unbounded width. */
    public static Size fromHeight(double height) {
        return new Size(Double.POSITIVE_INFINITY, height);
    }

    /** {@code Size.fromWidth}: a fixed width, unbounded height. */
    public static Size fromWidth(double width) {
        return new Size(width, Double.POSITIVE_INFINITY);
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    /** {@code Size.shortestSide}: the lesser of {@link #width()} and {@link #height()}. */
    public double shortestSide() {
        return Math.min(width, height);
    }

    /** {@code Size.longestSide}: the greater of {@link #width()} and {@link #height()}. */
    public double longestSide() {
        return Math.max(width, height);
    }

    /**
     * {@code Size.center}: the offset to the center of this size, given a
     * top-left {@code origin}.
     */
    public Offset center(Offset origin) {
        return new Offset(origin.dx() + width / 2, origin.dy() + height / 2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Size)) {
            return false;
        }
        Size s = (Size) o;
        return s.width == width && s.height == height;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(width) * 31 + Double.doubleToLongBits(height);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public String toString() {
        return "Size(" + width + ", " + height + ")";
    }
}
