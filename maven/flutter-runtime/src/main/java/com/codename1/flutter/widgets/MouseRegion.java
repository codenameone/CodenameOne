package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Tracks the pointer as it enters/exits/moves over its child. Pointer hover is
 * a no-op on touch targets; the child renders unchanged. See
 * {@link PassThroughRenderElement}.
 */
public class MouseRegion extends Widget implements HasChild {

    private Object cursor;
    private boolean opaque = true;
    private Object onEnter;
    private Object onExit;
    private Object onHover;
    private Object hitTestBehavior;
    private Widget child;

    public void cursor(Object v) {
        this.cursor = v;
    }

    public void opaque(boolean v) {
        this.opaque = v;
    }

    public void onEnter(Object v) {
        this.onEnter = v;
    }

    public void onExit(Object v) {
        this.onExit = v;
    }

    public void onHover(Object v) {
        this.onHover = v;
    }

    public void hitTestBehavior(Object v) {
        this.hitTestBehavior = v;
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
