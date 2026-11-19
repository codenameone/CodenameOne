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

/// Port of `Constraint`: adjusts its parent transform component's world
/// transform right after the parent composes it, in the order the
/// constraints were added.
public class Constraint extends ConstraintBase {
    static final float PI = (float) Math.PI;
    static final float PI2 = (float) (Math.PI * 2);
    static final int SPACE_WORLD = 0;
    static final int SPACE_LOCAL = 1;
    private static final Mat2D IDENTITY = new Mat2D();

    @Override
    public int onAddedClean(Artboard context) {
        if (parent instanceof TransformComponent) {
            ((TransformComponent) parent).addConstraint(this);
        }
        return STATUS_OK;
    }

    @Override
    public void buildDependencies() {
        if (parent != null) {
            parent.addDependent(this);
        }
    }

    /// Adjusts `component`'s world transform. The base constraint does nothing.
    public void constrain(TransformComponent component) {
    }

    public void markConstraintDirty() {
        if (parent instanceof TransformComponent) {
            ((TransformComponent) parent).markTransformDirty();
        }
    }

    @Override
    protected void onDirty(int value) {
        if ((value & ~DIRT_COLLAPSED) == DIRT_RENDER_OPACITY) {
            return;
        }
        markConstraintDirty();
    }

    @Override
    protected void strengthChanged() {
        markConstraintDirty();
    }

    /// Port of `getParentWorld`: the parent's world transform, or identity.
    static Mat2D getParentWorld(TransformComponent component) {
        ContainerComponent p = component.getParent();
        if (p instanceof WorldTransformComponent) {
            return ((WorldTransformComponent) p).getWorldTransform();
        }
        return IDENTITY;
    }

    /// Wraps an angle difference into (-PI, PI] the way upstream does.
    static float wrapDiff(float diff) {
        if (diff > PI) {
            return diff - PI2;
        }
        if (diff < -PI) {
            return diff + PI2;
        }
        return diff;
    }
}
