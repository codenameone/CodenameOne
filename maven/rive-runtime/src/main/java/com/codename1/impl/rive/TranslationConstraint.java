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

/// Port of `TranslationConstraint`.
public class TranslationConstraint extends TranslationConstraintBase {
    private final Mat2D transformB = new Mat2D();
    private final Mat2D inverse = new Mat2D();

    @Override
    public boolean requiresTarget() {
        return false;
    }

    @Override
    public void constrain(TransformComponent component) {
        TransformComponent tgt = target;
        if (tgt != null && tgt.isCollapsed()) {
            return;
        }
        float[] a = component.getWorldTransform().v;
        float ax = a[4];
        float ay = a[5];
        float bx;
        float by;
        if (tgt == null) {
            bx = ax;
            by = ay;
        } else {
            boolean useLocal = mOffset && (mDoesCopy || mDoesCopyY);
            float lx = useLocal ? component.getX() : 0f;
            float ly = useLocal ? component.getY() : 0f;
            transformB.set(tgt.getWorldTransform());
            if (mSourceSpaceValue == SPACE_LOCAL) {
                if (!inverse.invert(getParentWorld(tgt))) {
                    return;
                }
                transformB.multiply(inverse, transformB);
            }
            bx = transformB.v[4];
            by = transformB.v[5];
            boolean destLocal = mDestSpaceValue == SPACE_LOCAL;
            if (!mDoesCopy) {
                bx = destLocal ? 0f : ax;
            } else {
                bx *= mCopyFactor;
                if (mOffset) {
                    bx += lx;
                }
            }
            if (!mDoesCopyY) {
                by = destLocal ? 0f : ay;
            } else {
                by *= mCopyFactorY;
                if (mOffset) {
                    by += ly;
                }
            }
            if (destLocal) {
                Mat2D pw = getParentWorld(component);
                float nx = pw.mapX(bx, by);
                by = pw.mapY(bx, by);
                bx = nx;
            }
        }
        boolean clampLocal = mMinMaxSpaceValue == SPACE_LOCAL;
        if (clampLocal) {
            if (!inverse.invert(getParentWorld(component))) {
                return;
            }
            float nx = inverse.mapX(bx, by);
            by = inverse.mapY(bx, by);
            bx = nx;
        }
        if (mMax && bx > mMaxValue) {
            bx = mMaxValue;
        }
        if (mMin && bx < mMinValue) {
            bx = mMinValue;
        }
        if (mMaxY && by > mMaxValueY) {
            by = mMaxValueY;
        }
        if (mMinY && by < mMinValueY) {
            by = mMinValueY;
        }
        if (clampLocal) {
            Mat2D pw = getParentWorld(component);
            float nx = pw.mapX(bx, by);
            by = pw.mapY(bx, by);
            bx = nx;
        }
        float t = mStrength;
        float ti = 1.0f - t;
        a[4] = ax * ti + bx * t;
        a[5] = ay * ti + by * t;
    }
}
