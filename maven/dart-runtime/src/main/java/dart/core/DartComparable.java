package dart.core;

/**
 * Static helpers for Dart's {@code Comparable}. Dart exposes
 * {@code Comparable.compare(a, b)} as a static combinator; it delegates to the
 * receivers' {@code compareTo}. Instances map onto {@link java.lang.Comparable}.
 */
public final class DartComparable {

    private DartComparable() {
    }

    /**
     * {@code Comparable.compare}: returns a negative value, zero, or a positive
     * value as {@code a} orders before, equal to, or after {@code b}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static long compare(Comparable a, Comparable b) {
        return a.compareTo(b);
    }
}
