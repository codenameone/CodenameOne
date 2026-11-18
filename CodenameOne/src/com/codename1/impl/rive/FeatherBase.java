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

/// Generated from rive-runtime's `FeatherBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Feather`.
public abstract class FeatherBase extends ContainerComponent {
    public static final int TYPE_KEY = 533;
    public static final int SPACE_VALUE_PROPERTY_KEY = 748;
    public static final int STRENGTH_PROPERTY_KEY = 749;
    public static final int OFFSET_X_PROPERTY_KEY = 750;
    public static final int OFFSET_Y_PROPERTY_KEY = 751;
    public static final int INNER_PROPERTY_KEY = 752;

    protected int mSpaceValue = 0;
    protected float mStrength = 12.0f;
    protected float mOffsetX = 0.0f;
    protected float mOffsetY = 0.0f;
    protected boolean mInner = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getSpaceValue() {
        return mSpaceValue;
    }

    public void setSpaceValue(int value) {
        if (mSpaceValue == value) {
            return;
        }
        mSpaceValue = value;
        spaceValueChanged();
    }

    protected void spaceValueChanged() {
    }

    public float getStrength() {
        return mStrength;
    }

    public void setStrength(float value) {
        if (Float.floatToIntBits(mStrength) == Float.floatToIntBits(value)) {
            return;
        }
        mStrength = value;
        strengthChanged();
    }

    protected void strengthChanged() {
    }

    public float getOffsetX() {
        return mOffsetX;
    }

    public void setOffsetX(float value) {
        if (Float.floatToIntBits(mOffsetX) == Float.floatToIntBits(value)) {
            return;
        }
        mOffsetX = value;
        offsetXChanged();
    }

    protected void offsetXChanged() {
    }

    public float getOffsetY() {
        return mOffsetY;
    }

    public void setOffsetY(float value) {
        if (Float.floatToIntBits(mOffsetY) == Float.floatToIntBits(value)) {
            return;
        }
        mOffsetY = value;
        offsetYChanged();
    }

    protected void offsetYChanged() {
    }

    public boolean isInner() {
        return mInner;
    }

    public void setInner(boolean value) {
        if (mInner == value) {
            return;
        }
        mInner = value;
        innerChanged();
    }

    protected void innerChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case INNER_PROPERTY_KEY:
                mInner = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case INNER_PROPERTY_KEY:
                setInner(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case INNER_PROPERTY_KEY:
                return mInner;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case STRENGTH_PROPERTY_KEY:
                mStrength = value;
                return true;
            case OFFSET_X_PROPERTY_KEY:
                mOffsetX = value;
                return true;
            case OFFSET_Y_PROPERTY_KEY:
                mOffsetY = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case STRENGTH_PROPERTY_KEY:
                setStrength(value);
                return true;
            case OFFSET_X_PROPERTY_KEY:
                setOffsetX(value);
                return true;
            case OFFSET_Y_PROPERTY_KEY:
                setOffsetY(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case STRENGTH_PROPERTY_KEY:
                return mStrength;
            case OFFSET_X_PROPERTY_KEY:
                return mOffsetX;
            case OFFSET_Y_PROPERTY_KEY:
                return mOffsetY;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case SPACE_VALUE_PROPERTY_KEY:
                mSpaceValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case SPACE_VALUE_PROPERTY_KEY:
                setSpaceValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case SPACE_VALUE_PROPERTY_KEY:
                return mSpaceValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
