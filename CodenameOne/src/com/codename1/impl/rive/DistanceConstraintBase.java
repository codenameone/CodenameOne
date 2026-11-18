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

/// Generated from rive-runtime's `DistanceConstraintBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DistanceConstraint`.
public abstract class DistanceConstraintBase extends TargetedConstraint {
    public static final int TYPE_KEY = 82;
    public static final int DISTANCE_PROPERTY_KEY = 177;
    public static final int MODE_VALUE_PROPERTY_KEY = 178;

    protected float mDistance = 100.0f;
    protected int mModeValue = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getDistance() {
        return mDistance;
    }

    public void setDistance(float value) {
        if (Float.floatToIntBits(mDistance) == Float.floatToIntBits(value)) {
            return;
        }
        mDistance = value;
        distanceChanged();
    }

    protected void distanceChanged() {
    }

    public int getModeValue() {
        return mModeValue;
    }

    public void setModeValue(int value) {
        if (mModeValue == value) {
            return;
        }
        mModeValue = value;
        modeValueChanged();
    }

    protected void modeValueChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case DISTANCE_PROPERTY_KEY:
                mDistance = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case DISTANCE_PROPERTY_KEY:
                setDistance(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case DISTANCE_PROPERTY_KEY:
                return mDistance;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case MODE_VALUE_PROPERTY_KEY:
                mModeValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case MODE_VALUE_PROPERTY_KEY:
                setModeValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case MODE_VALUE_PROPERTY_KEY:
                return mModeValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
