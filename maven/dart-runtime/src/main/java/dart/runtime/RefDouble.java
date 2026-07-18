package dart.runtime;

/**
 * Primitive holder for a captured mutable Dart {@code double} local.
 */
public final class RefDouble {
    public double v;

    public RefDouble(double v) {
        this.v = v;
    }
}
