package dart.core;

/**
 * Dart's UnimplementedError (thrown by the UnimplementedError() idiom and
 * by transpiler-generated stubs for members outside the supported subset).
 */
public class UnimplementedError extends UnsupportedError {
    public UnimplementedError(String message) {
        super(message);
    }

    public UnimplementedError() {
        super("Unimplemented");
    }
}
