package com.codename1.flutter;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for restorable state values, mirroring Flutter's
 * {@code RestorableProperty<T>} (which extends {@code ChangeNotifier}). User
 * code subclasses this directly to restore custom values, overriding
 * {@link #createDefaultValue()}, {@link #fromPrimitives(Object)},
 * {@link #toPrimitives()} and {@link #initWithValue(Object)} and calling
 * {@link #notifyListeners()}.
 *
 * <p>Codename One does not persist restoration data, so the serialization hooks
 * are inert; what matters at runtime is the {@code ChangeNotifier} behaviour and
 * the value held by the {@link RestorableValue} subtypes.</p>
 *
 * @param <T> the restored value type
 */
public class RestorableProperty<T> {

    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
    private boolean registered;
    private boolean disposed;

    /** The value used when no restoration data is available. */
    public T createDefaultValue() {
        return null;
    }

    /** Adopt {@code value} as the current value (no persistence side effects). Returns the
     *  adopted value — Flutter's {@code initWithValue} returns {@code T}. */
    public void initWithValue(T value) {
    }

    /** Serialize the current value; inert because nothing is persisted. */
    public Object toPrimitives() {
        return null;
    }

    /** Deserialize a previously persisted value; never called (no persistence). */
    public T fromPrimitives(Object data) {
        return createDefaultValue();
    }

    /** Whether this property has been registered with a {@link RestorationMixin}. */
    public boolean isRegistered() {
        return registered;
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        listeners.remove(listener);
    }

    public void notifyListeners() {
        for (Funcs.VoidFunc0 l : new ArrayList<Funcs.VoidFunc0>(listeners)) {
            l.call();
        }
    }

    public void dispose() {
        disposed = true;
        listeners.clear();
    }

    // ------------------------------------------------------------------
    // Framework plumbing (used by RestorationMixin)
    // ------------------------------------------------------------------

    void markRegistered() {
        this.registered = true;
    }

    void markUnregistered() {
        this.registered = false;
    }
}
