package com.codename1.flutter.material;

import com.codename1.flutter.BorderRadius;
import com.codename1.flutter.Color;
import com.codename1.flutter.ShapeBorder;
import com.codename1.flutter.widgets.GestureDetector;

/**
 * The material tap-target with ink feedback — Flutter's {@code InkResponse}
 * (the superclass of {@link InkWell}). M2 renders it exactly like a
 * {@link GestureDetector} (transparent overlay, no ripple); the ink splash and
 * highlight are retained as configuration for a later milestone.
 */
public class InkResponse extends GestureDetector {

    private Color splashColor;
    private Color highlightColor;
    private Color focusColor;
    private Color hoverColor;
    private ShapeBorder customBorder;
    private BorderRadius borderRadius;
    private Double radius;
    private Boolean containedInkWell;

    public void splashColor(Color v) {
        this.splashColor = v;
    }

    public void highlightColor(Color v) {
        this.highlightColor = v;
    }

    public void focusColor(Color v) {
        this.focusColor = v;
    }

    public void hoverColor(Color v) {
        this.hoverColor = v;
    }

    public void customBorder(ShapeBorder v) {
        this.customBorder = v;
    }

    public void borderRadius(BorderRadius v) {
        this.borderRadius = v;
    }

    public void radius(double v) {
        this.radius = v;
    }

    public void containedInkWell(boolean v) {
        this.containedInkWell = v;
    }
}
