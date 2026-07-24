package com.codename1.flutter.foundation;

import dart.runtime.Funcs;

/**
 * An object exposing a value that changes over time and can be listened to
 * ({@code ValueListenable<T>} in Flutter). {@code ValueListenableBuilder}
 * rebuilds whenever the value changes. Implemented by {@link ValueNotifier}.
 *
 * @param <T> the value type
 */
public abstract class ValueListenable<T> {

    public abstract T value();

    public abstract void addListener(Funcs.VoidFunc0 listener);

    public abstract void removeListener(Funcs.VoidFunc0 listener);
}
