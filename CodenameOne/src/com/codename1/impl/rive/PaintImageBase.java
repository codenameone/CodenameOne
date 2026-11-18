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

/// Generated from rive-runtime's `PaintImageBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `PaintImage`.
public abstract class PaintImageBase extends Component {
    public static final int TYPE_KEY = 113;
    public static final int IMAGE_ASSET_ID_PROPERTY_KEY = 415;
    public static final int IMAGE_SAMPLER_FILTER_PROPERTY_KEY = 269;
    public static final int IMAGE_SAMPLER_WRAP_X_PROPERTY_KEY = 270;
    public static final int IMAGE_SAMPLER_WRAP_Y_PROPERTY_KEY = 271;
    public static final int IMAGE_SCALE_X_PROPERTY_KEY = 416;
    public static final int IMAGE_SCALE_Y_PROPERTY_KEY = 368;
    public static final int IMAGE_OFFSET_X_PROPERTY_KEY = 369;
    public static final int IMAGE_OFFSET_Y_PROPERTY_KEY = 410;
    public static final int IMAGE_ROTATION_PROPERTY_KEY = 411;
    public static final int IMAGE_SIZE_MODE_PROPERTY_KEY = 412;
    public static final int ARTBOARD_ID_PROPERTY_KEY = 487;

    protected int mImageAssetId = -1;
    protected int mImageSamplerFilter = 0;
    protected int mImageSamplerWrapX = 0;
    protected int mImageSamplerWrapY = 0;
    protected float mImageScaleX = 1.0f;
    protected float mImageScaleY = 1.0f;
    protected float mImageOffsetX = 0.0f;
    protected float mImageOffsetY = 0.0f;
    protected float mImageRotation = 0.0f;
    protected int mImageSizeMode = 0;
    protected int mArtboardId = -1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getImageAssetId() {
        return mImageAssetId;
    }

    public void setImageAssetId(int value) {
        if (mImageAssetId == value) {
            return;
        }
        mImageAssetId = value;
        imageAssetIdChanged();
    }

    protected void imageAssetIdChanged() {
    }

    public int getImageSamplerFilter() {
        return mImageSamplerFilter;
    }

    public void setImageSamplerFilter(int value) {
        if (mImageSamplerFilter == value) {
            return;
        }
        mImageSamplerFilter = value;
        imageSamplerFilterChanged();
    }

    protected void imageSamplerFilterChanged() {
    }

    public int getImageSamplerWrapX() {
        return mImageSamplerWrapX;
    }

    public void setImageSamplerWrapX(int value) {
        if (mImageSamplerWrapX == value) {
            return;
        }
        mImageSamplerWrapX = value;
        imageSamplerWrapXChanged();
    }

    protected void imageSamplerWrapXChanged() {
    }

    public int getImageSamplerWrapY() {
        return mImageSamplerWrapY;
    }

    public void setImageSamplerWrapY(int value) {
        if (mImageSamplerWrapY == value) {
            return;
        }
        mImageSamplerWrapY = value;
        imageSamplerWrapYChanged();
    }

    protected void imageSamplerWrapYChanged() {
    }

    public float getImageScaleX() {
        return mImageScaleX;
    }

    public void setImageScaleX(float value) {
        if (Float.floatToIntBits(mImageScaleX) == Float.floatToIntBits(value)) {
            return;
        }
        mImageScaleX = value;
        imageScaleXChanged();
    }

    protected void imageScaleXChanged() {
    }

    public float getImageScaleY() {
        return mImageScaleY;
    }

    public void setImageScaleY(float value) {
        if (Float.floatToIntBits(mImageScaleY) == Float.floatToIntBits(value)) {
            return;
        }
        mImageScaleY = value;
        imageScaleYChanged();
    }

    protected void imageScaleYChanged() {
    }

    public float getImageOffsetX() {
        return mImageOffsetX;
    }

    public void setImageOffsetX(float value) {
        if (Float.floatToIntBits(mImageOffsetX) == Float.floatToIntBits(value)) {
            return;
        }
        mImageOffsetX = value;
        imageOffsetXChanged();
    }

    protected void imageOffsetXChanged() {
    }

    public float getImageOffsetY() {
        return mImageOffsetY;
    }

    public void setImageOffsetY(float value) {
        if (Float.floatToIntBits(mImageOffsetY) == Float.floatToIntBits(value)) {
            return;
        }
        mImageOffsetY = value;
        imageOffsetYChanged();
    }

    protected void imageOffsetYChanged() {
    }

    public float getImageRotation() {
        return mImageRotation;
    }

    public void setImageRotation(float value) {
        if (Float.floatToIntBits(mImageRotation) == Float.floatToIntBits(value)) {
            return;
        }
        mImageRotation = value;
        imageRotationChanged();
    }

    protected void imageRotationChanged() {
    }

    public int getImageSizeMode() {
        return mImageSizeMode;
    }

    public void setImageSizeMode(int value) {
        if (mImageSizeMode == value) {
            return;
        }
        mImageSizeMode = value;
        imageSizeModeChanged();
    }

    protected void imageSizeModeChanged() {
    }

    public int getArtboardId() {
        return mArtboardId;
    }

    public void setArtboardId(int value) {
        if (mArtboardId == value) {
            return;
        }
        mArtboardId = value;
        artboardIdChanged();
    }

    protected void artboardIdChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case IMAGE_SCALE_X_PROPERTY_KEY:
                mImageScaleX = value;
                return true;
            case IMAGE_SCALE_Y_PROPERTY_KEY:
                mImageScaleY = value;
                return true;
            case IMAGE_OFFSET_X_PROPERTY_KEY:
                mImageOffsetX = value;
                return true;
            case IMAGE_OFFSET_Y_PROPERTY_KEY:
                mImageOffsetY = value;
                return true;
            case IMAGE_ROTATION_PROPERTY_KEY:
                mImageRotation = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case IMAGE_SCALE_X_PROPERTY_KEY:
                setImageScaleX(value);
                return true;
            case IMAGE_SCALE_Y_PROPERTY_KEY:
                setImageScaleY(value);
                return true;
            case IMAGE_OFFSET_X_PROPERTY_KEY:
                setImageOffsetX(value);
                return true;
            case IMAGE_OFFSET_Y_PROPERTY_KEY:
                setImageOffsetY(value);
                return true;
            case IMAGE_ROTATION_PROPERTY_KEY:
                setImageRotation(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case IMAGE_SCALE_X_PROPERTY_KEY:
                return mImageScaleX;
            case IMAGE_SCALE_Y_PROPERTY_KEY:
                return mImageScaleY;
            case IMAGE_OFFSET_X_PROPERTY_KEY:
                return mImageOffsetX;
            case IMAGE_OFFSET_Y_PROPERTY_KEY:
                return mImageOffsetY;
            case IMAGE_ROTATION_PROPERTY_KEY:
                return mImageRotation;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case IMAGE_ASSET_ID_PROPERTY_KEY:
                mImageAssetId = value;
                return true;
            case IMAGE_SAMPLER_FILTER_PROPERTY_KEY:
                mImageSamplerFilter = value;
                return true;
            case IMAGE_SAMPLER_WRAP_X_PROPERTY_KEY:
                mImageSamplerWrapX = value;
                return true;
            case IMAGE_SAMPLER_WRAP_Y_PROPERTY_KEY:
                mImageSamplerWrapY = value;
                return true;
            case IMAGE_SIZE_MODE_PROPERTY_KEY:
                mImageSizeMode = value;
                return true;
            case ARTBOARD_ID_PROPERTY_KEY:
                mArtboardId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case IMAGE_ASSET_ID_PROPERTY_KEY:
                setImageAssetId(value);
                return true;
            case IMAGE_SAMPLER_FILTER_PROPERTY_KEY:
                setImageSamplerFilter(value);
                return true;
            case IMAGE_SAMPLER_WRAP_X_PROPERTY_KEY:
                setImageSamplerWrapX(value);
                return true;
            case IMAGE_SAMPLER_WRAP_Y_PROPERTY_KEY:
                setImageSamplerWrapY(value);
                return true;
            case IMAGE_SIZE_MODE_PROPERTY_KEY:
                setImageSizeMode(value);
                return true;
            case ARTBOARD_ID_PROPERTY_KEY:
                setArtboardId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case IMAGE_ASSET_ID_PROPERTY_KEY:
                return mImageAssetId;
            case IMAGE_SAMPLER_FILTER_PROPERTY_KEY:
                return mImageSamplerFilter;
            case IMAGE_SAMPLER_WRAP_X_PROPERTY_KEY:
                return mImageSamplerWrapX;
            case IMAGE_SAMPLER_WRAP_Y_PROPERTY_KEY:
                return mImageSamplerWrapY;
            case IMAGE_SIZE_MODE_PROPERTY_KEY:
                return mImageSizeMode;
            case ARTBOARD_ID_PROPERTY_KEY:
                return mArtboardId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
