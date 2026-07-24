package com.codename1.flutter.navigation;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A route whose page is produced by a {@code WidgetBuilder}. Pushed with
 * {@link Navigator#push}; the builder runs lazily when the route's element
 * tree mounts, receiving a BuildContext inside the NEW page's tree.
 *
 * <p>The type parameter {@code T} is the route's result type (the value a
 * {@code Navigator.pop(result)} returns). It is phantom in this runtime but
 * lets transpiled {@code MaterialPageRoute<T>} subclasses and type arguments
 * resolve.</p>
 *
 * @param <T> the route's pop-result type
 */
public class MaterialPageRoute<T> extends Route<T> {

    private Funcs.Func1<BuildContext, Widget> builder;
    private Object maintainState;
    private Object fullscreenDialog;

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    /** Flutter's {@code maintainState} — whether the route stays mounted when covered. */
    public void maintainState(Object v) {
        this.maintainState = v;
    }

    /** Flutter's {@code fullscreenDialog} — whether the route is a full-screen modal. */
    public void fullscreenDialog(Object v) {
        this.fullscreenDialog = v;
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }
}
