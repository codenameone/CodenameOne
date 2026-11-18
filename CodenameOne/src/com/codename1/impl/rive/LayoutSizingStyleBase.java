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

/// Generated from rive-runtime's `LayoutSizingStyleBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `LayoutSizingStyle`.
public abstract class LayoutSizingStyleBase extends Component {
    public static final int TYPE_KEY = 1056;
    public static final int MIN_WIDTH_PROPERTY_KEY = 502;
    public static final int MAX_WIDTH_PROPERTY_KEY = 500;
    public static final int MIN_HEIGHT_PROPERTY_KEY = 503;
    public static final int MAX_HEIGHT_PROPERTY_KEY = 501;
    public static final int MIN_WIDTH_UNITS_VALUE_PROPERTY_KEY = 627;
    public static final int MAX_WIDTH_UNITS_VALUE_PROPERTY_KEY = 629;
    public static final int MIN_HEIGHT_UNITS_VALUE_PROPERTY_KEY = 628;
    public static final int MAX_HEIGHT_UNITS_VALUE_PROPERTY_KEY = 630;
    public static final int LAYOUT_WIDTH_SCALE_TYPE_PROPERTY_KEY = 655;
    public static final int LAYOUT_HEIGHT_SCALE_TYPE_PROPERTY_KEY = 656;
    public static final int HUG_UNBOUNDED_PROPERTY_KEY = 451;
    public static final int WIDTH_UNITS_VALUE_PROPERTY_KEY = 607;
    public static final int HEIGHT_UNITS_VALUE_PROPERTY_KEY = 608;
    public static final int JUSTIFY_SELF_VALUE_PROPERTY_KEY = 1046;
    public static final int DISPLAY_VALUE_PROPERTY_KEY = 596;

    protected float mMinWidth = 0.0f;
    protected float mMaxWidth = 0.0f;
    protected float mMinHeight = 0.0f;
    protected float mMaxHeight = 0.0f;
    protected int mMinWidthUnitsValue = 0;
    protected int mMaxWidthUnitsValue = 0;
    protected int mMinHeightUnitsValue = 0;
    protected int mMaxHeightUnitsValue = 0;
    protected int mLayoutWidthScaleType = 0;
    protected int mLayoutHeightScaleType = 0;
    protected boolean mHugUnbounded = false;
    protected int mWidthUnitsValue = 1;
    protected int mHeightUnitsValue = 1;
    protected int mJustifySelfValue = 6;
    protected int mDisplayValue = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getMinWidth() {
        return mMinWidth;
    }

    public void setMinWidth(float value) {
        if (Float.floatToIntBits(mMinWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mMinWidth = value;
        minWidthChanged();
    }

    protected void minWidthChanged() {
    }

    public float getMaxWidth() {
        return mMaxWidth;
    }

    public void setMaxWidth(float value) {
        if (Float.floatToIntBits(mMaxWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mMaxWidth = value;
        maxWidthChanged();
    }

    protected void maxWidthChanged() {
    }

    public float getMinHeight() {
        return mMinHeight;
    }

    public void setMinHeight(float value) {
        if (Float.floatToIntBits(mMinHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mMinHeight = value;
        minHeightChanged();
    }

    protected void minHeightChanged() {
    }

    public float getMaxHeight() {
        return mMaxHeight;
    }

    public void setMaxHeight(float value) {
        if (Float.floatToIntBits(mMaxHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mMaxHeight = value;
        maxHeightChanged();
    }

    protected void maxHeightChanged() {
    }

    public int getMinWidthUnitsValue() {
        return mMinWidthUnitsValue;
    }

    public void setMinWidthUnitsValue(int value) {
        if (mMinWidthUnitsValue == value) {
            return;
        }
        mMinWidthUnitsValue = value;
        minWidthUnitsValueChanged();
    }

    protected void minWidthUnitsValueChanged() {
    }

    public int getMaxWidthUnitsValue() {
        return mMaxWidthUnitsValue;
    }

    public void setMaxWidthUnitsValue(int value) {
        if (mMaxWidthUnitsValue == value) {
            return;
        }
        mMaxWidthUnitsValue = value;
        maxWidthUnitsValueChanged();
    }

    protected void maxWidthUnitsValueChanged() {
    }

    public int getMinHeightUnitsValue() {
        return mMinHeightUnitsValue;
    }

    public void setMinHeightUnitsValue(int value) {
        if (mMinHeightUnitsValue == value) {
            return;
        }
        mMinHeightUnitsValue = value;
        minHeightUnitsValueChanged();
    }

    protected void minHeightUnitsValueChanged() {
    }

    public int getMaxHeightUnitsValue() {
        return mMaxHeightUnitsValue;
    }

    public void setMaxHeightUnitsValue(int value) {
        if (mMaxHeightUnitsValue == value) {
            return;
        }
        mMaxHeightUnitsValue = value;
        maxHeightUnitsValueChanged();
    }

    protected void maxHeightUnitsValueChanged() {
    }

    public int getLayoutWidthScaleType() {
        return mLayoutWidthScaleType;
    }

    public void setLayoutWidthScaleType(int value) {
        if (mLayoutWidthScaleType == value) {
            return;
        }
        mLayoutWidthScaleType = value;
        layoutWidthScaleTypeChanged();
    }

    protected void layoutWidthScaleTypeChanged() {
    }

    public int getLayoutHeightScaleType() {
        return mLayoutHeightScaleType;
    }

    public void setLayoutHeightScaleType(int value) {
        if (mLayoutHeightScaleType == value) {
            return;
        }
        mLayoutHeightScaleType = value;
        layoutHeightScaleTypeChanged();
    }

    protected void layoutHeightScaleTypeChanged() {
    }

    public boolean isHugUnbounded() {
        return mHugUnbounded;
    }

    public void setHugUnbounded(boolean value) {
        if (mHugUnbounded == value) {
            return;
        }
        mHugUnbounded = value;
        hugUnboundedChanged();
    }

    protected void hugUnboundedChanged() {
    }

    public int getWidthUnitsValue() {
        return mWidthUnitsValue;
    }

    public void setWidthUnitsValue(int value) {
        if (mWidthUnitsValue == value) {
            return;
        }
        mWidthUnitsValue = value;
        widthUnitsValueChanged();
    }

    protected void widthUnitsValueChanged() {
    }

    public int getHeightUnitsValue() {
        return mHeightUnitsValue;
    }

    public void setHeightUnitsValue(int value) {
        if (mHeightUnitsValue == value) {
            return;
        }
        mHeightUnitsValue = value;
        heightUnitsValueChanged();
    }

    protected void heightUnitsValueChanged() {
    }

    public int getJustifySelfValue() {
        return mJustifySelfValue;
    }

    public void setJustifySelfValue(int value) {
        if (mJustifySelfValue == value) {
            return;
        }
        mJustifySelfValue = value;
        justifySelfValueChanged();
    }

    protected void justifySelfValueChanged() {
    }

    public int getDisplayValue() {
        return mDisplayValue;
    }

    public void setDisplayValue(int value) {
        if (mDisplayValue == value) {
            return;
        }
        mDisplayValue = value;
        displayValueChanged();
    }

    protected void displayValueChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case HUG_UNBOUNDED_PROPERTY_KEY:
                mHugUnbounded = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case HUG_UNBOUNDED_PROPERTY_KEY:
                setHugUnbounded(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case HUG_UNBOUNDED_PROPERTY_KEY:
                return mHugUnbounded;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case MIN_WIDTH_PROPERTY_KEY:
                mMinWidth = value;
                return true;
            case MAX_WIDTH_PROPERTY_KEY:
                mMaxWidth = value;
                return true;
            case MIN_HEIGHT_PROPERTY_KEY:
                mMinHeight = value;
                return true;
            case MAX_HEIGHT_PROPERTY_KEY:
                mMaxHeight = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case MIN_WIDTH_PROPERTY_KEY:
                setMinWidth(value);
                return true;
            case MAX_WIDTH_PROPERTY_KEY:
                setMaxWidth(value);
                return true;
            case MIN_HEIGHT_PROPERTY_KEY:
                setMinHeight(value);
                return true;
            case MAX_HEIGHT_PROPERTY_KEY:
                setMaxHeight(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case MIN_WIDTH_PROPERTY_KEY:
                return mMinWidth;
            case MAX_WIDTH_PROPERTY_KEY:
                return mMaxWidth;
            case MIN_HEIGHT_PROPERTY_KEY:
                return mMinHeight;
            case MAX_HEIGHT_PROPERTY_KEY:
                return mMaxHeight;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case MIN_WIDTH_UNITS_VALUE_PROPERTY_KEY:
                mMinWidthUnitsValue = value;
                return true;
            case MAX_WIDTH_UNITS_VALUE_PROPERTY_KEY:
                mMaxWidthUnitsValue = value;
                return true;
            case MIN_HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                mMinHeightUnitsValue = value;
                return true;
            case MAX_HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                mMaxHeightUnitsValue = value;
                return true;
            case LAYOUT_WIDTH_SCALE_TYPE_PROPERTY_KEY:
                mLayoutWidthScaleType = value;
                return true;
            case LAYOUT_HEIGHT_SCALE_TYPE_PROPERTY_KEY:
                mLayoutHeightScaleType = value;
                return true;
            case WIDTH_UNITS_VALUE_PROPERTY_KEY:
                mWidthUnitsValue = value;
                return true;
            case HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                mHeightUnitsValue = value;
                return true;
            case JUSTIFY_SELF_VALUE_PROPERTY_KEY:
                mJustifySelfValue = value;
                return true;
            case DISPLAY_VALUE_PROPERTY_KEY:
                mDisplayValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case MIN_WIDTH_UNITS_VALUE_PROPERTY_KEY:
                setMinWidthUnitsValue(value);
                return true;
            case MAX_WIDTH_UNITS_VALUE_PROPERTY_KEY:
                setMaxWidthUnitsValue(value);
                return true;
            case MIN_HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                setMinHeightUnitsValue(value);
                return true;
            case MAX_HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                setMaxHeightUnitsValue(value);
                return true;
            case LAYOUT_WIDTH_SCALE_TYPE_PROPERTY_KEY:
                setLayoutWidthScaleType(value);
                return true;
            case LAYOUT_HEIGHT_SCALE_TYPE_PROPERTY_KEY:
                setLayoutHeightScaleType(value);
                return true;
            case WIDTH_UNITS_VALUE_PROPERTY_KEY:
                setWidthUnitsValue(value);
                return true;
            case HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                setHeightUnitsValue(value);
                return true;
            case JUSTIFY_SELF_VALUE_PROPERTY_KEY:
                setJustifySelfValue(value);
                return true;
            case DISPLAY_VALUE_PROPERTY_KEY:
                setDisplayValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case MIN_WIDTH_UNITS_VALUE_PROPERTY_KEY:
                return mMinWidthUnitsValue;
            case MAX_WIDTH_UNITS_VALUE_PROPERTY_KEY:
                return mMaxWidthUnitsValue;
            case MIN_HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                return mMinHeightUnitsValue;
            case MAX_HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                return mMaxHeightUnitsValue;
            case LAYOUT_WIDTH_SCALE_TYPE_PROPERTY_KEY:
                return mLayoutWidthScaleType;
            case LAYOUT_HEIGHT_SCALE_TYPE_PROPERTY_KEY:
                return mLayoutHeightScaleType;
            case WIDTH_UNITS_VALUE_PROPERTY_KEY:
                return mWidthUnitsValue;
            case HEIGHT_UNITS_VALUE_PROPERTY_KEY:
                return mHeightUnitsValue;
            case JUSTIFY_SELF_VALUE_PROPERTY_KEY:
                return mJustifySelfValue;
            case DISPLAY_VALUE_PROPERTY_KEY:
                return mDisplayValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
