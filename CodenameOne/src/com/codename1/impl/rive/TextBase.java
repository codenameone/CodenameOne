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

/// Generated from rive-runtime's `TextBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Text`.
public abstract class TextBase extends Drawable {
    public static final int TYPE_KEY = 134;
    public static final int ALIGN_VALUE_PROPERTY_KEY = 281;
    public static final int SIZING_VALUE_PROPERTY_KEY = 284;
    public static final int OVERFLOW_VALUE_PROPERTY_KEY = 287;
    public static final int WIDTH_PROPERTY_KEY = 285;
    public static final int HEIGHT_PROPERTY_KEY = 286;
    public static final int ORIGIN_X_PROPERTY_KEY = 366;
    public static final int ORIGIN_Y_PROPERTY_KEY = 367;
    public static final int PARAGRAPH_SPACING_PROPERTY_KEY = 371;
    public static final int ORIGIN_VALUE_PROPERTY_KEY = 377;
    public static final int WRAP_VALUE_PROPERTY_KEY = 683;
    public static final int WORD_BREAK_VALUE_PROPERTY_KEY = 446;
    public static final int VERTICAL_ALIGN_VALUE_PROPERTY_KEY = 685;
    public static final int FIT_FROM_BASELINE_PROPERTY_KEY = 703;
    public static final int FIT_FONT_SIZE_RESIZES_BOX_PROPERTY_KEY = 315;
    public static final int TEXT_RUN_LIST_SOURCE_PROPERTY_KEY = 932;
    public static final int VERTICAL_TRIM_VALUE_PROPERTY_KEY = 1026;
    public static final int VERTICAL_TRIM_TOP_VALUE_PROPERTY_KEY = 1027;
    public static final int VERTICAL_TRIM_BOTTOM_VALUE_PROPERTY_KEY = 1028;
    public static final int LANGUAGE_PROPERTY_KEY = 1126;
    public static final int LANGUAGE_ID_PROPERTY_KEY = 1127;

    protected int mAlignValue = 0;
    protected int mSizingValue = 0;
    protected int mOverflowValue = 0;
    protected float mWidth = 0.0f;
    protected float mHeight = 0.0f;
    protected float mOriginX = 0.0f;
    protected float mOriginY = 0.0f;
    protected float mParagraphSpacing = 0.0f;
    protected int mOriginValue = 0;
    protected int mWrapValue = 0;
    protected int mWordBreakValue = 0;
    protected int mVerticalAlignValue = 0;
    protected boolean mFitFromBaseline = true;
    protected boolean mFitFontSizeResizesBox = true;
    protected int mTextRunListSource = -1;
    protected int mVerticalTrimValue = 0;
    protected int mVerticalTrimTopValue = 0;
    protected int mVerticalTrimBottomValue = 0;
    protected String mLanguage = "";
    protected int mLanguageId = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getAlignValue() {
        return mAlignValue;
    }

    public void setAlignValue(int value) {
        if (mAlignValue == value) {
            return;
        }
        mAlignValue = value;
        alignValueChanged();
    }

    protected void alignValueChanged() {
    }

    public int getSizingValue() {
        return mSizingValue;
    }

    public void setSizingValue(int value) {
        if (mSizingValue == value) {
            return;
        }
        mSizingValue = value;
        sizingValueChanged();
    }

    protected void sizingValueChanged() {
    }

    public int getOverflowValue() {
        return mOverflowValue;
    }

    public void setOverflowValue(int value) {
        if (mOverflowValue == value) {
            return;
        }
        mOverflowValue = value;
        overflowValueChanged();
    }

    protected void overflowValueChanged() {
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

    public float getParagraphSpacing() {
        return mParagraphSpacing;
    }

    public void setParagraphSpacing(float value) {
        if (Float.floatToIntBits(mParagraphSpacing) == Float.floatToIntBits(value)) {
            return;
        }
        mParagraphSpacing = value;
        paragraphSpacingChanged();
    }

    protected void paragraphSpacingChanged() {
    }

    public int getOriginValue() {
        return mOriginValue;
    }

    public void setOriginValue(int value) {
        if (mOriginValue == value) {
            return;
        }
        mOriginValue = value;
        originValueChanged();
    }

    protected void originValueChanged() {
    }

    public int getWrapValue() {
        return mWrapValue;
    }

    public void setWrapValue(int value) {
        if (mWrapValue == value) {
            return;
        }
        mWrapValue = value;
        wrapValueChanged();
    }

    protected void wrapValueChanged() {
    }

    public int getWordBreakValue() {
        return mWordBreakValue;
    }

    public void setWordBreakValue(int value) {
        if (mWordBreakValue == value) {
            return;
        }
        mWordBreakValue = value;
        wordBreakValueChanged();
    }

    protected void wordBreakValueChanged() {
    }

    public int getVerticalAlignValue() {
        return mVerticalAlignValue;
    }

    public void setVerticalAlignValue(int value) {
        if (mVerticalAlignValue == value) {
            return;
        }
        mVerticalAlignValue = value;
        verticalAlignValueChanged();
    }

    protected void verticalAlignValueChanged() {
    }

    public boolean isFitFromBaseline() {
        return mFitFromBaseline;
    }

    public void setFitFromBaseline(boolean value) {
        if (mFitFromBaseline == value) {
            return;
        }
        mFitFromBaseline = value;
        fitFromBaselineChanged();
    }

    protected void fitFromBaselineChanged() {
    }

    public boolean isFitFontSizeResizesBox() {
        return mFitFontSizeResizesBox;
    }

    public void setFitFontSizeResizesBox(boolean value) {
        if (mFitFontSizeResizesBox == value) {
            return;
        }
        mFitFontSizeResizesBox = value;
        fitFontSizeResizesBoxChanged();
    }

    protected void fitFontSizeResizesBoxChanged() {
    }

    public int getTextRunListSource() {
        return mTextRunListSource;
    }

    public void setTextRunListSource(int value) {
        if (mTextRunListSource == value) {
            return;
        }
        mTextRunListSource = value;
        textRunListSourceChanged();
    }

    protected void textRunListSourceChanged() {
    }

    public int getVerticalTrimValue() {
        return mVerticalTrimValue;
    }

    public void setVerticalTrimValue(int value) {
        if (mVerticalTrimValue == value) {
            return;
        }
        mVerticalTrimValue = value;
        verticalTrimValueChanged();
    }

    protected void verticalTrimValueChanged() {
    }

    public int getVerticalTrimTopValue() {
        return mVerticalTrimTopValue;
    }

    public void setVerticalTrimTopValue(int value) {
        if (mVerticalTrimTopValue == value) {
            return;
        }
        mVerticalTrimTopValue = value;
        verticalTrimTopValueChanged();
    }

    protected void verticalTrimTopValueChanged() {
    }

    public int getVerticalTrimBottomValue() {
        return mVerticalTrimBottomValue;
    }

    public void setVerticalTrimBottomValue(int value) {
        if (mVerticalTrimBottomValue == value) {
            return;
        }
        mVerticalTrimBottomValue = value;
        verticalTrimBottomValueChanged();
    }

    protected void verticalTrimBottomValueChanged() {
    }

    public String getLanguage() {
        return mLanguage;
    }

    public void setLanguage(String value) {
        if ((mLanguage == null ? value == null : mLanguage.equals(value))) {
            return;
        }
        mLanguage = value;
        languageChanged();
    }

    protected void languageChanged() {
    }

    public int getLanguageId() {
        return mLanguageId;
    }

    public void setLanguageId(int value) {
        if (mLanguageId == value) {
            return;
        }
        mLanguageId = value;
        languageIdChanged();
    }

    protected void languageIdChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case FIT_FROM_BASELINE_PROPERTY_KEY:
                mFitFromBaseline = value;
                return true;
            case FIT_FONT_SIZE_RESIZES_BOX_PROPERTY_KEY:
                mFitFontSizeResizesBox = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case FIT_FROM_BASELINE_PROPERTY_KEY:
                setFitFromBaseline(value);
                return true;
            case FIT_FONT_SIZE_RESIZES_BOX_PROPERTY_KEY:
                setFitFontSizeResizesBox(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case FIT_FROM_BASELINE_PROPERTY_KEY:
                return mFitFromBaseline;
            case FIT_FONT_SIZE_RESIZES_BOX_PROPERTY_KEY:
                return mFitFontSizeResizesBox;
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
            case ORIGIN_X_PROPERTY_KEY:
                mOriginX = value;
                return true;
            case ORIGIN_Y_PROPERTY_KEY:
                mOriginY = value;
                return true;
            case PARAGRAPH_SPACING_PROPERTY_KEY:
                mParagraphSpacing = value;
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
            case ORIGIN_X_PROPERTY_KEY:
                setOriginX(value);
                return true;
            case ORIGIN_Y_PROPERTY_KEY:
                setOriginY(value);
                return true;
            case PARAGRAPH_SPACING_PROPERTY_KEY:
                setParagraphSpacing(value);
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
            case ORIGIN_X_PROPERTY_KEY:
                return mOriginX;
            case ORIGIN_Y_PROPERTY_KEY:
                return mOriginY;
            case PARAGRAPH_SPACING_PROPERTY_KEY:
                return mParagraphSpacing;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeString(int propertyKey, String value) {
        switch (propertyKey) {
            case LANGUAGE_PROPERTY_KEY:
                mLanguage = value;
                return true;
            default:
                return super.deserializeString(propertyKey, value);
        }
    }

    @Override
    public boolean setStringProperty(int propertyKey, String value) {
        switch (propertyKey) {
            case LANGUAGE_PROPERTY_KEY:
                setLanguage(value);
                return true;
            default:
                return super.setStringProperty(propertyKey, value);
        }
    }

    @Override
    public String getStringProperty(int propertyKey) {
        switch (propertyKey) {
            case LANGUAGE_PROPERTY_KEY:
                return mLanguage;
            default:
                return super.getStringProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case ALIGN_VALUE_PROPERTY_KEY:
                mAlignValue = value;
                return true;
            case SIZING_VALUE_PROPERTY_KEY:
                mSizingValue = value;
                return true;
            case OVERFLOW_VALUE_PROPERTY_KEY:
                mOverflowValue = value;
                return true;
            case ORIGIN_VALUE_PROPERTY_KEY:
                mOriginValue = value;
                return true;
            case WRAP_VALUE_PROPERTY_KEY:
                mWrapValue = value;
                return true;
            case WORD_BREAK_VALUE_PROPERTY_KEY:
                mWordBreakValue = value;
                return true;
            case VERTICAL_ALIGN_VALUE_PROPERTY_KEY:
                mVerticalAlignValue = value;
                return true;
            case TEXT_RUN_LIST_SOURCE_PROPERTY_KEY:
                mTextRunListSource = value;
                return true;
            case VERTICAL_TRIM_VALUE_PROPERTY_KEY:
                mVerticalTrimValue = value;
                return true;
            case VERTICAL_TRIM_TOP_VALUE_PROPERTY_KEY:
                mVerticalTrimTopValue = value;
                return true;
            case VERTICAL_TRIM_BOTTOM_VALUE_PROPERTY_KEY:
                mVerticalTrimBottomValue = value;
                return true;
            case LANGUAGE_ID_PROPERTY_KEY:
                mLanguageId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case ALIGN_VALUE_PROPERTY_KEY:
                setAlignValue(value);
                return true;
            case SIZING_VALUE_PROPERTY_KEY:
                setSizingValue(value);
                return true;
            case OVERFLOW_VALUE_PROPERTY_KEY:
                setOverflowValue(value);
                return true;
            case ORIGIN_VALUE_PROPERTY_KEY:
                setOriginValue(value);
                return true;
            case WRAP_VALUE_PROPERTY_KEY:
                setWrapValue(value);
                return true;
            case WORD_BREAK_VALUE_PROPERTY_KEY:
                setWordBreakValue(value);
                return true;
            case VERTICAL_ALIGN_VALUE_PROPERTY_KEY:
                setVerticalAlignValue(value);
                return true;
            case TEXT_RUN_LIST_SOURCE_PROPERTY_KEY:
                setTextRunListSource(value);
                return true;
            case VERTICAL_TRIM_VALUE_PROPERTY_KEY:
                setVerticalTrimValue(value);
                return true;
            case VERTICAL_TRIM_TOP_VALUE_PROPERTY_KEY:
                setVerticalTrimTopValue(value);
                return true;
            case VERTICAL_TRIM_BOTTOM_VALUE_PROPERTY_KEY:
                setVerticalTrimBottomValue(value);
                return true;
            case LANGUAGE_ID_PROPERTY_KEY:
                setLanguageId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case ALIGN_VALUE_PROPERTY_KEY:
                return mAlignValue;
            case SIZING_VALUE_PROPERTY_KEY:
                return mSizingValue;
            case OVERFLOW_VALUE_PROPERTY_KEY:
                return mOverflowValue;
            case ORIGIN_VALUE_PROPERTY_KEY:
                return mOriginValue;
            case WRAP_VALUE_PROPERTY_KEY:
                return mWrapValue;
            case WORD_BREAK_VALUE_PROPERTY_KEY:
                return mWordBreakValue;
            case VERTICAL_ALIGN_VALUE_PROPERTY_KEY:
                return mVerticalAlignValue;
            case TEXT_RUN_LIST_SOURCE_PROPERTY_KEY:
                return mTextRunListSource;
            case VERTICAL_TRIM_VALUE_PROPERTY_KEY:
                return mVerticalTrimValue;
            case VERTICAL_TRIM_TOP_VALUE_PROPERTY_KEY:
                return mVerticalTrimTopValue;
            case VERTICAL_TRIM_BOTTOM_VALUE_PROPERTY_KEY:
                return mVerticalTrimBottomValue;
            case LANGUAGE_ID_PROPERTY_KEY:
                return mLanguageId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
