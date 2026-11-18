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

/// Generated from rive-runtime's `TransformComponentConstraintYBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TransformComponentConstraintY`.
public abstract class TransformComponentConstraintYBase extends TransformComponentConstraint {
    public static final int TYPE_KEY = 86;
    public static final int COPY_FACTOR_Y_PROPERTY_KEY = 185;
    public static final int MIN_VALUE_Y_PROPERTY_KEY = 186;
    public static final int MAX_VALUE_Y_PROPERTY_KEY = 187;
    public static final int DOES_COPY_Y_PROPERTY_KEY = 192;
    public static final int MIN_Y_PROPERTY_KEY = 193;
    public static final int MAX_Y_PROPERTY_KEY = 194;

    protected float mCopyFactorY = 1.0f;
    protected float mMinValueY = 0.0f;
    protected float mMaxValueY = 0.0f;
    protected boolean mDoesCopyY = true;
    protected boolean mMinY = false;
    protected boolean mMaxY = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getCopyFactorY() {
        return mCopyFactorY;
    }

    public void setCopyFactorY(float value) {
        if (Float.floatToIntBits(mCopyFactorY) == Float.floatToIntBits(value)) {
            return;
        }
        mCopyFactorY = value;
        copyFactorYChanged();
    }

    protected void copyFactorYChanged() {
    }

    public float getMinValueY() {
        return mMinValueY;
    }

    public void setMinValueY(float value) {
        if (Float.floatToIntBits(mMinValueY) == Float.floatToIntBits(value)) {
            return;
        }
        mMinValueY = value;
        minValueYChanged();
    }

    protected void minValueYChanged() {
    }

    public float getMaxValueY() {
        return mMaxValueY;
    }

    public void setMaxValueY(float value) {
        if (Float.floatToIntBits(mMaxValueY) == Float.floatToIntBits(value)) {
            return;
        }
        mMaxValueY = value;
        maxValueYChanged();
    }

    protected void maxValueYChanged() {
    }

    public boolean isDoesCopyY() {
        return mDoesCopyY;
    }

    public void setDoesCopyY(boolean value) {
        if (mDoesCopyY == value) {
            return;
        }
        mDoesCopyY = value;
        doesCopyYChanged();
    }

    protected void doesCopyYChanged() {
    }

    public boolean isMinY() {
        return mMinY;
    }

    public void setMinY(boolean value) {
        if (mMinY == value) {
            return;
        }
        mMinY = value;
        minYChanged();
    }

    protected void minYChanged() {
    }

    public boolean isMaxY() {
        return mMaxY;
    }

    public void setMaxY(boolean value) {
        if (mMaxY == value) {
            return;
        }
        mMaxY = value;
        maxYChanged();
    }

    protected void maxYChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case DOES_COPY_Y_PROPERTY_KEY:
                mDoesCopyY = value;
                return true;
            case MIN_Y_PROPERTY_KEY:
                mMinY = value;
                return true;
            case MAX_Y_PROPERTY_KEY:
                mMaxY = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case DOES_COPY_Y_PROPERTY_KEY:
                setDoesCopyY(value);
                return true;
            case MIN_Y_PROPERTY_KEY:
                setMinY(value);
                return true;
            case MAX_Y_PROPERTY_KEY:
                setMaxY(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case DOES_COPY_Y_PROPERTY_KEY:
                return mDoesCopyY;
            case MIN_Y_PROPERTY_KEY:
                return mMinY;
            case MAX_Y_PROPERTY_KEY:
                return mMaxY;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case COPY_FACTOR_Y_PROPERTY_KEY:
                mCopyFactorY = value;
                return true;
            case MIN_VALUE_Y_PROPERTY_KEY:
                mMinValueY = value;
                return true;
            case MAX_VALUE_Y_PROPERTY_KEY:
                mMaxValueY = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case COPY_FACTOR_Y_PROPERTY_KEY:
                setCopyFactorY(value);
                return true;
            case MIN_VALUE_Y_PROPERTY_KEY:
                setMinValueY(value);
                return true;
            case MAX_VALUE_Y_PROPERTY_KEY:
                setMaxValueY(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case COPY_FACTOR_Y_PROPERTY_KEY:
                return mCopyFactorY;
            case MIN_VALUE_Y_PROPERTY_KEY:
                return mMinValueY;
            case MAX_VALUE_Y_PROPERTY_KEY:
                return mMaxValueY;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
