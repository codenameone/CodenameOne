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

/// Generated from rive-runtime's `NSlicerTileModeBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `NSlicerTileMode`.
public abstract class NSlicerTileModeBase extends Component {
    public static final int TYPE_KEY = 491;
    public static final int PATCH_INDEX_PROPERTY_KEY = 672;
    public static final int STYLE_PROPERTY_KEY = 673;

    protected int mPatchIndex = 0;
    protected int mStyle = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getPatchIndex() {
        return mPatchIndex;
    }

    public void setPatchIndex(int value) {
        if (mPatchIndex == value) {
            return;
        }
        mPatchIndex = value;
        patchIndexChanged();
    }

    protected void patchIndexChanged() {
    }

    public int getStyle() {
        return mStyle;
    }

    public void setStyle(int value) {
        if (mStyle == value) {
            return;
        }
        mStyle = value;
        styleChanged();
    }

    protected void styleChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case PATCH_INDEX_PROPERTY_KEY:
                mPatchIndex = value;
                return true;
            case STYLE_PROPERTY_KEY:
                mStyle = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case PATCH_INDEX_PROPERTY_KEY:
                setPatchIndex(value);
                return true;
            case STYLE_PROPERTY_KEY:
                setStyle(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case PATCH_INDEX_PROPERTY_KEY:
                return mPatchIndex;
            case STYLE_PROPERTY_KEY:
                return mStyle;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
