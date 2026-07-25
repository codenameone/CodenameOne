package com.codename1.flutter.widgets;

import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.CustomPainter;
import com.codename1.flutter.rendering.Size;

/**
 * Provides a canvas for a {@link CustomPainter} to paint on, behind and/or in
 * front of an optional {@code child} — Flutter's {@code CustomPaint}. The
 * painters run against a Codename One {@code Graphics} through
 * {@link com.codename1.flutter.rendering.GraphicsCanvas}; see
 * {@link CustomPaintRenderElement}.
 */
public class CustomPaint extends Widget {

    private CustomPainter painter;
    private CustomPainter foregroundPainter;
    private Size size;
    private Widget child;

    public void painter(CustomPainter v) {
        this.painter = v;
    }

    public void foregroundPainter(CustomPainter v) {
        this.foregroundPainter = v;
    }

    public void size(Size v) {
        this.size = v;
    }

    public void isComplex(boolean v) {
    }

    public void willChange(boolean v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public CustomPainter getPainter() {
        return painter;
    }

    /** The painter drawn OVER the child — Flutter's {@code foregroundPainter}. */
    public CustomPainter getForegroundPainter() {
        return foregroundPainter;
    }

    /** The box the painter asks for when there is no child, in logical pixels. */
    public Size getSize() {
        return size;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new CustomPaintRenderElement(this);
    }
}
