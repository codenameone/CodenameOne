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
        out.setFillOpacity(toFloat(merged.get("fill-opacity")));
        out.setStrokeOpacity(toFloat(merged.get("stroke-opacity")));
        out.setOpacity(toFloat(merged.get("opacity")));
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
        if (fillRule != null) {
            out.setFillEvenOdd(Boolean.valueOf("evenodd".equals(fillRule.trim())));
        }
        String clipPath = merged.get("clip-path");
        if (clipPath != null) {
            String ref = SVGPaint.stripUrl(clipPath);
            if (ref != null) {
                out.setClipPathRef(ref);
            }
        }
        return out;
    }

    private static SVGPaint toPaint(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        if (ColorParser.isNone(v)) return SVGPaint.NONE;
        if (ColorParser.isCurrentColor(v)) return null; // treat as inherit; renderer defaults to black
        SVGPaint ref = SVGPaint.setReference(v);
        if (ref != null) return ref;
        try {
            return SVGPaint.ofColor(ColorParser.parse(v));
        } catch (RuntimeException e) {
            return null;
        }
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
