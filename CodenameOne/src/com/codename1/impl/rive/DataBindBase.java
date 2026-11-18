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

/// Generated from rive-runtime's `DataBindBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataBind`.
public abstract class DataBindBase extends Core {
    public static final int TYPE_KEY = 446;
    public static final int PROPERTY_KEY_PROPERTY_KEY = 586;
    public static final int FLAGS_PROPERTY_KEY = 587;
    public static final int CONVERTER_ID_PROPERTY_KEY = 660;

    protected int mPropertyKey = 0;
    protected int mFlags = 0;
    protected int mConverterId = -1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    /// Copies this type's properties from `source`, which is of this type.
    protected void copyFrom(Core source) {
        DataBindBase o = (DataBindBase) source;
        mPropertyKey = o.mPropertyKey;
        mFlags = o.mFlags;
        mConverterId = o.mConverterId;
    }

    /// A copy of this object.
    public Core makeCopy() {
        DataBind c = new DataBind();
        c.copyFrom(this);
        return c;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getPropertyKey() {
        return mPropertyKey;
    }

    public void setPropertyKey(int value) {
        if (mPropertyKey == value) {
            return;
        }
        mPropertyKey = value;
        propertyKeyChanged();
    }

    protected void propertyKeyChanged() {
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int value) {
        if (mFlags == value) {
            return;
        }
        mFlags = value;
        flagsChanged();
    }

    protected void flagsChanged() {
    }

    public int getConverterId() {
        return mConverterId;
    }

    public void setConverterId(int value) {
        if (mConverterId == value) {
            return;
        }
        mConverterId = value;
        converterIdChanged();
    }

    protected void converterIdChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case PROPERTY_KEY_PROPERTY_KEY:
                mPropertyKey = value;
                return true;
            case FLAGS_PROPERTY_KEY:
                mFlags = value;
                return true;
            case CONVERTER_ID_PROPERTY_KEY:
                mConverterId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case PROPERTY_KEY_PROPERTY_KEY:
                setPropertyKey(value);
                return true;
            case FLAGS_PROPERTY_KEY:
                setFlags(value);
                return true;
            case CONVERTER_ID_PROPERTY_KEY:
                setConverterId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case PROPERTY_KEY_PROPERTY_KEY:
                return mPropertyKey;
            case FLAGS_PROPERTY_KEY:
                return mFlags;
            case CONVERTER_ID_PROPERTY_KEY:
                return mConverterId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
