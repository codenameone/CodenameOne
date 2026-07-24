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

    /**
     * Walks up the element tree and returns the nearest ancestor widget that is
     * an instance of {@code type} (Flutter's InheritedWidget dependency lookup),
     * or null when there is none. The {@code <T>} type witness the Dart call
     * carries is threaded here as {@code type} by the transpiler.
     */
    <W extends Widget> W dependOnInheritedWidgetOfExactType(Class<W> type);

    /**
     * The no-type-argument form ({@code context.dependOnInheritedWidgetOfExactType()}), where Dart
     * infers the widget type from the surrounding context. Java infers {@code W} from the call's
     * target type. Not tree-walked at this milestone — returns null.
     */
    default <W extends Widget> W dependOnInheritedWidgetOfExactType() {
        return null;
    }

    /**
     * Walks up the element tree and returns the nearest ancestor {@code State}
     * of the given type ({@code BuildContext.findAncestorStateOfType}), or null.
     * Not tree-walked at this milestone — returns null.
     */
    default <T> T findAncestorStateOfType(Class<T> type) {
        return null;
    }

    /**
     * provider's {@code context.watch<T>()}: the nearest ancestor-provided value
     * assignable to {@code type} (rebuild-on-change is not modeled in this pass).
     */
    <T> T watch(Class<T> type);

    /**
     * provider's {@code context.read<T>()}: the nearest ancestor-provided value
     * assignable to {@code type}, without subscribing to changes.
     */
    <T> T read(Class<T> type);

    /**
     * The nearest value published by an ancestor {@link InheritedValueProvider}
     * (Provider / ScopedModel) that is assignable to {@code type}, or null.
     */
    Object providerValueOfType(Class<?> type);

    /**
     * Whether the element backing this context is still in the tree
     * ({@code BuildContext.mounted}). Elements override this; the default is
     * {@code true} for lightweight contexts that never detach.
     */
    default boolean mounted() {
        return true;
    }

    /**
     * The render object for this context ({@code BuildContext.findRenderObject}).
     * Not modelled at this milestone — returns null.
     */
    default Object findRenderObject() {
        return null;
    }
}
