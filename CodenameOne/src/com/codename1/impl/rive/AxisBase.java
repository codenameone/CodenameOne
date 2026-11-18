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

/// Generated from rive-runtime's `AxisBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Axis`.
public abstract class AxisBase extends Component {
    public static final int TYPE_KEY = 492;
    public static final int OFFSET_PROPERTY_KEY = 675;
    public static final int NORMALIZED_PROPERTY_KEY = 676;

    protected float mOffset = 0.0f;
    protected boolean mNormalized = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getOffset() {
        return mOffset;
    }

    public void setOffset(float value) {
        if (Float.floatToIntBits(mOffset) == Float.floatToIntBits(value)) {
            return;
        }
        mOffset = value;
        offsetChanged();
    }

    protected void offsetChanged() {
    }

    public boolean isNormalized() {
        return mNormalized;
    }

    public void setNormalized(boolean value) {
        if (mNormalized == value) {
            return;
        }
        mNormalized = value;
        normalizedChanged();
    }

    protected void normalizedChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case NORMALIZED_PROPERTY_KEY:
                mNormalized = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case NORMALIZED_PROPERTY_KEY:
                setNormalized(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case NORMALIZED_PROPERTY_KEY:
                return mNormalized;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case OFFSET_PROPERTY_KEY:
                mOffset = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case OFFSET_PROPERTY_KEY:
                setOffset(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case OFFSET_PROPERTY_KEY:
                return mOffset;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
