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

/// Generated from rive-runtime's `DataConverterOperationValueBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataConverterOperationValue`.
public abstract class DataConverterOperationValueBase extends DataConverterOperation {
    public static final int TYPE_KEY = 500;
    public static final int OPERATION_VALUE_PROPERTY_KEY = 681;

    protected float mOperationValue = 1.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    /// Copies this type's properties from `source`, which is of this type.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataConverterOperationValueBase o = (DataConverterOperationValueBase) source;
        mOperationValue = o.mOperationValue;
    }

    @Override
    public Core makeCopy() {
        DataConverterOperationValue c = new DataConverterOperationValue();
        c.copyFrom(this);
        return c;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getOperationValue() {
        return mOperationValue;
    }

    public void setOperationValue(float value) {
        if (Float.floatToIntBits(mOperationValue) == Float.floatToIntBits(value)) {
            return;
        }
        mOperationValue = value;
        operationValueChanged();
    }

    protected void operationValueChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case OPERATION_VALUE_PROPERTY_KEY:
                mOperationValue = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case OPERATION_VALUE_PROPERTY_KEY:
                setOperationValue(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case OPERATION_VALUE_PROPERTY_KEY:
                return mOperationValue;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
