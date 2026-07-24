package dart.collection;

/**
 * Dart's {@code Iterator<E>} protocol: advance with {@link #moveNext()}, then
 * read {@link #current()}. A transpiled {@code class X implements Iterator<E>}
 * supplies {@code moveNext} and the {@code current} property; the java-style
 * {@link #hasNext()}/{@link #next()} pair is provided for interop.
 *
 * <p>All members have defaults so an applying class only needs to override the
 * ones it declares (Dart's {@code moveNext} and {@code current}); anything it
 * omits falls back to an inert default.</p>
 *
 * @param <E> the element type
 */
public interface Iterator<E> {

    /** Advance to the next element; false when the iteration is exhausted. */
    default boolean moveNext() {
        return false;
    }

    /** The element reached by the most recent {@link #moveNext()}. */
    default E current() {
        return null;
    }

    /** Java-style peek: whether another element is available. */
    default boolean hasNext() {
        return moveNext();
    }

    /** Java-style advance: returns {@link #current()} after moving. */
    default E next() {
        moveNext();
        return current();
    }
}
