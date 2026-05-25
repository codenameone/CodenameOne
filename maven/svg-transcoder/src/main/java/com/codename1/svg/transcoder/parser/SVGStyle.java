package com.codename1.svg.transcoder.parser;

/**
 * Resolved style block for a node — everything the renderer needs to fill /
 * stroke this shape. Field "null" means "inherit from parent / leave unchanged".
 */
public final class SVGStyle {

    public static final int LINECAP_BUTT = 0;
    public static final int LINECAP_ROUND = 1;
    public static final int LINECAP_SQUARE = 2;

    public static final int LINEJOIN_MITER = 0;
    public static final int LINEJOIN_ROUND = 1;
    public static final int LINEJOIN_BEVEL = 2;

    private SVGPaint fill;
    private SVGPaint stroke;
    private Float fillOpacity;
    private Float strokeOpacity;
    private Float opacity;
    private Float strokeWidth;
    private Integer strokeLineCap;
    private Integer strokeLineJoin;
    private Float strokeMiterLimit;

    public SVGPaint getFill() { return fill; }
    public void setFill(SVGPaint fill) { this.fill = fill; }
    public SVGPaint getStroke() { return stroke; }
    public void setStroke(SVGPaint stroke) { this.stroke = stroke; }
    public Float getFillOpacity() { return fillOpacity; }
    public void setFillOpacity(Float v) { this.fillOpacity = v; }
    public Float getStrokeOpacity() { return strokeOpacity; }
    public void setStrokeOpacity(Float v) { this.strokeOpacity = v; }
    public Float getOpacity() { return opacity; }
    public void setOpacity(Float v) { this.opacity = v; }
    public Float getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(Float v) { this.strokeWidth = v; }
    public Integer getStrokeLineCap() { return strokeLineCap; }
    public void setStrokeLineCap(Integer v) { this.strokeLineCap = v; }
    public Integer getStrokeLineJoin() { return strokeLineJoin; }
    public void setStrokeLineJoin(Integer v) { this.strokeLineJoin = v; }
    public Float getStrokeMiterLimit() { return strokeMiterLimit; }
    public void setStrokeMiterLimit(Float v) { this.strokeMiterLimit = v; }

    /** Overlay other's set fields on top of this. */
    public SVGStyle inherit(SVGStyle parent) {
        if (parent == null) return this;
        if (fill == null) fill = parent.fill;
        if (stroke == null) stroke = parent.stroke;
        if (fillOpacity == null) fillOpacity = parent.fillOpacity;
        if (strokeOpacity == null) strokeOpacity = parent.strokeOpacity;
        // opacity does NOT inherit per SVG spec — leave alone.
        if (strokeWidth == null) strokeWidth = parent.strokeWidth;
        if (strokeLineCap == null) strokeLineCap = parent.strokeLineCap;
        if (strokeLineJoin == null) strokeLineJoin = parent.strokeLineJoin;
        if (strokeMiterLimit == null) strokeMiterLimit = parent.strokeMiterLimit;
        return this;
    }
}
