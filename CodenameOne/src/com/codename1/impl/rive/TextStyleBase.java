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

/// Generated from rive-runtime's `TextStyleBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TextStyle`.
public abstract class TextStyleBase extends TextStyleReference {
    public static final int TYPE_KEY = 573;
    public static final int FONT_SIZE_PROPERTY_KEY = 274;
    public static final int LINE_HEIGHT_PROPERTY_KEY = 370;
    public static final int LETTER_SPACING_PROPERTY_KEY = 390;
    public static final int FONT_ASSET_ID_PROPERTY_KEY = 279;

    protected float mFontSize = 12.0f;
    protected float mLineHeight = -1.0f;
    protected float mLetterSpacing = 0.0f;
    protected int mFontAssetId = -1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getFontSize() {
        return mFontSize;
    }

    public void setFontSize(float value) {
        if (Float.floatToIntBits(mFontSize) == Float.floatToIntBits(value)) {
            return;
        }
        mFontSize = value;
        fontSizeChanged();
    }

    protected void fontSizeChanged() {
    }

    public float getLineHeight() {
        return mLineHeight;
    }

    public void setLineHeight(float value) {
        if (Float.floatToIntBits(mLineHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mLineHeight = value;
        lineHeightChanged();
    }

    protected void lineHeightChanged() {
    }

    public float getLetterSpacing() {
        return mLetterSpacing;
    }

    public void setLetterSpacing(float value) {
        if (Float.floatToIntBits(mLetterSpacing) == Float.floatToIntBits(value)) {
            return;
        }
        mLetterSpacing = value;
        letterSpacingChanged();
    }

    protected void letterSpacingChanged() {
    }

    public int getFontAssetId() {
        return mFontAssetId;
    }

    public void setFontAssetId(int value) {
        if (mFontAssetId == value) {
            return;
        }
        mFontAssetId = value;
        fontAssetIdChanged();
    }

    protected void fontAssetIdChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case FONT_SIZE_PROPERTY_KEY:
                mFontSize = value;
                return true;
            case LINE_HEIGHT_PROPERTY_KEY:
                mLineHeight = value;
                return true;
            case LETTER_SPACING_PROPERTY_KEY:
                mLetterSpacing = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case FONT_SIZE_PROPERTY_KEY:
                setFontSize(value);
                return true;
            case LINE_HEIGHT_PROPERTY_KEY:
                setLineHeight(value);
                return true;
            case LETTER_SPACING_PROPERTY_KEY:
                setLetterSpacing(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case FONT_SIZE_PROPERTY_KEY:
                return mFontSize;
            case LINE_HEIGHT_PROPERTY_KEY:
                return mLineHeight;
            case LETTER_SPACING_PROPERTY_KEY:
                return mLetterSpacing;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case FONT_ASSET_ID_PROPERTY_KEY:
                mFontAssetId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case FONT_ASSET_ID_PROPERTY_KEY:
                setFontAssetId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case FONT_ASSET_ID_PROPERTY_KEY:
                return mFontAssetId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
