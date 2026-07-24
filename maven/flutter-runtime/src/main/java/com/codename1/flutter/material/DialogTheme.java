package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.TextStyle;

/**
 * Material {@code DialogTheme}: write-once dialog styling. Named Dart
 * constructor parameters map to setter methods; unset values stay null.
 */
public class DialogTheme {

    private Color backgroundColor;
    private Double elevation;
    private Color shadowColor;
    private Color surfaceTintColor;
    private Object shape;
    private Object alignment;
    private TextStyle titleTextStyle;
    private TextStyle contentTextStyle;
    private Object iconColor;

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void shadowColor(Color v) {
        this.shadowColor = v;
    }

    public void surfaceTintColor(Color v) {
        this.surfaceTintColor = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void alignment(Object v) {
        this.alignment = v;
    }

    public void titleTextStyle(TextStyle v) {
        this.titleTextStyle = v;
    }

    public void contentTextStyle(TextStyle v) {
        this.contentTextStyle = v;
    }

    public void iconColor(Object v) {
        this.iconColor = v;
    }

    public Color backgroundColor() {
        return backgroundColor;
    }
}
