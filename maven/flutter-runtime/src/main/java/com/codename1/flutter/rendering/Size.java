package com.codename1.flutter.rendering;

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

    public double width() {
        return width;
    }

    public double height() {
        return height;
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
