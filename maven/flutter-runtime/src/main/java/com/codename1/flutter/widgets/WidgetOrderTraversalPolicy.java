package com.codename1.flutter.widgets;

/**
 * Traverses focus in widget (tree) order — Flutter's {@code WidgetOrderTraversalPolicy}. Captured for API shape by
 * {@link FocusTraversalGroup}; live focus traversal is deferred to a later pass.
 */
public class WidgetOrderTraversalPolicy {

    private Object secondary;

    public WidgetOrderTraversalPolicy() {
    }

    public void secondary(Object v) {
        this.secondary = v;
    }
}
