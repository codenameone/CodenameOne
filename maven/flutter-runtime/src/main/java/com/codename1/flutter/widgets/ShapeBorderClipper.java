package com.codename1.flutter.widgets;

import com.codename1.flutter.TextDirection;

/**
 * Adapts a {@code ShapeBorder} to the {@code CustomClipper} protocol consumed
 * by {@code PhysicalShape} — Flutter's {@code ShapeBorderClipper}. crane's
 * backdrop uses it to round the front layer's top corners. API-shape only for
 * this milestone; the shape is captured for the clipper to apply.
 */
public class ShapeBorderClipper {

    private Object shape;
    private TextDirection textDirection;

    public void shape(Object v) {
        this.shape = v;
    }

    public void textDirection(TextDirection v) {
        this.textDirection = v;
    }

    public Object getShape() {
        return shape;
    }

    public TextDirection getTextDirection() {
        return textDirection;
    }
}
