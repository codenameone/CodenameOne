package dart.core;

import dart.runtime.DartRuntime;

/**
 * Dart's ArgumentError.
 */
public class ArgumentError extends RuntimeException {
    private final Object invalidValue;
    private final boolean hasValue;
    private final String name;

    public ArgumentError(String message) {
        super(message);
        this.invalidValue = null;
        this.hasValue = false;
        this.name = null;
    }

    public ArgumentError(Object value, String name, String message) {
        super(message);
        this.invalidValue = value;
        this.hasValue = true;
        this.name = name;
    }

    public static ArgumentError value(Object value, String name, String message) {
        return new ArgumentError(value, name, message);
    }

    public Object invalidValue() {
        return invalidValue;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Invalid argument");
        if (name != null) {
            sb.append(" (").append(name).append(")");
        }
        if (getMessage() != null) {
            sb.append(": ").append(getMessage());
        }
        if (hasValue) {
            sb.append(": ").append(DartRuntime.str(invalidValue));
        }
        return sb.toString();
    }
}
