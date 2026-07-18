package com.codename1.flutter;

/**
 * Base class for widget keys. Keys control how the element reconciler matches
 * widgets across rebuilds: two widgets can only update the same element when
 * their runtime class matches and their keys are equal.
 */
public abstract class Key {
    protected Key() {
    }
}
