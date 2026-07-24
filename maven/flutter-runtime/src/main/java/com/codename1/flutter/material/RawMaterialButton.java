package com.codename1.flutter.material;

import com.codename1.flutter.Color;

/**
 * The lowest-level material button — Flutter's {@code RawMaterialButton}.
 * Shares {@link ButtonBase}'s press/child plumbing and adds a raw
 * {@code fillColor}. Used by color-picker swatches and other custom buttons
 * that want the material tap semantics without the higher-level button styles.
 */
public class RawMaterialButton extends ButtonBase {

    private Color fillColor;
    private Double elevation;
    private com.codename1.flutter.EdgeInsets padding;
    private Object shape;

    public void fillColor(Color v) {
        this.fillColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void padding(com.codename1.flutter.EdgeInsets v) {
        this.padding = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public Color getFillColor() {
        return fillColor;
    }
}
