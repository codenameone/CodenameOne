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

/// Generated from rive-runtime's `CubicInterpolatorBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `CubicInterpolator`.
public abstract class CubicInterpolatorBase extends KeyFrameInterpolator {
    public static final int TYPE_KEY = 139;
    public static final int X1_PROPERTY_KEY = 63;
    public static final int Y1_PROPERTY_KEY = 64;
    public static final int X2_PROPERTY_KEY = 65;
    public static final int Y2_PROPERTY_KEY = 66;

    protected float mX1 = 0.42f;
    protected float mY1 = 0.0f;
    protected float mX2 = 0.58f;
    protected float mY2 = 1.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getX1() {
        return mX1;
    }

    public void setX1(float value) {
        if (Float.floatToIntBits(mX1) == Float.floatToIntBits(value)) {
            return;
        }
        mX1 = value;
        x1Changed();
    }

    protected void x1Changed() {
    }

    public float getY1() {
        return mY1;
    }

    public void setY1(float value) {
        if (Float.floatToIntBits(mY1) == Float.floatToIntBits(value)) {
            return;
        }
        mY1 = value;
        y1Changed();
    }

    protected void y1Changed() {
    }

    public float getX2() {
        return mX2;
    }

    public void setX2(float value) {
        if (Float.floatToIntBits(mX2) == Float.floatToIntBits(value)) {
            return;
        }
        mX2 = value;
        x2Changed();
    }

    protected void x2Changed() {
    }

    public float getY2() {
        return mY2;
    }

    public void setY2(float value) {
        if (Float.floatToIntBits(mY2) == Float.floatToIntBits(value)) {
            return;
        }
        mY2 = value;
        y2Changed();
    }

    protected void y2Changed() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case X1_PROPERTY_KEY:
                mX1 = value;
                return true;
            case Y1_PROPERTY_KEY:
                mY1 = value;
                return true;
            case X2_PROPERTY_KEY:
                mX2 = value;
                return true;
            case Y2_PROPERTY_KEY:
                mY2 = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case X1_PROPERTY_KEY:
                setX1(value);
                return true;
            case Y1_PROPERTY_KEY:
                setY1(value);
                return true;
            case X2_PROPERTY_KEY:
                setX2(value);
                return true;
            case Y2_PROPERTY_KEY:
                setY2(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case X1_PROPERTY_KEY:
                return mX1;
            case Y1_PROPERTY_KEY:
                return mY1;
            case X2_PROPERTY_KEY:
                return mX2;
            case Y2_PROPERTY_KEY:
                return mY2;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
