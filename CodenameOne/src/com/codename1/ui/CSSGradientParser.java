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
package com.codename1.ui;

import java.util.ArrayList;
import java.util.List;

/// Runtime parser that turns CSS gradient strings (`linear-gradient`,
/// `radial-gradient`, `conic-gradient`, plus the `repeating-*` variants)
/// into [Gradient] instances. Mirrors the build-time css-compiler's
/// parser so apps can apply the same CSS at runtime - e.g. when style
/// strings come from network responses or are constructed dynamically.
///
/// The CSS subset matches the build-time compiler: arbitrary angles in
/// `deg`/`rad`/`grad`/`turn`, `to <side> [<side2>]`, multi-stop colors
/// with optional position hints (percent only, no length units), the
/// full radial shape/extent/center syntax, conic `from <angle>` with
/// optional center, and named color subset.
final class CSSGradientParser {
    private CSSGradientParser() {
    }

    /// Parses a single CSS gradient function call. Returns null on
    /// recognised-but-unsupported syntax; throws IllegalArgumentException
    /// on hard parse errors so callers can decide between a default and
    /// a propagated failure.
    static Gradient parse(String css) {
        if (css == null) {
            return null;
        }
        String s = css.trim();
        if (s.length() == 0) {
            return null;
        }
        int open = s.indexOf('(');
        if (open < 0 || !s.endsWith(")")) {
            return null;
        }
        String name = s.substring(0, open).trim().toLowerCase();
        String args = s.substring(open + 1, s.length() - 1);
        boolean repeating = false;
        if (name.startsWith("repeating-")) {
            repeating = true;
            name = name.substring("repeating-".length());
        }
        if ("linear-gradient".equals(name)) {
            return parseLinear(args, repeating);
        }
        if ("radial-gradient".equals(name)) {
            return parseRadial(args, repeating);
        }
        if ("conic-gradient".equals(name)) {
            return parseConic(args);
        }
        return null;
    }

    private static Gradient parseLinear(String args, boolean repeating) {
        List<String> parts = splitTopLevel(args, ',');
        if (parts.size() < 2) {
            throw new IllegalArgumentException("linear-gradient needs at least 2 stops");
        }
        float angleDeg = 180f;
        int firstStopIdx = 0;
        String head = parts.get(0).trim();
        if (looksLikeAngle(head) || head.startsWith("to ")) {
            angleDeg = parseLinearAngle(head);
            firstStopIdx = 1;
        }
        List<String> stopParts = parts.subList(firstStopIdx, parts.size());
        Stops stops = parseStops(stopParts);
        LinearGradient g = new LinearGradient(angleDeg, stops.colors, stops.positions);
        if (repeating) {
            g.setCycleMethod(Gradient.CYCLE_REPEAT);
        }
        return g;
    }

    private static Gradient parseRadial(String args, boolean repeating) {
        List<String> parts = splitTopLevel(args, ',');
        if (parts.size() < 2) {
            throw new IllegalArgumentException("radial-gradient needs at least 2 stops");
        }
        byte shape = RadialGradient.SHAPE_ELLIPSE;
        byte extent = RadialGradient.EXTENT_FARTHEST_CORNER;
        float cx = 0.5f;
        float cy = 0.5f;
        int firstStopIdx = 0;
        String head = parts.get(0).trim();
        if (isRadialHeader(head)) {
            firstStopIdx = 1;
            String[] tokens = splitWhitespaceTopLevel(head);
            int i = 0;
            while (i < tokens.length) {
                String tok = tokens[i].toLowerCase();
                if ("circle".equals(tok)) {
                    shape = RadialGradient.SHAPE_CIRCLE;
                    i++;
                } else if ("ellipse".equals(tok)) {
                    shape = RadialGradient.SHAPE_ELLIPSE;
                    i++;
                } else if ("closest-side".equals(tok)) {
                    extent = RadialGradient.EXTENT_CLOSEST_SIDE;
                    i++;
                } else if ("closest-corner".equals(tok)) {
                    extent = RadialGradient.EXTENT_CLOSEST_CORNER;
                    i++;
                } else if ("farthest-side".equals(tok)) {
                    extent = RadialGradient.EXTENT_FARTHEST_SIDE;
                    i++;
                } else if ("farthest-corner".equals(tok)) {
                    extent = RadialGradient.EXTENT_FARTHEST_CORNER;
                    i++;
                } else if ("at".equals(tok)) {
                    i++;
                    if (i < tokens.length) {
                        cx = parsePositionCoord(tokens[i]);
                    }
                    i++;
                    if (i < tokens.length) {
                        cy = parsePositionCoord(tokens[i]);
                    }
                    i++;
                } else {
                    i++;
                }
            }
        }
        List<String> stopParts = parts.subList(firstStopIdx, parts.size());
        Stops stops = parseStops(stopParts);
        RadialGradient g = new RadialGradient(stops.colors, stops.positions);
        g.setShape(shape);
        g.setExtent(extent);
        g.setRelativeCenterX(cx);
        g.setRelativeCenterY(cy);
        if (repeating) {
            g.setCycleMethod(Gradient.CYCLE_REPEAT);
        }
        return g;
    }

    private static Gradient parseConic(String args) {
        List<String> parts = splitTopLevel(args, ',');
        if (parts.size() < 2) {
            throw new IllegalArgumentException("conic-gradient needs at least 2 stops");
        }
        float fromDeg = 0f;
        float cx = 0.5f;
        float cy = 0.5f;
        int firstStopIdx = 0;
        String head = parts.get(0).trim();
        String headLower = head.toLowerCase();
        if (headLower.startsWith("from ") || headLower.startsWith("at ")) {
            firstStopIdx = 1;
            String[] tokens = splitWhitespaceTopLevel(head);
            int i = 0;
            while (i < tokens.length) {
                String tok = tokens[i].toLowerCase();
                if ("from".equals(tok) && i + 1 < tokens.length) {
                    fromDeg = parseAngleToken(tokens[i + 1]);
                    i += 2;
                } else if ("at".equals(tok)) {
                    i++;
                    if (i < tokens.length) {
                        cx = parsePositionCoord(tokens[i]);
                    }
                    i++;
                    if (i < tokens.length) {
                        cy = parsePositionCoord(tokens[i]);
                    }
                    i++;
                } else {
                    i++;
                }
            }
        }
        List<String> stopParts = parts.subList(firstStopIdx, parts.size());
        Stops stops = parseStops(stopParts);
        ConicGradient g = new ConicGradient(stops.colors, stops.positions);
        g.setFromAngleDegrees(fromDeg);
        g.setRelativeCenterX(cx);
        g.setRelativeCenterY(cy);
        return g;
    }

    private static boolean isRadialHeader(String head) {
        String h = head.toLowerCase();
        return h.startsWith("circle") || h.startsWith("ellipse")
                || h.startsWith("closest-") || h.startsWith("farthest-")
                || h.startsWith("at ");
    }

    private static boolean looksLikeAngle(String s) {
        String lower = s.toLowerCase();
        return lower.endsWith("deg") || lower.endsWith("rad")
                || lower.endsWith("grad") || lower.endsWith("turn");
    }

    private static float parseLinearAngle(String head) {
        String lower = head.toLowerCase();
        if (lower.startsWith("to ")) {
            String dirs = head.substring(3).trim().toLowerCase();
            // CSS "to top" = 0deg, "to right" = 90deg, "to bottom" = 180deg, "to left" = 270deg.
            // For two-side directions we pick the bisecting 45-degree variant.
            if ("top".equals(dirs)) {
                return 0f;
            }
            if ("right".equals(dirs)) {
                return 90f;
            }
            if ("bottom".equals(dirs)) {
                return 180f;
            }
            if ("left".equals(dirs)) {
                return 270f;
            }
            if ("top right".equals(dirs) || "right top".equals(dirs)) {
                return 45f;
            }
            if ("bottom right".equals(dirs) || "right bottom".equals(dirs)) {
                return 135f;
            }
            if ("bottom left".equals(dirs) || "left bottom".equals(dirs)) {
                return 225f;
            }
            if ("top left".equals(dirs) || "left top".equals(dirs)) {
                return 315f;
            }
            throw new IllegalArgumentException("Unknown direction: to " + dirs);
        }
        return parseAngleToken(head);
    }

    private static float parseAngleToken(String s) {
        String lower = s.toLowerCase();
        if (lower.endsWith("deg")) {
            return parseFloat(lower.substring(0, lower.length() - 3));
        }
        if (lower.endsWith("grad")) {
            return parseFloat(lower.substring(0, lower.length() - 4)) * 0.9f;
        }
        if (lower.endsWith("turn")) {
            return parseFloat(lower.substring(0, lower.length() - 4)) * 360f;
        }
        if (lower.endsWith("rad")) {
            return (float) (parseFloat(lower.substring(0, lower.length() - 3)) * 180.0 / Math.PI);
        }
        return parseFloat(lower);
    }

    private static float parsePositionCoord(String s) {
        String lower = s.toLowerCase();
        if ("left".equals(lower) || "top".equals(lower)) {
            return 0f;
        }
        if ("right".equals(lower) || "bottom".equals(lower)) {
            return 1f;
        }
        if ("center".equals(lower)) {
            return 0.5f;
        }
        if (lower.endsWith("%")) {
            return parseFloat(lower.substring(0, lower.length() - 1)) / 100f;
        }
        // px / unitless treated as fraction; CSS allows px here but at parse
        // time we have no rect dimension to resolve against, so fall back
        // to the fractional interpretation.
        return parseFloat(lower);
    }

    private static Stops parseStops(List<String> parts) {
        List<Integer> colors = new ArrayList<Integer>(parts.size());
        List<Float> positions = new ArrayList<Float>(parts.size());
        for (String raw : parts) {
            String part = raw.trim();
            int splitAt = findColorPositionSplit(part);
            String colorPart;
            String posPart;
            if (splitAt < 0) {
                colorPart = part;
                posPart = null;
            } else {
                colorPart = part.substring(0, splitAt).trim();
                posPart = part.substring(splitAt).trim();
            }
            int rgba = CSSColor.parse(colorPart);
            colors.add(Integer.valueOf(rgba));
            if (posPart != null && posPart.endsWith("%")) {
                positions.add(Float.valueOf(
                        parseFloat(posPart.substring(0, posPart.length() - 1)) / 100f));
            } else {
                positions.add(null);
            }
        }
        // Auto-distribute the unset positions between adjacent fixed anchors.
        int n = positions.size();
        if (positions.get(0) == null) {
            positions.set(0, Float.valueOf(0f));
        }
        if (positions.get(n - 1) == null) {
            positions.set(n - 1, Float.valueOf(1f));
        }
        int last = 0;
        for (int i = 1; i < n; i++) {
            if (positions.get(i) != null) {
                int gap = i - last;
                if (gap > 1) {
                    float p0 = positions.get(last).floatValue();
                    float p1 = positions.get(i).floatValue();
                    for (int k = 1; k < gap; k++) {
                        positions.set(last + k, Float.valueOf(p0 + (p1 - p0) * k / gap));
                    }
                }
                last = i;
            }
        }
        int[] colorArr = new int[n];
        float[] posArr = new float[n];
        for (int i = 0; i < n; i++) {
            colorArr[i] = colors.get(i).intValue();
            posArr[i] = positions.get(i).floatValue();
        }
        Stops out = new Stops();
        out.colors = colorArr;
        out.positions = posArr;
        return out;
    }

    private static int findColorPositionSplit(String part) {
        // The trailing percentage (if any) is preceded by whitespace separating
        // it from the color, but inside `rgb(...)` / `rgba(...)` the whitespace
        // is irrelevant. Scan from the right for the first top-level
        // whitespace that follows a digit-or-%, and return that index.
        int depth = 0;
        for (int i = part.length() - 1; i > 0; i--) {
            char c = part.charAt(i);
            if (c == ')') {
                depth++;
            } else if (c == '(') {
                depth--;
            } else if (depth == 0 && (c == ' ' || c == '\t')) {
                String tail = part.substring(i + 1).trim();
                if (tail.length() > 0 && (tail.endsWith("%") || isNumericStart(tail))) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private static boolean isNumericStart(String s) {
        if (s.length() == 0) {
            return false;
        }
        char c = s.charAt(0);
        return c == '-' || c == '+' || c == '.' || (c >= '0' && c <= '9');
    }

    private static float parseFloat(String s) {
        return Float.parseFloat(s.trim());
    }

    /// Splits a top-level comma-separated list, respecting nested parens.
    private static List<String> splitTopLevel(String s, char delim) {
        List<String> out = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == delim && depth == 0) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }

    /// Splits a header (the section before the first stop) on whitespace,
    /// keeping `(` / `)` groups together so `rgba(0, 0, 0, 0)` survives.
    private static String[] splitWhitespaceTopLevel(String s) {
        List<String> out = new ArrayList<String>();
        int depth = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
                sb.append(c);
            } else if (c == ')') {
                depth--;
                sb.append(c);
            } else if (depth == 0 && (c == ' ' || c == '\t')) {
                if (sb.length() > 0) {
                    out.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            out.add(sb.toString());
        }
        return out.toArray(new String[out.size()]);
    }

    static final class Stops {
        int[] colors;
        float[] positions;
    }
}
