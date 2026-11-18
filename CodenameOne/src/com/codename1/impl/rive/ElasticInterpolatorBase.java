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

/// Generated from rive-runtime's `ElasticInterpolatorBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ElasticInterpolator`.
public abstract class ElasticInterpolatorBase extends KeyFrameInterpolator {
    public static final int TYPE_KEY = 174;
    public static final int EASING_VALUE_PROPERTY_KEY = 405;
    public static final int AMPLITUDE_PROPERTY_KEY = 406;
    public static final int PERIOD_PROPERTY_KEY = 407;

    protected int mEasingValue = 1;
    protected float mAmplitude = 1.0f;
    protected float mPeriod = 1.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getEasingValue() {
        return mEasingValue;
    }

    public void setEasingValue(int value) {
        if (mEasingValue == value) {
            return;
        }
        mEasingValue = value;
        easingValueChanged();
    }

    protected void easingValueChanged() {
    }

    public float getAmplitude() {
        return mAmplitude;
    }

    public void setAmplitude(float value) {
        if (Float.floatToIntBits(mAmplitude) == Float.floatToIntBits(value)) {
            return;
        }
        mAmplitude = value;
        amplitudeChanged();
    }

    protected void amplitudeChanged() {
    }

    public float getPeriod() {
        return mPeriod;
    }

    public void setPeriod(float value) {
        if (Float.floatToIntBits(mPeriod) == Float.floatToIntBits(value)) {
            return;
        }
        mPeriod = value;
        periodChanged();
    }

    protected void periodChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case AMPLITUDE_PROPERTY_KEY:
                mAmplitude = value;
                return true;
            case PERIOD_PROPERTY_KEY:
                mPeriod = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case AMPLITUDE_PROPERTY_KEY:
                setAmplitude(value);
                return true;
            case PERIOD_PROPERTY_KEY:
                setPeriod(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case AMPLITUDE_PROPERTY_KEY:
                return mAmplitude;
            case PERIOD_PROPERTY_KEY:
                return mPeriod;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case EASING_VALUE_PROPERTY_KEY:
                mEasingValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case EASING_VALUE_PROPERTY_KEY:
                setEasingValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case EASING_VALUE_PROPERTY_KEY:
                return mEasingValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
