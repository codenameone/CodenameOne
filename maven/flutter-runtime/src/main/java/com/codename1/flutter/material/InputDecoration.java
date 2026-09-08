package com.codename1.flutter.material;

/**
 * Decoration configuration for a {@link TextField}. M3 renders both
 * {@code labelText} and {@code hintText} through the CN1 hint mechanism
 * (labelText wins when both are set) — a floating label is a later
 * milestone.
 */
public class InputDecoration {

    private String labelText;
    private String hintText;
    private String helperText;
    private String errorText;
    private String prefixText;
    private String suffixText;
    private com.codename1.flutter.Widget icon;
    private com.codename1.flutter.Widget prefixIcon;
    private com.codename1.flutter.Widget suffixIcon;
    private Boolean filled;
    private com.codename1.flutter.Color fillColor;
    private com.codename1.flutter.InputBorder border;
    private com.codename1.flutter.TextStyle labelStyle;
    private com.codename1.flutter.TextStyle hintStyle;
    private com.codename1.flutter.EdgeInsetsGeometry contentPadding;
    private FloatingLabelBehavior floatingLabelBehavior;

    public void labelText(String v) {
        this.labelText = v;
    }

    public void hintText(String v) {
        this.hintText = v;
    }

    public void helperText(String v) {
        this.helperText = v;
    }

    public void errorText(String v) {
        this.errorText = v;
    }

    public void prefixText(String v) {
        this.prefixText = v;
    }

    public void suffixText(String v) {
        this.suffixText = v;
    }

    public void icon(com.codename1.flutter.Widget v) {
        this.icon = v;
    }

    public void prefixIcon(com.codename1.flutter.Widget v) {
        this.prefixIcon = v;
    }

    public void suffixIcon(com.codename1.flutter.Widget v) {
        this.suffixIcon = v;
    }

    public void filled(boolean v) {
        this.filled = v;
    }

    public void fillColor(com.codename1.flutter.Color v) {
        this.fillColor = v;
    }

    public void border(com.codename1.flutter.InputBorder v) {
        this.border = v;
    }

    public void labelStyle(com.codename1.flutter.TextStyle v) {
        this.labelStyle = v;
    }

    public void hintStyle(com.codename1.flutter.TextStyle v) {
        this.hintStyle = v;
    }

    public void contentPadding(com.codename1.flutter.EdgeInsetsGeometry v) {
        this.contentPadding = v;
    }

    public void floatingLabelBehavior(FloatingLabelBehavior v) {
        this.floatingLabelBehavior = v;
    }

    public String getLabelText() {
        return labelText;
    }

    /** Whether the field paints a solid fill behind its content. */
    public boolean isFilled() {
        return filled != null && filled.booleanValue();
    }

    /** The fill colour, or null to take the theme's. */
    public com.codename1.flutter.Color getFillColor() {
        return fillColor;
    }

    /** The glyph shown before the content, or null. */
    /** {@code hintStyle} -- the type the placeholder is set in. */
    public com.codename1.flutter.TextStyle getHintStyle() {
        return hintStyle;
    }

    public com.codename1.flutter.Widget getPrefixIcon() {
        return prefixIcon;
    }

    /** The requested border, or null for the theme's. */
    public com.codename1.flutter.InputBorder getBorder() {
        return border;
    }

    /** The requested content padding, or null. */
    public com.codename1.flutter.EdgeInsetsGeometry getContentPadding() {
        return contentPadding;
    }

    public String getHintText() {
        return hintText;
    }

    /**
     * {@code InputDecoration.collapsed}: a minimal decoration with no label,
     * border or padding — only a hint. Styling parameters are accepted for API
     * shape and ignored at this milestone.
     */
    public static InputDecoration collapsed(String hintText, Object hintStyle, Object border,
            Boolean filled, com.codename1.flutter.Color fillColor) {
        InputDecoration d = new InputDecoration();
        d.hintText(hintText);
        return d;
    }
}
