package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.TextStyle;

/**
 * Material {@code AppBarTheme}: write-once app-bar styling. Named Dart
 * constructor parameters map to setter methods; unset values stay null.
 */
public class AppBarTheme {

    private Color backgroundColor;
    private Color foregroundColor;
    private Color color;
    private Color shadowColor;
    private Color surfaceTintColor;
    private Double elevation;
    private Double scrolledUnderElevation;
    private IconThemeData iconTheme;
    private IconThemeData actionsIconTheme;
    private TextStyle titleTextStyle;
    private TextStyle toolbarTextStyle;
    private Boolean centerTitle;
    private Double titleSpacing;
    private Double toolbarHeight;
    private Object systemOverlayStyle;
    private Object shape;

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void foregroundColor(Color v) {
        this.foregroundColor = v;
    }

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

    public void scrolledUnderElevation(double v) {
        this.scrolledUnderElevation = v;
    }

    public void iconTheme(IconThemeData v) {
        this.iconTheme = v;
    }

    public void actionsIconTheme(IconThemeData v) {
        this.actionsIconTheme = v;
    }

    public void titleTextStyle(TextStyle v) {
        this.titleTextStyle = v;
    }

    public void toolbarTextStyle(TextStyle v) {
        this.toolbarTextStyle = v;
    }

    public void centerTitle(boolean v) {
        this.centerTitle = v;
    }

    public void titleSpacing(double v) {
        this.titleSpacing = v;
    }

    public void toolbarHeight(double v) {
        this.toolbarHeight = v;
    }

    public void systemOverlayStyle(Object v) {
        this.systemOverlayStyle = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public Color backgroundColor() {
        return backgroundColor;
    }

    public Double elevation() {
        return elevation;
    }

    public IconThemeData iconTheme() {
        return iconTheme;
    }
}
