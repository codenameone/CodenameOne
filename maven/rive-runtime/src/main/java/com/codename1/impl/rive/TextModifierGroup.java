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

/// Rive `TextModifierGroup`: per-glyph opacity, translation, rotation and
/// scale, weighted by the coverage its ranges give each character.
///
/// Not ported: follow-path and variation modifiers (glyphs keep their
/// layout position; variable font axes are ignored).
public class TextModifierGroup extends TextModifierGroupBase {
    public static final int MODIFY_ORIGIN = 1;
    public static final int MODIFY_TRANSLATION = 1 << 2;
    public static final int MODIFY_ROTATION = 1 << 3;
    public static final int MODIFY_SCALE = 1 << 4;
    public static final int MODIFY_OPACITY = 1 << 5;
    public static final int INVERT_OPACITY = 1 << 6;

    private final ArrayList<TextModifierRange> ranges = new ArrayList<TextModifierRange>();
    private float[] coverage = new float[0];
    private final Mat2D parts = new Mat2D();
    private final Mat2D tmp = new Mat2D();

    @Override
    public int onAddedClean(Artboard context) {
        int code = super.onAddedClean(context);
        ArrayList<Component> kids = getChildren();
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            if (kids.get(i) instanceof TextModifierRange) {
                ranges.add((TextModifierRange) kids.get(i));
            }
        }
        if (parent instanceof Text) {
            ((Text) parent).addModifierGroup(this);
        }
        return code;
    }

    /// Computes the coverage of every code point. `units` holds, per range
    /// unit kind, the unit index of each code point and, at `count`, the
    /// number of units.
    void computeCoverage(int count, int[][] units) {
        if (coverage.length < count) {
            coverage = new float[count];
        }
        for (int i = 0; i < count; i++) {
            coverage[i] = 0f;
        }
        for (int i = 0; i < ranges.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            TextModifierRange r = ranges.get(i);
            int kind = Math.max(0, Math.min(3, r.getUnitsValue()));
            int[] u = units[kind];
            r.computeCoverage(coverage, u, count, u[count]);
        }
    }

    float glyphCoverage(int textIndex, int codePointCount) {
        if (textIndex < 0 || textIndex >= coverage.length) {
            return 0f;
        }
        float c = coverage[textIndex];
        int n = Math.max(1, codePointCount);
        for (int i = 1; i < n && textIndex + i < coverage.length; i++) {
            c += coverage[textIndex + i];
        }
        return c / n;
    }

    boolean modifiesOpacity() {
        return (mModifierFlags & MODIFY_OPACITY) != 0;
    }

    float computeOpacity(float current, float t) {
        if ((mModifierFlags & INVERT_OPACITY) != 0) {
            return current * (1f - t) + mOpacity * t;
        }
        return current * mOpacity * t;
    }

    /// Premultiplies `ctm` by this group's transform weighted by `amount`.
    void transform(float amount, Mat2D ctm) {
        int f = mModifierFlags;
        if (amount == 0f || (f & (MODIFY_TRANSLATION | MODIFY_ROTATION | MODIFY_SCALE)) == 0) {
            return;
        }
        float x = 0f;
        float y = 0f;
        float sx = 1f;
        float sy = 1f;
        float rot = 0f;
        if ((f & MODIFY_TRANSLATION) != 0) {
            x = mX * amount;
            y = mY * amount;
        }
        if ((f & MODIFY_SCALE) != 0) {
            float ia = 1f - amount;
            sx = ia + mScaleX * amount;
            sy = ia + mScaleY * amount;
        }
        if ((f & MODIFY_ROTATION) != 0) {
            rot = mRotation * amount;
        }
        parts.compose(x, y, rot, sx, sy);
        boolean origin = (f & MODIFY_ORIGIN) != 0;
        if (origin) {
            ctm.v[4] += mOriginX;
            ctm.v[5] += mOriginY;
        }
        tmp.multiply(parts, ctm);
        ctm.set(tmp);
        if (origin) {
            ctm.v[4] -= mOriginX;
            ctm.v[5] -= mOriginY;
        }
    }

    private void changed() {
        if (parent instanceof Text) {
            parent.addDirt(DIRT_TEXT_SHAPE);
        }
    }

    @Override
    protected void modifierFlagsChanged() {
        changed();
    }

    @Override
    protected void originXChanged() {
        changed();
    }

    @Override
    protected void originYChanged() {
        changed();
    }

    @Override
    protected void opacityChanged() {
        changed();
    }

    @Override
    protected void xChanged() {
        changed();
    }

    @Override
    protected void yChanged() {
        changed();
    }

    @Override
    protected void rotationChanged() {
        changed();
    }

    @Override
    protected void scaleXChanged() {
        changed();
    }

    @Override
    protected void scaleYChanged() {
        changed();
    }
}
