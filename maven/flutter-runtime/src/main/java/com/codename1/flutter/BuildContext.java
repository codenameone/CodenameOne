package com.codename1.flutter;

/**
 * A handle to the location of a widget in the element tree. Implemented by
 * {@link Element}. Passed to build methods so widgets can look up inherited
 * configuration (e.g. {@code Theme.of(context)}).
 */
public interface BuildContext {

    /**
     * Walks up the element tree and returns the nearest ancestor widget whose
     * runtime class is exactly {@code widgetType}, or null when there is none.
     */
    <W extends Widget> W findAncestorWidgetOfExactType(Class<W> widgetType);
}
