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

/// Port of `RotationConstraint`.
public class RotationConstraint extends RotationConstraintBase {
    private final Mat2D transformB = new Mat2D();
    private final Mat2D inverse = new Mat2D();
    private final float[] ca = new float[6];
    private final float[] cb = new float[6];

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
        Mat2D transformA = component.getWorldTransform();
        transformA.decompose(ca);
        if (tgt == null) {
            transformB.set(transformA);
            System.arraycopy(ca, 0, cb, 0, 6);
        } else {
            transformB.set(tgt.getWorldTransform());
            if (mSourceSpaceValue == SPACE_LOCAL) {
                if (!inverse.invert(getParentWorld(tgt))) {
                    return;
                }
                transformB.multiply(inverse, transformB);
            }
            transformB.decompose(cb);
            if (!mDoesCopy) {
                cb[2] = mDestSpaceValue == SPACE_LOCAL ? 0f : ca[2];
            } else {
                cb[2] *= mCopyFactor;
                if (mOffset) {
                    cb[2] += component.getRotation();
                }
            }
            if (mDestSpaceValue == SPACE_LOCAL) {
                transformB.recompose(cb);
                transformB.multiply(getParentWorld(component), transformB);
                transformB.decompose(cb);
            }
        }
        boolean clampLocal = mMinMaxSpaceValue == SPACE_LOCAL;
        if (clampLocal) {
            transformB.recompose(cb);
            if (!inverse.invert(getParentWorld(component))) {
                return;
            }
            transformB.multiply(inverse, transformB);
            transformB.decompose(cb);
        }
        if (mMin || mMax) {
            float center = mMin && mMax ? (mMinValue + mMaxValue) * 0.5f : mMax ? mMaxValue : mMinValue;
            float rotation = cb[2] - center;
            rotation -= PI2 * (float) Math.floor((rotation + PI) / PI2);
            rotation += center;
            if (mMax && rotation > mMaxValue) {
                rotation = mMaxValue;
            }
            if (mMin && rotation < mMinValue) {
                rotation = mMinValue;
            }
            cb[2] = rotation;
        }
        if (clampLocal) {
            transformB.recompose(cb);
            transformB.multiply(getParentWorld(component), transformB);
            transformB.decompose(cb);
        }
        float angleA = ca[2] % PI2;
        float angleB = cb[2] % PI2;
        float diff = wrapDiff(angleB - angleA);
        ca[2] = ca[2] + diff * mStrength;
        transformA.recompose(ca);
    }
}
