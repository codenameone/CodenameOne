package com.codename1.flutter;

/**
 * A side of a border: its color, width and line style — Flutter's
 * {@code BorderSide}.
 */
public final class BorderSide {

    public static final BorderSide none = makeNone();

    private Color color = new Color(0xFF000000L);
    private double width = 1.0;
    private BorderStyle style = BorderStyle.solid;

    /// Where the stroke sits relative to the path it follows: -1 fully inside, 0 centred
    /// on it, 1 fully outside. Codename One strokes centred, so these are carried for the
    /// geometry pass rather than honoured today - but a border naming one has to compile.
    public static final double strokeAlignInside = -1.0;
    public static final double strokeAlignCenter = 0.0;
    public static final double strokeAlignOutside = 1.0;

    private Double strokeAlign;

    public void strokeAlign(double v) {
        this.strokeAlign = v;
    }

    public Double getStrokeAlign() {
        return strokeAlign;
    }

    public BorderSide() {
    }

    private static BorderSide makeNone() {
        BorderSide b = new BorderSide();
        b.width = 0.0;
        b.style = BorderStyle.none;
        return b;
    }

    public Color color() {
        return color;
    }

    public void color(Color v) {
        this.color = v;
    }

    public double width() {
        return width;
    }

    public void width(double v) {
        this.width = v;
    }

    public BorderStyle style() {
        return style;
    }

    public void style(BorderStyle v) {
        this.style = v == null ? BorderStyle.solid : v;
    }

    /**
     * Dart's {@code BorderSide.lerp(a, b, t)}: linear interpolation between two
     * sides. Widths interpolate; the color and style are taken from the side the
     * blend is closest to. Deferred rendering does not read the result, so a
     * simple threshold blend is sufficient.
     */
    public static BorderSide lerp(BorderSide a, BorderSide b, double t) {
        if (a == null) return b;
        if (b == null) return a;
        BorderSide r = new BorderSide();
        r.width = a.width + (b.width - a.width) * t;
        BorderSide dominant = t < 0.5 ? a : b;
        r.color = dominant.color;
        r.style = dominant.style;
        return r;
    }

    /**
     * Dart's {@code BorderSide.toPaint()}: a stroking {@link Paint} for this
     * side (its color at its width, or a hairline fill when the style is none).
     */
    public Paint toPaint() {
        Paint p = new Paint();
        p.color(color);
        p.strokeWidth(width);
        p.style(PaintingStyle.stroke);
        return p;
    }
}
