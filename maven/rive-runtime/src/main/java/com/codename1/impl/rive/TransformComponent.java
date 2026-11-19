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

/// A component with a local transform (rotation, scale and, in subclasses,
/// translation) composed onto its parent's world transform.
public class TransformComponent extends TransformComponentBase {
    protected final Mat2D transform = new Mat2D();
    protected WorldTransformComponent parentTransform;
    protected float renderOpacity = 1f;
    private ArrayList<Constraint> constraints;

    public float getX() {
        return 0f;
    }

    public float getY() {
        return 0f;
    }

    public Mat2D getTransform() {
        return transform;
    }

    public float getRenderOpacity() {
        return renderOpacity;
    }

    @Override
    public float childOpacity() {
        return renderOpacity;
    }

    public WorldTransformComponent getParentTransform() {
        return parentTransform;
    }

    /// The constraints applied after this component's world transform is
    /// composed, in order; empty when there are none.
    public ArrayList<Constraint> getConstraints() {
        if (constraints == null) {
            constraints = new ArrayList<Constraint>();
        }
        return constraints;
    }

    public void addConstraint(Constraint c) {
        if (constraints == null) {
            constraints = new ArrayList<Constraint>();
        }
        constraints.add(c);
    }

    @Override
    public int onAddedClean(Artboard context) {
        parentTransform = parent instanceof WorldTransformComponent ? (WorldTransformComponent) parent : null;
        return STATUS_OK;
    }

    @Override
    public void buildDependencies() {
        if (parent != null) {
            parent.addDependent(this);
        }
    }

    public void markTransformDirty() {
        if (!addDirt(DIRT_TRANSFORM)) {
            return;
        }
        markWorldTransformDirty();
    }

    public void updateTransform() {
        transform.compose(getX(), getY(), mRotation, mScaleX, mScaleY);
    }

    /// Port of `TransformComponent::composeWorldTransform`: parent world times
    /// local, without constraints. IK re-composes solved bones with it.
    public void composeWorldTransform() {
        if (parentTransform != null) {
            worldTransform.multiply(parentTransform.getWorldTransform(), transform);
        } else {
            worldTransform.set(transform);
        }
    }

    public void updateWorldTransform() {
        composeWorldTransform();
        if (constraints != null) {
            for (int i = 0; i < constraints.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                constraints.get(i).constrain(this);
            }
        }
    }

    @Override
    public void update(int value) {
        if (hasDirt(value, DIRT_TRANSFORM)) {
            updateTransform();
        }
        if (hasDirt(value, DIRT_WORLD_TRANSFORM)) {
            updateWorldTransform();
        }
        if (hasDirt(value, DIRT_RENDER_OPACITY)) {
            renderOpacity = mOpacity;
            if (parentTransform != null) {
                renderOpacity *= parentTransform.childOpacity();
            }
        }
    }

    @Override
    protected void rotationChanged() {
        markTransformDirty();
    }

    @Override
    protected void scaleXChanged() {
        markTransformDirty();
    }

    @Override
    protected void scaleYChanged() {
        markTransformDirty();
    }
}
