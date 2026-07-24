package com.codename1.flutter;

/**
 * The border drawn around a Material text field — Flutter's {@code InputBorder}.
 */
public abstract class InputBorder extends ShapeBorder {

    /** {@code InputBorder.none}: the "no border" sentinel. */
    public static final InputBorder none = new NoInputBorder();

    BorderSide borderSide = BorderSide.none;

    public void borderSide(BorderSide v) {
        this.borderSide = v == null ? BorderSide.none : v;
    }

    public BorderSide getBorderSide() {
        return borderSide;
    }
}
