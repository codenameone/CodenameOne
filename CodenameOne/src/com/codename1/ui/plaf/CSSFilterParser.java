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
package com.codename1.ui.plaf;

import java.util.ArrayList;
import java.util.List;

/// Runtime parser for CSS `filter:` / `backdrop-filter:` chains. Mirrors
/// the build-time css-compiler's parser. Returns a [Style.FilterChain]
/// holding a blur radius (in pixels) and a 4x5 color matrix that
/// composes the chain's color-style functions
/// (brightness / contrast / grayscale / hue-rotate / invert / opacity /
/// saturate / sepia). The matrix is null when the chain reduces to the
/// identity transform (no color-style functions present, or all of them
/// at their no-op argument).
public final class CSSFilterParser {
    private CSSFilterParser() {
    }

    /// Parsed filter chain payload.
    public static final class FilterChain {
        public final float blurRadius;
        public final float[] colorMatrix;

        FilterChain(float blurRadius, float[] colorMatrix) {
            this.blurRadius = blurRadius;
            this.colorMatrix = colorMatrix;
        }
    }

    /// Parses a CSS filter chain. Returns null when `css` is null, empty,
    /// or literally `none`. Throws `IllegalArgumentException` when a
    /// function name or argument is unrecognised.
    public static FilterChain parse(String css) {
        if (css == null) {
            return null;
        }
        String s = css.trim();
        if (s.length() == 0 || "none".equalsIgnoreCase(s)) {
            return null;
        }
        List<String> calls = splitFunctions(s);
        float blurRadius = 0f;
        float[] matrix = null;
        for (String rawCall : calls) {
            String call = rawCall.trim();
            int open = call.indexOf('(');
            if (open < 0 || !call.endsWith(")")) {
                throw new IllegalArgumentException("Bad filter function: " + call);
            }
            String name = call.substring(0, open).trim().toLowerCase();
            String arg = call.substring(open + 1, call.length() - 1).trim();
            if ("blur".equals(name)) {
                blurRadius += parseLengthPx(arg);
                continue;
            }
            float[] m = colorMatrixForFunction(name, arg);
            matrix = (matrix == null) ? m : compose(m, matrix);
        }
        if (matrix != null && isIdentity(matrix)) {
            matrix = null;
        }
        return new FilterChain(blurRadius, matrix);
    }

    private static float[] colorMatrixForFunction(String name, String arg) {
        if ("brightness".equals(name)) {
            return brightnessMatrix(parseAmount(arg, 1f));
        }
        if ("contrast".equals(name)) {
            return contrastMatrix(parseAmount(arg, 1f));
        }
        if ("grayscale".equals(name)) {
            return grayscaleMatrix(clamp01(parseAmount(arg, 1f)));
        }
        if ("invert".equals(name)) {
            return invertMatrix(clamp01(parseAmount(arg, 1f)));
        }
        if ("opacity".equals(name)) {
            return opacityMatrix(clamp01(parseAmount(arg, 1f)));
        }
        if ("saturate".equals(name)) {
            return saturateMatrix(parseAmount(arg, 1f));
        }
        if ("sepia".equals(name)) {
            return sepiaMatrix(clamp01(parseAmount(arg, 1f)));
        }
        if ("hue-rotate".equals(name)) {
            return hueRotateMatrix(parseAngleDeg(arg));
        }
        throw new IllegalArgumentException("Unknown filter function: " + name);
    }

    private static float parseAmount(String arg, float defaultValue) {
        if (arg.length() == 0) {
            return defaultValue;
        }
        if (arg.endsWith("%")) {
            return Float.parseFloat(arg.substring(0, arg.length() - 1).trim()) / 100f;
        }
        return Float.parseFloat(arg.trim());
    }

    private static float parseAngleDeg(String arg) {
        String lower = arg.toLowerCase();
        if (lower.endsWith("deg")) {
            return Float.parseFloat(lower.substring(0, lower.length() - 3).trim());
        }
        if (lower.endsWith("grad")) {
            return Float.parseFloat(lower.substring(0, lower.length() - 4).trim()) * 0.9f;
        }
        if (lower.endsWith("turn")) {
            return Float.parseFloat(lower.substring(0, lower.length() - 4).trim()) * 360f;
        }
        if (lower.endsWith("rad")) {
            return (float) (Float.parseFloat(lower.substring(0, lower.length() - 3).trim())
                    * 180.0 / Math.PI);
        }
        return Float.parseFloat(arg.trim());
    }

    private static float parseLengthPx(String arg) {
        String lower = arg.trim().toLowerCase();
        if (lower.length() == 0) {
            return 0f;
        }
        if (lower.endsWith("px")) {
            return Float.parseFloat(lower.substring(0, lower.length() - 2).trim());
        }
        return Float.parseFloat(lower);
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 1f) {
            return 1f;
        }
        return v;
    }

    /// Splits "fn1(...) fn2(...) ..." on top-level whitespace boundaries.
    private static List<String> splitFunctions(String s) {
        List<String> out = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    out.add(s.substring(start, i + 1));
                    while (i + 1 < s.length()
                            && (s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\t')) {
                        i++;
                    }
                    start = i + 1;
                }
            }
        }
        if (start < s.length()) {
            String tail = s.substring(start).trim();
            if (tail.length() > 0) {
                out.add(tail);
            }
        }
        return out;
    }

    private static float[] identity() {
        return new float[]{
                1, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 1, 0, 0,
                0, 0, 0, 1, 0
        };
    }

    private static float[] brightnessMatrix(float b) {
        return new float[]{
                b, 0, 0, 0, 0,
                0, b, 0, 0, 0,
                0, 0, b, 0, 0,
                0, 0, 0, 1, 0
        };
    }

    private static float[] contrastMatrix(float c) {
        float offset = 128f * (1f - c);
        return new float[]{
                c, 0, 0, 0, offset,
                0, c, 0, 0, offset,
                0, 0, c, 0, offset,
                0, 0, 0, 1, 0
        };
    }

    private static float[] grayscaleMatrix(float a) {
        // Rec 709 luma weights.
        float rW = 0.2126f;
        float gW = 0.7152f;
        float bW = 0.0722f;
        return new float[]{
                (1f - a) + rW * a, gW * a, bW * a, 0, 0,
                rW * a, (1f - a) + gW * a, bW * a, 0, 0,
                rW * a, gW * a, (1f - a) + bW * a, 0, 0,
                0, 0, 0, 1, 0
        };
    }

    private static float[] invertMatrix(float a) {
        return new float[]{
                1f - 2f * a, 0, 0, 0, 255f * a,
                0, 1f - 2f * a, 0, 0, 255f * a,
                0, 0, 1f - 2f * a, 0, 255f * a,
                0, 0, 0, 1, 0
        };
    }

    private static float[] opacityMatrix(float a) {
        return new float[]{
                1, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 1, 0, 0,
                0, 0, 0, a, 0
        };
    }

    private static float[] saturateMatrix(float s) {
        // Rec 601 luma weights, per the CSS Filter Effects spec.
        float rW = 0.213f;
        float gW = 0.715f;
        float bW = 0.072f;
        return new float[]{
                rW + (1f - rW) * s, gW - gW * s, bW - bW * s, 0, 0,
                rW - rW * s, gW + (1f - gW) * s, bW - bW * s, 0, 0,
                rW - rW * s, gW - gW * s, bW + (1f - bW) * s, 0, 0,
                0, 0, 0, 1, 0
        };
    }

    private static float[] sepiaMatrix(float a) {
        float i = 1f - a;
        return new float[]{
                0.393f * a + i, 0.769f * a, 0.189f * a, 0, 0,
                0.349f * a, 0.686f * a + i, 0.168f * a, 0, 0,
                0.272f * a, 0.534f * a, 0.131f * a + i, 0, 0,
                0, 0, 0, 1, 0
        };
    }

    private static float[] hueRotateMatrix(float deg) {
        double rad = deg * Math.PI / 180.0;
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        return new float[]{
                0.213f + cos * 0.787f - sin * 0.213f,
                0.715f - cos * 0.715f - sin * 0.715f,
                0.072f - cos * 0.072f + sin * 0.928f, 0, 0,
                0.213f - cos * 0.213f + sin * 0.143f,
                0.715f + cos * 0.285f + sin * 0.140f,
                0.072f - cos * 0.072f - sin * 0.283f, 0, 0,
                0.213f - cos * 0.213f - sin * 0.787f,
                0.715f - cos * 0.715f + sin * 0.715f,
                0.072f + cos * 0.928f + sin * 0.072f, 0, 0,
                0, 0, 0, 1, 0
        };
    }

    private static float[] compose(float[] applySecond, float[] applyFirst) {
        // Both are 4x5 matrices treated as 5x5 with an implicit
        // [0,0,0,0,1] row. Multiply applySecond * applyFirst.
        float[] out = new float[20];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 5; c++) {
                float sum = 0f;
                for (int k = 0; k < 4; k++) {
                    sum += applySecond[r * 5 + k] * applyFirst[k * 5 + c];
                }
                if (c == 4) {
                    sum += applySecond[r * 5 + 4];
                }
                out[r * 5 + c] = sum;
            }
        }
        return out;
    }

    private static boolean isIdentity(float[] m) {
        float[] id = identity();
        for (int i = 0; i < 20; i++) {
            if (Math.abs(m[i] - id[i]) > 1e-4f) {
                return false;
            }
        }
        return true;
    }
}
