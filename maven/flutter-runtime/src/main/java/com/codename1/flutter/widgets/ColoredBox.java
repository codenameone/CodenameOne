package com.codename1.flutter.widgets;

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Paints a solid color behind its child — Flutter's {@code ColoredBox}. Sizes
 * to the child, or fills the incoming constraints when childless.
 */
public class ColoredBox extends Widget {

    private Color color;
    private Widget child;

    public void color(Color v) {
        this.color = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Color getColor() {
        return color;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new ColoredBoxRenderElement(this);
    }
}
