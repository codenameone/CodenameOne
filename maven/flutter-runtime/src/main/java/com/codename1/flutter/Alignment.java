package com.codename1.flutter;

/**
 * A point within a rectangle expressed in Flutter's -1..1 coordinate system:
 * (-1,-1) is the top left, (0,0) the center, (1,1) the bottom right.
 */
public class Alignment {

    public static final Alignment topLeft = new Alignment(-1, -1);
    public static final Alignment topCenter = new Alignment(0, -1);
    public static final Alignment topRight = new Alignment(1, -1);
    public static final Alignment centerLeft = new Alignment(-1, 0);
    public static final Alignment center = new Alignment(0, 0);
    public static final Alignment centerRight = new Alignment(1, 0);
    public static final Alignment bottomLeft = new Alignment(-1, 1);
    public static final Alignment bottomCenter = new Alignment(0, 1);
    public static final Alignment bottomRight = new Alignment(1, 1);

    private final double x;
    private final double y;

    public Alignment(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /**
     * The offset of a child of the given extent within a parent of the given
     * extent, along one axis.
     */
    public static double along(double alignment, double parentExtent, double childExtent) {
        return (parentExtent - childExtent) * (alignment + 1) / 2;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Alignment)) {
            return false;
        }
        Alignment a = (Alignment) o;
        return a.x == x && a.y == y;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y);
        return (int) (bits ^ (bits >>> 32));
    }
}
