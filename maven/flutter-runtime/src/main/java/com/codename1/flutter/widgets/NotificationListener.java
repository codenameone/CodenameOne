package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Listens for a {@link Notification} bubbling up from its subtree — Flutter's
 * {@code NotificationListener<T>}. new_gallery only ever listens for scroll
 * notifications, so {@code onNotification} is typed against
 * {@link ScrollNotification}; the return value ({@code true} to stop the
 * notification bubbling) is captured. Structural pass-through for this
 * milestone: the {@code child} renders unchanged.
 *
 * @param <T> the notification type (erased at this pass)
 */
public class NotificationListener<T> extends Widget implements HasChild {

    private Funcs.Func1<ScrollNotification, Boolean> onNotification;
    private Widget child;

    public void onNotification(Funcs.Func1<ScrollNotification, Boolean> v) {
        this.onNotification = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Funcs.Func1<ScrollNotification, Boolean> getOnNotification() {
        return onNotification;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
