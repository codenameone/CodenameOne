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

/// Generated from rive-runtime's `RootBoneBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `RootBone`.
public abstract class RootBoneBase extends Bone {
    public static final int TYPE_KEY = 41;
    public static final int X_PROPERTY_KEY = 90;
    public static final int Y_PROPERTY_KEY = 91;

    protected float mX = 0.0f;
    protected float mY = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    @Override
    public float getX() {
        return mX;
    }

    public void setX(float value) {
        if (Float.floatToIntBits(mX) == Float.floatToIntBits(value)) {
            return;
        }
        mX = value;
        xChanged();
    }

    protected void xChanged() {
    }

    @Override
    public float getY() {
        return mY;
    }

    public void setY(float value) {
        if (Float.floatToIntBits(mY) == Float.floatToIntBits(value)) {
            return;
        }
        mY = value;
        yChanged();
    }

    protected void yChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case X_PROPERTY_KEY:
                mX = value;
                return true;
            case Y_PROPERTY_KEY:
                mY = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case X_PROPERTY_KEY:
                setX(value);
                return true;
            case Y_PROPERTY_KEY:
                setY(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case X_PROPERTY_KEY:
                return mX;
            case Y_PROPERTY_KEY:
                return mY;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
