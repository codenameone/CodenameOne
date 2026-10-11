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
 * Parses an SVG transform attribute: a sequence of translate / rotate / scale
 * / skewX / skewY / matrix functions applied left-to-right.
 */
public final class TransformParser {

    private TransformParser() { }

    public static SVGTransform parse(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;

        SVGTransform result = SVGTransform.identity();
        int i = 0;
        int len = t.length();
        while (i < len) {
            while (i < len && (t.charAt(i) == ' ' || t.charAt(i) == ',')) i++;
            if (i >= len) break;

            int nameStart = i;
            while (i < len && (Character.isLetter(t.charAt(i)))) i++;
            String name = t.substring(nameStart, i).trim();
            while (i < len && t.charAt(i) != '(') i++;
            if (i >= len) throw new IllegalArgumentException("Expected '(' after " + name);
            i++;
            int close = t.indexOf(')', i);
            if (close < 0) throw new IllegalArgumentException("Unclosed transform " + name);
            String inside = t.substring(i, close);
            i = close + 1;

            float[] args = parseArgs(inside);
            SVGTransform op = build(name, args);
            result = result.multiply(op);
        }
        return result.isIdentity() ? null : result;
    }

    /**
     * Parses the value of the CSS {@code transform} property, which names
     * more functions than the attribute and gives its arguments units:
     * {@code translate(10px, 5px) rotate(0.25turn)}.
     *
     * @return the transform, or null for {@code none} and for one that changes nothing
     * @throws IllegalArgumentException for a function that is not a flat transform
     */
    public static SVGTransform parseCss(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty() || "none".equals(t)) return null;
        SVGTransform result = SVGTransform.identity();
        int i = 0;
        while (i < t.length()) {
            int open = t.indexOf('(', i);
            if (open < 0) {
                if (t.substring(i).trim().isEmpty()) break;
                throw new IllegalArgumentException("Expected a function in " + s);
            }
            int close = t.indexOf(')', open);
            if (close < 0) throw new IllegalArgumentException("Unclosed transform in " + s);
            String name = t.substring(i, open).trim();
            String inside = t.substring(open + 1, close).trim();
            String[] parts = inside.isEmpty() ? new String[0] : inside.split("[\\s,]+");
            boolean turns = "rotate".equals(name) || name.startsWith("skew");
            float[] args = new float[parts.length];
            for (int k = 0; k < parts.length; k++) {
                args[k] = turns ? degrees(parts[k]) : NumberParser.parseFloat(parts[k]);
            }
            SVGTransform op;
            if ("translateX".equals(name)) {
                op = SVGTransform.translate(args.length > 0 ? args[0] : 0, 0);
            } else if ("translateY".equals(name)) {
                op = SVGTransform.translate(0, args.length > 0 ? args[0] : 0);
            } else if ("scaleX".equals(name)) {
                op = SVGTransform.scale(args.length > 0 ? args[0] : 1, 1);
            } else if ("scaleY".equals(name)) {
                op = SVGTransform.scale(1, args.length > 0 ? args[0] : 1);
            } else if ("rotate".equals(name)) {
                // Always about the origin; the attribute's centre is not CSS.
                op = SVGTransform.rotate(args.length > 0 ? args[0] : 0, 0, 0);
            } else if ("skew".equals(name)) {
                op = new SVGTransform(1, (float) Math.tan(Math.toRadians(args.length > 1 ? args[1] : 0)),
                        (float) Math.tan(Math.toRadians(args.length > 0 ? args[0] : 0)), 1, 0, 0);
            } else {
                op = build(name, args);
            }
            result = result.multiply(op);
            i = close + 1;
        }
        return result.isIdentity() ? null : result;
    }

    /** An angle in degrees, from any of the units CSS writes one in; a bare number is degrees. */
    private static float degrees(String value) {
        String v = value.trim();
        if (v.endsWith("grad")) return Float.parseFloat(v.substring(0, v.length() - 4)) * 0.9f;
        if (v.endsWith("rad")) return (float) Math.toDegrees(Float.parseFloat(v.substring(0, v.length() - 3)));
        if (v.endsWith("turn")) return Float.parseFloat(v.substring(0, v.length() - 4)) * 360f;
        if (v.endsWith("deg")) return Float.parseFloat(v.substring(0, v.length() - 3));
        return Float.parseFloat(v);
    }

    private static float[] parseArgs(String s) {
        NumberParser np = new NumberParser(s);
        java.util.ArrayList<Float> list = new java.util.ArrayList<Float>();
        while (np.hasMore()) list.add(np.nextFloat());
        float[] r = new float[list.size()];
        for (int i = 0; i < r.length; i++) r[i] = list.get(i);
        return r;
    }

    private static SVGTransform build(String name, float[] args) {
        if ("translate".equals(name)) {
            float tx = args.length > 0 ? args[0] : 0;
            float ty = args.length > 1 ? args[1] : 0;
            return SVGTransform.translate(tx, ty);
        }
        if ("scale".equals(name)) {
            float sx = args.length > 0 ? args[0] : 1;
            float sy = args.length > 1 ? args[1] : sx;
            return SVGTransform.scale(sx, sy);
        }
        if ("rotate".equals(name)) {
            float ang = args.length > 0 ? args[0] : 0;
            float cx = args.length > 1 ? args[1] : 0;
            float cy = args.length > 2 ? args[2] : 0;
            return SVGTransform.rotate(ang, cx, cy);
        }
        if ("skewX".equals(name)) {
            return SVGTransform.skewX(args.length > 0 ? args[0] : 0);
        }
        if ("skewY".equals(name)) {
            return SVGTransform.skewY(args.length > 0 ? args[0] : 0);
        }
        if ("matrix".equals(name)) {
            if (args.length < 6) throw new IllegalArgumentException("matrix() needs 6 args");
            return new SVGTransform(args[0], args[1], args[2], args[3], args[4], args[5]);
        }
        throw new IllegalArgumentException("Unknown transform: " + name);
    }
}
