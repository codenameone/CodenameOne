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

/// Generated from rive-runtime's `DataConverterRounderBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataConverterRounder`.
public abstract class DataConverterRounderBase extends DataConverter {
    public static final int TYPE_KEY = 489;
    public static final int DECIMALS_PROPERTY_KEY = 669;

    protected int mDecimals = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    /// Copies this type's properties from `source`, which is of this type.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataConverterRounderBase o = (DataConverterRounderBase) source;
        mDecimals = o.mDecimals;
    }

    @Override
    public Core makeCopy() {
        DataConverterRounder c = new DataConverterRounder();
        c.copyFrom(this);
        return c;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getDecimals() {
        return mDecimals;
    }

    public void setDecimals(int value) {
        if (mDecimals == value) {
            return;
        }
        mDecimals = value;
        decimalsChanged();
    }

    protected void decimalsChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case DECIMALS_PROPERTY_KEY:
                mDecimals = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case DECIMALS_PROPERTY_KEY:
                setDecimals(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case DECIMALS_PROPERTY_KEY:
                return mDecimals;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
