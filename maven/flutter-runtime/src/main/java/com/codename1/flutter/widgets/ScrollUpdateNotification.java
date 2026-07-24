package com.codename1.flutter.widgets;

/**
 * A {@link ScrollNotification} fired as the scroll offset changes — Flutter's
 * {@code ScrollUpdateNotification}, carrying the {@link #scrollDelta()} since the
 * previous update.
 */
public class ScrollUpdateNotification extends ScrollNotification {

    private Double scrollDelta;

    public Double scrollDelta() {
        return scrollDelta;
    }

    public void scrollDelta(Double v) {
        this.scrollDelta = v;
    }
}
