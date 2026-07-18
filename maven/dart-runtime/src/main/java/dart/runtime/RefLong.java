package dart.runtime;

/**
 * Primitive holder for a captured mutable Dart {@code int} local
 * (64-bit, mapped to Java {@code long}); avoids boxing in loops.
 */
public final class RefLong {
    public long v;

    public RefLong(long v) {
        this.v = v;
    }
}
