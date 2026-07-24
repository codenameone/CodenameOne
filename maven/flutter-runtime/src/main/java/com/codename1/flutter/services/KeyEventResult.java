package com.codename1.flutter.services;

/**
 * The result a key handler reports to the focus system, mirroring Flutter's
 * {@code KeyEventResult}. Returned from {@code Focus.onKeyEvent} handlers so
 * {@code return KeyEventResult.handled}/{@code ignored} transpiles directly.
 */
public enum KeyEventResult {
    /** The key event was handled; stop propagation. */
    handled,
    /** The key event was not handled; continue propagation. */
    ignored,
    /** Handled here, but skip remaining handlers in this node. */
    skipRemainingHandlers
}
