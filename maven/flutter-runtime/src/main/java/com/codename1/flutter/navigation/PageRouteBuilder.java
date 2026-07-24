package com.codename1.flutter.navigation;

import com.codename1.flutter.Color;

import dart.core.Duration;

/**
 * A {@link Route} whose page and transition are supplied by builder callbacks —
 * Flutter's {@code PageRouteBuilder}. {@code pageBuilder} produces the
 * destination widget and {@code transitionsBuilder} wraps it in the animated
 * transition; both receive the {@code (context, animation, secondaryAnimation)}
 * triple. This pass records the callbacks and route configuration; driving the
 * transition animation is deferred.
 *
 * @param <T> the value the route completes with when popped
 */
public class PageRouteBuilder<T> extends Route<T> {

    private RouteSettings settings;
    private Object pageBuilder;
    private Object transitionsBuilder;
    private Duration transitionDuration;
    private Duration reverseTransitionDuration;
    private boolean opaque = true;
    private boolean barrierDismissible;
    private Color barrierColor;
    private String barrierLabel;
    private boolean maintainState = true;
    private boolean fullscreenDialog;

    public void settings(RouteSettings v) {
        this.settings = v;
    }

    public void pageBuilder(dart.runtime.Funcs.Func3<com.codename1.flutter.BuildContext,
            com.codename1.flutter.animation.Animation<Double>,
            com.codename1.flutter.animation.Animation<Double>, com.codename1.flutter.Widget> v) {
        this.pageBuilder = v;
    }

    public void transitionsBuilder(dart.runtime.Funcs.Func4<com.codename1.flutter.BuildContext,
            com.codename1.flutter.animation.Animation<Double>,
            com.codename1.flutter.animation.Animation<Double>,
            com.codename1.flutter.Widget, com.codename1.flutter.Widget> v) {
        this.transitionsBuilder = v;
    }

    public void transitionDuration(Duration v) {
        this.transitionDuration = v;
    }

    public void reverseTransitionDuration(Duration v) {
        this.reverseTransitionDuration = v;
    }

    public void opaque(boolean v) {
        this.opaque = v;
    }

    public void barrierDismissible(boolean v) {
        this.barrierDismissible = v;
    }

    public void barrierColor(Color v) {
        this.barrierColor = v;
    }

    public void barrierLabel(String v) {
        this.barrierLabel = v;
    }

    public void maintainState(boolean v) {
        this.maintainState = v;
    }

    public void fullscreenDialog(boolean v) {
        this.fullscreenDialog = v;
    }

    public RouteSettings getSettings() {
        return settings;
    }

    public Object getPageBuilder() {
        return pageBuilder;
    }

    public Object getTransitionsBuilder() {
        return transitionsBuilder;
    }

    public Duration getTransitionDuration() {
        return transitionDuration;
    }
}
