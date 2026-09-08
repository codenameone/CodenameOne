package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsetsGeometry;
import com.codename1.flutter.TextStyle;

/**
 * Theming values applied to descendant InputDecorators — Flutter's
 * {@code InputDecorationThemeData} (the Material-3 value-type spelling of the
 * older {@code InputDecorationTheme}). Signature-only: the gallery only sets a
 * handful of fields and never reads them back, so borders are held opaquely.
 */
public class InputDecorationThemeData {

    private TextStyle labelStyle;
    private TextStyle floatingLabelStyle;
    private TextStyle helperStyle;
    private TextStyle hintStyle;
    private TextStyle errorStyle;
    private TextStyle prefixStyle;
    private TextStyle suffixStyle;
    private TextStyle counterStyle;
    private boolean filled;
    private Color fillColor;
    private Color focusColor;
    private Color hoverColor;
    private EdgeInsetsGeometry contentPadding;
    private boolean isDense;
    private boolean isCollapsed;
    private Object border;
    private Object enabledBorder;
    private Object focusedBorder;
    private Object errorBorder;
    private Object focusedErrorBorder;
    private Object disabledBorder;
    private Object floatingLabelBehavior;
    private double gapPadding;
    private boolean alignLabelWithHint;
    private Object constraints;

    /** Whether descendant fields paint a fill by default. */
    public boolean isFilled() { return filled; }

    /** The default fill colour for descendant fields. */
    public Color getFillColor() { return fillColor; }

    /** The default content padding for descendant fields. */
    public EdgeInsetsGeometry getContentPadding() { return contentPadding; }

    /** The default type for descendant fields' labels. */
    public TextStyle getLabelStyle() { return labelStyle; }

    public void labelStyle(TextStyle v) { this.labelStyle = v; }
    public void floatingLabelStyle(TextStyle v) { this.floatingLabelStyle = v; }
    public void helperStyle(TextStyle v) { this.helperStyle = v; }
    public void hintStyle(TextStyle v) { this.hintStyle = v; }
    public void errorStyle(TextStyle v) { this.errorStyle = v; }
    public void prefixStyle(TextStyle v) { this.prefixStyle = v; }
    public void suffixStyle(TextStyle v) { this.suffixStyle = v; }
    public void counterStyle(TextStyle v) { this.counterStyle = v; }
    public void filled(boolean v) { this.filled = v; }
    public void fillColor(Color v) { this.fillColor = v; }
    public void focusColor(Color v) { this.focusColor = v; }
    public void hoverColor(Color v) { this.hoverColor = v; }
    public void contentPadding(EdgeInsetsGeometry v) { this.contentPadding = v; }
    public void isDense(boolean v) { this.isDense = v; }
    public void isCollapsed(boolean v) { this.isCollapsed = v; }
    public void border(Object v) { this.border = v; }
    public void enabledBorder(Object v) { this.enabledBorder = v; }
    public void focusedBorder(Object v) { this.focusedBorder = v; }
    public void errorBorder(Object v) { this.errorBorder = v; }
    public void focusedErrorBorder(Object v) { this.focusedErrorBorder = v; }
    public void disabledBorder(Object v) { this.disabledBorder = v; }
    public void floatingLabelBehavior(Object v) { this.floatingLabelBehavior = v; }
    public void gapPadding(double v) { this.gapPadding = v; }
    public void alignLabelWithHint(boolean v) { this.alignLabelWithHint = v; }
    public void constraints(Object v) { this.constraints = v; }
}
