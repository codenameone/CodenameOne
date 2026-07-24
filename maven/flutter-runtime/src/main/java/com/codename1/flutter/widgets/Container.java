package com.codename1.flutter.widgets;

import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;

/**
 * A convenience widget that combines painting, positioning and sizing —
 * Flutter's {@code Container}. Applies (in order) margin, decoration/color,
 * additional constraints + explicit width/height, padding and alignment around
 * an optional child.
 *
 * <p>Loosely-typed properties ({@code alignment}, {@code padding},
 * {@code margin}, {@code decoration}) accept their several Flutter value types;
 * the render element interprets the ones it supports
 * ({@link com.codename1.flutter.Alignment}/{@link com.codename1.flutter.AlignmentDirectional},
 * {@link com.codename1.flutter.EdgeInsets}, {@link com.codename1.flutter.BoxDecoration}).</p>
 */
public class Container extends Widget {

    private Object alignment;
    private Object padding;
    private Color color;
    private Object decoration;
    private Object foregroundDecoration;
    private Double width;
    private Double height;
    private BoxConstraints constraints;
    private Object margin;
    private Object transform;
    private Object transformAlignment;
    private Clip clipBehavior = Clip.none;
    private Widget child;

    public void alignment(Object v) {
        this.alignment = v;
    }

    public void padding(Object v) {
        this.padding = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void decoration(Object v) {
        this.decoration = v;
    }

    public void foregroundDecoration(Object v) {
        this.foregroundDecoration = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void constraints(BoxConstraints v) {
        this.constraints = v;
    }

    public void margin(Object v) {
        this.margin = v;
    }

    public void transform(Object v) {
        this.transform = v;
    }

    public void transformAlignment(Object v) {
        this.transformAlignment = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getAlignment() {
        return alignment;
    }

    public Object getPadding() {
        return padding;
    }

    public Color getColor() {
        return color;
    }

    public Object getDecoration() {
        return decoration;
    }

    public Double getWidth() {
        return width;
    }

    public Double getHeight() {
        return height;
    }

    public BoxConstraints getConstraints() {
        return constraints;
    }

    public Object getMargin() {
        return margin;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new ContainerRenderElement(this);
    }
}
