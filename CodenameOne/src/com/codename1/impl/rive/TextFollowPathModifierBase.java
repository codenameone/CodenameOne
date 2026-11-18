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

/// Generated from rive-runtime's `TextFollowPathModifierBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TextFollowPathModifier`.
public abstract class TextFollowPathModifierBase extends TextTargetModifier {
    public static final int TYPE_KEY = 547;
    public static final int RADIAL_PROPERTY_KEY = 779;
    public static final int ORIENT_PROPERTY_KEY = 782;
    public static final int START_PROPERTY_KEY = 783;
    public static final int END_PROPERTY_KEY = 784;
    public static final int STRENGTH_PROPERTY_KEY = 785;
    public static final int OFFSET_PROPERTY_KEY = 786;

    protected boolean mRadial = false;
    protected boolean mOrient = true;
    protected float mStart = 0.0f;
    protected float mEnd = 1.0f;
    protected float mStrength = 1.0f;
    protected float mOffset = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public boolean isRadial() {
        return mRadial;
    }

    public void setRadial(boolean value) {
        if (mRadial == value) {
            return;
        }
        mRadial = value;
        radialChanged();
    }

    protected void radialChanged() {
    }

    public boolean isOrient() {
        return mOrient;
    }

    public void setOrient(boolean value) {
        if (mOrient == value) {
            return;
        }
        mOrient = value;
        orientChanged();
    }

    protected void orientChanged() {
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

    public float getStrength() {
        return mStrength;
    }

    public void setStrength(float value) {
        if (Float.floatToIntBits(mStrength) == Float.floatToIntBits(value)) {
            return;
        }
        mStrength = value;
        strengthChanged();
    }

    protected void strengthChanged() {
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

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case RADIAL_PROPERTY_KEY:
                mRadial = value;
                return true;
            case ORIENT_PROPERTY_KEY:
                mOrient = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case RADIAL_PROPERTY_KEY:
                setRadial(value);
                return true;
            case ORIENT_PROPERTY_KEY:
                setOrient(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case RADIAL_PROPERTY_KEY:
                return mRadial;
            case ORIENT_PROPERTY_KEY:
                return mOrient;
            default:
                return super.getBoolProperty(propertyKey);
        }
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
            case STRENGTH_PROPERTY_KEY:
                mStrength = value;
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
            case STRENGTH_PROPERTY_KEY:
                setStrength(value);
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
            case STRENGTH_PROPERTY_KEY:
                return mStrength;
            case OFFSET_PROPERTY_KEY:
                return mOffset;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
