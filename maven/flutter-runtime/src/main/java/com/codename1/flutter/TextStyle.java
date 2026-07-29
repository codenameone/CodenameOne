package com.codename1.flutter;

/**
 * Text styling configuration. Like the widgets, named Dart parameters become
 * void setter methods; unset properties stay null and inherit the CN1
 * default style. {@link #copyWith} and {@link #apply} return derived copies
 * (the P2 cascade fixers) so member access on a text style stays statically
 * typed rather than collapsing to dynamic.
 */
public class TextStyle {

    private Double fontSize;
    private FontWeight fontWeight;
    private Color color;
    private String fontFamily;
    private Double letterSpacing;
    private Double height;

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

    public void letterSpacing(double v) {
        this.letterSpacing = v;
    }

    public Double getLetterSpacing() {
        return letterSpacing;
    }

    public void height(double v) {
        this.height = v;
    }

    // ------------------------------------------------------------------
    // Dart getters -> no-arg methods
    // ------------------------------------------------------------------

    public Color color() {
        return color;
    }

    public Double fontSize() {
        return fontSize;
    }

    public FontWeight fontWeight() {
        return fontWeight;
    }

    public String fontFamily() {
        return fontFamily;
    }

    public Double letterSpacing() {
        return letterSpacing;
    }

    public Double height() {
        return height;
    }

    // ------------------------------------------------------------------
    // Legacy accessors used by the render elements
    // ------------------------------------------------------------------

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

    private TextStyle shallowClone() {
        TextStyle t = new TextStyle();
        t.fontSize = fontSize;
        t.fontWeight = fontWeight;
        t.color = color;
        t.fontFamily = fontFamily;
        t.letterSpacing = letterSpacing;
        t.height = height;
        return t;
    }

    /**
     * Returns a copy with the supplied (non-null) values overridden. Parameter
     * order matches the Dart stub. Properties this pass does not model
     * (fontStyle, wordSpacing, background, foreground, decoration) are accepted
     * for API shape and ignored.
     */
    public TextStyle copyWith(Boolean inherit, Color color, Color backgroundColor, String fontFamily,
                              Double fontSize, FontWeight fontWeight, Object fontStyle, Double letterSpacing,
                              Double wordSpacing, Double height, Object background, Object foreground,
                              Object decoration) {
        TextStyle t = shallowClone();
        if (color != null) t.color = color;
        if (fontFamily != null) t.fontFamily = fontFamily;
        if (fontSize != null) t.fontSize = fontSize;
        if (fontWeight != null) t.fontWeight = fontWeight;
        if (letterSpacing != null) t.letterSpacing = letterSpacing;
        if (height != null) t.height = height;
        return t;
    }

    /**
     * Returns a copy with a foreground {@code color} override and the font size
     * scaled by {@code fontSizeFactor} then offset by {@code fontSizeDelta}
     * (both default to no-op when null). Mirrors Flutter's {@code TextStyle.apply}.
     */
    public TextStyle apply(Color color, Color backgroundColor, String fontFamily,
                           Double fontSizeFactor, Double fontSizeDelta, Object decoration) {
        TextStyle t = shallowClone();
        if (color != null) t.color = color;
        if (fontFamily != null) t.fontFamily = fontFamily;
        if (t.fontSize != null) {
            double factor = fontSizeFactor != null ? fontSizeFactor : 1.0;
            double delta = fontSizeDelta != null ? fontSizeDelta : 0.0;
            t.fontSize = t.fontSize * factor + delta;
        }
        return t;
    }
}
