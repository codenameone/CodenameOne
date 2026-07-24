package com.codename1.flutter;

/** A rectangular border with flattened (beveled) corners — Flutter's {@code BeveledRectangleBorder}. */
public class BeveledRectangleBorder extends OutlinedBorder {

    private Object borderRadius;

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public Object getBorderRadius() {
        return borderRadius;
    }
}
