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

/// A point of a path or mesh. When skinned, a `Weight` child supplies the
/// deformed position through [#setDeformed(float, float)].
public class Vertex extends VertexBase {
    protected float renderX;
    protected float renderY;
    protected boolean deformed;
    protected Weight weight;

    public float getRenderX() {
        return deformed ? renderX : mX;
    }

    public float getRenderY() {
        return deformed ? renderY : mY;
    }

    public Weight getWeight() {
        return weight;
    }

    void setWeight(Weight w) {
        weight = w;
    }

    public void setDeformed(float x, float y) {
        renderX = x;
        renderY = y;
        deformed = true;
    }

    private static final float[] DEFORM_OUT = new float[2];

    /// Port of `Vertex::deform`. A skinned vertex with no weight holds its
    /// bind position.
    public void deform(Mat2D world, float[] boneTransforms) {
        if (weight == null) {
            return;
        }
        Weight.deform(mX, mY, weight.mIndices, weight.mValues, world, boneTransforms, DEFORM_OUT);
        setDeformed(DEFORM_OUT[0], DEFORM_OUT[1]);
    }

    protected void markGeometryDirty() {
        if (parent != null) {
            parent.addDirt(DIRT_VERTICES | DIRT_PATH, false);
            if (parent instanceof Path) {
                ((Path) parent).markPathDirty();
            }
        }
    }

    @Override
    protected void xChanged() {
        markGeometryDirty();
    }

    @Override
    protected void yChanged() {
        markGeometryDirty();
    }
}
