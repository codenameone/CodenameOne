package com.codename1.flutter.widgets;

/**
 * Traverses focus in reading order for the ambient text direction — Flutter's {@code ReadingOrderTraversalPolicy}. Captured for API shape by
 * {@link FocusTraversalGroup}; live focus traversal is deferred to a later pass.
 */
public class ReadingOrderTraversalPolicy {

    private Object secondary;

    public ReadingOrderTraversalPolicy() {
    }

    public void secondary(Object v) {
        this.secondary = v;
    }
}
