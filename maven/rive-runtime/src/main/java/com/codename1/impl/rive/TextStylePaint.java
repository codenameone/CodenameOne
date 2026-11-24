/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import java.util.ArrayList;

/// Rive `TextStylePaint`: a text style that owns fills and strokes. A text
/// draws the glyph outlines it lays out in this style with the style's
/// paints, so gradients, strokes and blend modes behave as on a shape.
public class TextStylePaint extends TextStylePaintBase {
    private final ArrayList<ShapePaint> paints = new ArrayList<ShapePaint>();
    private final RivePaint scratch = new RivePaint();
    private float renderOpacity = 1f;
    private int blendMode = 3;

    void addPaint(ShapePaint p) {
        paints.add(p);
    }

    public ArrayList<ShapePaint> getPaints() {
        return paints;
    }

    float getRenderOpacity() {
        return renderOpacity;
    }

    int getTextBlendMode() {
        return blendMode;
    }

    /// Recomputes the paints for the owning text's opacity and blend mode.
    void updatePaints(float opacity, int textBlendMode) {
        renderOpacity = opacity;
        blendMode = textBlendMode;
        for (int i = 0; i < paints.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            paints.get(i).updatePaint(opacity);
        }
    }

    /// Draws `path` (glyphs a text laid out in this style) with every visible
    /// paint of the style, faded by `opacity`.
    void drawPath(RiveRenderer renderer, RivePath path, float opacity) {
        for (int i = 0; i < paints.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            ShapePaint p = paints.get(i);
            if (!p.isVisible() || p.isCollapsed()) {
                continue;
            }
            path.fillRule = p instanceof Fill ? ((Fill) p).getFillRule() : RivePath.FILL_NON_ZERO;
            renderer.drawPath(path, opacity >= 1f ? p.getRenderPaint() : fade(p.getRenderPaint(), opacity));
        }
    }

    private RivePaint fade(RivePaint src, float o) {
        RivePaint d = scratch;
        d.stroke = src.stroke;
        d.color = alpha(src.color, o);
        d.thickness = src.thickness;
        d.cap = src.cap;
        d.join = src.join;
        d.blendMode = src.blendMode;
        d.gradient = src.gradient;
        d.startX = src.startX;
        d.startY = src.startY;
        d.endX = src.endX;
        d.endY = src.endY;
        if (d.stopColors.length < src.stopCount) {
            d.stopColors = new int[src.stopCount];
            d.stopPositions = new float[src.stopCount];
        }
        for (int i = 0; i < src.stopCount; i++) {
            d.stopColors[i] = alpha(src.stopColors[i], o);
            d.stopPositions[i] = src.stopPositions[i];
        }
        d.stopCount = src.stopCount;
        d.version++;
        return d;
    }

    private static int alpha(int argb, float o) {
        int a = (int) (((argb >>> 24) & 0xff) * Math.max(0f, o) + 0.5f);
        return Math.min(255, a) << 24 | argb & 0xffffff;
    }
}
