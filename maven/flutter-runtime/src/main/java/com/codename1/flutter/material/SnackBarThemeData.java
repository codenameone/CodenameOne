package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.TextStyle;

/**
 * Material {@code SnackBarThemeData}: write-once snack-bar styling. Named Dart
 * constructor parameters map to setter methods; unset values stay null.
 */
public class SnackBarThemeData {

    private Color backgroundColor;
    private Color actionTextColor;
    private Color disabledActionTextColor;
    private TextStyle contentTextStyle;
    private Double elevation;
    private Object shape;
    private SnackBarBehavior behavior;
    private Double width;
    private Object insetPadding;
    private Boolean showCloseIcon;
    private Color closeIconColor;

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void actionTextColor(Color v) {
        this.actionTextColor = v;
    }

    public void disabledActionTextColor(Color v) {
        this.disabledActionTextColor = v;
    }

    public void contentTextStyle(TextStyle v) {
        this.contentTextStyle = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void behavior(SnackBarBehavior v) {
        this.behavior = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void insetPadding(Object v) {
        this.insetPadding = v;
    }

    public void showCloseIcon(boolean v) {
        this.showCloseIcon = v;
    }

    public void closeIconColor(Color v) {
        this.closeIconColor = v;
    }

    public Color backgroundColor() {
        return backgroundColor;
    }

    public SnackBarBehavior behavior() {
        return behavior;
    }
}
