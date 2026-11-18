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

/// Generated from rive-runtime's `StrokeBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Stroke`.
public abstract class StrokeBase extends ShapePaint {
    public static final int TYPE_KEY = 24;
    public static final int THICKNESS_PROPERTY_KEY = 47;
    public static final int CAP_PROPERTY_KEY = 48;
    public static final int JOIN_PROPERTY_KEY = 49;
    public static final int TRANSFORM_AFFECTS_STROKE_PROPERTY_KEY = 50;
    public static final int POSITION_PROPERTY_KEY = 470;

    protected float mThickness = 1.0f;
    protected int mCap = 0;
    protected int mJoin = 0;
    protected boolean mTransformAffectsStroke = true;
    protected int mPosition = 1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getThickness() {
        return mThickness;
    }

    public void setThickness(float value) {
        if (Float.floatToIntBits(mThickness) == Float.floatToIntBits(value)) {
            return;
        }
        mThickness = value;
        thicknessChanged();
    }

    protected void thicknessChanged() {
    }

    public int getCap() {
        return mCap;
    }

    public void setCap(int value) {
        if (mCap == value) {
            return;
        }
        mCap = value;
        capChanged();
    }

    protected void capChanged() {
    }

    public int getJoin() {
        return mJoin;
    }

    public void setJoin(int value) {
        if (mJoin == value) {
            return;
        }
        mJoin = value;
        joinChanged();
    }

    protected void joinChanged() {
    }

    public boolean isTransformAffectsStroke() {
        return mTransformAffectsStroke;
    }

    public void setTransformAffectsStroke(boolean value) {
        if (mTransformAffectsStroke == value) {
            return;
        }
        mTransformAffectsStroke = value;
        transformAffectsStrokeChanged();
    }

    protected void transformAffectsStrokeChanged() {
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int value) {
        if (mPosition == value) {
            return;
        }
        mPosition = value;
        positionChanged();
    }

    protected void positionChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case TRANSFORM_AFFECTS_STROKE_PROPERTY_KEY:
                mTransformAffectsStroke = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case TRANSFORM_AFFECTS_STROKE_PROPERTY_KEY:
                setTransformAffectsStroke(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case TRANSFORM_AFFECTS_STROKE_PROPERTY_KEY:
                return mTransformAffectsStroke;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case THICKNESS_PROPERTY_KEY:
                mThickness = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case THICKNESS_PROPERTY_KEY:
                setThickness(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case THICKNESS_PROPERTY_KEY:
                return mThickness;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case CAP_PROPERTY_KEY:
                mCap = value;
                return true;
            case JOIN_PROPERTY_KEY:
                mJoin = value;
                return true;
            case POSITION_PROPERTY_KEY:
                mPosition = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case CAP_PROPERTY_KEY:
                setCap(value);
                return true;
            case JOIN_PROPERTY_KEY:
                setJoin(value);
                return true;
            case POSITION_PROPERTY_KEY:
                setPosition(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case CAP_PROPERTY_KEY:
                return mCap;
            case JOIN_PROPERTY_KEY:
                return mJoin;
            case POSITION_PROPERTY_KEY:
                return mPosition;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
