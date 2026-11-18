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

/// Generated from rive-runtime's `BlendAnimationDirectBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `BlendAnimationDirect`.
public abstract class BlendAnimationDirectBase extends BlendAnimation {
    public static final int TYPE_KEY = 77;
    public static final int INPUT_ID_PROPERTY_KEY = 168;
    public static final int MIX_VALUE_PROPERTY_KEY = 297;
    public static final int BLEND_SOURCE_PROPERTY_KEY = 298;

    protected int mInputId = -1;
    protected float mMixValue = 100.0f;
    protected int mBlendSource = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getInputId() {
        return mInputId;
    }

    public void setInputId(int value) {
        if (mInputId == value) {
            return;
        }
        mInputId = value;
        inputIdChanged();
    }

    protected void inputIdChanged() {
    }

    public float getMixValue() {
        return mMixValue;
    }

    public void setMixValue(float value) {
        if (Float.floatToIntBits(mMixValue) == Float.floatToIntBits(value)) {
            return;
        }
        mMixValue = value;
        mixValueChanged();
    }

    protected void mixValueChanged() {
    }

    public int getBlendSource() {
        return mBlendSource;
    }

    public void setBlendSource(int value) {
        if (mBlendSource == value) {
            return;
        }
        mBlendSource = value;
        blendSourceChanged();
    }

    protected void blendSourceChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case MIX_VALUE_PROPERTY_KEY:
                mMixValue = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case MIX_VALUE_PROPERTY_KEY:
                setMixValue(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case MIX_VALUE_PROPERTY_KEY:
                return mMixValue;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case INPUT_ID_PROPERTY_KEY:
                mInputId = value;
                return true;
            case BLEND_SOURCE_PROPERTY_KEY:
                mBlendSource = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case INPUT_ID_PROPERTY_KEY:
                setInputId(value);
                return true;
            case BLEND_SOURCE_PROPERTY_KEY:
                setBlendSource(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case INPUT_ID_PROPERTY_KEY:
                return mInputId;
            case BLEND_SOURCE_PROPERTY_KEY:
                return mBlendSource;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
