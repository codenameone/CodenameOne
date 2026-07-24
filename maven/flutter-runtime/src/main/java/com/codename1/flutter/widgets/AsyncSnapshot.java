package com.codename1.flutter.widgets;

/**
 * An immutable snapshot of interaction with an asynchronous computation, handed
 * to a {@code FutureBuilder} / {@code StreamBuilder} builder, mirroring
 * Flutter's {@code AsyncSnapshot<T>}. The about page reads {@link #hasData()}
 * and {@link #data()}.
 *
 * @param <T> the type of the async value
 */
public class AsyncSnapshot<T> {

    private final ConnectionState connectionState;
    private final T data;
    private final Object error;
    private final Object stackTrace;

    public AsyncSnapshot() {
        this(ConnectionState.none, null, null, null);
    }

    public AsyncSnapshot(ConnectionState connectionState, T data, Object error, Object stackTrace) {
        this.connectionState = connectionState;
        this.data = data;
        this.error = error;
        this.stackTrace = stackTrace;
    }

    public ConnectionState connectionState() {
        return connectionState;
    }

    public T data() {
        return data;
    }

    public Object error() {
        return error;
    }

    public Object stackTrace() {
        return stackTrace;
    }

    public boolean hasData() {
        return data != null;
    }

    public boolean hasError() {
        return error != null;
    }

    public T requireData() {
        return data;
    }
}
