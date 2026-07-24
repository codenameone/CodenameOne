package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;
import com.codename1.flutter.navigation.Route;

import dart.runtime.Funcs;

/**
 * A route that shows an iOS alert dialog — Flutter's {@code CupertinoDialogRoute}.
 * Configuration only (the page {@code builder} and barrier options); it is
 * handed to the navigator's {@code restorablePush}, which drives presentation.
 *
 * @param <T> the value type the route completes with when popped
 */
public class CupertinoDialogRoute<T> extends Route<T> {

    private BuildContext context;
    private Funcs.Func1<BuildContext, Widget> builder;

    public void context(BuildContext v) {
        this.context = v;
    }

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void settings(Object v) {
    }

    public void barrierDismissible(boolean v) {
    }

    public void barrierColor(Color v) {
    }

    public void barrierLabel(String v) {
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }

    public BuildContext getContext() {
        return context;
    }
}
