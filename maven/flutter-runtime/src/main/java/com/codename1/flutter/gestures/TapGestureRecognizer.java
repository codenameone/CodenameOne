package com.codename1.flutter.gestures;

import dart.runtime.Funcs;

/**
 * Recognizes single taps — Flutter's {@code TapGestureRecognizer}. The about
 * page wires one to each link span, so only {@code onTap} and {@code dispose}
 * are consumed.
 */
public final class TapGestureRecognizer {

    private Funcs.VoidFunc0 onTap;
    private Object debugOwner;

    public TapGestureRecognizer() {
    }

    public TapGestureRecognizer(Object debugOwner) {
        this.debugOwner = debugOwner;
    }

    /** Dart's {@code set onTap(VoidCallback? handler)}. */
    public void onTap(Funcs.VoidFunc0 handler) {
        this.onTap = handler;
    }

    /** The registered tap handler, if any. */
    public Funcs.VoidFunc0 onTap() {
        return onTap;
    }

    /** Invoke the registered handler (used when the host wires a real tap). */
    public void handleTap() {
        if (onTap != null) {
            onTap.call();
        }
    }

    public void dispose() {
        this.onTap = null;
    }
}
