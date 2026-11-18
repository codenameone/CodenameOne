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

/// Generated from rive-runtime's `TextModifierRangeBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TextModifierRange`.
public abstract class TextModifierRangeBase extends ContainerComponent {
    public static final int TYPE_KEY = 158;
    public static final int MODIFY_FROM_PROPERTY_KEY = 327;
    public static final int MODIFY_TO_PROPERTY_KEY = 336;
    public static final int STRENGTH_PROPERTY_KEY = 334;
    public static final int UNITS_VALUE_PROPERTY_KEY = 316;
    public static final int TYPE_VALUE_PROPERTY_KEY = 325;
    public static final int MODE_VALUE_PROPERTY_KEY = 326;
    public static final int CLAMP_PROPERTY_KEY = 333;
    public static final int FALLOFF_FROM_PROPERTY_KEY = 317;
    public static final int FALLOFF_TO_PROPERTY_KEY = 318;
    public static final int OFFSET_PROPERTY_KEY = 319;
    public static final int RUN_ID_PROPERTY_KEY = 378;

    protected float mModifyFrom = 0.0f;
    protected float mModifyTo = 1.0f;
    protected float mStrength = 1.0f;
    protected int mUnitsValue = 0;
    protected int mTypeValue = 0;
    protected int mModeValue = 0;
    protected boolean mClamp = false;
    protected float mFalloffFrom = 0.0f;
    protected float mFalloffTo = 1.0f;
    protected float mOffset = 0.0f;
    protected int mRunId = -1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getModifyFrom() {
        return mModifyFrom;
    }

    public void setModifyFrom(float value) {
        if (Float.floatToIntBits(mModifyFrom) == Float.floatToIntBits(value)) {
            return;
        }
        mModifyFrom = value;
        modifyFromChanged();
    }

    protected void modifyFromChanged() {
    }

    public float getModifyTo() {
        return mModifyTo;
    }

    public void setModifyTo(float value) {
        if (Float.floatToIntBits(mModifyTo) == Float.floatToIntBits(value)) {
            return;
        }
        mModifyTo = value;
        modifyToChanged();
    }

    protected void modifyToChanged() {
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

    public int getUnitsValue() {
        return mUnitsValue;
    }

    public void setUnitsValue(int value) {
        if (mUnitsValue == value) {
            return;
        }
        mUnitsValue = value;
        unitsValueChanged();
    }

    protected void unitsValueChanged() {
    }

    public int getTypeValue() {
        return mTypeValue;
    }

    public void setTypeValue(int value) {
        if (mTypeValue == value) {
            return;
        }
        mTypeValue = value;
        typeValueChanged();
    }

    protected void typeValueChanged() {
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

    public boolean isClamp() {
        return mClamp;
    }

    public void setClamp(boolean value) {
        if (mClamp == value) {
            return;
        }
        mClamp = value;
        clampChanged();
    }

    protected void clampChanged() {
    }

    public float getFalloffFrom() {
        return mFalloffFrom;
    }

    public void setFalloffFrom(float value) {
        if (Float.floatToIntBits(mFalloffFrom) == Float.floatToIntBits(value)) {
            return;
        }
        mFalloffFrom = value;
        falloffFromChanged();
    }

    protected void falloffFromChanged() {
    }

    public float getFalloffTo() {
        return mFalloffTo;
    }

    public void setFalloffTo(float value) {
        if (Float.floatToIntBits(mFalloffTo) == Float.floatToIntBits(value)) {
            return;
        }
        mFalloffTo = value;
        falloffToChanged();
    }

    protected void falloffToChanged() {
    }

    public float getOffset() {
        return mOffset;
    }

    public void setOffset(float value) {
        if (Float.floatToIntBits(mOffset) == Float.floatToIntBits(value)) {
            return;
        }
        mOffset = value;
        offsetChanged();
    }

    protected void offsetChanged() {
    }

    public int getRunId() {
        return mRunId;
    }

    public void setRunId(int value) {
        if (mRunId == value) {
            return;
        }
        mRunId = value;
        runIdChanged();
    }

    protected void runIdChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case CLAMP_PROPERTY_KEY:
                mClamp = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case CLAMP_PROPERTY_KEY:
                setClamp(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case CLAMP_PROPERTY_KEY:
                return mClamp;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case MODIFY_FROM_PROPERTY_KEY:
                mModifyFrom = value;
                return true;
            case MODIFY_TO_PROPERTY_KEY:
                mModifyTo = value;
                return true;
            case STRENGTH_PROPERTY_KEY:
                mStrength = value;
                return true;
            case FALLOFF_FROM_PROPERTY_KEY:
                mFalloffFrom = value;
                return true;
            case FALLOFF_TO_PROPERTY_KEY:
                mFalloffTo = value;
                return true;
            case OFFSET_PROPERTY_KEY:
                mOffset = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case MODIFY_FROM_PROPERTY_KEY:
                setModifyFrom(value);
                return true;
            case MODIFY_TO_PROPERTY_KEY:
                setModifyTo(value);
                return true;
            case STRENGTH_PROPERTY_KEY:
                setStrength(value);
                return true;
            case FALLOFF_FROM_PROPERTY_KEY:
                setFalloffFrom(value);
                return true;
            case FALLOFF_TO_PROPERTY_KEY:
                setFalloffTo(value);
                return true;
            case OFFSET_PROPERTY_KEY:
                setOffset(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case MODIFY_FROM_PROPERTY_KEY:
                return mModifyFrom;
            case MODIFY_TO_PROPERTY_KEY:
                return mModifyTo;
            case STRENGTH_PROPERTY_KEY:
                return mStrength;
            case FALLOFF_FROM_PROPERTY_KEY:
                return mFalloffFrom;
            case FALLOFF_TO_PROPERTY_KEY:
                return mFalloffTo;
            case OFFSET_PROPERTY_KEY:
                return mOffset;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case UNITS_VALUE_PROPERTY_KEY:
                mUnitsValue = value;
                return true;
            case TYPE_VALUE_PROPERTY_KEY:
                mTypeValue = value;
                return true;
            case MODE_VALUE_PROPERTY_KEY:
                mModeValue = value;
                return true;
            case RUN_ID_PROPERTY_KEY:
                mRunId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case UNITS_VALUE_PROPERTY_KEY:
                setUnitsValue(value);
                return true;
            case TYPE_VALUE_PROPERTY_KEY:
                setTypeValue(value);
                return true;
            case MODE_VALUE_PROPERTY_KEY:
                setModeValue(value);
                return true;
            case RUN_ID_PROPERTY_KEY:
                setRunId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case UNITS_VALUE_PROPERTY_KEY:
                return mUnitsValue;
            case TYPE_VALUE_PROPERTY_KEY:
                return mTypeValue;
            case MODE_VALUE_PROPERTY_KEY:
                return mModeValue;
            case RUN_ID_PROPERTY_KEY:
                return mRunId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
