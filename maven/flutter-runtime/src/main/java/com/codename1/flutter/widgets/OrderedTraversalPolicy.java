package com.codename1.flutter.widgets;

/**
 * Traverses focus in explicit FocusTraversalOrder — Flutter's {@code OrderedTraversalPolicy}. Captured for API shape by
 * {@link FocusTraversalGroup}; live focus traversal is deferred to a later pass.
 */
public class OrderedTraversalPolicy {

    private Object secondary;

    public OrderedTraversalPolicy() {
    }

    public void secondary(Object v) {
        this.secondary = v;
    }
}
