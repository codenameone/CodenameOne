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

/// Generated from rive-runtime's `ArtboardComponentListOverrideBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ArtboardComponentListOverride`.
public abstract class ArtboardComponentListOverrideBase extends Component {
    public static final int TYPE_KEY = 606;
    public static final int ARTBOARD_ID_PROPERTY_KEY = 858;
    public static final int INSTANCE_WIDTH_PROPERTY_KEY = 859;
    public static final int INSTANCE_HEIGHT_PROPERTY_KEY = 860;
    public static final int INSTANCE_WIDTH_SCALE_TYPE_PROPERTY_KEY = 862;

    protected int mArtboardId = -1;
    protected float mInstanceWidth = -1.0f;
    protected float mInstanceHeight = -1.0f;
    protected int mInstanceWidthScaleType = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
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

    public float getInstanceWidth() {
        return mInstanceWidth;
    }

    public void setInstanceWidth(float value) {
        if (Float.floatToIntBits(mInstanceWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mInstanceWidth = value;
        instanceWidthChanged();
    }

    protected void instanceWidthChanged() {
    }

    public float getInstanceHeight() {
        return mInstanceHeight;
    }

    public void setInstanceHeight(float value) {
        if (Float.floatToIntBits(mInstanceHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mInstanceHeight = value;
        instanceHeightChanged();
    }

    protected void instanceHeightChanged() {
    }

    public int getInstanceWidthScaleType() {
        return mInstanceWidthScaleType;
    }

    public void setInstanceWidthScaleType(int value) {
        if (mInstanceWidthScaleType == value) {
            return;
        }
        mInstanceWidthScaleType = value;
        instanceWidthScaleTypeChanged();
    }

    protected void instanceWidthScaleTypeChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case INSTANCE_WIDTH_PROPERTY_KEY:
                mInstanceWidth = value;
                return true;
            case INSTANCE_HEIGHT_PROPERTY_KEY:
                mInstanceHeight = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case INSTANCE_WIDTH_PROPERTY_KEY:
                setInstanceWidth(value);
                return true;
            case INSTANCE_HEIGHT_PROPERTY_KEY:
                setInstanceHeight(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case INSTANCE_WIDTH_PROPERTY_KEY:
                return mInstanceWidth;
            case INSTANCE_HEIGHT_PROPERTY_KEY:
                return mInstanceHeight;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case ARTBOARD_ID_PROPERTY_KEY:
                mArtboardId = value;
                return true;
            case INSTANCE_WIDTH_SCALE_TYPE_PROPERTY_KEY:
                mInstanceWidthScaleType = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case ARTBOARD_ID_PROPERTY_KEY:
                setArtboardId(value);
                return true;
            case INSTANCE_WIDTH_SCALE_TYPE_PROPERTY_KEY:
                setInstanceWidthScaleType(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case ARTBOARD_ID_PROPERTY_KEY:
                return mArtboardId;
            case INSTANCE_WIDTH_SCALE_TYPE_PROPERTY_KEY:
                return mInstanceWidthScaleType;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
