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

/// Generated from rive-runtime's `ComponentOriginBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ComponentOrigin`.
public abstract class ComponentOriginBase extends Component {
    public static final int TYPE_KEY = 1039;
    public static final int ORIGIN_X_PROPERTY_KEY = 1040;
    public static final int ORIGIN_Y_PROPERTY_KEY = 1041;

    protected float mOriginX = 0.0f;
    protected float mOriginY = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getOriginX() {
        return mOriginX;
    }

    public void setOriginX(float value) {
        if (Float.floatToIntBits(mOriginX) == Float.floatToIntBits(value)) {
            return;
        }
        mOriginX = value;
        originXChanged();
    }

    protected void originXChanged() {
    }

    public float getOriginY() {
        return mOriginY;
    }

    public void setOriginY(float value) {
        if (Float.floatToIntBits(mOriginY) == Float.floatToIntBits(value)) {
            return;
        }
        mOriginY = value;
        originYChanged();
    }

    protected void originYChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case ORIGIN_X_PROPERTY_KEY:
                mOriginX = value;
                return true;
            case ORIGIN_Y_PROPERTY_KEY:
                mOriginY = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case ORIGIN_X_PROPERTY_KEY:
                setOriginX(value);
                return true;
            case ORIGIN_Y_PROPERTY_KEY:
                setOriginY(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case ORIGIN_X_PROPERTY_KEY:
                return mOriginX;
            case ORIGIN_Y_PROPERTY_KEY:
                return mOriginY;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
