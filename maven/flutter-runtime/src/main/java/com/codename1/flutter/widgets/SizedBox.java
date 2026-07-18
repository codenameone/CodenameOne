package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * A box with a fixed width and/or height (logical pixels). Without a child
 * it is a fixed-size spacer; with a child it tightens the child to the given
 * dimensions.
 */
public class SizedBox extends Widget {

    private Double width;
    private Double height;
    private Widget child;

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Double getWidth() {
        return width;
    }

    public Double getHeight() {
        return height;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new SizedBoxRenderElement(this);
    }
}
