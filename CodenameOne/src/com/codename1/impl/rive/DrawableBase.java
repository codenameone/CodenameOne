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

/// Generated from rive-runtime's `DrawableBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Drawable`.
public abstract class DrawableBase extends Node {
    public static final int TYPE_KEY = 13;
    public static final int BLEND_MODE_VALUE_PROPERTY_KEY = 23;
    public static final int ADDITIVE_AMOUNT_PROPERTY_KEY = 450;
    public static final int DRAWABLE_FLAGS_PROPERTY_KEY = 129;

    protected int mBlendModeValue = 3;
    protected int mAdditiveAmount = 255;
    protected int mDrawableFlags = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
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

    public int getDrawableFlags() {
        return mDrawableFlags;
    }

    public void setDrawableFlags(int value) {
        if (mDrawableFlags == value) {
            return;
        }
        mDrawableFlags = value;
        drawableFlagsChanged();
    }

    protected void drawableFlagsChanged() {
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
            case DRAWABLE_FLAGS_PROPERTY_KEY:
                mDrawableFlags = value;
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
            case DRAWABLE_FLAGS_PROPERTY_KEY:
                setDrawableFlags(value);
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
            case DRAWABLE_FLAGS_PROPERTY_KEY:
                return mDrawableFlags;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
