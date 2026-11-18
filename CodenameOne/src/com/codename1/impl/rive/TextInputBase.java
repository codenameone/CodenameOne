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

/// Generated from rive-runtime's `TextInputBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TextInput`.
public abstract class TextInputBase extends Drawable {
    public static final int TYPE_KEY = 569;
    public static final int TEXT_PROPERTY_KEY = 817;
    public static final int SELECTION_RADIUS_PROPERTY_KEY = 818;
    public static final int MULTILINE_PROPERTY_KEY = 979;
    public static final int ALIGN_VALUE_PROPERTY_KEY = 222;
    public static final int VERTICAL_ALIGN_VALUE_PROPERTY_KEY = 1094;
    public static final int OBSCURED_PROPERTY_KEY = 1095;
    public static final int SELECT_ALL_ON_FOCUS_PROPERTY_KEY = 1099;
    public static final int MAX_CHARACTERS_PROPERTY_KEY = 489;
    public static final int MAX_LINES_PROPERTY_KEY = 490;
    public static final int LANGUAGE_PROPERTY_KEY = 1124;
    public static final int LANGUAGE_ID_PROPERTY_KEY = 1125;

    protected String mText = "";
    protected float mSelectionRadius = 5.0f;
    protected boolean mMultiline = true;
    protected int mAlignValue = 0;
    protected int mVerticalAlignValue = 0;
    protected boolean mObscured = false;
    protected boolean mSelectAllOnFocus = false;
    protected int mMaxCharacters = 0;
    protected int mMaxLines = 0;
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

    public String getText() {
        return mText;
    }

    public void setText(String value) {
        if ((mText == null ? value == null : mText.equals(value))) {
            return;
        }
        mText = value;
        textChanged();
    }

    protected void textChanged() {
    }

    public float getSelectionRadius() {
        return mSelectionRadius;
    }

    public void setSelectionRadius(float value) {
        if (Float.floatToIntBits(mSelectionRadius) == Float.floatToIntBits(value)) {
            return;
        }
        mSelectionRadius = value;
        selectionRadiusChanged();
    }

    protected void selectionRadiusChanged() {
    }

    public boolean isMultiline() {
        return mMultiline;
    }

    public void setMultiline(boolean value) {
        if (mMultiline == value) {
            return;
        }
        mMultiline = value;
        multilineChanged();
    }

    protected void multilineChanged() {
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

    public boolean isObscured() {
        return mObscured;
    }

    public void setObscured(boolean value) {
        if (mObscured == value) {
            return;
        }
        mObscured = value;
        obscuredChanged();
    }

    protected void obscuredChanged() {
    }

    public boolean isSelectAllOnFocus() {
        return mSelectAllOnFocus;
    }

    public void setSelectAllOnFocus(boolean value) {
        if (mSelectAllOnFocus == value) {
            return;
        }
        mSelectAllOnFocus = value;
        selectAllOnFocusChanged();
    }

    protected void selectAllOnFocusChanged() {
    }

    public int getMaxCharacters() {
        return mMaxCharacters;
    }

    public void setMaxCharacters(int value) {
        if (mMaxCharacters == value) {
            return;
        }
        mMaxCharacters = value;
        maxCharactersChanged();
    }

    protected void maxCharactersChanged() {
    }

    public int getMaxLines() {
        return mMaxLines;
    }

    public void setMaxLines(int value) {
        if (mMaxLines == value) {
            return;
        }
        mMaxLines = value;
        maxLinesChanged();
    }

    protected void maxLinesChanged() {
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
            case MULTILINE_PROPERTY_KEY:
                mMultiline = value;
                return true;
            case OBSCURED_PROPERTY_KEY:
                mObscured = value;
                return true;
            case SELECT_ALL_ON_FOCUS_PROPERTY_KEY:
                mSelectAllOnFocus = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case MULTILINE_PROPERTY_KEY:
                setMultiline(value);
                return true;
            case OBSCURED_PROPERTY_KEY:
                setObscured(value);
                return true;
            case SELECT_ALL_ON_FOCUS_PROPERTY_KEY:
                setSelectAllOnFocus(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case MULTILINE_PROPERTY_KEY:
                return mMultiline;
            case OBSCURED_PROPERTY_KEY:
                return mObscured;
            case SELECT_ALL_ON_FOCUS_PROPERTY_KEY:
                return mSelectAllOnFocus;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case SELECTION_RADIUS_PROPERTY_KEY:
                mSelectionRadius = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case SELECTION_RADIUS_PROPERTY_KEY:
                setSelectionRadius(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case SELECTION_RADIUS_PROPERTY_KEY:
                return mSelectionRadius;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeString(int propertyKey, String value) {
        switch (propertyKey) {
            case TEXT_PROPERTY_KEY:
                mText = value;
                return true;
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
            case TEXT_PROPERTY_KEY:
                setText(value);
                return true;
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
            case TEXT_PROPERTY_KEY:
                return mText;
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
            case VERTICAL_ALIGN_VALUE_PROPERTY_KEY:
                mVerticalAlignValue = value;
                return true;
            case MAX_CHARACTERS_PROPERTY_KEY:
                mMaxCharacters = value;
                return true;
            case MAX_LINES_PROPERTY_KEY:
                mMaxLines = value;
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
            case VERTICAL_ALIGN_VALUE_PROPERTY_KEY:
                setVerticalAlignValue(value);
                return true;
            case MAX_CHARACTERS_PROPERTY_KEY:
                setMaxCharacters(value);
                return true;
            case MAX_LINES_PROPERTY_KEY:
                setMaxLines(value);
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
            case VERTICAL_ALIGN_VALUE_PROPERTY_KEY:
                return mVerticalAlignValue;
            case MAX_CHARACTERS_PROPERTY_KEY:
                return mMaxCharacters;
            case MAX_LINES_PROPERTY_KEY:
                return mMaxLines;
            case LANGUAGE_ID_PROPERTY_KEY:
                return mLanguageId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
