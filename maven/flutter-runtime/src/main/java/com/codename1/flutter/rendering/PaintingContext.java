package com.codename1.flutter.rendering;

import com.codename1.flutter.Canvas;
import com.codename1.flutter.Offset;
import com.codename1.flutter.Rect;

/**
 * The canvas + child-painting handle passed to {@code RenderObject.paint} —
 * Flutter's {@code PaintingContext}. The sliders demo's custom slider shapes
 * read {@link #canvas()} to draw the thumb / value indicator.
 */
public class PaintingContext {

    private final Canvas canvas;
    private final Rect estimatedBounds;

    public PaintingContext() {
        this(new Canvas(), Rect.zero);
    }

    public PaintingContext(Canvas canvas, Rect estimatedBounds) {
        this.canvas = canvas;
        this.estimatedBounds = estimatedBounds;
    }

    /** The canvas onto which painting should be done. */
    public Canvas canvas() {
        return canvas;
    }

    /** Paints a child render object at the given offset (no-op stub). */
    public void paintChild(RenderObject child, Offset offset) {
    }

    /** An estimate of the bounds within which painting will happen. */
    public Rect estimatedBounds() {
        return estimatedBounds;
    }
}
