package com.codename1.flutter.material;

import com.codename1.flutter.Color;

/**
 * Material {@code DividerThemeData}: write-once divider styling.
 */
public class DividerThemeData {

    private Double thickness;
    private Color color;
    private Double space;
    private Double indent;
    private Double endIndent;

    public void thickness(double v) {
        this.thickness = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void space(double v) {
        this.space = v;
    }

    public void indent(double v) {
        this.indent = v;
    }

    public void endIndent(double v) {
        this.endIndent = v;
    }

    public Double thickness() {
        return thickness;
    }

    public Color color() {
        return color;
    }

    public Double space() {
        return space;
    }
}
