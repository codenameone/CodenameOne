package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * A material card: a rounded, subtly elevated surface around its child.
 * Backed by a CN1 Container (UIID "FlutterCard") with a 12lp round-rect
 * border; default margin 4lp on every edge.
 */
public class Card extends Widget {

    private Color color;
    private Double elevation;
    private EdgeInsets margin;
    private Widget child;

    public void color(Color v) {
        this.color = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void margin(EdgeInsets v) {
        this.margin = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Color getColor() {
        return color;
    }

    public Double getElevation() {
        return elevation;
    }

    public EdgeInsets getMargin() {
        return margin;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new CardRenderElement(this);
    }
}
