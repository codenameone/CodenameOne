package com.codename1.flutter;

/**
 * A {@link ShapeBorder} that draws a uniform {@link BorderSide} outline around
 * a closed shape — Flutter's {@code OutlinedBorder}.
 */
public abstract class OutlinedBorder extends ShapeBorder {

    BorderSide side = BorderSide.none;

    public void side(BorderSide v) {
        this.side = v == null ? BorderSide.none : v;
    }

    public BorderSide getSide() {
        return side;
    }
}
