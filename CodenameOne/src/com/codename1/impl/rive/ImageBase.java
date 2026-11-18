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

/// Generated from rive-runtime's `ImageBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Image`.
public abstract class ImageBase extends Drawable {
    public static final int TYPE_KEY = 100;
    public static final int ASSET_ID_PROPERTY_KEY = 206;
    public static final int ORIGIN_X_PROPERTY_KEY = 380;
    public static final int ORIGIN_Y_PROPERTY_KEY = 381;
    public static final int FIT_PROPERTY_KEY = 974;
    public static final int ALIGNMENT_X_PROPERTY_KEY = 975;
    public static final int ALIGNMENT_Y_PROPERTY_KEY = 976;
    public static final int SAMPLER_FILTER_PROPERTY_KEY = 1076;
    public static final int SAMPLER_WRAP_X_PROPERTY_KEY = 1077;
    public static final int SAMPLER_WRAP_Y_PROPERTY_KEY = 1078;

    protected int mAssetId = -1;
    protected float mOriginX = 0.5f;
    protected float mOriginY = 0.5f;
    protected int mFit = 0;
    protected float mAlignmentX = 0.0f;
    protected float mAlignmentY = 0.0f;
    protected int mSamplerFilter = 0;
    protected int mSamplerWrapX = 0;
    protected int mSamplerWrapY = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getAssetId() {
        return mAssetId;
    }

    public void setAssetId(int value) {
        if (mAssetId == value) {
            return;
        }
        mAssetId = value;
        assetIdChanged();
    }

    protected void assetIdChanged() {
    }

    public float getOriginX() {
        return mOriginX;
    }

    public void setOriginX(float value) {
        if (Float.floatToIntBits(mOriginX) == Float.floatToIntBits(value)) {
            return;
        }
        mOriginX = value;
        originXChanged();
    }

    protected void originXChanged() {
    }

    public float getOriginY() {
        return mOriginY;
    }

    public void setOriginY(float value) {
        if (Float.floatToIntBits(mOriginY) == Float.floatToIntBits(value)) {
            return;
        }
        mOriginY = value;
        originYChanged();
    }

    protected void originYChanged() {
    }

    public int getFit() {
        return mFit;
    }

    public void setFit(int value) {
        if (mFit == value) {
            return;
        }
        mFit = value;
        fitChanged();
    }

    protected void fitChanged() {
    }

    public float getAlignmentX() {
        return mAlignmentX;
    }

    public void setAlignmentX(float value) {
        if (Float.floatToIntBits(mAlignmentX) == Float.floatToIntBits(value)) {
            return;
        }
        mAlignmentX = value;
        alignmentXChanged();
    }

    protected void alignmentXChanged() {
    }

    public float getAlignmentY() {
        return mAlignmentY;
    }

    public void setAlignmentY(float value) {
        if (Float.floatToIntBits(mAlignmentY) == Float.floatToIntBits(value)) {
            return;
        }
        mAlignmentY = value;
        alignmentYChanged();
    }

    protected void alignmentYChanged() {
    }

    public int getSamplerFilter() {
        return mSamplerFilter;
    }

    public void setSamplerFilter(int value) {
        if (mSamplerFilter == value) {
            return;
        }
        mSamplerFilter = value;
        samplerFilterChanged();
    }

    protected void samplerFilterChanged() {
    }

    public int getSamplerWrapX() {
        return mSamplerWrapX;
    }

    public void setSamplerWrapX(int value) {
        if (mSamplerWrapX == value) {
            return;
        }
        mSamplerWrapX = value;
        samplerWrapXChanged();
    }

    protected void samplerWrapXChanged() {
    }

    public int getSamplerWrapY() {
        return mSamplerWrapY;
    }

    public void setSamplerWrapY(int value) {
        if (mSamplerWrapY == value) {
            return;
        }
        mSamplerWrapY = value;
        samplerWrapYChanged();
    }

    protected void samplerWrapYChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case ORIGIN_X_PROPERTY_KEY:
                mOriginX = value;
                return true;
            case ORIGIN_Y_PROPERTY_KEY:
                mOriginY = value;
                return true;
            case ALIGNMENT_X_PROPERTY_KEY:
                mAlignmentX = value;
                return true;
            case ALIGNMENT_Y_PROPERTY_KEY:
                mAlignmentY = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case ORIGIN_X_PROPERTY_KEY:
                setOriginX(value);
                return true;
            case ORIGIN_Y_PROPERTY_KEY:
                setOriginY(value);
                return true;
            case ALIGNMENT_X_PROPERTY_KEY:
                setAlignmentX(value);
                return true;
            case ALIGNMENT_Y_PROPERTY_KEY:
                setAlignmentY(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case ORIGIN_X_PROPERTY_KEY:
                return mOriginX;
            case ORIGIN_Y_PROPERTY_KEY:
                return mOriginY;
            case ALIGNMENT_X_PROPERTY_KEY:
                return mAlignmentX;
            case ALIGNMENT_Y_PROPERTY_KEY:
                return mAlignmentY;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case ASSET_ID_PROPERTY_KEY:
                mAssetId = value;
                return true;
            case FIT_PROPERTY_KEY:
                mFit = value;
                return true;
            case SAMPLER_FILTER_PROPERTY_KEY:
                mSamplerFilter = value;
                return true;
            case SAMPLER_WRAP_X_PROPERTY_KEY:
                mSamplerWrapX = value;
                return true;
            case SAMPLER_WRAP_Y_PROPERTY_KEY:
                mSamplerWrapY = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case ASSET_ID_PROPERTY_KEY:
                setAssetId(value);
                return true;
            case FIT_PROPERTY_KEY:
                setFit(value);
                return true;
            case SAMPLER_FILTER_PROPERTY_KEY:
                setSamplerFilter(value);
                return true;
            case SAMPLER_WRAP_X_PROPERTY_KEY:
                setSamplerWrapX(value);
                return true;
            case SAMPLER_WRAP_Y_PROPERTY_KEY:
                setSamplerWrapY(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case ASSET_ID_PROPERTY_KEY:
                return mAssetId;
            case FIT_PROPERTY_KEY:
                return mFit;
            case SAMPLER_FILTER_PROPERTY_KEY:
                return mSamplerFilter;
            case SAMPLER_WRAP_X_PROPERTY_KEY:
                return mSamplerWrapX;
            case SAMPLER_WRAP_Y_PROPERTY_KEY:
                return mSamplerWrapY;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
