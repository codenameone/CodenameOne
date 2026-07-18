package dart.runtime;

/**
 * Holder for a captured mutable local of reference type. Dart closures may
 * assign captured locals; Java lambdas require effectively-final captures,
 * so the transpiler's CaptureBoxer rewrites such locals to a {@code final}
 * holder whose {@code v} field is mutated instead.
 */
public final class Ref<T> {
    public T v;

    public Ref(T v) {
        this.v = v;
    }
}
