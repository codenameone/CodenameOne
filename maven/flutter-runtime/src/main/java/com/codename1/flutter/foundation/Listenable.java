package com.codename1.flutter.foundation;

import dart.runtime.Funcs;

/**
 * The root of Flutter's observable protocol ({@code Listenable}): an object
 * that maintains a list of listeners and notifies them when it changes.
 * Implemented by {@link ChangeNotifier} and by the animation {@code Animation}
 * types. {@code AnimatedWidget} is driven by one.
 */
public interface Listenable {

    void addListener(Funcs.VoidFunc0 listener);

    void removeListener(Funcs.VoidFunc0 listener);
}
