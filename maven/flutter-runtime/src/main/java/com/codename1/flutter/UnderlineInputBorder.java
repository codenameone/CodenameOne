package com.codename1.flutter;

/** A single underline drawn beneath a Material text field — Flutter's {@code UnderlineInputBorder}. */
public class UnderlineInputBorder extends InputBorder {

    private Object borderRadius;

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public Object getBorderRadius() {
        return borderRadius;
    }
}
