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

/// Port of `FollowPathConstraint`: places (and optionally orients) the parent
/// at a fraction of the target shape's or path's world contour.
public class FollowPathConstraint extends FollowPathConstraintBase {
    private final RivePath rawPath = new RivePath();
    private final RivePathMeasure measure = new RivePathMeasure();
    private final Mat2D transformB = new Mat2D();
    private final Mat2D inverse = new Mat2D();
    private final float[] posTan = new float[4];
    private final float[] ca = new float[6];
    private final float[] cb = new float[6];

    @Override
    protected void distanceChanged() {
        markConstraintDirty();
    }

    @Override
    protected void orientChanged() {
        markConstraintDirty();
    }

    @Override
    protected void offsetChanged() {
        markConstraintDirty();
    }

    private void targetTransform(float distanceOffset) {
        TransformComponent tgt = target;
        transformB.set(tgt.getWorldTransform());
        if (!(tgt instanceof Shape) && !(tgt instanceof Path)) {
            return;
        }
        measure.atPercentage(distanceOffset, posTan);
        if (mOrient) {
            transformB.decompose(cb);
            float tangentRotation = (float) com.codename1.util.MathUtil.atan2(posTan[3], posTan[2]);
            float angleB = cb[2] % PI2;
            float diff = wrapDiff(tangentRotation - angleB);
            transformB.compose(0, 0, angleB + diff * mStrength, 1, 1);
        }
        float ox = 0;
        float oy = 0;
        if (mOffset && parent instanceof TransformComponent) {
            ox = ((TransformComponent) parent).getX();
            oy = ((TransformComponent) parent).getY();
        }
        transformB.v[4] = posTan[0] + ox;
        transformB.v[5] = posTan[1] + oy;
    }

    @Override
    public void constrain(TransformComponent component) {
        TransformComponent tgt = target;
        if (tgt == null || tgt.isCollapsed()) {
            return;
        }
        targetTransform(mDistance);
        Mat2D a = component.getWorldTransform();
        if (mSourceSpaceValue == SPACE_LOCAL) {
            if (!inverse.invert(getParentWorld(tgt))) {
                // A default TransformComponents: origin, no rotation, scale 1.
                for (int i = 0; i < 6; i++) {
                    cb[i] = i == 3 || i == 4 ? 1f : 0f;
                }
                a.recompose(cb);
                return;
            }
            transformB.multiply(inverse, transformB);
        }
        if (mDestSpaceValue == SPACE_LOCAL) {
            transformB.multiply(getParentWorld(component), transformB);
        }
        a.decompose(ca);
        transformB.decompose(cb);
        float t = mStrength;
        float ti = 1.0f - t;
        if (!mOrient) {
            cb[2] = ca[2] % PI2;
        }
        cb[0] = ca[0] * ti + cb[0] * t;
        cb[1] = ca[1] * ti + cb[1] * t;
        cb[3] = ca[3];
        cb[4] = ca[4];
        cb[5] = ca[5];
        a.recompose(cb);
    }

    @Override
    public void update(int value) {
        if (value == DIRT_RENDER_OPACITY || target == null) {
            return;
        }
        rawPath.rewind();
        if (target instanceof Shape) {
            ArrayList<Path> paths = ((Shape) target).getPaths();
            for (int i = 0; i < paths.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                Path p = paths.get(i);
                rawPath.addPath(p.getRawPath(), p.getPathTransform());
            }
        } else if (target instanceof Path) {
            Path p = (Path) target;
            rawPath.addPath(p.getRawPath(), p.getPathTransform());
        }
        measure.build(rawPath);
    }

    @Override
    public void buildDependencies() {
        TransformComponent tgt = target;
        if (tgt instanceof Shape) {
            PathComposer composer = ((Shape) tgt).getComposer();
            if (composer != null) {
                composer.addDependent(this);
            } else {
                tgt.addDependent(this);
            }
        } else if (tgt instanceof Path) {
            Shape shape = ((Path) tgt).getShape();
            if (shape != null && shape.getComposer() != null) {
                shape.getComposer().addDependent(this);
            } else {
                tgt.addDependent(this);
            }
        }
        if (parent != null) {
            addDependent(parent);
        }
    }
}
