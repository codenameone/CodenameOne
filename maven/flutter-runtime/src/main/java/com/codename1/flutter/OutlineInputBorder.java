package com.codename1.flutter;

/** A rounded-rectangle outline drawn around a Material text field — Flutter's {@code OutlineInputBorder}. */
public class OutlineInputBorder extends InputBorder {

    private Object borderRadius;
    private double gapPadding = 4.0;

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public void gapPadding(double v) {
        this.gapPadding = v;
    }

    public Object getBorderRadius() {
        return borderRadius;
    }

    public double getGapPadding() {
        return gapPadding;
    }

    // Dart-getter-named accessors the shrine study's CutCornersBorder reads off
    // `this`/`super` (Dart `get borderSide` / `borderRadius` / `gapPadding`).

    public BorderSide borderSide() {
        return borderSide;
    }

    public BorderRadius borderRadius() {
        return borderRadius instanceof BorderRadius ? (BorderRadius) borderRadius : null;
    }

    public double gapPadding() {
        return gapPadding;
    }

    /**
     * Dart's {@code ShapeBorder.lerpFrom} / {@code lerpTo}: interpolate this
     * border to/from another. The base outline has no distinctive geometry to
     * blend here, so the fallback returns null (subclasses like CutCornersBorder
     * override with their own blend).
     */
    public ShapeBorder lerpFrom(ShapeBorder a, double t) {
        return null;
    }

    public ShapeBorder lerpTo(ShapeBorder b, double t) {
        return null;
    }
}
