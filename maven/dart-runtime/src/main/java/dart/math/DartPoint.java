package dart.math;

/**
 * Dart's {@code dart:math} {@code Point<T extends num>}. The gallery only uses
 * {@code Point<double>}, so coordinates are stored as {@code double}; the type
 * parameter {@code T} exists so the transpiler's {@code Point<double>} type
 * argument resolves.
 *
 * @param <T> the (numeric) coordinate type; phantom in this runtime
 */
public final class DartPoint<T> {

    private final double x;
    private final double y;

    public DartPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double distanceTo(DartPoint<T> other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
