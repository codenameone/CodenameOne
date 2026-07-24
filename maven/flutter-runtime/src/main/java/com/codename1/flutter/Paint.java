package com.codename1.flutter;

/**
 * A mutable description of how to stroke or fill a shape on a {@link Canvas} —
 * Flutter's dart:ui {@code Paint}. Transpiled Dart mutates it field-by-field
 * ({@code paint..color = ...  ..style = ...}); each field is a getter/setter
 * pair here.
 */
public final class Paint {

    private Color color = new Color(0xFF000000L);
    private PaintingStyle style = PaintingStyle.fill;
    private double strokeWidth;
    private StrokeCap strokeCap = StrokeCap.butt;
    private StrokeJoin strokeJoin = StrokeJoin.miter;
    private double strokeMiterLimit = 4.0;
    private boolean isAntiAlias = true;
    private Shader shader;
    private Object maskFilter;
    private Object colorFilter;
    private Object blendMode;

    public Paint() {
    }

    public Color color() {
        return color;
    }

    public void color(Color v) {
        this.color = v;
    }

    public PaintingStyle style() {
        return style;
    }

    public void style(PaintingStyle v) {
        this.style = v == null ? PaintingStyle.fill : v;
    }

    public double strokeWidth() {
        return strokeWidth;
    }

    public void strokeWidth(double v) {
        this.strokeWidth = v;
    }

    public StrokeCap strokeCap() {
        return strokeCap;
    }

    public void strokeCap(StrokeCap v) {
        this.strokeCap = v == null ? StrokeCap.butt : v;
    }

    public StrokeJoin strokeJoin() {
        return strokeJoin;
    }

    public void strokeJoin(StrokeJoin v) {
        this.strokeJoin = v == null ? StrokeJoin.miter : v;
    }

    public double strokeMiterLimit() {
        return strokeMiterLimit;
    }

    public void strokeMiterLimit(double v) {
        this.strokeMiterLimit = v;
    }

    public boolean isAntiAlias() {
        return isAntiAlias;
    }

    public void isAntiAlias(boolean v) {
        this.isAntiAlias = v;
    }

    public Shader shader() {
        return shader;
    }

    public void shader(Shader v) {
        this.shader = v;
    }

    public Object maskFilter() {
        return maskFilter;
    }

    public void maskFilter(Object v) {
        this.maskFilter = v;
    }

    public Object colorFilter() {
        return colorFilter;
    }

    public void colorFilter(Object v) {
        this.colorFilter = v;
    }

    public Object blendMode() {
        return blendMode;
    }

    public void blendMode(Object v) {
        this.blendMode = v;
    }
}
