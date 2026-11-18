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

/// Generated from rive-runtime's `TextStyleFeatureBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TextStyleFeature`.
public abstract class TextStyleFeatureBase extends Component {
    public static final int TYPE_KEY = 164;
    public static final int TAG_PROPERTY_KEY = 356;
    public static final int FEATURE_VALUE_PROPERTY_KEY = 357;

    protected int mTag = 0;
    protected int mFeatureValue = 1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getTag() {
        return mTag;
    }

    public void setTag(int value) {
        if (mTag == value) {
            return;
        }
        mTag = value;
        tagChanged();
    }

    protected void tagChanged() {
    }

    public int getFeatureValue() {
        return mFeatureValue;
    }

    public void setFeatureValue(int value) {
        if (mFeatureValue == value) {
            return;
        }
        mFeatureValue = value;
        featureValueChanged();
    }

    protected void featureValueChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case TAG_PROPERTY_KEY:
                mTag = value;
                return true;
            case FEATURE_VALUE_PROPERTY_KEY:
                mFeatureValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case TAG_PROPERTY_KEY:
                setTag(value);
                return true;
            case FEATURE_VALUE_PROPERTY_KEY:
                setFeatureValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case TAG_PROPERTY_KEY:
                return mTag;
            case FEATURE_VALUE_PROPERTY_KEY:
                return mFeatureValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
