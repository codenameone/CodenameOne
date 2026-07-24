package com.codename1.flutter.rendering;

import com.codename1.flutter.Rect;

/**
 * The base of the render tree — Flutter's {@code RenderObject}. new_gallery
 * reaches one via {@code BuildContext.findRenderObject()} and casts it to
 * {@link RenderBox}. This is a structural stub: the Codename One runtime lays
 * out with its own {@link com.codename1.flutter.RenderElement} tree, so the
 * geometry accessors return neutral values until a later rendering milestone
 * wires them to the live layout.
 */
public class RenderObject {

    /** Whether this render object is attached to the render tree. */
    public boolean attached() {
        return false;
    }

    /** The bounds painted by this object, in its own coordinate space. */
    public Rect paintBounds() {
        return Rect.zero;
    }

    /** The bounds used for semantics, in its own coordinate space. */
    public Rect semanticBounds() {
        return Rect.zero;
    }

    /** Marks this object as needing a repaint (no-op in this milestone). */
    public void markNeedsPaint() {
    }

    /** Marks this object as needing layout (no-op in this milestone). */
    public void markNeedsLayout() {
    }
}
