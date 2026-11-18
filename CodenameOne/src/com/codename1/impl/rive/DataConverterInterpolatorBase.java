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

/// Generated from rive-runtime's `DataConverterInterpolatorBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataConverterInterpolator`.
public abstract class DataConverterInterpolatorBase extends DataConverter {
    public static final int TYPE_KEY = 534;
    public static final int INTERPOLATION_TYPE_PROPERTY_KEY = 757;
    public static final int INTERPOLATOR_ID_PROPERTY_KEY = 758;
    public static final int DURATION_PROPERTY_KEY = 756;

    protected int mInterpolationType = 1;
    protected int mInterpolatorId = -1;
    protected float mDuration = 1.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    /// Copies this type's properties from `source`, which is of this type.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataConverterInterpolatorBase o = (DataConverterInterpolatorBase) source;
        mInterpolationType = o.mInterpolationType;
        mInterpolatorId = o.mInterpolatorId;
        mDuration = o.mDuration;
    }

    @Override
    public Core makeCopy() {
        DataConverterInterpolator c = new DataConverterInterpolator();
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

    public float getDuration() {
        return mDuration;
    }

    public void setDuration(float value) {
        if (Float.floatToIntBits(mDuration) == Float.floatToIntBits(value)) {
            return;
        }
        mDuration = value;
        durationChanged();
    }

    protected void durationChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case DURATION_PROPERTY_KEY:
                mDuration = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case DURATION_PROPERTY_KEY:
                setDuration(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case DURATION_PROPERTY_KEY:
                return mDuration;
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
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
