package dart.core;

/**
 * Dart's FormatException (int.parse / double.parse failures etc.).
 */
public class FormatException extends RuntimeException {
    public FormatException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return "FormatException: " + getMessage();
    }
}
