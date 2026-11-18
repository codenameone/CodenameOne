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

/// Generated from rive-runtime's `TextValueRunBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TextValueRun`.
public abstract class TextValueRunBase extends Component {
    public static final int TYPE_KEY = 135;
    public static final int STYLE_ID_PROPERTY_KEY = 272;
    public static final int TEXT_PROPERTY_KEY = 268;
    public static final int LANGUAGE_PROPERTY_KEY = 1128;
    public static final int LANGUAGE_ID_PROPERTY_KEY = 1129;

    protected int mStyleId = -1;
    protected String mText = "";
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
            case STYLE_ID_PROPERTY_KEY:
                mStyleId = value;
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
            case STYLE_ID_PROPERTY_KEY:
                setStyleId(value);
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
            case STYLE_ID_PROPERTY_KEY:
                return mStyleId;
            case LANGUAGE_ID_PROPERTY_KEY:
                return mLanguageId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
