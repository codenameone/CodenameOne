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

/// Generated from rive-runtime's `ShapePaintBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ShapePaint`.
public abstract class ShapePaintBase extends ContainerComponent {
    public static final int TYPE_KEY = 21;
    public static final int IS_VISIBLE_PROPERTY_KEY = 41;
    public static final int BLEND_MODE_VALUE_PROPERTY_KEY = 747;
    public static final int ADDITIVE_AMOUNT_PROPERTY_KEY = 452;

    protected boolean mIsVisible = true;
    protected int mBlendModeValue = 127;
    protected int mAdditiveAmount = 255;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public void setIsVisible(boolean value) {
        if (mIsVisible == value) {
            return;
        }
        mIsVisible = value;
        isVisibleChanged();
    }

    protected void isVisibleChanged() {
    }

    public int getBlendModeValue() {
        return mBlendModeValue;
    }

    public void setBlendModeValue(int value) {
        if (mBlendModeValue == value) {
            return;
        }
        mBlendModeValue = value;
        blendModeValueChanged();
    }

    protected void blendModeValueChanged() {
    }

    public int getAdditiveAmount() {
        return mAdditiveAmount;
    }

    public void setAdditiveAmount(int value) {
        if (mAdditiveAmount == value) {
            return;
        }
        mAdditiveAmount = value;
        additiveAmountChanged();
    }

    protected void additiveAmountChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_VISIBLE_PROPERTY_KEY:
                mIsVisible = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_VISIBLE_PROPERTY_KEY:
                setIsVisible(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case IS_VISIBLE_PROPERTY_KEY:
                return mIsVisible;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case BLEND_MODE_VALUE_PROPERTY_KEY:
                mBlendModeValue = value;
                return true;
            case ADDITIVE_AMOUNT_PROPERTY_KEY:
                mAdditiveAmount = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case BLEND_MODE_VALUE_PROPERTY_KEY:
                setBlendModeValue(value);
                return true;
            case ADDITIVE_AMOUNT_PROPERTY_KEY:
                setAdditiveAmount(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case BLEND_MODE_VALUE_PROPERTY_KEY:
                return mBlendModeValue;
            case ADDITIVE_AMOUNT_PROPERTY_KEY:
                return mAdditiveAmount;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
