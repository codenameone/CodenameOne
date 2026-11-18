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

/// Generated from rive-runtime's `DataEnumValueBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DataEnumValue`.
public abstract class DataEnumValueBase extends Core {
    public static final int TYPE_KEY = 445;
    public static final int KEY_PROPERTY_KEY = 578;
    public static final int VALUE_PROPERTY_KEY = 579;

    protected String mKey = "";
    protected String mValue = "";

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public String getKey() {
        return mKey;
    }

    public void setKey(String value) {
        if ((mKey == null ? value == null : mKey.equals(value))) {
            return;
        }
        mKey = value;
        keyChanged();
    }

    protected void keyChanged() {
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        if ((mValue == null ? value == null : mValue.equals(value))) {
            return;
        }
        mValue = value;
        valueChanged();
    }

    protected void valueChanged() {
    }

    @Override
    public boolean deserializeString(int propertyKey, String value) {
        switch (propertyKey) {
            case KEY_PROPERTY_KEY:
                mKey = value;
                return true;
            case VALUE_PROPERTY_KEY:
                mValue = value;
                return true;
            default:
                return super.deserializeString(propertyKey, value);
        }
    }

    @Override
    public boolean setStringProperty(int propertyKey, String value) {
        switch (propertyKey) {
            case KEY_PROPERTY_KEY:
                setKey(value);
                return true;
            case VALUE_PROPERTY_KEY:
                setValue(value);
                return true;
            default:
                return super.setStringProperty(propertyKey, value);
        }
    }

    @Override
    public String getStringProperty(int propertyKey) {
        switch (propertyKey) {
            case KEY_PROPERTY_KEY:
                return mKey;
            case VALUE_PROPERTY_KEY:
                return mValue;
            default:
                return super.getStringProperty(propertyKey);
        }
    }
}
