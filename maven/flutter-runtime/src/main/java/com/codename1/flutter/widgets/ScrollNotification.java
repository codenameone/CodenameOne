package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.rendering.ScrollDirection;

/**
 * A notification that bubbles up the widget tree as a scrollable scrolls —
 * Flutter's {@code ScrollNotification}. The reply adaptive-nav reads
 * {@link #direction()} and the nesting {@code depth} to drive the bottom app
 * bar. Instances are produced by the scroll machinery; this pass captures the
 * inspected shape.
 */
public class ScrollNotification {

    private ScrollMetrics metrics;
    private long depth;
    private BuildContext context;
    private ScrollDirection direction;

    public ScrollMetrics metrics() {
        return metrics;
    }

    public void metrics(ScrollMetrics v) {
        this.metrics = v;
    }

    /** The number of scrollables this notification has bubbled through. */
    public long get$depth() {
        return depth;
    }

    public void depth(long v) {
        this.depth = v;
    }

    public BuildContext context() {
        return context;
    }

    public void context(BuildContext v) {
        this.context = v;
    }

    public ScrollDirection direction() {
        return direction;
    }

    public void direction(ScrollDirection v) {
        this.direction = v;
    }

    /** Dispatches this notification up to the nearest ancestor listener. */
    public boolean dispatch(BuildContext target) {
        return false;
    }
}
