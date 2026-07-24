package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Adds a scrollbar to a scrollable child. Codename One draws its own
 * scrollbars, so this renders the child unchanged. See
 * {@link PassThroughRenderElement}.
 */
public class Scrollbar extends Widget implements HasChild {

    private Object controller;
    private boolean thumbVisibility;
    private boolean trackVisibility;
    private double thickness;
    private Object radius;
    private boolean interactive;
    private Object notificationPredicate;
    private Object scrollbarOrientation;
    private Widget child;

    public void controller(Object v) {
        this.controller = v;
    }

    public void thumbVisibility(boolean v) {
        this.thumbVisibility = v;
    }

    public void trackVisibility(boolean v) {
        this.trackVisibility = v;
    }

    public void thickness(double v) {
        this.thickness = v;
    }

    public void radius(Object v) {
        this.radius = v;
    }

    public void interactive(boolean v) {
        this.interactive = v;
    }

    public void notificationPredicate(Object v) {
        this.notificationPredicate = v;
    }

    public void scrollbarOrientation(Object v) {
        this.scrollbarOrientation = v;
    }

    public void child(Widget v) {
        this.child = v;
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
