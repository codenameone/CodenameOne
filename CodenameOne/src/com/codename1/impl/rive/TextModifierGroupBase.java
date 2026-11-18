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

/// Generated from rive-runtime's `TextModifierGroupBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TextModifierGroup`.
public abstract class TextModifierGroupBase extends ContainerComponent {
    public static final int TYPE_KEY = 159;
    public static final int MODIFIER_FLAGS_PROPERTY_KEY = 335;
    public static final int ORIGIN_X_PROPERTY_KEY = 328;
    public static final int ORIGIN_Y_PROPERTY_KEY = 329;
    public static final int OPACITY_PROPERTY_KEY = 324;
    public static final int X_PROPERTY_KEY = 322;
    public static final int Y_PROPERTY_KEY = 323;
    public static final int ROTATION_PROPERTY_KEY = 332;
    public static final int SCALE_X_PROPERTY_KEY = 330;
    public static final int SCALE_Y_PROPERTY_KEY = 331;

    protected int mModifierFlags = 0;
    protected float mOriginX = 0.0f;
    protected float mOriginY = 0.0f;
    protected float mOpacity = 1.0f;
    protected float mX = 0.0f;
    protected float mY = 0.0f;
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

    public int getModifierFlags() {
        return mModifierFlags;
    }

    public void setModifierFlags(int value) {
        if (mModifierFlags == value) {
            return;
        }
        mModifierFlags = value;
        modifierFlagsChanged();
    }

    protected void modifierFlagsChanged() {
    }

    public float getOriginX() {
        return mOriginX;
    }

    public void setOriginX(float value) {
        if (Float.floatToIntBits(mOriginX) == Float.floatToIntBits(value)) {
            return;
        }
        mOriginX = value;
        originXChanged();
    }

    protected void originXChanged() {
    }

    public float getOriginY() {
        return mOriginY;
    }

    public void setOriginY(float value) {
        if (Float.floatToIntBits(mOriginY) == Float.floatToIntBits(value)) {
            return;
        }
        mOriginY = value;
        originYChanged();
    }

    protected void originYChanged() {
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

    public float getX() {
        return mX;
    }

    public void setX(float value) {
        if (Float.floatToIntBits(mX) == Float.floatToIntBits(value)) {
            return;
        }
        mX = value;
        xChanged();
    }

    protected void xChanged() {
    }

    public float getY() {
        return mY;
    }

    public void setY(float value) {
        if (Float.floatToIntBits(mY) == Float.floatToIntBits(value)) {
            return;
        }
        mY = value;
        yChanged();
    }

    protected void yChanged() {
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
            case ORIGIN_X_PROPERTY_KEY:
                mOriginX = value;
                return true;
            case ORIGIN_Y_PROPERTY_KEY:
                mOriginY = value;
                return true;
            case OPACITY_PROPERTY_KEY:
                mOpacity = value;
                return true;
            case X_PROPERTY_KEY:
                mX = value;
                return true;
            case Y_PROPERTY_KEY:
                mY = value;
                return true;
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
            case ORIGIN_X_PROPERTY_KEY:
                setOriginX(value);
                return true;
            case ORIGIN_Y_PROPERTY_KEY:
                setOriginY(value);
                return true;
            case OPACITY_PROPERTY_KEY:
                setOpacity(value);
                return true;
            case X_PROPERTY_KEY:
                setX(value);
                return true;
            case Y_PROPERTY_KEY:
                setY(value);
                return true;
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
            case ORIGIN_X_PROPERTY_KEY:
                return mOriginX;
            case ORIGIN_Y_PROPERTY_KEY:
                return mOriginY;
            case OPACITY_PROPERTY_KEY:
                return mOpacity;
            case X_PROPERTY_KEY:
                return mX;
            case Y_PROPERTY_KEY:
                return mY;
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

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case MODIFIER_FLAGS_PROPERTY_KEY:
                mModifierFlags = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case MODIFIER_FLAGS_PROPERTY_KEY:
                setModifierFlags(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case MODIFIER_FLAGS_PROPERTY_KEY:
                return mModifierFlags;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
