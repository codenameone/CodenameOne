package com.codename1.flutter.animation;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsets;

import dart.core.Duration;

/**
 * A Container whose properties animate to new values over a {@link Duration}
 * when the widget rebuilds — Flutter's {@code AnimatedContainer}. This pass
 * stores the properties and hosts the child; the tweened transitions are
 * deferred.
 */
public class AnimatedContainer extends AnimatedChildWidget {

    private Duration duration;
    private Curve curve;
    private Double width;
    private Double height;
    private Color color;
    private EdgeInsets padding;
    private EdgeInsets margin;
    private Alignment alignment;
    private Object decoration;

    public void duration(Duration v) {
        this.duration = v;
    }

    public void curve(Curve v) {
        this.curve = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void padding(EdgeInsets v) {
        this.padding = v;
    }

    public void margin(EdgeInsets v) {
        this.margin = v;
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }

    public void decoration(Object v) {
        this.decoration = v;
    }
}
