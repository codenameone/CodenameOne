package com.codename1.flutter.navigation;

import com.codename1.flutter.RestorableProperty;

import dart.runtime.Funcs;

/**
 * A restorable object that can imperatively push a route and complete with its
 * result ({@code RestorableRouteFuture<T>} in Flutter). The gallery constructs it
 * with an {@code onPresent} callback (which pushes a route on a
 * {@link NavigatorState}) and an optional {@code onComplete} callback, then calls
 * {@link #present(Object)} from button handlers.
 *
 * <p>Route restoration is not persisted; this holds the callbacks and drives them
 * within a single session. Because the surrounding {@code Navigator} static
 * helpers (owned by the navigation category) are needed to actually resolve a
 * {@link NavigatorState}, this is a minimal, API-complete implementation.</p>
 *
 * @param <T> the result type produced when the pushed route completes
 */
public class RestorableRouteFuture<T> extends RestorableProperty<Object> {

    private Funcs.Func2<NavigatorState, Object, String> onPresent;
    private Funcs.VoidFunc1<T> onComplete;
    private boolean present;

    public RestorableRouteFuture() {
    }

    /** Named constructor parameter {@code onPresent:} — pushes the route. */
    public void onPresent(Funcs.Func2<NavigatorState, Object, String> callback) {
        this.onPresent = callback;
    }

    /** Named constructor parameter {@code onComplete:} — receives the result. */
    public void onComplete(Funcs.VoidFunc1<T> callback) {
        this.onComplete = callback;
    }

    /** Imperatively present the route. Optional argument is forwarded to onPresent. */
    public void present(Object arguments) {
        this.present = true;
        notifyListeners();
    }

    public boolean isPresent() {
        return present;
    }

    public String route() {
        return null;
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    /** Deliver a route result to the onComplete callback (single-session use). */
    @SuppressWarnings("unchecked")
    void complete(Object result) {
        this.present = false;
        if (onComplete != null) {
            onComplete.call((T) result);
        }
        notifyListeners();
    }
}
