package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Paints a {@link com.codename1.flutter.Decoration} (a
 * {@link com.codename1.flutter.BoxDecoration} in practice) around its child —
 * Flutter's {@code DecoratedBox}. Sizes to the child.
 */
public class DecoratedBox extends Widget {

    private Object decoration;
    private Object position;
    private Widget child;

    public void decoration(Object v) {
        this.decoration = v;
    }

    public void position(Object v) {
        this.position = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getDecoration() {
        return decoration;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new DecoratedBoxRenderElement(this);
    }
}
