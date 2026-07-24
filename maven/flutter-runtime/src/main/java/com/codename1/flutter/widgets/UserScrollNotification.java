package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.rendering.ScrollDirection;

/**
 * A {@link ScrollNotification} fired when the user starts or stops dragging,
 * carrying the new scroll {@link #direction()} — Flutter's
 * {@code UserScrollNotification}.
 */
public class UserScrollNotification extends ScrollNotification {

    public UserScrollNotification() {
    }

    public void context(BuildContext v) {
        super.context(v);
    }

    public void metrics(ScrollMetrics v) {
        super.metrics(v);
    }

    public void direction(ScrollDirection v) {
        super.direction(v);
    }
}
