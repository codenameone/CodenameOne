package dart.async;

/**
 * Dart's Completer&lt;T&gt;.
 */
public class Completer<T> {

    private final Future<T> future = new Future<T>();

    public Future<T> future() {
        return future;
    }

    public void complete(T value) {
        future.complete(value);
    }

    public void completeError(Object error) {
        future.completeError(error);
    }

    public boolean isCompleted() {
        return future.isDone();
    }
}
