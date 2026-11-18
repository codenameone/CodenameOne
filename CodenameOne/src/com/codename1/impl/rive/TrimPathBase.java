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

/// Generated from rive-runtime's `TrimPathBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TrimPath`.
public abstract class TrimPathBase extends Component {
    public static final int TYPE_KEY = 47;
    public static final int START_PROPERTY_KEY = 114;
    public static final int END_PROPERTY_KEY = 115;
    public static final int OFFSET_PROPERTY_KEY = 116;
    public static final int MODE_VALUE_PROPERTY_KEY = 117;

    protected float mStart = 0.0f;
    protected float mEnd = 0.0f;
    protected float mOffset = 0.0f;
    protected int mModeValue = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getStart() {
        return mStart;
    }

    public void setStart(float value) {
        if (Float.floatToIntBits(mStart) == Float.floatToIntBits(value)) {
            return;
        }
        mStart = value;
        startChanged();
    }

    protected void startChanged() {
    }

    public float getEnd() {
        return mEnd;
    }

    public void setEnd(float value) {
        if (Float.floatToIntBits(mEnd) == Float.floatToIntBits(value)) {
            return;
        }
        mEnd = value;
        endChanged();
    }

    protected void endChanged() {
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

    public int getModeValue() {
        return mModeValue;
    }

    public void setModeValue(int value) {
        if (mModeValue == value) {
            return;
        }
        mModeValue = value;
        modeValueChanged();
    }

    protected void modeValueChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case START_PROPERTY_KEY:
                mStart = value;
                return true;
            case END_PROPERTY_KEY:
                mEnd = value;
                return true;
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
            case START_PROPERTY_KEY:
                setStart(value);
                return true;
            case END_PROPERTY_KEY:
                setEnd(value);
                return true;
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
            case START_PROPERTY_KEY:
                return mStart;
            case END_PROPERTY_KEY:
                return mEnd;
            case OFFSET_PROPERTY_KEY:
                return mOffset;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case MODE_VALUE_PROPERTY_KEY:
                mModeValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case MODE_VALUE_PROPERTY_KEY:
                setModeValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case MODE_VALUE_PROPERTY_KEY:
                return mModeValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
