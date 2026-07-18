package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;

/**
 * Imposes additional {@link BoxConstraints} (logical pixels) on its child,
 * intersected with the incoming constraints.
 */
public class ConstrainedBox extends Widget {

    private BoxConstraints constraints;
    private Widget child;

    public void constraints(BoxConstraints v) {
        this.constraints = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public BoxConstraints getConstraints() {
        return constraints;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new ConstrainedBoxRenderElement(this);
    }
}
