package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;
import com.codename1.flutter.navigation.Route;

import dart.runtime.Funcs;

/**
 * A route that slides an iOS modal popup / action sheet up from the bottom —
 * Flutter's {@code CupertinoModalPopupRoute}. Configuration only (the popup
 * {@code builder} and barrier options).
 *
 * @param <T> the value type the route completes with when popped
 */
public class CupertinoModalPopupRoute<T> extends Route<T> {

    private Funcs.Func1<BuildContext, Widget> builder;

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void settings(Object v) {
    }

    public void barrierColor(Color v) {
    }

    public void barrierDismissible(boolean v) {
    }

    public void barrierLabel(String v) {
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }
}
