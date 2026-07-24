package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A widget that can be dismissed by dragging — Flutter's {@code Dismissible}.
 * This milestone renders the {@code child}; the swipe-to-dismiss gesture, the
 * reveal of {@code background}/{@code secondaryBackground} and the resize
 * animation are deferred. {@code onDismissed} carries a {@link DismissDirection}.
 */
public class Dismissible extends StatelessWidget {

    private Widget child;
    private Widget background;
    private Widget secondaryBackground;
    private Funcs.VoidFunc1<DismissDirection> onDismissed;
    private Funcs.Func1<DismissDirection, Object> confirmDismiss;
    private Funcs.VoidFunc0 onResize;
    private Object direction;
    private Object dismissThresholds;

    public void child(Widget v) {
        this.child = v;
    }

    public void background(Widget v) {
        this.background = v;
    }

    public void secondaryBackground(Widget v) {
        this.secondaryBackground = v;
    }

    public void confirmDismiss(Funcs.Func1<DismissDirection, Object> v) {
        this.confirmDismiss = v;
    }

    public void onResize(Funcs.VoidFunc0 v) {
        this.onResize = v;
    }

    public void onUpdate(Object v) {
    }

    public void onDismissed(Funcs.VoidFunc1<DismissDirection> v) {
        this.onDismissed = v;
    }

    public void direction(Object v) {
        this.direction = v;
    }

    public void resizeDuration(Object v) {
    }

    public void dismissThresholds(Object v) {
        this.dismissThresholds = v;
    }

    public void movementDuration(Object v) {
    }

    public void crossAxisEndOffset(double v) {
    }

    public void dragStartBehavior(Object v) {
    }

    public void behavior(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}
