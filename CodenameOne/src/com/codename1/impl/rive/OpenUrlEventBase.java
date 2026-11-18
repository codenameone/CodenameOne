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

/// Generated from rive-runtime's `OpenUrlEventBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `OpenUrlEvent`.
public abstract class OpenUrlEventBase extends Event {
    public static final int TYPE_KEY = 131;
    public static final int URL_PROPERTY_KEY = 248;
    public static final int TARGET_VALUE_PROPERTY_KEY = 249;

    protected String mUrl = "";
    protected int mTargetValue = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String value) {
        if ((mUrl == null ? value == null : mUrl.equals(value))) {
            return;
        }
        mUrl = value;
        urlChanged();
    }

    protected void urlChanged() {
    }

    public int getTargetValue() {
        return mTargetValue;
    }

    public void setTargetValue(int value) {
        if (mTargetValue == value) {
            return;
        }
        mTargetValue = value;
        targetValueChanged();
    }

    protected void targetValueChanged() {
    }

    @Override
    public boolean deserializeString(int propertyKey, String value) {
        switch (propertyKey) {
            case URL_PROPERTY_KEY:
                mUrl = value;
                return true;
            default:
                return super.deserializeString(propertyKey, value);
        }
    }

    @Override
    public boolean setStringProperty(int propertyKey, String value) {
        switch (propertyKey) {
            case URL_PROPERTY_KEY:
                setUrl(value);
                return true;
            default:
                return super.setStringProperty(propertyKey, value);
        }
    }

    @Override
    public String getStringProperty(int propertyKey) {
        switch (propertyKey) {
            case URL_PROPERTY_KEY:
                return mUrl;
            default:
                return super.getStringProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case TARGET_VALUE_PROPERTY_KEY:
                mTargetValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case TARGET_VALUE_PROPERTY_KEY:
                setTargetValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case TARGET_VALUE_PROPERTY_KEY:
                return mTargetValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
