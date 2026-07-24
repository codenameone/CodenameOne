package com.codename1.flutter.semantics;

import com.codename1.flutter.Key;
import com.codename1.flutter.Rect;

/**
 * A single accessibility node emitted by a {@code CustomPainter} —  Flutter's
 * {@code CustomPainterSemantics}. It maps a {@link Rect} region of the painted
 * surface to a set of semantic {@code properties} (a
 * {@code SemanticsProperties}). The Rally line chart emits one per data group so
 * screen readers can announce each day's balance. {@code transform} and
 * {@code tags} are captured for API shape.
 */
public class CustomPainterSemantics {

    private Rect rect;
    private Object properties;
    private Object transform;
    private Object tags;
    private Key key;

    public CustomPainterSemantics() {
    }

    public void rect(Rect v) {
        this.rect = v;
    }

    public void properties(Object v) {
        this.properties = v;
    }

    public void transform(Object v) {
        this.transform = v;
    }

    public void tags(Object v) {
        this.tags = v;
    }

    public void key(Key v) {
        this.key = v;
    }

    public Rect getRect() {
        return rect;
    }

    public Object getProperties() {
        return properties;
    }

    public Key getKey() {
        return key;
    }
}
