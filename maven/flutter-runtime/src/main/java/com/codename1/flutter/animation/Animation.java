package com.codename1.flutter.animation;

import com.codename1.flutter.foundation.Listenable;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * A value of type {@code T} that changes over the lifetime of an animation,
 * plus a {@link AnimationStatus} and listener registration — Flutter's
 * {@code Animation<T>}.
 *
 * <p>Concrete subclasses ({@link AnimationController}, {@link CurvedAnimation},
 * {@link AlwaysStoppedAnimation}, and the tween-driven evaluations) supply the
 * current {@link #value()} and {@link #status()}. This base class owns the
 * value- and status-listener bookkeeping so subclasses can broadcast changes
 * with {@link #notifyListeners()} / {@link #notifyStatusListeners}.</p>
 */
public abstract class Animation<T> implements Listenable {

    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
    private final List<Funcs.VoidFunc1<AnimationStatus>> statusListeners =
            new ArrayList<Funcs.VoidFunc1<AnimationStatus>>();

    /** The current animated value. */
    public abstract T value();

    /** The current phase; the default is {@link AnimationStatus#dismissed}. */
    public AnimationStatus status() {
        return AnimationStatus.dismissed;
    }

    public boolean isCompleted() {
        return status() == AnimationStatus.completed;
    }

    public boolean isDismissed() {
        return status() == AnimationStatus.dismissed;
    }

    public boolean isAnimating() {
        AnimationStatus s = status();
        return s == AnimationStatus.forward || s == AnimationStatus.reverse;
    }

    public boolean isForwardOrCompleted() {
        AnimationStatus s = status();
        return s == AnimationStatus.forward || s == AnimationStatus.completed;
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        listeners.remove(listener);
    }

    public void addStatusListener(Funcs.VoidFunc1<AnimationStatus> listener) {
        if (listener != null) {
            statusListeners.add(listener);
        }
    }

    public void removeStatusListener(Funcs.VoidFunc1<AnimationStatus> listener) {
        statusListeners.remove(listener);
    }

    protected void notifyListeners() {
        for (Funcs.VoidFunc0 l : new ArrayList<Funcs.VoidFunc0>(listeners)) {
            l.call();
        }
    }

    protected void notifyStatusListeners(AnimationStatus s) {
        for (Funcs.VoidFunc1<AnimationStatus> l
                : new ArrayList<Funcs.VoidFunc1<AnimationStatus>>(statusListeners)) {
            l.call(s);
        }
    }

    /**
     * Drives {@code child} from this animation (which must produce doubles):
     * {@code controller.drive(tween)} == {@code tween.animate(controller)}.
     */
    @SuppressWarnings("unchecked")
    public <R> Animation<R> drive(Animatable<R> child) {
        return child.animate((Animation<Double>) (Animation<?>) this);
    }
}
