package com.codename1.flutter;

/**
 * Text styling configuration. Like the widgets, named Dart parameters become
 * void setter methods; unset properties stay null and inherit the CN1
 * default style.
 */
public class TextStyle {

    private Double fontSize;
    private FontWeight fontWeight;
    private Color color;
    private String fontFamily;

    public void fontSize(double v) {
        this.fontSize = v;
    }

    public void fontWeight(FontWeight v) {
        this.fontWeight = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void fontFamily(String v) {
        this.fontFamily = v;
    }

    /**
     * Font size in logical pixels, or null when inherited.
     */
    public Double getFontSize() {
        return fontSize;
    }

    public FontWeight getFontWeight() {
        return fontWeight;
    }

    public Color getColor() {
        return color;
    }

    public String getFontFamily() {
        return fontFamily;
    }
}
