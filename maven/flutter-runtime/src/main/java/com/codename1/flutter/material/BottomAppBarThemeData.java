package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsets;

/**
 * Material {@code BottomAppBarThemeData}: write-once bottom-app-bar styling.
 */
public class BottomAppBarThemeData {

    private Color color;
    private Color surfaceTintColor;
    private Color shadowColor;
    private Double elevation;
    private Double height;
    private EdgeInsets padding;
    private Object shape;

    public void color(Color v) {
        this.color = v;
    }

    public void surfaceTintColor(Color v) {
        this.surfaceTintColor = v;
    }

    public void shadowColor(Color v) {
        this.shadowColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public Color color() {
        return color;
    }

    public Double elevation() {
        return elevation;
    }
}
