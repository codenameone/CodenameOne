package dart.core;

/**
 * Dart's UnsupportedError (e.g. mutating a fixed-length or unmodifiable list).
 */
public class UnsupportedError extends RuntimeException {
    public UnsupportedError(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return "Unsupported operation: " + getMessage();
    }
}
