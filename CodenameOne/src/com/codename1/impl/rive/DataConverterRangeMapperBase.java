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

/// Generated from rive-runtime's `DataConverterRangeMapperBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataConverterRangeMapper`.
public abstract class DataConverterRangeMapperBase extends DataConverter {
    public static final int TYPE_KEY = 519;
    public static final int INTERPOLATION_TYPE_PROPERTY_KEY = 713;
    public static final int INTERPOLATOR_ID_PROPERTY_KEY = 714;
    public static final int FLAGS_PROPERTY_KEY = 715;
    public static final int MIN_INPUT_PROPERTY_KEY = 716;
    public static final int MAX_INPUT_PROPERTY_KEY = 717;
    public static final int MIN_OUTPUT_PROPERTY_KEY = 718;
    public static final int MAX_OUTPUT_PROPERTY_KEY = 719;

    protected int mInterpolationType = 1;
    protected int mInterpolatorId = -1;
    protected int mFlags = 0;
    protected float mMinInput = 1.0f;
    protected float mMaxInput = 1.0f;
    protected float mMinOutput = 1.0f;
    protected float mMaxOutput = 1.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    /// Copies this type's properties from `source`, which is of this type.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataConverterRangeMapperBase o = (DataConverterRangeMapperBase) source;
        mInterpolationType = o.mInterpolationType;
        mInterpolatorId = o.mInterpolatorId;
        mFlags = o.mFlags;
        mMinInput = o.mMinInput;
        mMaxInput = o.mMaxInput;
        mMinOutput = o.mMinOutput;
        mMaxOutput = o.mMaxOutput;
    }

    @Override
    public Core makeCopy() {
        DataConverterRangeMapper c = new DataConverterRangeMapper();
        c.copyFrom(this);
        return c;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getInterpolationType() {
        return mInterpolationType;
    }

    public void setInterpolationType(int value) {
        if (mInterpolationType == value) {
            return;
        }
        mInterpolationType = value;
        interpolationTypeChanged();
    }

    protected void interpolationTypeChanged() {
    }

    public int getInterpolatorId() {
        return mInterpolatorId;
    }

    public void setInterpolatorId(int value) {
        if (mInterpolatorId == value) {
            return;
        }
        mInterpolatorId = value;
        interpolatorIdChanged();
    }

    protected void interpolatorIdChanged() {
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int value) {
        if (mFlags == value) {
            return;
        }
        mFlags = value;
        flagsChanged();
    }

    protected void flagsChanged() {
    }

    public float getMinInput() {
        return mMinInput;
    }

    public void setMinInput(float value) {
        if (Float.floatToIntBits(mMinInput) == Float.floatToIntBits(value)) {
            return;
        }
        mMinInput = value;
        minInputChanged();
    }

    protected void minInputChanged() {
    }

    public float getMaxInput() {
        return mMaxInput;
    }

    public void setMaxInput(float value) {
        if (Float.floatToIntBits(mMaxInput) == Float.floatToIntBits(value)) {
            return;
        }
        mMaxInput = value;
        maxInputChanged();
    }

    protected void maxInputChanged() {
    }

    public float getMinOutput() {
        return mMinOutput;
    }

    public void setMinOutput(float value) {
        if (Float.floatToIntBits(mMinOutput) == Float.floatToIntBits(value)) {
            return;
        }
        mMinOutput = value;
        minOutputChanged();
    }

    protected void minOutputChanged() {
    }

    public float getMaxOutput() {
        return mMaxOutput;
    }

    public void setMaxOutput(float value) {
        if (Float.floatToIntBits(mMaxOutput) == Float.floatToIntBits(value)) {
            return;
        }
        mMaxOutput = value;
        maxOutputChanged();
    }

    protected void maxOutputChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case MIN_INPUT_PROPERTY_KEY:
                mMinInput = value;
                return true;
            case MAX_INPUT_PROPERTY_KEY:
                mMaxInput = value;
                return true;
            case MIN_OUTPUT_PROPERTY_KEY:
                mMinOutput = value;
                return true;
            case MAX_OUTPUT_PROPERTY_KEY:
                mMaxOutput = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case MIN_INPUT_PROPERTY_KEY:
                setMinInput(value);
                return true;
            case MAX_INPUT_PROPERTY_KEY:
                setMaxInput(value);
                return true;
            case MIN_OUTPUT_PROPERTY_KEY:
                setMinOutput(value);
                return true;
            case MAX_OUTPUT_PROPERTY_KEY:
                setMaxOutput(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case MIN_INPUT_PROPERTY_KEY:
                return mMinInput;
            case MAX_INPUT_PROPERTY_KEY:
                return mMaxInput;
            case MIN_OUTPUT_PROPERTY_KEY:
                return mMinOutput;
            case MAX_OUTPUT_PROPERTY_KEY:
                return mMaxOutput;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case INTERPOLATION_TYPE_PROPERTY_KEY:
                mInterpolationType = value;
                return true;
            case INTERPOLATOR_ID_PROPERTY_KEY:
                mInterpolatorId = value;
                return true;
            case FLAGS_PROPERTY_KEY:
                mFlags = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case INTERPOLATION_TYPE_PROPERTY_KEY:
                setInterpolationType(value);
                return true;
            case INTERPOLATOR_ID_PROPERTY_KEY:
                setInterpolatorId(value);
                return true;
            case FLAGS_PROPERTY_KEY:
                setFlags(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case INTERPOLATION_TYPE_PROPERTY_KEY:
                return mInterpolationType;
            case INTERPOLATOR_ID_PROPERTY_KEY:
                return mInterpolatorId;
            case FLAGS_PROPERTY_KEY:
                return mFlags;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
