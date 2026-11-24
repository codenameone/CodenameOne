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

/// Port of `TransformConstraint`: blends the parent's whole world transform
/// towards the target's.
public class TransformConstraint extends TransformConstraintBase {
    private final Mat2D transformB = new Mat2D();
    private final Mat2D inverse = new Mat2D();
    private final float[] componentsA = new float[6];
    private final float[] componentsB = new float[6];

    @Override
    public void constrain(TransformComponent component) {
        TransformComponent tgt = target;
        if (tgt == null || tgt.isCollapsed()) {
            return;
        }
        // A plain transform component has empty constraint bounds, so the
        // origin offset is zero (layouts are not handled here).
        transformB.set(tgt.getWorldTransform());
        if (mSourceSpaceValue == SPACE_LOCAL) {
            if (!inverse.invert(getParentWorld(tgt))) {
                return;
            }
            transformB.multiply(inverse, transformB);
        }
        if (mDestSpaceValue == SPACE_LOCAL) {
            transformB.multiply(getParentWorld(component), transformB);
        }
        constrainWorld(component.getWorldTransform(), transformB, componentsA, componentsB, mStrength);
    }

    /// Port of `TransformConstraint::constrainWorld`: writes the blend of
    /// `from` towards `to` into `from`.
    static void constrainWorld(Mat2D from, Mat2D to, float[] a, float[] b, float strength) {
        from.decompose(a);
        to.decompose(b);
        float angleA = a[2] % PI2;
        float angleB = b[2] % PI2;
        float diff = wrapDiff(angleB - angleA);
        float t = strength;
        float ti = 1.0f - t;
        b[2] = angleA + diff * t;
        b[0] = a[0] * ti + b[0] * t;
        b[1] = a[1] * ti + b[1] * t;
        b[3] = a[3] * ti + b[3] * t;
        b[4] = a[4] * ti + b[4] * t;
        b[5] = a[5] * ti + b[5] * t;
        from.recompose(b);
    }

    @Override
    protected void originXChanged() {
        markConstraintDirty();
    }

    @Override
    protected void originYChanged() {
        markConstraintDirty();
    }
}
