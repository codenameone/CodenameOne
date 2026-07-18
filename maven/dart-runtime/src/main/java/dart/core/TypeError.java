package dart.core;

/**
 * Dart's TypeError (thrown by failed casts and the ! null-check operator).
 */
public class TypeError extends RuntimeException {
    public TypeError(String message) {
        super(message);
    }
}
