package com.codename1.flutter.rendering;

import com.codename1.flutter.Canvas;
import com.codename1.flutter.semantics.SemanticsBuilderCallback;

/**
 * Base class an application implements to paint directly onto a {@link Canvas}
 * — Flutter's {@code CustomPainter}. Subclasses override {@link #paint} to draw
 * and {@link #shouldRepaint} to decide when a re-paint is required. The
 * optional {@code repaint} listenable (a Listenable that triggers repaints) is
 * captured for API shape.
 */
public abstract class CustomPainter {

    private Object repaint;

    public CustomPainter() {
    }

    public void repaint(Object v) {
        this.repaint = v;
    }

    /**
     * Draws this painter's content within a box of the given {@code size}.
     */
    public abstract void paint(Canvas canvas, Size size);

    /**
     * Whether a repaint is needed when the delegate is replaced by
     * {@code oldDelegate}. Dart subclasses narrow the parameter type
     * ({@code covariant}), which becomes an overload rather than an override in
     * Java; this default keeps the base concrete so those subclasses compile.
     */
    public boolean shouldRepaint(CustomPainter oldDelegate) {
        return true;
    }

    /**
     * Returns the callback that produces this painter's accessibility nodes, or
     * {@code null} when the painter contributes no custom semantics — Flutter's
     * {@code CustomPainter.semanticsBuilder}. Painters that annotate their
     * drawing for screen readers (e.g. the Rally line chart) override this to
     * return a {@link SemanticsBuilderCallback}.
     */
    public SemanticsBuilderCallback semanticsBuilder() {
        return null;
    }
}
