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

/// Generated from rive-runtime's `DataConverterStringTemplateBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataConverterStringTemplate`.
public abstract class DataConverterStringTemplateBase extends DataConverter {
    public static final int TYPE_KEY = 177;
    public static final int OPEN_DELIMITER_PROPERTY_KEY = 485;
    public static final int CLOSE_DELIMITER_PROPERTY_KEY = 486;

    protected String mOpenDelimiter = "${";
    protected String mCloseDelimiter = "}";

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    /// Copies this type's properties from `source`, which is of this type.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataConverterStringTemplateBase o = (DataConverterStringTemplateBase) source;
        mOpenDelimiter = o.mOpenDelimiter;
        mCloseDelimiter = o.mCloseDelimiter;
    }

    @Override
    public Core makeCopy() {
        DataConverterStringTemplate c = new DataConverterStringTemplate();
        c.copyFrom(this);
        return c;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public String getOpenDelimiter() {
        return mOpenDelimiter;
    }

    public void setOpenDelimiter(String value) {
        if ((mOpenDelimiter == null ? value == null : mOpenDelimiter.equals(value))) {
            return;
        }
        mOpenDelimiter = value;
        openDelimiterChanged();
    }

    protected void openDelimiterChanged() {
    }

    public String getCloseDelimiter() {
        return mCloseDelimiter;
    }

    public void setCloseDelimiter(String value) {
        if ((mCloseDelimiter == null ? value == null : mCloseDelimiter.equals(value))) {
            return;
        }
        mCloseDelimiter = value;
        closeDelimiterChanged();
    }

    protected void closeDelimiterChanged() {
    }

    @Override
    public boolean deserializeString(int propertyKey, String value) {
        switch (propertyKey) {
            case OPEN_DELIMITER_PROPERTY_KEY:
                mOpenDelimiter = value;
                return true;
            case CLOSE_DELIMITER_PROPERTY_KEY:
                mCloseDelimiter = value;
                return true;
            default:
                return super.deserializeString(propertyKey, value);
        }
    }

    @Override
    public boolean setStringProperty(int propertyKey, String value) {
        switch (propertyKey) {
            case OPEN_DELIMITER_PROPERTY_KEY:
                setOpenDelimiter(value);
                return true;
            case CLOSE_DELIMITER_PROPERTY_KEY:
                setCloseDelimiter(value);
                return true;
            default:
                return super.setStringProperty(propertyKey, value);
        }
    }

    @Override
    public String getStringProperty(int propertyKey) {
        switch (propertyKey) {
            case OPEN_DELIMITER_PROPERTY_KEY:
                return mOpenDelimiter;
            case CLOSE_DELIMITER_PROPERTY_KEY:
                return mCloseDelimiter;
            default:
                return super.getStringProperty(propertyKey);
        }
    }
}
