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

/// A vertex with in and out control points.
public abstract class CubicVertex extends CubicVertexBase {
    protected float inX;
    protected float inY;
    protected float outX;
    protected float outY;
    protected boolean inDeformed;
    protected boolean outDeformed;

    /// Computes the undeformed control points into in/out fields.
    protected abstract void computeControls();

    public float getInX() {
        computeControls();
        return inX;
    }

    public float getInY() {
        computeControls();
        return inY;
    }

    public float getOutX() {
        computeControls();
        return outX;
    }

    public float getOutY() {
        computeControls();
        return outY;
    }

    private float dIx;
    private float dIy;
    private float dOx;
    private float dOy;

    public void setDeformedIn(float x, float y) {
        dIx = x;
        dIy = y;
        inDeformed = true;
    }

    public void setDeformedOut(float x, float y) {
        dOx = x;
        dOy = y;
        outDeformed = true;
    }

    public float getRenderInX() {
        return inDeformed ? dIx : getInX();
    }

    public float getRenderInY() {
        return inDeformed ? dIy : getInY();
    }

    public float getRenderOutX() {
        return outDeformed ? dOx : getOutX();
    }

    public float getRenderOutY() {
        return outDeformed ? dOy : getOutY();
    }

    private static final float[] CUBIC_OUT = new float[2];

    /// Port of `CubicVertex::deform`: also deforms the control points by the
    /// cubic weight's in and out influences.
    @Override
    public void deform(Mat2D world, float[] boneTransforms) {
        super.deform(world, boneTransforms);
        if (!(weight instanceof CubicWeight)) {
            return;
        }
        CubicWeight cw = (CubicWeight) weight;
        Weight.deform(getInX(), getInY(), cw.mInIndices, cw.mInValues, world, boneTransforms, CUBIC_OUT);
        setDeformedIn(CUBIC_OUT[0], CUBIC_OUT[1]);
        Weight.deform(getOutX(), getOutY(), cw.mOutIndices, cw.mOutValues, world, boneTransforms, CUBIC_OUT);
        setDeformedOut(CUBIC_OUT[0], CUBIC_OUT[1]);
    }
}
