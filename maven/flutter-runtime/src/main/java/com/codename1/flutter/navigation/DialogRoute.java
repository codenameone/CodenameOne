package com.codename1.flutter.navigation;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A modal route that displays a dialog above the current page — Flutter's
 * {@code DialogRoute<T>}. Pushed onto the {@link Navigator}; the {@code builder}
 * produces the dialog content lazily when the route is shown. This pass records
 * the builder, barrier appearance and settings for API shape; the actual modal
 * presentation is handled by the navigation layer.
 *
 * @param <T> the value the route completes with when popped
 */
public class DialogRoute<T> extends Route<T> {

    private BuildContext context;
    private Funcs.Func1<BuildContext, Widget> builder;
    private Object settings;
    private Color barrierColor;
    private boolean barrierDismissible = true;
    private String barrierLabel;
    private boolean useSafeArea = true;

    public void context(BuildContext v) {
        this.context = v;
    }

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void settings(Object v) {
        this.settings = v;
    }

    public void barrierColor(Color v) {
        this.barrierColor = v;
    }

    public void barrierDismissible(boolean v) {
        this.barrierDismissible = v;
    }

    public void barrierLabel(String v) {
        this.barrierLabel = v;
    }

    public void useSafeArea(boolean v) {
        this.useSafeArea = v;
    }

    public void themes(Object v) {
    }

    public void anchorPoint(Object v) {
    }

    public void traversalEdgeBehavior(Object v) {
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }
}
