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

/// Generated from rive-runtime's `NestedArtboardLayoutBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `NestedArtboardLayout`.
public abstract class NestedArtboardLayoutBase extends NestedArtboard {
    public static final int TYPE_KEY = 452;
    public static final int INSTANCE_WIDTH_PROPERTY_KEY = 663;
    public static final int INSTANCE_HEIGHT_PROPERTY_KEY = 664;
    public static final int INSTANCE_WIDTH_UNITS_VALUE_PROPERTY_KEY = 665;
    public static final int INSTANCE_HEIGHT_UNITS_VALUE_PROPERTY_KEY = 666;
    public static final int INSTANCE_WIDTH_SCALE_TYPE_PROPERTY_KEY = 667;
    public static final int INSTANCE_HEIGHT_SCALE_TYPE_PROPERTY_KEY = 668;

    protected float mInstanceWidth = -1.0f;
    protected float mInstanceHeight = -1.0f;
    protected int mInstanceWidthUnitsValue = 1;
    protected int mInstanceHeightUnitsValue = 1;
    protected int mInstanceWidthScaleType = 0;
    protected int mInstanceHeightScaleType = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getInstanceWidth() {
        return mInstanceWidth;
    }

    public void setInstanceWidth(float value) {
        if (Float.floatToIntBits(mInstanceWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mInstanceWidth = value;
        instanceWidthChanged();
    }

    protected void instanceWidthChanged() {
    }

    public float getInstanceHeight() {
        return mInstanceHeight;
    }

    public void setInstanceHeight(float value) {
        if (Float.floatToIntBits(mInstanceHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mInstanceHeight = value;
        instanceHeightChanged();
    }

    protected void instanceHeightChanged() {
    }

    public int getInstanceWidthUnitsValue() {
        return mInstanceWidthUnitsValue;
    }

    public void setInstanceWidthUnitsValue(int value) {
        if (mInstanceWidthUnitsValue == value) {
            return;
        }
        mInstanceWidthUnitsValue = value;
        instanceWidthUnitsValueChanged();
    }

    protected void instanceWidthUnitsValueChanged() {
    }

    public int getInstanceHeightUnitsValue() {
        return mInstanceHeightUnitsValue;
    }

    public void setInstanceHeightUnitsValue(int value) {
        if (mInstanceHeightUnitsValue == value) {
            return;
        }
        mInstanceHeightUnitsValue = value;
        instanceHeightUnitsValueChanged();
    }

    protected void instanceHeightUnitsValueChanged() {
    }

    public int getInstanceWidthScaleType() {
        return mInstanceWidthScaleType;
    }

    public void setInstanceWidthScaleType(int value) {
        if (mInstanceWidthScaleType == value) {
            return;
        }
        mInstanceWidthScaleType = value;
        instanceWidthScaleTypeChanged();
    }

    protected void instanceWidthScaleTypeChanged() {
    }

    public int getInstanceHeightScaleType() {
        return mInstanceHeightScaleType;
    }

    public void setInstanceHeightScaleType(int value) {
        if (mInstanceHeightScaleType == value) {
            return;
        }
        mInstanceHeightScaleType = value;
        instanceHeightScaleTypeChanged();
    }

    protected void instanceHeightScaleTypeChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case INSTANCE_WIDTH_PROPERTY_KEY:
                mInstanceWidth = value;
                return true;
            case INSTANCE_HEIGHT_PROPERTY_KEY:
                mInstanceHeight = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case INSTANCE_WIDTH_PROPERTY_KEY:
                setInstanceWidth(value);
                return true;
            case INSTANCE_HEIGHT_PROPERTY_KEY:
                setInstanceHeight(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case INSTANCE_WIDTH_PROPERTY_KEY:
                return mInstanceWidth;
            case INSTANCE_HEIGHT_PROPERTY_KEY:
                return mInstanceHeight;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case INSTANCE_WIDTH_UNITS_VALUE_PROPERTY_KEY:
                mInstanceWidthUnitsValue = value;
                return true;
            case INSTANCE_HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                mInstanceHeightUnitsValue = value;
                return true;
            case INSTANCE_WIDTH_SCALE_TYPE_PROPERTY_KEY:
                mInstanceWidthScaleType = value;
                return true;
            case INSTANCE_HEIGHT_SCALE_TYPE_PROPERTY_KEY:
                mInstanceHeightScaleType = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case INSTANCE_WIDTH_UNITS_VALUE_PROPERTY_KEY:
                setInstanceWidthUnitsValue(value);
                return true;
            case INSTANCE_HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                setInstanceHeightUnitsValue(value);
                return true;
            case INSTANCE_WIDTH_SCALE_TYPE_PROPERTY_KEY:
                setInstanceWidthScaleType(value);
                return true;
            case INSTANCE_HEIGHT_SCALE_TYPE_PROPERTY_KEY:
                setInstanceHeightScaleType(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case INSTANCE_WIDTH_UNITS_VALUE_PROPERTY_KEY:
                return mInstanceWidthUnitsValue;
            case INSTANCE_HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                return mInstanceHeightUnitsValue;
            case INSTANCE_WIDTH_SCALE_TYPE_PROPERTY_KEY:
                return mInstanceWidthScaleType;
            case INSTANCE_HEIGHT_SCALE_TYPE_PROPERTY_KEY:
                return mInstanceHeightScaleType;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
