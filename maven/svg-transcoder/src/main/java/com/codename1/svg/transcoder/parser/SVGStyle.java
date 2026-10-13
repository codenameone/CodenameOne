/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.svg.transcoder.parser;

/**
 * Resolved style block for a node -- everything the renderer needs to fill /
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
    /** Element id referenced by `clip-path: url(#id)`, or {@code null} when
     *  no clip is set. The id resolves to an [com.codename1.svg.transcoder.model.SVGClipPath]
     *  registered in the document's definitions map. */
    private String clipPathRef;
    private Boolean fillEvenOdd;
    private float[] strokeDashArray;
    private boolean displayNone;
    private String maskRef;
    private Boolean visible;
    private Float strokeDashOffset;

    private boolean nonScalingStroke;

    /**
     * Whether the stroke keeps its width however the drawing is scaled
     * ({@code vector-effect: non-scaling-stroke}). Not inherited.
     */
    public boolean isNonScalingStroke() { return nonScalingStroke; }
    public void setNonScalingStroke(boolean v) { this.nonScalingStroke = v; }

    private Boolean clipEvenOdd;

    /** Whether clip-rule is evenodd; null when the element does not say. Inherited. */
    public Boolean getClipEvenOdd() { return clipEvenOdd; }
    public void setClipEvenOdd(Boolean v) { this.clipEvenOdd = v; }

    private Integer color;
    private String paintOrder;
    /**
     * The paint-order property as written: which of the fill, the stroke
     * and the markers is painted first. Null when it is not set, which is
     * {@code normal}: in that order.
     */
    public String getPaintOrder() { return paintOrder; }
    public void setPaintOrder(String v) { this.paintOrder = v; }
    private String fillContext;
    private String strokeContext;
    private boolean fillCurrent;
    private boolean strokeCurrent;
    private String filterRef;
    private String markerStart;
    private String markerMid;
    private String markerEnd;

    /** The {@code color} property as ARGB, which is what {@code currentColor} stands for; null when not set. Inherited. */
    public Integer getColor() { return color; }
    public void setColor(Integer color) { this.color = color; }
    /**
     * Whether the fill is {@code currentColor}. The fill itself is then
     * left unset, as it always was; a renderer that knows the colour puts
     * it in.
     */
    /**
     * What the fill is taken from when it is {@code context-fill} or
     * {@code context-stroke}: "fill" or "stroke" of the element a marker is
     * drawn on. Null when it is neither; the fill itself is then left unset.
     */
    public String getFillContext() { return fillContext; }
    public void setFillContext(String v) { this.fillContext = v; }
    /** The same for the stroke; see {@link #getFillContext()}. */
    public String getStrokeContext() { return strokeContext; }
    public void setStrokeContext(String v) { this.strokeContext = v; }
    public boolean isFillCurrent() { return fillCurrent; }
    public void setFillCurrent(boolean v) { this.fillCurrent = v; }
    /** Whether the stroke is {@code currentColor}; see {@link #isFillCurrent()}. */
    public boolean isStrokeCurrent() { return strokeCurrent; }
    public void setStrokeCurrent(boolean v) { this.strokeCurrent = v; }
    /** The id of the filter the element is drawn through, or null. Not inherited. */
    public String getFilterRef() { return filterRef; }
    public void setFilterRef(String v) { this.filterRef = v; }
    /** The ids of the markers drawn at the first, the middle and the last points of a line; null for none. Inherited. */
    public String getMarkerStart() { return markerStart; }
    public void setMarkerStart(String v) { this.markerStart = v; }
    public String getMarkerMid() { return markerMid; }
    public void setMarkerMid(String v) { this.markerMid = v; }
    public String getMarkerEnd() { return markerEnd; }
    public void setMarkerEnd(String v) { this.markerEnd = v; }

    public SVGPaint getFill() { return fill; }
    public void setFill(SVGPaint fill) { this.fill = fill; }
    public SVGPaint getStroke() { return stroke; }
    public void setStroke(SVGPaint stroke) { this.stroke = stroke; }
    public Float getFillOpacity() { return fillOpacity; }
    public void setFillOpacity(Float v) { this.fillOpacity = v; }
    public Float getStrokeOpacity() { return strokeOpacity; }
    public void setStrokeOpacity(Float v) { this.strokeOpacity = v; }
    private boolean opacityInherited;
    /** Whether the opacity is written as {@code inherit}: that of the element this one is in, applied to it too. */
    public boolean isOpacityInherited() { return opacityInherited; }
    public void setOpacityInherited(boolean v) { this.opacityInherited = v; }
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
    public String getClipPathRef() { return clipPathRef; }
    public void setClipPathRef(String clipPathRef) { this.clipPathRef = clipPathRef; }

    /** Overlay other's set fields on top of this. */
    /** Whether fill-rule is evenodd; null when the element does not say. */
    public Boolean getFillEvenOdd() { return fillEvenOdd; }
    public void setFillEvenOdd(Boolean v) { this.fillEvenOdd = v; }
    /**
     * The lengths of the dashes and gaps of a stroke, an even number of
     * them; empty for a solid line, null when the element does not say.
     */
    public float[] getStrokeDashArray() { return strokeDashArray; }
    public void setStrokeDashArray(float[] v) { this.strokeDashArray = v; }
    /** How far into the dash pattern a stroke starts. */
    public Float getStrokeDashOffset() { return strokeDashOffset; }
    public void setStrokeDashOffset(Float v) { this.strokeDashOffset = v; }
    /** Whether the element says display: none, which takes it and everything in it out of the drawing. */
    public boolean isDisplayNone() { return displayNone; }
    public void setDisplayNone(boolean v) { this.displayNone = v; }
    /** The visibility of the element; null when it does not say. A hidden element's children may still be visible. */
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean v) { this.visible = v; }
    /** The id of the mask the element is seen through, or null. Not inherited. */
    public String getMaskRef() { return maskRef; }
    public void setMaskRef(String maskRef) { this.maskRef = maskRef; }
    public SVGStyle inherit(SVGStyle parent) {
        if (parent == null) return this;
        if (fill == null) {
            fill = parent.fill;
            fillCurrent = fillCurrent || parent.fillCurrent;
            if (fillContext == null && !fillCurrent) fillContext = parent.fillContext;
        }
        if (stroke == null) {
            stroke = parent.stroke;
            strokeCurrent = strokeCurrent || parent.strokeCurrent;
            if (strokeContext == null && !strokeCurrent) strokeContext = parent.strokeContext;
        }
        if (color == null) color = parent.color;
        if (paintOrder == null) paintOrder = parent.paintOrder;
        if (clipEvenOdd == null) clipEvenOdd = parent.clipEvenOdd;
        // "" is `none` written out, which stops a marker being inherited.
        if (markerStart == null) markerStart = parent.markerStart;
        if (markerMid == null) markerMid = parent.markerMid;
        if (markerEnd == null) markerEnd = parent.markerEnd;
        if (fillOpacity == null) fillOpacity = parent.fillOpacity;
        if (strokeOpacity == null) strokeOpacity = parent.strokeOpacity;
        // opacity does NOT inherit per SVG spec -- leave alone.
        if (strokeWidth == null) strokeWidth = parent.strokeWidth;
        if (strokeLineCap == null) strokeLineCap = parent.strokeLineCap;
        if (strokeLineJoin == null) strokeLineJoin = parent.strokeLineJoin;
        if (strokeMiterLimit == null) strokeMiterLimit = parent.strokeMiterLimit;
        if (fillEvenOdd == null) fillEvenOdd = parent.fillEvenOdd;
        if (strokeDashArray == null) strokeDashArray = parent.strokeDashArray;
        if (visible == null) visible = parent.visible;
        // display does NOT inherit: a hidden element is skipped with its subtree.
        if (strokeDashOffset == null) strokeDashOffset = parent.strokeDashOffset;
        // clip-path does NOT inherit per SVG spec.
        return this;
    }
}
