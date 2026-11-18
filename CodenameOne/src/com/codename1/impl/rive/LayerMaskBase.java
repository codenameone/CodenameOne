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

/// Generated from rive-runtime's `LayerMaskBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `LayerMask`.
public abstract class LayerMaskBase extends Component {
    public static final int TYPE_KEY = 154;
    public static final int SOURCE_ID_PROPERTY_KEY = 459;
    public static final int MASK_FLAGS_PROPERTY_KEY = 460;
    public static final int MASK_MODE_VALUE_PROPERTY_KEY = 455;
    public static final int IS_VISIBLE_PROPERTY_KEY = 456;
    public static final int SOURCE_DRAWS_PROPERTY_KEY = 457;
    public static final int USE_CUSTOM_BOUNDS_PROPERTY_KEY = 461;
    public static final int RESOLUTION_PROPERTY_KEY = 458;
    public static final int BOUNDS_X_PROPERTY_KEY = 462;
    public static final int BOUNDS_Y_PROPERTY_KEY = 463;
    public static final int BOUNDS_WIDTH_PROPERTY_KEY = 464;
    public static final int BOUNDS_HEIGHT_PROPERTY_KEY = 465;

    protected int mSourceId = -1;
    protected int mMaskFlags = 4;
    protected int mMaskModeValue = 0;
    protected boolean mIsVisible = false;
    protected boolean mSourceDraws = false;
    protected boolean mUseCustomBounds = false;
    protected float mResolution = 1.0f;
    protected float mBoundsX = 0.0f;
    protected float mBoundsY = 0.0f;
    protected float mBoundsWidth = 0.0f;
    protected float mBoundsHeight = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getSourceId() {
        return mSourceId;
    }

    public void setSourceId(int value) {
        if (mSourceId == value) {
            return;
        }
        mSourceId = value;
        sourceIdChanged();
    }

    protected void sourceIdChanged() {
    }

    public int getMaskFlags() {
        return mMaskFlags;
    }

    public void setMaskFlags(int value) {
        if (mMaskFlags == value) {
            return;
        }
        mMaskFlags = value;
        maskFlagsChanged();
    }

    protected void maskFlagsChanged() {
    }

    public int getMaskModeValue() {
        return mMaskModeValue;
    }

    public void setMaskModeValue(int value) {
        if (mMaskModeValue == value) {
            return;
        }
        mMaskModeValue = value;
        maskModeValueChanged();
    }

    protected void maskModeValueChanged() {
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

    public boolean isSourceDraws() {
        return mSourceDraws;
    }

    public void setSourceDraws(boolean value) {
        if (mSourceDraws == value) {
            return;
        }
        mSourceDraws = value;
        sourceDrawsChanged();
    }

    protected void sourceDrawsChanged() {
    }

    public boolean isUseCustomBounds() {
        return mUseCustomBounds;
    }

    public void setUseCustomBounds(boolean value) {
        if (mUseCustomBounds == value) {
            return;
        }
        mUseCustomBounds = value;
        useCustomBoundsChanged();
    }

    protected void useCustomBoundsChanged() {
    }

    public float getResolution() {
        return mResolution;
    }

    public void setResolution(float value) {
        if (Float.floatToIntBits(mResolution) == Float.floatToIntBits(value)) {
            return;
        }
        mResolution = value;
        resolutionChanged();
    }

    protected void resolutionChanged() {
    }

    public float getBoundsX() {
        return mBoundsX;
    }

    public void setBoundsX(float value) {
        if (Float.floatToIntBits(mBoundsX) == Float.floatToIntBits(value)) {
            return;
        }
        mBoundsX = value;
        boundsXChanged();
    }

    protected void boundsXChanged() {
    }

    public float getBoundsY() {
        return mBoundsY;
    }

    public void setBoundsY(float value) {
        if (Float.floatToIntBits(mBoundsY) == Float.floatToIntBits(value)) {
            return;
        }
        mBoundsY = value;
        boundsYChanged();
    }

    protected void boundsYChanged() {
    }

    public float getBoundsWidth() {
        return mBoundsWidth;
    }

    public void setBoundsWidth(float value) {
        if (Float.floatToIntBits(mBoundsWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mBoundsWidth = value;
        boundsWidthChanged();
    }

    protected void boundsWidthChanged() {
    }

    public float getBoundsHeight() {
        return mBoundsHeight;
    }

    public void setBoundsHeight(float value) {
        if (Float.floatToIntBits(mBoundsHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mBoundsHeight = value;
        boundsHeightChanged();
    }

    protected void boundsHeightChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_VISIBLE_PROPERTY_KEY:
                mIsVisible = value;
                return true;
            case SOURCE_DRAWS_PROPERTY_KEY:
                mSourceDraws = value;
                return true;
            case USE_CUSTOM_BOUNDS_PROPERTY_KEY:
                mUseCustomBounds = value;
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
            case SOURCE_DRAWS_PROPERTY_KEY:
                setSourceDraws(value);
                return true;
            case USE_CUSTOM_BOUNDS_PROPERTY_KEY:
                setUseCustomBounds(value);
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
            case SOURCE_DRAWS_PROPERTY_KEY:
                return mSourceDraws;
            case USE_CUSTOM_BOUNDS_PROPERTY_KEY:
                return mUseCustomBounds;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case RESOLUTION_PROPERTY_KEY:
                mResolution = value;
                return true;
            case BOUNDS_X_PROPERTY_KEY:
                mBoundsX = value;
                return true;
            case BOUNDS_Y_PROPERTY_KEY:
                mBoundsY = value;
                return true;
            case BOUNDS_WIDTH_PROPERTY_KEY:
                mBoundsWidth = value;
                return true;
            case BOUNDS_HEIGHT_PROPERTY_KEY:
                mBoundsHeight = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case RESOLUTION_PROPERTY_KEY:
                setResolution(value);
                return true;
            case BOUNDS_X_PROPERTY_KEY:
                setBoundsX(value);
                return true;
            case BOUNDS_Y_PROPERTY_KEY:
                setBoundsY(value);
                return true;
            case BOUNDS_WIDTH_PROPERTY_KEY:
                setBoundsWidth(value);
                return true;
            case BOUNDS_HEIGHT_PROPERTY_KEY:
                setBoundsHeight(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case RESOLUTION_PROPERTY_KEY:
                return mResolution;
            case BOUNDS_X_PROPERTY_KEY:
                return mBoundsX;
            case BOUNDS_Y_PROPERTY_KEY:
                return mBoundsY;
            case BOUNDS_WIDTH_PROPERTY_KEY:
                return mBoundsWidth;
            case BOUNDS_HEIGHT_PROPERTY_KEY:
                return mBoundsHeight;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case SOURCE_ID_PROPERTY_KEY:
                mSourceId = value;
                return true;
            case MASK_FLAGS_PROPERTY_KEY:
                mMaskFlags = value;
                return true;
            case MASK_MODE_VALUE_PROPERTY_KEY:
                mMaskModeValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case SOURCE_ID_PROPERTY_KEY:
                setSourceId(value);
                return true;
            case MASK_FLAGS_PROPERTY_KEY:
                setMaskFlags(value);
                return true;
            case MASK_MODE_VALUE_PROPERTY_KEY:
                setMaskModeValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case SOURCE_ID_PROPERTY_KEY:
                return mSourceId;
            case MASK_FLAGS_PROPERTY_KEY:
                return mMaskFlags;
            case MASK_MODE_VALUE_PROPERTY_KEY:
                return mMaskModeValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
