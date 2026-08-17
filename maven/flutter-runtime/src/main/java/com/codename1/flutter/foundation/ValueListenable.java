package com.codename1.flutter.foundation;

import dart.runtime.Funcs;

/**
 * An object exposing a value that changes over time and can be listened to
 * ({@code ValueListenable<T>} in Flutter). {@code ValueListenableBuilder}
 * rebuilds whenever the value changes. Implemented by {@link ValueNotifier}.
 *
 * <p>It IS a {@link Listenable}, as in Flutter, so a notifier can drive anything that takes
 * one — {@code AnimatedWidget(listenable: notifier)}, {@code AnimatedBuilder(animation:
 * notifier)}. Leaving the two hierarchies unrelated made those a compile error in transpiled
 * code for no reason the Dart could explain.</p>
 *
 * @param <T> the value type
 */
public abstract class ValueListenable<T> implements Listenable {

    public abstract T value();

    public abstract void addListener(Funcs.VoidFunc0 listener);

    public abstract void removeListener(Funcs.VoidFunc0 listener);
}
