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

/// Generated from rive-runtime's `DataBindContextBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataBindContext`.
public abstract class DataBindContextBase extends DataBind {
    public static final int TYPE_KEY = 447;
    public static final int SOURCE_PATH_IDS_PROPERTY_KEY = 588;

    protected byte[] mSourcePathIds = null;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    /// Copies this type's properties from `source`, which is of this type.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataBindContextBase o = (DataBindContextBase) source;
        mSourcePathIds = o.mSourcePathIds;
    }

    @Override
    public Core makeCopy() {
        DataBindContext c = new DataBindContext();
        c.copyFrom(this);
        return c;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public byte[] getSourcePathIds() {
        return mSourcePathIds;
    }

    protected void decodeSourcePathIds(byte[] value) {
        mSourcePathIds = value;
    }

    @Override
    public boolean deserializeBytes(int propertyKey, byte[] value) {
        switch (propertyKey) {
            case SOURCE_PATH_IDS_PROPERTY_KEY:
                decodeSourcePathIds(value);
                return true;
            default:
                return super.deserializeBytes(propertyKey, value);
        }
    }
}
