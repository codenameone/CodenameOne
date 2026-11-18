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

/// Generated from rive-runtime's `CubicAsymmetricVertexBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `CubicAsymmetricVertex`.
public abstract class CubicAsymmetricVertexBase extends CubicVertex {
    public static final int TYPE_KEY = 34;
    public static final int ROTATION_PROPERTY_KEY = 79;
    public static final int IN_DISTANCE_PROPERTY_KEY = 80;
    public static final int OUT_DISTANCE_PROPERTY_KEY = 81;

    protected float mRotation = 0.0f;
    protected float mInDistance = 0.0f;
    protected float mOutDistance = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getRotation() {
        return mRotation;
    }

    public void setRotation(float value) {
        if (Float.floatToIntBits(mRotation) == Float.floatToIntBits(value)) {
            return;
        }
        mRotation = value;
        rotationChanged();
    }

    protected void rotationChanged() {
    }

    public float getInDistance() {
        return mInDistance;
    }

    public void setInDistance(float value) {
        if (Float.floatToIntBits(mInDistance) == Float.floatToIntBits(value)) {
            return;
        }
        mInDistance = value;
        inDistanceChanged();
    }

    protected void inDistanceChanged() {
    }

    public float getOutDistance() {
        return mOutDistance;
    }

    public void setOutDistance(float value) {
        if (Float.floatToIntBits(mOutDistance) == Float.floatToIntBits(value)) {
            return;
        }
        mOutDistance = value;
        outDistanceChanged();
    }

    protected void outDistanceChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case ROTATION_PROPERTY_KEY:
                mRotation = value;
                return true;
            case IN_DISTANCE_PROPERTY_KEY:
                mInDistance = value;
                return true;
            case OUT_DISTANCE_PROPERTY_KEY:
                mOutDistance = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case ROTATION_PROPERTY_KEY:
                setRotation(value);
                return true;
            case IN_DISTANCE_PROPERTY_KEY:
                setInDistance(value);
                return true;
            case OUT_DISTANCE_PROPERTY_KEY:
                setOutDistance(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case ROTATION_PROPERTY_KEY:
                return mRotation;
            case IN_DISTANCE_PROPERTY_KEY:
                return mInDistance;
            case OUT_DISTANCE_PROPERTY_KEY:
                return mOutDistance;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
