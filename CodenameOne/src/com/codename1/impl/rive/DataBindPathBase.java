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

/// Generated from rive-runtime's `DataBindPathBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataBindPath`.
public abstract class DataBindPathBase extends Core {
    public static final int TYPE_KEY = 643;
    public static final int PATH_PROPERTY_KEY = 920;
    public static final int IS_RELATIVE_PROPERTY_KEY = 921;

    protected byte[] mPath = null;
    protected boolean mIsRelative = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public byte[] getPath() {
        return mPath;
    }

    protected void decodePath(byte[] value) {
        mPath = value;
    }

    public boolean isRelative() {
        return mIsRelative;
    }

    public void setIsRelative(boolean value) {
        if (mIsRelative == value) {
            return;
        }
        mIsRelative = value;
        isRelativeChanged();
    }

    protected void isRelativeChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_RELATIVE_PROPERTY_KEY:
                mIsRelative = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_RELATIVE_PROPERTY_KEY:
                setIsRelative(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case IS_RELATIVE_PROPERTY_KEY:
                return mIsRelative;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeBytes(int propertyKey, byte[] value) {
        switch (propertyKey) {
            case PATH_PROPERTY_KEY:
                decodePath(value);
                return true;
            default:
                return super.deserializeBytes(propertyKey, value);
        }
    }
}
