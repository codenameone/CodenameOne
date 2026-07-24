package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
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

    /**
     * {@code Positioned.fill}: pins the child to all four edges of the stack
     * (each unspecified inset defaults to 0), so it fills the stack.
     */
    public static Positioned fill(Key key, Double left, Double top, Double right, Double bottom,
            Widget child) {
        Positioned p = new Positioned();
        p.key(key);
        p.left(left == null ? 0 : left);
        p.top(top == null ? 0 : top);
        p.right(right == null ? 0 : right);
        p.bottom(bottom == null ? 0 : bottom);
        if (child != null) {
            p.child(child);
        }
        return p;
    }

    @Override
    public Element createElement() {
        return new PositionedRenderElement(this);
    }
}
