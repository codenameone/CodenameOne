package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.TextStyle;

/**
 * Material {@code FloatingActionButtonThemeData}: write-once FAB styling.
 * Named Dart constructor parameters map to setter methods; unset values stay
 * null.
 */
public class FloatingActionButtonThemeData {

    private Color foregroundColor;
    private Color backgroundColor;
    private Color focusColor;
    private Color hoverColor;
    private Color splashColor;
    private Double elevation;
    private Double focusElevation;
    private Double hoverElevation;
    private Double disabledElevation;
    private Double highlightElevation;
    private Object shape;
    private Boolean enableFeedback;
    private Double iconSize;
    private Object sizeConstraints;
    private TextStyle extendedTextStyle;

    public void foregroundColor(Color v) {
        this.foregroundColor = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void focusColor(Color v) {
        this.focusColor = v;
    }

    public void hoverColor(Color v) {
        this.hoverColor = v;
    }

    public void splashColor(Color v) {
        this.splashColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void focusElevation(double v) {
        this.focusElevation = v;
    }

    public void hoverElevation(double v) {
        this.hoverElevation = v;
    }

    public void disabledElevation(double v) {
        this.disabledElevation = v;
    }

    public void highlightElevation(double v) {
        this.highlightElevation = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void enableFeedback(boolean v) {
        this.enableFeedback = v;
    }

    public void iconSize(double v) {
        this.iconSize = v;
    }

    public void sizeConstraints(Object v) {
        this.sizeConstraints = v;
    }

    public void extendedTextStyle(TextStyle v) {
        this.extendedTextStyle = v;
    }

    public Color backgroundColor() {
        return backgroundColor;
    }
}
