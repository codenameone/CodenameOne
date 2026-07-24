package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Marks a subtree as a shared-element that flies between routes during a
 * navigation transition — Flutter's {@code Hero}. This milestone renders the
 * {@code child} in place; the cross-route flight animation is deferred.
 */
public class Hero extends StatelessWidget {

    private Object tag;
    private Widget child;
    private boolean transitionOnUserGestures;

    public void tag(Object v) {
        this.tag = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public void createRectTween(Object v) {
    }

    public void flightShuttleBuilder(Object v) {
    }

    public void placeholderBuilder(Object v) {
    }

    public void transitionOnUserGestures(boolean v) {
        this.transitionOnUserGestures = v;
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}
