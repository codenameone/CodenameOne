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

import java.util.HashMap;
import java.util.Map;

/**
 * Parses the SVG style="..." attribute and the equivalent presentation
 * attributes (fill, stroke, ...). The two sources are merged with inline
 * style winning, matching the SVG cascade rules.
 */
public final class StyleParser {

    private StyleParser() { }

    public static SVGStyle parse(Map<String, String> presentationAttrs, String styleAttr) {
        Map<String, String> merged = new HashMap<String, String>();
        if (presentationAttrs != null) {
            for (Map.Entry<String, String> e : presentationAttrs.entrySet()) {
                merged.put(e.getKey(), e.getValue());
            }
        }
        if (styleAttr != null && !styleAttr.trim().isEmpty()) {
            for (String decl : styleAttr.split(";")) {
                int colon = decl.indexOf(':');
                if (colon <= 0) continue;
                String k = decl.substring(0, colon).trim();
                String v = decl.substring(colon + 1).trim();
                if (!k.isEmpty()) merged.put(k, v);
            }
        }

        SVGStyle out = new SVGStyle();
        out.setFill(toPaint(merged.get("fill")));
        out.setStroke(toPaint(merged.get("stroke")));
        String paintOrder = merged.get("paint-order");
        if (paintOrder != null && !"inherit".equals(paintOrder.trim())) {
            out.setPaintOrder(paintOrder.trim());
        }
        out.setFillContext(toContext(merged.get("fill")));
        out.setStrokeContext(toContext(merged.get("stroke")));
        out.setFillCurrent(ColorParser.isCurrentColor(merged.get("fill")));
        out.setStrokeCurrent(ColorParser.isCurrentColor(merged.get("stroke")));
        String color = merged.get("color");
        if (color != null && !ColorParser.isCurrentColor(color) && !"inherit".equals(color.trim())) {
            try {
                out.setColor(Integer.valueOf(ColorParser.parse(color)));
            } catch (RuntimeException e) {
                // not a colour: inherited
            }
        }
        String filter = merged.get("filter");
        if (filter != null) out.setFilterRef(SVGPaint.stripUrl(filter));
        String all = merged.get("marker");
        out.setMarkerStart(toMarker(merged.get("marker-start"), all));
        out.setMarkerMid(toMarker(merged.get("marker-mid"), all));
        out.setMarkerEnd(toMarker(merged.get("marker-end"), all));
        out.setFillOpacity(toAlpha(merged.get("fill-opacity")));
        out.setStrokeOpacity(toAlpha(merged.get("stroke-opacity")));
        out.setOpacity(toAlpha(merged.get("opacity")));
        out.setStrokeWidth(toFloat(merged.get("stroke-width")));

        String cap = merged.get("stroke-linecap");
        if (cap != null) {
            cap = cap.trim().toLowerCase();
            if ("round".equals(cap)) out.setStrokeLineCap(SVGStyle.LINECAP_ROUND);
            else if ("square".equals(cap)) out.setStrokeLineCap(SVGStyle.LINECAP_SQUARE);
            else out.setStrokeLineCap(SVGStyle.LINECAP_BUTT);
        }
        String join = merged.get("stroke-linejoin");
        if (join != null) {
            join = join.trim().toLowerCase();
            if ("round".equals(join)) out.setStrokeLineJoin(SVGStyle.LINEJOIN_ROUND);
            else if ("bevel".equals(join)) out.setStrokeLineJoin(SVGStyle.LINEJOIN_BEVEL);
            else out.setStrokeLineJoin(SVGStyle.LINEJOIN_MITER);
        }
        out.setStrokeMiterLimit(toFloat(merged.get("stroke-miterlimit")));
        String fillRule = merged.get("fill-rule");
        // `inherit` and `unset` are the rule of what the element is in, which
        // is what leaving it unset here hands down.
        if (fillRule != null && !"inherit".equals(fillRule.trim()) && !"unset".equals(fillRule.trim())) {
            out.setFillEvenOdd(Boolean.valueOf("evenodd".equals(fillRule.trim())));
        }
        String effect = merged.get("vector-effect");
        out.setNonScalingStroke(effect != null && "non-scaling-stroke".equals(effect.trim()));
        String clipRule = merged.get("clip-rule");
        if (clipRule != null && !"inherit".equals(clipRule.trim()) && !"unset".equals(clipRule.trim())) {
            out.setClipEvenOdd(Boolean.valueOf("evenodd".equals(clipRule.trim())));
        }
        String mask = merged.get("mask");
        if (mask != null) {
            out.setMaskRef(SVGPaint.stripUrl(mask));
        }
        String display = merged.get("display");
        out.setDisplayNone(display != null && "none".equals(display.trim()));
        String visibility = merged.get("visibility");
        if (visibility != null) {
            String v = visibility.trim();
            if ("hidden".equals(v) || "collapse".equals(v)) out.setVisible(Boolean.FALSE);
            else if ("visible".equals(v)) out.setVisible(Boolean.TRUE);
        }
        out.setStrokeDashArray(toDashes(merged.get("stroke-dasharray")));
        out.setStrokeDashOffset(toFloat(merged.get("stroke-dashoffset")));
        String clipPath = merged.get("clip-path");
        if (clipPath != null) {
            String ref = SVGPaint.stripUrl(clipPath);
            if (ref != null) {
                out.setClipPathRef(ref);
            }
        }
        return out;
    }

    /** The id a marker property names, "" for {@code none}, null when neither it nor the shorthand is set. */
    private static String toMarker(String value, String shorthand) {
        String v = value != null ? value : shorthand;
        if (v == null) return null;
        String ref = SVGPaint.stripUrl(v);
        return ref == null ? ("none".equals(v.trim()) ? "" : null) : ref;
    }

    /** "fill" for {@code context-fill}, "stroke" for {@code context-stroke}, null for anything else. */
    private static String toContext(String value) {
        if (value == null) return null;
        String v = value.trim();
        return "context-fill".equals(v) ? "fill" : "context-stroke".equals(v) ? "stroke" : null;
    }

    private static SVGPaint toPaint(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        if (ColorParser.isNone(v)) return SVGPaint.NONE;
        if (toContext(v) != null) return null; // left unset; SVGStyle.getFillContext / getStrokeContext say which
        if (ColorParser.isCurrentColor(v)) return null; // left unset; SVGStyle.isFillCurrent / isStrokeCurrent say it was written
        SVGPaint ref = SVGPaint.setReference(v);
        if (ref != null) return ref;
        try {
            return SVGPaint.ofColor(ColorParser.parse(v));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * A stroke-dasharray: empty for {@code none} and for a list that adds
     * up to nothing, null for one that is missing or not valid. A list of
     * an odd number of lengths is repeated to make it even.
     */
    private static float[] toDashes(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        if ("none".equals(v)) return new float[0];
        String[] parts = v.split("[\\s,]+");
        float[] lengths = new float[parts.length];
        float sum = 0f;
        for (int i = 0; i < parts.length; i++) {
            try {
                lengths[i] = NumberParser.parseFloat(parts[i]);
            } catch (RuntimeException e) {
                return null;
            }
            if (!(lengths[i] >= 0f) || Float.isInfinite(lengths[i])) return null;
            sum += lengths[i];
        }
        if (!(sum > 0f)) return new float[0];
        if (lengths.length % 2 == 0) return lengths;
        float[] doubled = new float[lengths.length * 2];
        System.arraycopy(lengths, 0, doubled, 0, lengths.length);
        System.arraycopy(lengths, 0, doubled, lengths.length, lengths.length);
        return doubled;
    }

    /**
     * An opacity, which is written as a number from 0 to 1 or as a
     * percentage: {@code 50%} is {@code 0.5}, not the fifty that reading it
     * as a plain number gives and that is then clamped to fully opaque.
     */
    static Float toAlpha(String s) {
        Float v = toFloat(s);
        if (v != null && s.trim().endsWith("%")) {
            return Float.valueOf(v.floatValue() / 100f);
        }
        return v;
    }

    private static Float toFloat(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            return NumberParser.parseFloat(t);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
