package com.codename1.flutter;

import dart.runtime.Funcs;

/**
 * Mutable state for a {@link StatefulWidget}. Owned by a
 * {@link StatefulElement}: created on mount, retargeted at new widget
 * instances on update (with {@link #didUpdateWidget}), disposed on unmount.
 */
public abstract class State<T extends StatefulWidget> {

    StatefulElement element;
    private T widgetValue;

    /**
     * The current widget configuration for this state.
     */
    public T widget() {
        return widgetValue;
    }

    /**
     * The location of this state's widget in the element tree.
     */
    public BuildContext context() {
        return element;
    }

    /**
     * Runs {@code fn} (which mutates fields of this state) and schedules a
     * rebuild of this element for the next frame.
     */
    public void setState(Funcs.VoidFunc0 fn) {
        FlutterUI.assertEdt();
        if (fn != null) {
            fn.call();
        }
        if (element != null) {
            element.markNeedsBuild();
        }
    }

    /**
     * Called once when the element is first mounted, before the first build.
     */
    public void initState() {
    }

    /**
     * Called when the element absorbed a new widget configuration. The new
     * widget is already available via {@link #widget()}.
     */
    public void didUpdateWidget(T oldWidget) {
    }

    /**
     * Called when the element is removed from the tree permanently.
     */
    public void dispose() {
    }

    public abstract Widget build(BuildContext context);

    // ------------------------------------------------------------------
    // Framework plumbing (package private)
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    void attach(StatefulElement element, StatefulWidget widget) {
        this.element = element;
        this.widgetValue = (T) widget;
    }

    @SuppressWarnings("unchecked")
    void updateWidget(StatefulWidget widget) {
        this.widgetValue = (T) widget;
    }

    @SuppressWarnings("unchecked")
    void invokeDidUpdateWidget(StatefulWidget oldWidget) {
        didUpdateWidget((T) oldWidget);
    }

    void detach() {
        this.element = null;
    }
}
