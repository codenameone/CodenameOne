package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;

/**
 * The base of notifications that bubble up the widget tree — Flutter's
 * {@code Notification}. A subclass is dispatched with {@link #dispatch} from a
 * build context; an enclosing {@code NotificationListener} of a matching type
 * receives it. This pass captures the dispatch API shape.
 */
public class Notification {

    public Notification() {
    }

    /**
     * Sends this notification up the tree from {@code target}. Returns whether
     * it was consumed (always {@code false} until listener wiring lands).
     */
    public boolean dispatch(BuildContext target) {
        return false;
    }
}
