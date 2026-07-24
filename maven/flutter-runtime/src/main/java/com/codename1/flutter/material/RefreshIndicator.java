package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

import dart.runtime.Funcs;

/**
 * Wraps a scrollable to add pull-to-refresh — Flutter's
 * {@code RefreshIndicator}. This milestone renders the {@code child}; the
 * overscroll gesture that triggers {@code onRefresh} (a {@code Future}-returning
 * callback) is deferred.
 */
public class RefreshIndicator extends StatelessWidget {

    private Widget child;
    private Funcs.Func0<Object> onRefresh;

    public void child(Widget v) {
        this.child = v;
    }

    public void displacement(double v) {
    }

    public void onRefresh(Funcs.Func0<Object> v) {
        this.onRefresh = v;
    }

    public void color(Color v) {
    }

    public void backgroundColor(Color v) {
    }

    public void strokeWidth(double v) {
    }

    public void notificationPredicate(Object v) {
    }

    public void semanticsLabel(String v) {
    }

    public void semanticsValue(String v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}
