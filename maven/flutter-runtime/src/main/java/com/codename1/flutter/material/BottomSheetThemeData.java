package com.codename1.flutter.material;

import com.codename1.flutter.Color;

/**
 * The theme overrides for bottom sheets — Flutter's {@code BottomSheetThemeData}.
 * Configuration only; consumed when a bottom sheet is shown.
 */
public class BottomSheetThemeData {

    private Color backgroundColor;
    private Color modalBackgroundColor;
    private Double elevation;
    private Double modalElevation;
    private Object shape;

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void modalBackgroundColor(Color v) {
        this.modalBackgroundColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void modalElevation(double v) {
        this.modalElevation = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public Color backgroundColor() {
        return backgroundColor;
    }

    public Color modalBackgroundColor() {
        return modalBackgroundColor;
    }
}
