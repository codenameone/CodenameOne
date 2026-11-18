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

/// Generated from rive-runtime's `ListenerViewModelChangeBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ListenerViewModelChange`.
public abstract class ListenerViewModelChangeBase extends ListenerAction {
    public static final int TYPE_KEY = 487;
    public static final int INPUT_VALUE_PROPERTY_KEY = 453;
    public static final int INPUT_VALUE_INDEX_PROPERTY_KEY = 454;

    protected int mInputValue = 0;
    protected int mInputValueIndex = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getInputValue() {
        return mInputValue;
    }

    public void setInputValue(int value) {
        if (mInputValue == value) {
            return;
        }
        mInputValue = value;
        inputValueChanged();
    }

    protected void inputValueChanged() {
    }

    public int getInputValueIndex() {
        return mInputValueIndex;
    }

    public void setInputValueIndex(int value) {
        if (mInputValueIndex == value) {
            return;
        }
        mInputValueIndex = value;
        inputValueIndexChanged();
    }

    protected void inputValueIndexChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case INPUT_VALUE_PROPERTY_KEY:
                mInputValue = value;
                return true;
            case INPUT_VALUE_INDEX_PROPERTY_KEY:
                mInputValueIndex = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case INPUT_VALUE_PROPERTY_KEY:
                setInputValue(value);
                return true;
            case INPUT_VALUE_INDEX_PROPERTY_KEY:
                setInputValueIndex(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case INPUT_VALUE_PROPERTY_KEY:
                return mInputValue;
            case INPUT_VALUE_INDEX_PROPERTY_KEY:
                return mInputValueIndex;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
