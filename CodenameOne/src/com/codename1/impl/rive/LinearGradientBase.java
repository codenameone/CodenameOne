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

/// Generated from rive-runtime's `LinearGradientBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `LinearGradient`.
public abstract class LinearGradientBase extends ContainerComponent {
    public static final int TYPE_KEY = 22;
    public static final int START_X_PROPERTY_KEY = 42;
    public static final int START_Y_PROPERTY_KEY = 33;
    public static final int END_X_PROPERTY_KEY = 34;
    public static final int END_Y_PROPERTY_KEY = 35;
    public static final int OPACITY_PROPERTY_KEY = 46;
    public static final int TRANSFORM_X_PROPERTY_KEY = 1103;
    public static final int TRANSFORM_Y_PROPERTY_KEY = 1104;
    public static final int TRANSFORM_ROTATION_PROPERTY_KEY = 1105;
    public static final int TRANSFORM_SCALE_X_PROPERTY_KEY = 1106;
    public static final int TRANSFORM_SCALE_Y_PROPERTY_KEY = 1107;

    protected float mStartX = 0.0f;
    protected float mStartY = 0.0f;
    protected float mEndX = 0.0f;
    protected float mEndY = 0.0f;
    protected float mOpacity = 1.0f;
    protected float mTransformX = 0.0f;
    protected float mTransformY = 0.0f;
    protected float mTransformRotation = 0.0f;
    protected float mTransformScaleX = 1.0f;
    protected float mTransformScaleY = 1.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getStartX() {
        return mStartX;
    }

    public void setStartX(float value) {
        if (Float.floatToIntBits(mStartX) == Float.floatToIntBits(value)) {
            return;
        }
        mStartX = value;
        startXChanged();
    }

    protected void startXChanged() {
    }

    public float getStartY() {
        return mStartY;
    }

    public void setStartY(float value) {
        if (Float.floatToIntBits(mStartY) == Float.floatToIntBits(value)) {
            return;
        }
        mStartY = value;
        startYChanged();
    }

    protected void startYChanged() {
    }

    public float getEndX() {
        return mEndX;
    }

    public void setEndX(float value) {
        if (Float.floatToIntBits(mEndX) == Float.floatToIntBits(value)) {
            return;
        }
        mEndX = value;
        endXChanged();
    }

    protected void endXChanged() {
    }

    public float getEndY() {
        return mEndY;
    }

    public void setEndY(float value) {
        if (Float.floatToIntBits(mEndY) == Float.floatToIntBits(value)) {
            return;
        }
        mEndY = value;
        endYChanged();
    }

    protected void endYChanged() {
    }

    public float getOpacity() {
        return mOpacity;
    }

    public void setOpacity(float value) {
        if (Float.floatToIntBits(mOpacity) == Float.floatToIntBits(value)) {
            return;
        }
        mOpacity = value;
        opacityChanged();
    }

    protected void opacityChanged() {
    }

    public float getTransformX() {
        return mTransformX;
    }

    public void setTransformX(float value) {
        if (Float.floatToIntBits(mTransformX) == Float.floatToIntBits(value)) {
            return;
        }
        mTransformX = value;
        transformXChanged();
    }

    protected void transformXChanged() {
    }

    public float getTransformY() {
        return mTransformY;
    }

    public void setTransformY(float value) {
        if (Float.floatToIntBits(mTransformY) == Float.floatToIntBits(value)) {
            return;
        }
        mTransformY = value;
        transformYChanged();
    }

    protected void transformYChanged() {
    }

    public float getTransformRotation() {
        return mTransformRotation;
    }

    public void setTransformRotation(float value) {
        if (Float.floatToIntBits(mTransformRotation) == Float.floatToIntBits(value)) {
            return;
        }
        mTransformRotation = value;
        transformRotationChanged();
    }

    protected void transformRotationChanged() {
    }

    public float getTransformScaleX() {
        return mTransformScaleX;
    }

    public void setTransformScaleX(float value) {
        if (Float.floatToIntBits(mTransformScaleX) == Float.floatToIntBits(value)) {
            return;
        }
        mTransformScaleX = value;
        transformScaleXChanged();
    }

    protected void transformScaleXChanged() {
    }

    public float getTransformScaleY() {
        return mTransformScaleY;
    }

    public void setTransformScaleY(float value) {
        if (Float.floatToIntBits(mTransformScaleY) == Float.floatToIntBits(value)) {
            return;
        }
        mTransformScaleY = value;
        transformScaleYChanged();
    }

    protected void transformScaleYChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case START_X_PROPERTY_KEY:
                mStartX = value;
                return true;
            case START_Y_PROPERTY_KEY:
                mStartY = value;
                return true;
            case END_X_PROPERTY_KEY:
                mEndX = value;
                return true;
            case END_Y_PROPERTY_KEY:
                mEndY = value;
                return true;
            case OPACITY_PROPERTY_KEY:
                mOpacity = value;
                return true;
            case TRANSFORM_X_PROPERTY_KEY:
                mTransformX = value;
                return true;
            case TRANSFORM_Y_PROPERTY_KEY:
                mTransformY = value;
                return true;
            case TRANSFORM_ROTATION_PROPERTY_KEY:
                mTransformRotation = value;
                return true;
            case TRANSFORM_SCALE_X_PROPERTY_KEY:
                mTransformScaleX = value;
                return true;
            case TRANSFORM_SCALE_Y_PROPERTY_KEY:
                mTransformScaleY = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case START_X_PROPERTY_KEY:
                setStartX(value);
                return true;
            case START_Y_PROPERTY_KEY:
                setStartY(value);
                return true;
            case END_X_PROPERTY_KEY:
                setEndX(value);
                return true;
            case END_Y_PROPERTY_KEY:
                setEndY(value);
                return true;
            case OPACITY_PROPERTY_KEY:
                setOpacity(value);
                return true;
            case TRANSFORM_X_PROPERTY_KEY:
                setTransformX(value);
                return true;
            case TRANSFORM_Y_PROPERTY_KEY:
                setTransformY(value);
                return true;
            case TRANSFORM_ROTATION_PROPERTY_KEY:
                setTransformRotation(value);
                return true;
            case TRANSFORM_SCALE_X_PROPERTY_KEY:
                setTransformScaleX(value);
                return true;
            case TRANSFORM_SCALE_Y_PROPERTY_KEY:
                setTransformScaleY(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case START_X_PROPERTY_KEY:
                return mStartX;
            case START_Y_PROPERTY_KEY:
                return mStartY;
            case END_X_PROPERTY_KEY:
                return mEndX;
            case END_Y_PROPERTY_KEY:
                return mEndY;
            case OPACITY_PROPERTY_KEY:
                return mOpacity;
            case TRANSFORM_X_PROPERTY_KEY:
                return mTransformX;
            case TRANSFORM_Y_PROPERTY_KEY:
                return mTransformY;
            case TRANSFORM_ROTATION_PROPERTY_KEY:
                return mTransformRotation;
            case TRANSFORM_SCALE_X_PROPERTY_KEY:
                return mTransformScaleX;
            case TRANSFORM_SCALE_Y_PROPERTY_KEY:
                return mTransformScaleY;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
