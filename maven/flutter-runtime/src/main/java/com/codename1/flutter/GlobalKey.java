package com.codename1.flutter;

/**
 * A key that is unique across the entire app and provides access to the element,
 * state and context it is attached to ({@code GlobalKey<T>} in Flutter). The
 * gallery mostly uses global keys as stable identity tokens passed to widgets;
 * {@link #currentState()} / {@link #currentContext()} return the live targets
 * once the keyed widget is mounted (null until then).
 *
 * @param <T> the {@code State} (or other) type exposed via {@link #currentState()}
 */
public class GlobalKey<T> extends Key {

    private String debugLabel;
    private T state;
    private BuildContext context;
    private Widget widget;

    public GlobalKey() {
    }

    /** Named constructor parameter {@code debugLabel:}. */
    public void debugLabel(String label) {
        this.debugLabel = label;
    }

    public T currentState() {
        return state;
    }

    public BuildContext currentContext() {
        return context;
    }

    public Widget currentWidget() {
        return widget;
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    public void attach(T state, BuildContext context, Widget widget) {
        this.state = state;
        this.context = context;
        this.widget = widget;
    }

    public void detach() {
        this.state = null;
        this.context = null;
        this.widget = null;
    }
}
