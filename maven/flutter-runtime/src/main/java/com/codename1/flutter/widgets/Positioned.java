package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Positions a child of a {@link Stack} by insets from the stack's edges
 * and/or an explicit extent (all in logical pixels). Only has an effect when
 * its render element sits directly below a Stack.
 */
public class Positioned extends Widget {

    private Double left;
    private Double top;
    private Double right;
    private Double bottom;
    private Double width;
    private Double height;
    private Widget child;

    public void left(double v) {
        this.left = v;
    }

    public void top(double v) {
        this.top = v;
    }

    public void right(double v) {
        this.right = v;
    }

    public void bottom(double v) {
        this.bottom = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Double getLeft() {
        return left;
    }

    public Double getTop() {
        return top;
    }

    public Double getRight() {
        return right;
    }

    public Double getBottom() {
        return bottom;
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
        return new PositionedRenderElement(this);
    }
}
