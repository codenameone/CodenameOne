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

/// Generated from rive-runtime's `DashBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Dash`.
public abstract class DashBase extends Component {
    public static final int TYPE_KEY = 507;
    public static final int LENGTH_PROPERTY_KEY = 692;
    public static final int LENGTH_IS_PERCENTAGE_PROPERTY_KEY = 693;

    protected float mLength = 0.0f;
    protected boolean mLengthIsPercentage = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getLength() {
        return mLength;
    }

    public void setLength(float value) {
        if (Float.floatToIntBits(mLength) == Float.floatToIntBits(value)) {
            return;
        }
        mLength = value;
        lengthChanged();
    }

    protected void lengthChanged() {
    }

    public boolean isLengthIsPercentage() {
        return mLengthIsPercentage;
    }

    public void setLengthIsPercentage(boolean value) {
        if (mLengthIsPercentage == value) {
            return;
        }
        mLengthIsPercentage = value;
        lengthIsPercentageChanged();
    }

    protected void lengthIsPercentageChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case LENGTH_IS_PERCENTAGE_PROPERTY_KEY:
                mLengthIsPercentage = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case LENGTH_IS_PERCENTAGE_PROPERTY_KEY:
                setLengthIsPercentage(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case LENGTH_IS_PERCENTAGE_PROPERTY_KEY:
                return mLengthIsPercentage;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case LENGTH_PROPERTY_KEY:
                mLength = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case LENGTH_PROPERTY_KEY:
                setLength(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case LENGTH_PROPERTY_KEY:
                return mLength;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
