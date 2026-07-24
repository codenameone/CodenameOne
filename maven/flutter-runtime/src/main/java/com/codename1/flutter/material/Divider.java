package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * A thin horizontal rule with vertical breathing room. {@code height} is the
 * total vertical extent the divider occupies (default 16lp);
 * {@code thickness} is the painted line (default 1lp). Backed by a CN1
 * hairline strip component (UIID "FlutterDivider").
 */
public class Divider extends Widget {

    private Double height;
    private Double thickness;
    private Color color;
    private Double indent;
    private Double endIndent;

    public void height(double v) {
        this.height = v;
    }

    public void indent(double v) {
        this.indent = v;
    }

    public void endIndent(double v) {
        this.endIndent = v;
    }

    public Double getIndent() {
        return indent;
    }

    public Double getEndIndent() {
        return endIndent;
    }

    public void thickness(double v) {
        this.thickness = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public Double getHeight() {
        return height;
    }

    public Double getThickness() {
        return thickness;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public Element createElement() {
        return new DividerRenderElement(this);
    }
}
