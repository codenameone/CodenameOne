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
    public String getClipPathRef() { return clipPathRef; }
    public void setClipPathRef(String clipPathRef) { this.clipPathRef = clipPathRef; }

    /** Overlay other's set fields on top of this. */
    /** Whether fill-rule is evenodd; null when the element does not say. */
    public Boolean getFillEvenOdd() { return fillEvenOdd; }
    public void setFillEvenOdd(Boolean v) { this.fillEvenOdd = v; }
    public SVGStyle inherit(SVGStyle parent) {
        if (parent == null) return this;
        if (fill == null) fill = parent.fill;
        if (stroke == null) stroke = parent.stroke;
        if (fillOpacity == null) fillOpacity = parent.fillOpacity;
        if (strokeOpacity == null) strokeOpacity = parent.strokeOpacity;
        // opacity does NOT inherit per SVG spec -- leave alone.
        if (strokeWidth == null) strokeWidth = parent.strokeWidth;
        if (strokeLineCap == null) strokeLineCap = parent.strokeLineCap;
        if (strokeLineJoin == null) strokeLineJoin = parent.strokeLineJoin;
        if (strokeMiterLimit == null) strokeMiterLimit = parent.strokeMiterLimit;
        if (fillEvenOdd == null) fillEvenOdd = parent.fillEvenOdd;
        // clip-path does NOT inherit per SVG spec.
        return this;
    }
}
