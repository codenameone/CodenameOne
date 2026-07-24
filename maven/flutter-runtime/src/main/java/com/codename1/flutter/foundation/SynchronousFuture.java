package com.codename1.flutter.foundation;

import dart.async.Future;

/**
 * A {@link Future} that is already complete and invokes its listeners
 * synchronously — Flutter foundation's {@code SynchronousFuture<T>}. Localization
 * delegates return one from {@code load} so the app can obtain its strings
 * without an asynchronous frame. Because the value is available at construction,
 * {@code then}/{@code whenComplete}/{@code catchError} (inherited from
 * {@link Future}) run immediately.
 */
public class SynchronousFuture<T> extends Future<T> {

    public SynchronousFuture(T value) {
        super(value);
    }
}
