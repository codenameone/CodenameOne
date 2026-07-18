package dart.core;

/**
 * Dart's Exception('message') — the generic exception users throw.
 */
public class DartException extends RuntimeException {
    public DartException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return "Exception: " + getMessage();
    }
}
