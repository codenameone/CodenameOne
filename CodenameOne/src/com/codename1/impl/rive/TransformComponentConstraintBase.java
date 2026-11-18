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

/// Generated from rive-runtime's `TransformComponentConstraintBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TransformComponentConstraint`.
public abstract class TransformComponentConstraintBase extends TransformSpaceConstraint {
    public static final int TYPE_KEY = 85;
    public static final int MIN_MAX_SPACE_VALUE_PROPERTY_KEY = 195;
    public static final int COPY_FACTOR_PROPERTY_KEY = 182;
    public static final int MIN_VALUE_PROPERTY_KEY = 183;
    public static final int MAX_VALUE_PROPERTY_KEY = 184;
    public static final int OFFSET_PROPERTY_KEY = 188;
    public static final int DOES_COPY_PROPERTY_KEY = 189;
    public static final int MIN_PROPERTY_KEY = 190;
    public static final int MAX_PROPERTY_KEY = 191;

    protected int mMinMaxSpaceValue = 0;
    protected float mCopyFactor = 1.0f;
    protected float mMinValue = 0.0f;
    protected float mMaxValue = 0.0f;
    protected boolean mOffset = false;
    protected boolean mDoesCopy = true;
    protected boolean mMin = false;
    protected boolean mMax = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getMinMaxSpaceValue() {
        return mMinMaxSpaceValue;
    }

    public void setMinMaxSpaceValue(int value) {
        if (mMinMaxSpaceValue == value) {
            return;
        }
        mMinMaxSpaceValue = value;
        minMaxSpaceValueChanged();
    }

    protected void minMaxSpaceValueChanged() {
    }

    public float getCopyFactor() {
        return mCopyFactor;
    }

    public void setCopyFactor(float value) {
        if (Float.floatToIntBits(mCopyFactor) == Float.floatToIntBits(value)) {
            return;
        }
        mCopyFactor = value;
        copyFactorChanged();
    }

    protected void copyFactorChanged() {
    }

    public float getMinValue() {
        return mMinValue;
    }

    public void setMinValue(float value) {
        if (Float.floatToIntBits(mMinValue) == Float.floatToIntBits(value)) {
            return;
        }
        mMinValue = value;
        minValueChanged();
    }

    protected void minValueChanged() {
    }

    public float getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(float value) {
        if (Float.floatToIntBits(mMaxValue) == Float.floatToIntBits(value)) {
            return;
        }
        mMaxValue = value;
        maxValueChanged();
    }

    protected void maxValueChanged() {
    }

    public boolean isOffset() {
        return mOffset;
    }

    public void setOffset(boolean value) {
        if (mOffset == value) {
            return;
        }
        mOffset = value;
        offsetChanged();
    }

    protected void offsetChanged() {
    }

    public boolean isDoesCopy() {
        return mDoesCopy;
    }

    public void setDoesCopy(boolean value) {
        if (mDoesCopy == value) {
            return;
        }
        mDoesCopy = value;
        doesCopyChanged();
    }

    protected void doesCopyChanged() {
    }

    public boolean isMin() {
        return mMin;
    }

    public void setMin(boolean value) {
        if (mMin == value) {
            return;
        }
        mMin = value;
        minChanged();
    }

    protected void minChanged() {
    }

    public boolean isMax() {
        return mMax;
    }

    public void setMax(boolean value) {
        if (mMax == value) {
            return;
        }
        mMax = value;
        maxChanged();
    }

    protected void maxChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case OFFSET_PROPERTY_KEY:
                mOffset = value;
                return true;
            case DOES_COPY_PROPERTY_KEY:
                mDoesCopy = value;
                return true;
            case MIN_PROPERTY_KEY:
                mMin = value;
                return true;
            case MAX_PROPERTY_KEY:
                mMax = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case OFFSET_PROPERTY_KEY:
                setOffset(value);
                return true;
            case DOES_COPY_PROPERTY_KEY:
                setDoesCopy(value);
                return true;
            case MIN_PROPERTY_KEY:
                setMin(value);
                return true;
            case MAX_PROPERTY_KEY:
                setMax(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case OFFSET_PROPERTY_KEY:
                return mOffset;
            case DOES_COPY_PROPERTY_KEY:
                return mDoesCopy;
            case MIN_PROPERTY_KEY:
                return mMin;
            case MAX_PROPERTY_KEY:
                return mMax;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case COPY_FACTOR_PROPERTY_KEY:
                mCopyFactor = value;
                return true;
            case MIN_VALUE_PROPERTY_KEY:
                mMinValue = value;
                return true;
            case MAX_VALUE_PROPERTY_KEY:
                mMaxValue = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case COPY_FACTOR_PROPERTY_KEY:
                setCopyFactor(value);
                return true;
            case MIN_VALUE_PROPERTY_KEY:
                setMinValue(value);
                return true;
            case MAX_VALUE_PROPERTY_KEY:
                setMaxValue(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case COPY_FACTOR_PROPERTY_KEY:
                return mCopyFactor;
            case MIN_VALUE_PROPERTY_KEY:
                return mMinValue;
            case MAX_VALUE_PROPERTY_KEY:
                return mMaxValue;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case MIN_MAX_SPACE_VALUE_PROPERTY_KEY:
                mMinMaxSpaceValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case MIN_MAX_SPACE_VALUE_PROPERTY_KEY:
                setMinMaxSpaceValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case MIN_MAX_SPACE_VALUE_PROPERTY_KEY:
                return mMinMaxSpaceValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
