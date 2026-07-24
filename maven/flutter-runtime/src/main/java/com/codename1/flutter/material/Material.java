package com.codename1.flutter.material;

import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;

/**
 * A piece of material — Flutter's {@code Material}. Provides a surface color
 * (and, in Flutter, elevation shadow and ink effects) behind its child. This
 * milestone paints the {@code color} surface and sizes to the child; elevation
 * shadow, shape and ink are retained but not yet rendered.
 */
public class Material extends Widget {

    private Object type;
    private double elevation;
    private Color color;
    private Color shadowColor;
    private Color surfaceTintColor;
    private TextStyle textStyle;
    private Object borderRadius;
    private Object shape;
    private boolean borderOnForeground = true;
    private Clip clipBehavior = Clip.none;
    private Widget child;

    public void type(Object v) {
        this.type = v;
    }

    public void elevation(double v) {
        this.elevation = v;
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

    public void textStyle(TextStyle v) {
        this.textStyle = v;
    }

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void borderOnForeground(boolean v) {
        this.borderOnForeground = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    /** The duration of ink/elevation animations — Flutter's {@code animationDuration}. */
    public void animationDuration(dart.core.Duration v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Color getColor() {
        return color;
    }

    public double getElevation() {
        return elevation;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new MaterialRenderElement(this);
    }
}
