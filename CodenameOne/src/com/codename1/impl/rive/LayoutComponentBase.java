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

/// Generated from rive-runtime's `LayoutComponentBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `LayoutComponent`.
public abstract class LayoutComponentBase extends Drawable {
    public static final int TYPE_KEY = 409;
    public static final int CLIP_PROPERTY_KEY = 196;
    public static final int WIDTH_PROPERTY_KEY = 7;
    public static final int HEIGHT_PROPERTY_KEY = 8;
    public static final int STYLE_ID_PROPERTY_KEY = 494;
    public static final int FRACTIONAL_WIDTH_PROPERTY_KEY = 706;
    public static final int FRACTIONAL_HEIGHT_PROPERTY_KEY = 707;

    protected boolean mClip = false;
    protected float mWidth = 0.0f;
    protected float mHeight = 0.0f;
    protected int mStyleId = -1;
    protected float mFractionalWidth = 1.0f;
    protected float mFractionalHeight = 1.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public boolean isClip() {
        return mClip;
    }

    public void setClip(boolean value) {
        if (mClip == value) {
            return;
        }
        mClip = value;
        clipChanged();
    }

    protected void clipChanged() {
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float value) {
        if (Float.floatToIntBits(mWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mWidth = value;
        widthChanged();
    }

    protected void widthChanged() {
    }

    public float getHeight() {
        return mHeight;
    }

    public void setHeight(float value) {
        if (Float.floatToIntBits(mHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mHeight = value;
        heightChanged();
    }

    protected void heightChanged() {
    }

    public int getStyleId() {
        return mStyleId;
    }

    public void setStyleId(int value) {
        if (mStyleId == value) {
            return;
        }
        mStyleId = value;
        styleIdChanged();
    }

    protected void styleIdChanged() {
    }

    public float getFractionalWidth() {
        return mFractionalWidth;
    }

    public void setFractionalWidth(float value) {
        if (Float.floatToIntBits(mFractionalWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mFractionalWidth = value;
        fractionalWidthChanged();
    }

    protected void fractionalWidthChanged() {
    }

    public float getFractionalHeight() {
        return mFractionalHeight;
    }

    public void setFractionalHeight(float value) {
        if (Float.floatToIntBits(mFractionalHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mFractionalHeight = value;
        fractionalHeightChanged();
    }

    protected void fractionalHeightChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case CLIP_PROPERTY_KEY:
                mClip = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case CLIP_PROPERTY_KEY:
                setClip(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case CLIP_PROPERTY_KEY:
                return mClip;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case WIDTH_PROPERTY_KEY:
                mWidth = value;
                return true;
            case HEIGHT_PROPERTY_KEY:
                mHeight = value;
                return true;
            case FRACTIONAL_WIDTH_PROPERTY_KEY:
                mFractionalWidth = value;
                return true;
            case FRACTIONAL_HEIGHT_PROPERTY_KEY:
                mFractionalHeight = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case WIDTH_PROPERTY_KEY:
                setWidth(value);
                return true;
            case HEIGHT_PROPERTY_KEY:
                setHeight(value);
                return true;
            case FRACTIONAL_WIDTH_PROPERTY_KEY:
                setFractionalWidth(value);
                return true;
            case FRACTIONAL_HEIGHT_PROPERTY_KEY:
                setFractionalHeight(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case WIDTH_PROPERTY_KEY:
                return mWidth;
            case HEIGHT_PROPERTY_KEY:
                return mHeight;
            case FRACTIONAL_WIDTH_PROPERTY_KEY:
                return mFractionalWidth;
            case FRACTIONAL_HEIGHT_PROPERTY_KEY:
                return mFractionalHeight;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case STYLE_ID_PROPERTY_KEY:
                mStyleId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case STYLE_ID_PROPERTY_KEY:
                setStyleId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case STYLE_ID_PROPERTY_KEY:
                return mStyleId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
