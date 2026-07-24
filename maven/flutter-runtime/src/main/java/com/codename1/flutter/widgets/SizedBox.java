package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Size;

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

    /**
     * {@code SizedBox.shrink}: a zero-size box (a minimal spacer / placeholder).
     */
    public static SizedBox shrink(Key key, Widget child) {
        SizedBox b = new SizedBox();
        b.key(key);
        b.width(0);
        b.height(0);
        if (child != null) {
            b.child(child);
        }
        return b;
    }

    /**
     * {@code SizedBox.expand}: a box that expands to fill its parent (infinite
     * width and height).
     */
    public static SizedBox expand(Key key, Widget child) {
        SizedBox b = new SizedBox();
        b.key(key);
        b.width(Double.POSITIVE_INFINITY);
        b.height(Double.POSITIVE_INFINITY);
        if (child != null) {
            b.child(child);
        }
        return b;
    }

    /**
     * {@code SizedBox.fromSize}: a box tightened to the given {@link Size}.
     */
    public static SizedBox fromSize(Key key, Size size, Widget child) {
        SizedBox b = new SizedBox();
        b.key(key);
        if (size != null) {
            b.width(size.width());
            b.height(size.height());
        }
        if (child != null) {
            b.child(child);
        }
        return b;
    }

    @Override
    public Element createElement() {
        return new SizedBoxRenderElement(this);
    }
}
