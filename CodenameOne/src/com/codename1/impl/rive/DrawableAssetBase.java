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

/// Generated from rive-runtime's `DrawableAssetBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DrawableAsset`.
public abstract class DrawableAssetBase extends FileAsset {
    public static final int TYPE_KEY = 104;
    public static final int HEIGHT_PROPERTY_KEY = 207;
    public static final int WIDTH_PROPERTY_KEY = 208;

    protected float mHeight = 0.0f;
    protected float mWidth = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getHeight() {
        return mHeight;
    }

    public void setHeight(float value) {
        if (Float.floatToIntBits(mHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mHeight = value;
        heightChanged();
    }

    protected void heightChanged() {
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float value) {
        if (Float.floatToIntBits(mWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mWidth = value;
        widthChanged();
    }

    protected void widthChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case HEIGHT_PROPERTY_KEY:
                mHeight = value;
                return true;
            case WIDTH_PROPERTY_KEY:
                mWidth = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case HEIGHT_PROPERTY_KEY:
                setHeight(value);
                return true;
            case WIDTH_PROPERTY_KEY:
                setWidth(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case HEIGHT_PROPERTY_KEY:
                return mHeight;
            case WIDTH_PROPERTY_KEY:
                return mWidth;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
