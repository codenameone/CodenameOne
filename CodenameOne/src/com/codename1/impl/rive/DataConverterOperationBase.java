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

/// Generated from rive-runtime's `DataConverterOperationBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataConverterOperation`.
public abstract class DataConverterOperationBase extends DataConverter {
    public static final int TYPE_KEY = 516;
    public static final int OPERATION_TYPE_PROPERTY_KEY = 682;

    protected int mOperationType = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    /// Copies this type's properties from `source`, which is of this type.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        DataConverterOperationBase o = (DataConverterOperationBase) source;
        mOperationType = o.mOperationType;
    }

    @Override
    public Core makeCopy() {
        DataConverterOperation c = new DataConverterOperation();
        c.copyFrom(this);
        return c;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getOperationType() {
        return mOperationType;
    }

    public void setOperationType(int value) {
        if (mOperationType == value) {
            return;
        }
        mOperationType = value;
        operationTypeChanged();
    }

    protected void operationTypeChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case OPERATION_TYPE_PROPERTY_KEY:
                mOperationType = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case OPERATION_TYPE_PROPERTY_KEY:
                setOperationType(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case OPERATION_TYPE_PROPERTY_KEY:
                return mOperationType;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
