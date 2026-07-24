package com.codename1.flutter.foundation;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@code ChangeNotifier} that holds a single value ({@code ValueNotifier<T>}
 * in Flutter); assigning {@link #value(Object)} notifies listeners when the value
 * actually changes. new_gallery drives {@code ValueListenableBuilder<bool>} from
 * these (the settings sheet's open flag, the extended nav-rail flag).
 *
 * <p>Transpiler surface: the Dart {@code value} getter maps to {@link #value()},
 * {@code notifier.value = v} to {@link #value(Object)}.</p>
 *
 * @param <T> the value type
 */
public class ValueNotifier<T> extends ValueListenable<T> {

    private T current;
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();

    public ValueNotifier(T value) {
        this.current = value;
    }

    @Override
    public T value() {
        return current;
    }

    public void value(T newValue) {
        boolean changed = current == null ? newValue != null : !current.equals(newValue);
        if (changed) {
            current = newValue;
            notifyListeners();
        }
    }

    @Override
    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(Funcs.VoidFunc0 listener) {
        listeners.remove(listener);
    }

    public void notifyListeners() {
        for (Funcs.VoidFunc0 l : new ArrayList<Funcs.VoidFunc0>(listeners)) {
            l.call();
        }
    }

    public void dispose() {
        listeners.clear();
    }
}
