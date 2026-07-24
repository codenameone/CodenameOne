package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsets;

/**
 * Material {@code CardThemeData}: the Material-3 rename of {@link CardTheme};
 * same write-once card styling shape.
 */
public class CardThemeData {

    private Color color;
    private Color shadowColor;
    private Color surfaceTintColor;
    private Double elevation;
    private EdgeInsets margin;
    private Object shape;
    private Object clipBehavior;

    public void color(Color v) {
        this.color = v;
    }

    public void shadowColor(Color v) {
        this.shadowColor = v;
    }

    public void surfaceTintColor(Color v) {
        this.surfaceTintColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void margin(EdgeInsets v) {
        this.margin = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void clipBehavior(Object v) {
        this.clipBehavior = v;
    }

    public Color color() {
        return color;
    }

    public Double elevation() {
        return elevation;
    }
}
