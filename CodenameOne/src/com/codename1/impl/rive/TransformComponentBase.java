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

/// Generated from rive-runtime's `TransformComponentBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TransformComponent`.
public abstract class TransformComponentBase extends WorldTransformComponent {
    public static final int TYPE_KEY = 38;
    public static final int ROTATION_PROPERTY_KEY = 15;
    public static final int SCALE_X_PROPERTY_KEY = 16;
    public static final int SCALE_Y_PROPERTY_KEY = 17;

    protected float mRotation = 0.0f;
    protected float mScaleX = 1.0f;
    protected float mScaleY = 1.0f;

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

    public float getScaleX() {
        return mScaleX;
    }

    public void setScaleX(float value) {
        if (Float.floatToIntBits(mScaleX) == Float.floatToIntBits(value)) {
            return;
        }
        mScaleX = value;
        scaleXChanged();
    }

    protected void scaleXChanged() {
    }

    public float getScaleY() {
        return mScaleY;
    }

    public void setScaleY(float value) {
        if (Float.floatToIntBits(mScaleY) == Float.floatToIntBits(value)) {
            return;
        }
        mScaleY = value;
        scaleYChanged();
    }

    protected void scaleYChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case ROTATION_PROPERTY_KEY:
                mRotation = value;
                return true;
            case SCALE_X_PROPERTY_KEY:
                mScaleX = value;
                return true;
            case SCALE_Y_PROPERTY_KEY:
                mScaleY = value;
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
            case SCALE_X_PROPERTY_KEY:
                setScaleX(value);
                return true;
            case SCALE_Y_PROPERTY_KEY:
                setScaleY(value);
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
            case SCALE_X_PROPERTY_KEY:
                return mScaleX;
            case SCALE_Y_PROPERTY_KEY:
                return mScaleY;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
