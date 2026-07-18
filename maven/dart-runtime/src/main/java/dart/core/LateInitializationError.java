package dart.core;

/**
 * Dart's LateInitializationError — a {@code late} variable was read before
 * being assigned (or a late final assigned twice).
 */
public class LateInitializationError extends RuntimeException {
    public LateInitializationError(String message) {
        super(message);
    }

    public static LateInitializationError notInitialized(String name) {
        return new LateInitializationError("LateInitializationError: Field '" + name + "' has not been initialized.");
    }

    public static LateInitializationError alreadyInitialized(String name) {
        return new LateInitializationError("LateInitializationError: Field '" + name + "' has already been initialized.");
    }
}
