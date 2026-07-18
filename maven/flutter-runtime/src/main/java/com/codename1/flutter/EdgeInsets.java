package com.codename1.flutter;

/**
 * Immutable offsets for each of the four box edges, in logical pixels.
 */
public final class EdgeInsets {

    private final double left;
    private final double top;
    private final double right;
    private final double bottom;

    private EdgeInsets(double left, double top, double right, double bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static EdgeInsets all(double value) {
        return new EdgeInsets(value, value, value, value);
    }

    public static EdgeInsets only(double left, double top, double right, double bottom) {
        return new EdgeInsets(left, top, right, bottom);
    }

    public static EdgeInsets symmetric(double horizontal, double vertical) {
        return new EdgeInsets(horizontal, vertical, horizontal, vertical);
    }

    public double left() {
        return left;
    }

    public double top() {
        return top;
    }

    public double right() {
        return right;
    }

    public double bottom() {
        return bottom;
    }

    public double horizontal() {
        return left + right;
    }

    public double vertical() {
        return top + bottom;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EdgeInsets)) {
            return false;
        }
        EdgeInsets e = (EdgeInsets) o;
        return e.left == left && e.top == top && e.right == right && e.bottom == bottom;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(left);
        bits = bits * 31 + Double.doubleToLongBits(top);
        bits = bits * 31 + Double.doubleToLongBits(right);
        bits = bits * 31 + Double.doubleToLongBits(bottom);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public String toString() {
        return "EdgeInsets(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
}
