package com.codename1.flutter;

/**
 * Base class of the widget hierarchy. Widgets are write-once configuration
 * objects: transpiled Dart code allocates a widget, calls its named-parameter
 * setter methods, and hands it to the framework. The element tree (see
 * {@link Element}) is the retained structure; widgets are cheap descriptions
 * that are diffed against the previous configuration on every rebuild.
 */
public abstract class Widget {
    private Key key;

    /**
     * Named parameter setter for the Dart {@code key:} parameter.
     */
    public void key(Key v) {
        this.key = v;
    }

    public Key getKey() {
        return key;
    }

    /**
     * Flutter's Widget.canUpdate: an existing element can absorb a new widget
     * when the runtime type and key both match.
     */
    public static boolean canUpdate(Widget oldWidget, Widget newWidget) {
        if (oldWidget == null || newWidget == null) {
            return false;
        }
        return oldWidget.getClass() == newWidget.getClass()
                && eq(oldWidget.getKey(), newWidget.getKey());
    }

    static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Inflates this widget's configuration into an element. Framework widget
     * subclasses supply this; application widgets inherit it from
     * StatelessWidget/StatefulWidget.
     */
    public abstract Element createElement();
}
