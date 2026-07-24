package com.codename1.flutter;

/** A rectangular border with continuous, smoothly-tapered corners — Flutter's {@code ContinuousRectangleBorder}. */
public class ContinuousRectangleBorder extends OutlinedBorder {

    private Object borderRadius;

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public Object getBorderRadius() {
        return borderRadius;
    }
}
