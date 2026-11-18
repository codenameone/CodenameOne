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

/// Generated from rive-runtime's `DataConverterStringTrimBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataConverterStringTrim`.
public abstract class DataConverterStringTrimBase extends DataConverter {
    public static final int TYPE_KEY = 532;
    public static final int TRIM_TYPE_PROPERTY_KEY = 746;

    protected int mTrimType = 1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    /// Copies this type's properties from `source`, which is of this type.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataConverterStringTrimBase o = (DataConverterStringTrimBase) source;
        mTrimType = o.mTrimType;
    }

    @Override
    public Core makeCopy() {
        DataConverterStringTrim c = new DataConverterStringTrim();
        c.copyFrom(this);
        return c;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getTrimType() {
        return mTrimType;
    }

    public void setTrimType(int value) {
        if (mTrimType == value) {
            return;
        }
        mTrimType = value;
        trimTypeChanged();
    }

    protected void trimTypeChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case TRIM_TYPE_PROPERTY_KEY:
                mTrimType = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case TRIM_TYPE_PROPERTY_KEY:
                setTrimType(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case TRIM_TYPE_PROPERTY_KEY:
                return mTrimType;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
