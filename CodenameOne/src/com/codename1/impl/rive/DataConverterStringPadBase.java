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

/// Generated from rive-runtime's `DataConverterStringPadBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataConverterStringPad`.
public abstract class DataConverterStringPadBase extends DataConverter {
    public static final int TYPE_KEY = 530;
    public static final int LENGTH_PROPERTY_KEY = 743;
    public static final int TEXT_PROPERTY_KEY = 744;
    public static final int PAD_TYPE_PROPERTY_KEY = 745;

    protected int mLength = 1;
    protected String mText = "";
    protected int mPadType = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    /// Copies this type's properties from `source`, which is of this type.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataConverterStringPadBase o = (DataConverterStringPadBase) source;
        mLength = o.mLength;
        mText = o.mText;
        mPadType = o.mPadType;
    }

    @Override
    public Core makeCopy() {
        DataConverterStringPad c = new DataConverterStringPad();
        c.copyFrom(this);
        return c;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getLength() {
        return mLength;
    }

    public void setLength(int value) {
        if (mLength == value) {
            return;
        }
        mLength = value;
        lengthChanged();
    }

    protected void lengthChanged() {
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

    public int getPadType() {
        return mPadType;
    }

    public void setPadType(int value) {
        if (mPadType == value) {
            return;
        }
        mPadType = value;
        padTypeChanged();
    }

    protected void padTypeChanged() {
    }

    @Override
    public boolean deserializeString(int propertyKey, String value) {
        switch (propertyKey) {
            case TEXT_PROPERTY_KEY:
                mText = value;
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
            default:
                return super.setStringProperty(propertyKey, value);
        }
    }

    @Override
    public String getStringProperty(int propertyKey) {
        switch (propertyKey) {
            case TEXT_PROPERTY_KEY:
                return mText;
            default:
                return super.getStringProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case LENGTH_PROPERTY_KEY:
                mLength = value;
                return true;
            case PAD_TYPE_PROPERTY_KEY:
                mPadType = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case LENGTH_PROPERTY_KEY:
                setLength(value);
                return true;
            case PAD_TYPE_PROPERTY_KEY:
                setPadType(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case LENGTH_PROPERTY_KEY:
                return mLength;
            case PAD_TYPE_PROPERTY_KEY:
                return mPadType;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
