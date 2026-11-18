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

/// Generated from rive-runtime's `ViewModelInstanceNumberBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ViewModelInstanceNumber`.
public abstract class ViewModelInstanceNumberBase extends ViewModelInstanceValue {
    public static final int TYPE_KEY = 442;
    public static final int PROPERTY_VALUE_PROPERTY_KEY = 575;

    protected float mPropertyValue = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getPropertyValue() {
        return mPropertyValue;
    }

    public void setPropertyValue(float value) {
        if (Float.floatToIntBits(mPropertyValue) == Float.floatToIntBits(value)) {
            return;
        }
        mPropertyValue = value;
        propertyValueChanged();
    }

    protected void propertyValueChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case PROPERTY_VALUE_PROPERTY_KEY:
                mPropertyValue = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case PROPERTY_VALUE_PROPERTY_KEY:
                setPropertyValue(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case PROPERTY_VALUE_PROPERTY_KEY:
                return mPropertyValue;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
