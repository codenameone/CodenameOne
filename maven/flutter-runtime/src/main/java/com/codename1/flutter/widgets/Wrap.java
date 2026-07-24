package com.codename1.flutter.widgets;

import com.codename1.flutter.Axis;
import com.codename1.flutter.Clip;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.WrapAlignment;
import com.codename1.flutter.WrapCrossAlignment;

import dart.core.DartList;

/**
 * Lays its children out in runs along a main axis, wrapping to a new run when
 * the current one is full — Flutter's {@code Wrap}.
 */
public class Wrap extends Widget {

    private Axis direction = Axis.horizontal;
    private WrapAlignment alignment = WrapAlignment.start;
    private double spacing;
    private WrapAlignment runAlignment = WrapAlignment.start;
    private double runSpacing;
    private WrapCrossAlignment crossAxisAlignment = WrapCrossAlignment.start;
    private Object textDirection;
    private Object verticalDirection;
    private Clip clipBehavior = Clip.none;
    private DartList<Widget> children;

    public void direction(Axis v) {
        this.direction = v == null ? Axis.horizontal : v;
    }

    public void alignment(WrapAlignment v) {
        this.alignment = v == null ? WrapAlignment.start : v;
    }

    public void spacing(double v) {
        this.spacing = v;
    }

    public void runAlignment(WrapAlignment v) {
        this.runAlignment = v == null ? WrapAlignment.start : v;
    }

    public void runSpacing(double v) {
        this.runSpacing = v;
    }

    public void crossAxisAlignment(WrapCrossAlignment v) {
        this.crossAxisAlignment = v == null ? WrapCrossAlignment.start : v;
    }

    public void textDirection(Object v) {
        this.textDirection = v;
    }

    public void verticalDirection(Object v) {
        this.verticalDirection = v;
    }

    public void clipBehavior(Clip v) {
        this.clipBehavior = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public Axis getDirection() {
        return direction;
    }

    public WrapAlignment getAlignment() {
        return alignment;
    }

    public double getSpacing() {
        return spacing;
    }

    public double getRunSpacing() {
        return runSpacing;
    }

    public WrapCrossAlignment getCrossAxisAlignment() {
        return crossAxisAlignment;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    @Override
    public Element createElement() {
        return new WrapRenderElement(this);
    }
}
