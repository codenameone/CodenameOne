package dart.core;

/**
 * Dart's StateError — an object was in an invalid state for the operation
 * (e.g. Iterable.first on an empty iterable).
 */
public class StateError extends RuntimeException {
    public StateError(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return "Bad state: " + getMessage();
    }
}
