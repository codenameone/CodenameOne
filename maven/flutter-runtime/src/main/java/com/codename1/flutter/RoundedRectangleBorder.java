package com.codename1.flutter;

/**
 * A rectangular border with rounded corners — Flutter's
 * {@code RoundedRectangleBorder}. {@code borderRadius} accepts a
 * {@link BorderRadius}/{@link BorderRadiusDirectional} (held as an opaque value
 * for this milestone).
 */
public class RoundedRectangleBorder extends OutlinedBorder {

    private Object borderRadius;

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public Object getBorderRadius() {
        return borderRadius;
    }
}
